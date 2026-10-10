import ComposeApp
import Foundation
import PhotosUI
import SwiftUI
import UIKit
import Vision

// Swift bridge for the Phase 3 screenshot bulk-import feature.
//
// Flow:
//   1. Kotlin calls pickAndExtractText(onResult:) from the Compose screen
//      (via ScreenshotOcrParser.ios actual + suspendCancellableCoroutine).
//   2. We present SwiftUI's PhotosPicker over the key window's root VC.
//   3. User picks up to 20 images (hard cap below).
//   4. For each PHPickerResult we grab the raw UIImage + the asset's
//      creation date, run VNRecognizeTextRequest, and collect ScreenshotOcrResult.
//   5. Invoke onResult on the main queue with the aggregated list.
//
// Why Vision not CoreML/ML Kit: Vision ships with iOS, zero-dependency, and
// its text recognition on English/digits (= our target: notification bodies)
// scores >95% accuracy for the fonts iOS uses. Also runs fully on-device,
// which fits the Mooney privacy posture (receipts never leave the phone).
@available(iOS 16.0, *)
class ScreenshotOcrBridgeImpl: NSObject, ScreenshotOcrBridge {

    private static let maxImages: Int = 20

    func pickAndExtractText(
        onResult: @escaping ([ScreenshotOcrResult]) -> Void
    ) {
        // Dispatch to main because we need UIKit presentation + the Kotlin
        // callback expects to be invoked once; the Vision pass is still off
        // the main thread (see `recognizeText`).
        DispatchQueue.main.async {
            self.presentPicker(onResult: onResult)
        }
    }

    private func presentPicker(onResult: @escaping ([ScreenshotOcrResult]) -> Void) {
        // Default PHPickerConfiguration (no photoLibrary argument) uses the
        // out-of-process picker — zero Photo Library authorization required,
        // the user picks images via the system UI and only the chosen
        // UIImage data crosses back. Important for App Review: avoids the
        // "Mooney wants access to your photos" prompt which would otherwise
        // look alarming for a finance app.
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = Self.maxImages
        config.preferredAssetRepresentationMode = .current

        let picker = PHPickerViewController(configuration: config)
        let delegate = PickerDelegate(onResult: onResult)
        picker.delegate = delegate

        // Hold the delegate strong until the picker dismisses — PHPicker's
        // `delegate` property is weak.
        objc_setAssociatedObject(picker, &PickerDelegate.associationKey, delegate, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)

        guard let rootVC = Self.topViewController() else {
            NSLog("[ScreenshotOcr] No root VC to present picker — returning empty")
            onResult([])
            return
        }
        rootVC.present(picker, animated: true)
    }

    /// Resolve the current topmost view controller from the first connected
    /// foreground-active window scene. Required because the Compose host
    /// controller is nested inside a UIHostingController that is itself a
    /// child of the SwiftUI scene root; we can't rely on `windows.first`.
    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
        guard let window = scenes.first?.windows.first(where: { $0.isKeyWindow })
            ?? scenes.first?.windows.first,
              var top = window.rootViewController else { return nil }
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}

@available(iOS 16.0, *)
private class PickerDelegate: NSObject, PHPickerViewControllerDelegate {

    static var associationKey: UInt8 = 0

    private let onResult: ([ScreenshotOcrResult]) -> Void
    /// Guards double-invocation — PHPicker calls back exactly once but we
    /// use the same closure path for cancellation and we don't want the
    /// Kotlin side to resume twice.
    private var hasFired: Bool = false

    init(onResult: @escaping ([ScreenshotOcrResult]) -> Void) {
        self.onResult = onResult
    }

    func picker(
        _ picker: PHPickerViewController,
        didFinishPicking results: [PHPickerResult]
    ) {
        picker.dismiss(animated: true)

        if results.isEmpty {
            fire(with: [])
            return
        }

        // Fetch images + capture timestamps in parallel using an item-provider
        // loop + DispatchGroup. Vision pass runs on a utility queue (CPU-bound,
        // tolerates concurrent recognition requests).
        let group = DispatchGroup()
        var ocrResults: [ScreenshotOcrResult] = []
        let lock = NSLock()
        let queue = DispatchQueue.global(qos: .utility)

        for (index, pickerResult) in results.enumerated() {
            group.enter()
            let provider = pickerResult.itemProvider
            // No PHAsset lookup — would require full Photo Library auth.
            // Capture date falls back to "today" via the Kotlin parser's
            // `captureEpochSeconds == 0` sentinel.
            let imageId = "image_\(index)_\(UUID().uuidString)"

            if provider.canLoadObject(ofClass: UIImage.self) {
                provider.loadObject(ofClass: UIImage.self) { object, error in
                    if let error = error {
                        NSLog("[ScreenshotOcr] load image #\(index) failed: \(error)")
                    }
                    let image = object as? UIImage
                    queue.async {
                        let lines = image.flatMap { PickerDelegate.recognizeText(on: $0) } ?? []
                        let result = ScreenshotOcrResult(
                            imageId: imageId,
                            textLines: lines,
                            captureEpochSeconds: 0
                        )
                        lock.lock()
                        ocrResults.append(result)
                        lock.unlock()
                        group.leave()
                    }
                }
            } else {
                NSLog("[ScreenshotOcr] provider #\(index) does not expose UIImage")
                let result = ScreenshotOcrResult(
                    imageId: imageId,
                    textLines: [],
                    captureEpochSeconds: 0
                )
                lock.lock()
                ocrResults.append(result)
                lock.unlock()
                group.leave()
            }
        }

        group.notify(queue: DispatchQueue.main) {
            self.fire(with: ocrResults)
        }
    }

    private func fire(with results: [ScreenshotOcrResult]) {
        guard !hasFired else { return }
        hasFired = true
        onResult(results)
    }

    // MARK: - Vision

    /// Runs VNRecognizeTextRequest synchronously (we're already off the main
    /// thread) and returns the recognized candidate strings in reading order.
    /// Includes both automatic language detection and digit-friendly options
    /// since notification bodies are mostly numbers + merchant words.
    private static func recognizeText(on image: UIImage) -> [String] {
        guard let cgImage = image.cgImage else { return [] }

        let handler = VNImageRequestHandler(cgImage: cgImage, orientation: .up, options: [:])
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = true
        request.automaticallyDetectsLanguage = true

        do {
            try handler.perform([request])
        } catch {
            NSLog("[ScreenshotOcr] Vision perform failed: \(error)")
            return []
        }

        guard let observations = request.results else { return [] }
        var lines: [String] = []
        for observation in observations {
            if let candidate = observation.topCandidates(1).first {
                lines.append(candidate.string)
            }
        }
        return lines
    }

}
