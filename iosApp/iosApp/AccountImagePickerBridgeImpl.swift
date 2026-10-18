import ComposeApp
import Foundation
import PhotosUI
import SwiftUI
import UIKit

/// Fixed output aspect ratio for the account hero image (width / height).
/// Must match the Compose-side `ImageHero` which renders at full-width ×
/// 200dp; on typical phones that's ~2:1. Keeping the output ratio fixed
/// here means the saved file always maps cleanly into the hero slot with
/// no distortion or letterboxing on the Compose side.
private let heroAspectRatio: CGFloat = 2.0

/// Swift bridge for the Account photo-hero feature (Vehicle / Real Estate
/// detail sheet).
///
/// Presents SwiftUI's PhotosPicker with a single-image selection limit,
/// resizes the picked image to <=1600px on the long edge (keeps JPEG size
/// well under 1 MB), saves it as a JPEG under the app's Documents
/// directory, and returns the absolute file path. The Kotlin side stores
/// the path on the Account; subsequent reads load bytes via ImageStorage
/// and decode via `decodeImageBitmap`.
///
/// Files are kept under `Documents/account_images/{uuid}.jpg` so they
/// survive app upgrades + app-storage migrations. Caller is responsible
/// for deleting the old file when the user replaces or removes the image.
@available(iOS 16.0, *)
class AccountImagePickerBridgeImpl: NSObject, AccountImagePickerBridge {

    fileprivate static let maxDimension: CGFloat = 1600
    fileprivate static let jpegQuality: CGFloat = 0.85

    func pickImage(onResult: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            self.presentPicker(onResult: onResult)
        }
    }

    private func presentPicker(onResult: @escaping (String?) -> Void) {
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = 1
        config.preferredAssetRepresentationMode = .current

        let picker = PHPickerViewController(configuration: config)
        let delegate = SinglePickerDelegate(onResult: onResult)
        picker.delegate = delegate

        // Retain delegate until the picker dismisses — PHPicker's delegate
        // property is weak.
        objc_setAssociatedObject(
            picker,
            &SinglePickerDelegate.associationKey,
            delegate,
            .OBJC_ASSOCIATION_RETAIN_NONATOMIC
        )

        guard let rootVC = Self.topViewController() else {
            NSLog("[AccountImagePicker] No root VC to present picker — returning nil")
            onResult(nil)
            return
        }
        rootVC.present(picker, animated: true)
    }

    fileprivate static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
        guard let window = scenes.first?.windows.first(where: { $0.isKeyWindow })
            ?? scenes.first?.windows.first,
              var top = window.rootViewController else { return nil }
        while let presented = top.presentedViewController { top = presented }
        return top
    }

    /// Resize + JPEG-compress + persist. Returns absolute path on success.
    /// Shared between the direct-save (iOS 15 fallback) and the crop-done
    /// handler on iOS 16+.
    fileprivate static func saveToDocuments(image: UIImage) -> String? {
        let resized = resize(image: image, maxDim: AccountImagePickerBridgeImpl.maxDimension)
        guard let data = resized.jpegData(compressionQuality: AccountImagePickerBridgeImpl.jpegQuality) else {
            return nil
        }
        let fileManager = FileManager.default
        guard let docs = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        let dir = docs.appendingPathComponent("account_images", isDirectory: true)
        do {
            try fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        } catch {
            NSLog("[AccountImagePicker] mkdir failed: \(error)")
            return nil
        }
        let fileURL = dir.appendingPathComponent("\(UUID().uuidString).jpg")
        do {
            try data.write(to: fileURL, options: Data.WritingOptions.atomic)
            return fileURL.path
        } catch {
            NSLog("[AccountImagePicker] write failed: \(error)")
            return nil
        }
    }

    private static func resize(image: UIImage, maxDim: CGFloat) -> UIImage {
        let w = image.size.width
        let h = image.size.height
        let longest = max(w, h)
        guard longest > maxDim else { return image }
        let scale = maxDim / longest
        let newSize = CGSize(width: w * scale, height: h * scale)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1.0
        let renderer = UIGraphicsImageRenderer(size: newSize, format: format)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}

/// Normalizes EXIF-rotated UIImages to upright orientation so downstream
/// rendering (SwiftUI preview + UIGraphicsImageRenderer) matches what the
/// user saw in the picker. Photos taken portrait on iPhone commonly come
/// back with .right / .leftMirrored orientation metadata.
private extension UIImage {
    func imageByNormalizingOrientation() -> UIImage {
        if self.imageOrientation == .up { return self }
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = self.scale
        let renderer = UIGraphicsImageRenderer(size: self.size, format: format)
        return renderer.image { _ in
            self.draw(in: CGRect(origin: .zero, size: self.size))
        }
    }
}

@available(iOS 16.0, *)
private class SinglePickerDelegate: NSObject, PHPickerViewControllerDelegate {

    static var associationKey: UInt8 = 0

    private let onResult: (String?) -> Void
    private var hasFired: Bool = false

    init(onResult: @escaping (String?) -> Void) {
        self.onResult = onResult
    }

    func picker(
        _ picker: PHPickerViewController,
        didFinishPicking results: [PHPickerResult]
    ) {
        // CRITICAL: chain everything AFTER the dismiss completion. Trying
        // to present a new modal (the crop view) while the picker is still
        // mid-dismiss-animation silently fails on iOS, which was the
        // "no crop screen, spinner forever" bug.
        guard let pickerResult = results.first else {
            picker.dismiss(animated: true) { [weak self] in self?.fire(with: nil) }
            return
        }
        let provider = pickerResult.itemProvider
        guard provider.canLoadObject(ofClass: UIImage.self) else {
            NSLog("[AccountImagePicker] provider does not expose UIImage")
            picker.dismiss(animated: true) { [weak self] in self?.fire(with: nil) }
            return
        }

        // Hold a STRONG reference to self across the async load + crop flow.
        // The PHPicker's associated-object retain of this delegate releases
        // the moment the picker dismisses, so without this `self` would be
        // nil when the crop screen's Done/Cancel buttons fire — the whole
        // bridge would stall and the ViewModel's spinner would never clear.
        let strongSelf = self
        picker.dismiss(animated: true) {
            provider.loadObject(ofClass: UIImage.self) { object, error in
                if let error = error {
                    NSLog("[AccountImagePicker] load image failed: \(error)")
                }
                guard let image = object as? UIImage else {
                    DispatchQueue.main.async { strongSelf.fire(with: nil) }
                    return
                }
                let normalized = image.imageByNormalizingOrientation()
                DispatchQueue.main.async {
                    strongSelf.presentCropScreen(image: normalized)
                }
            }
        }
    }

    static var hostAssociationKey: UInt8 = 1

    private func presentCropScreen(image: UIImage) {
        guard let rootVC = AccountImagePickerBridgeImpl.topViewController() else {
            NSLog("[AccountImagePicker] no root VC to present crop screen")
            fire(with: nil)
            return
        }
        if #available(iOS 16.0, *) {
            let cropView = ImageCropScreen(
                source: image,
                aspectRatio: heroAspectRatio,
                onConfirm: { cropped in
                    rootVC.dismiss(animated: true) {
                        DispatchQueue.global(qos: .userInitiated).async {
                            let path = AccountImagePickerBridgeImpl.saveToDocuments(image: cropped)
                            DispatchQueue.main.async { self.fire(with: path) }
                        }
                    }
                },
                onCancel: {
                    rootVC.dismiss(animated: true) {
                        self.fire(with: nil)
                    }
                }
            )
            let host = UIHostingController(rootView: cropView)
            host.modalPresentationStyle = .fullScreen
            // Retain self for the duration of the crop screen's lifecycle.
            // When the host dismisses, the associated object is released,
            // which releases this delegate — exactly the lifetime we want.
            objc_setAssociatedObject(
                host,
                &SinglePickerDelegate.hostAssociationKey,
                self,
                .OBJC_ASSOCIATION_RETAIN_NONATOMIC
            )
            rootVC.present(host, animated: true) {
                NSLog("[AccountImagePicker] crop screen presented")
            }
        } else {
            // iOS 15 fallback — skip the crop step and save the raw image.
            DispatchQueue.global(qos: .userInitiated).async {
                let path = AccountImagePickerBridgeImpl.saveToDocuments(image: image)
                DispatchQueue.main.async { self.fire(with: path) }
            }
        }
    }

    private func fire(with path: String?) {
        guard !hasFired else { return }
        hasFired = true
        onResult(path)
    }

}

/// Full-screen SwiftUI crop editor. The user adjusts a source image inside
/// a fixed-aspect canvas (2:1 to match the Compose hero slot) via pinch to
/// zoom and drag to pan. Zooming OUT past the image's natural bounds leaves
/// white fill in the gaps — the user explicitly asked for this behavior so
/// side-icon style images don't lose their original letterboxing.
///
/// Output is rendered with SwiftUI's ImageRenderer (iOS 16+) which captures
/// the exact composite the user confirms. The renderer's `scale` is set to
/// upscale the on-screen preview to a 1600-wide output image — gives us a
/// high-DPI JPEG without re-doing the transform math in UIKit coordinates.
@available(iOS 16.0, *)
private struct ImageCropScreen: View {
    let source: UIImage
    let aspectRatio: CGFloat
    let onConfirm: (UIImage) -> Void
    let onCancel: () -> Void

    @State private var scale: CGFloat = 1.0
    @State private var baseScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var baseOffset: CGSize = .zero
    @State private var canvasPixelWidth: CGFloat = 0

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onCancel) {
                    Text("Cancel").font(.body)
                }
                Spacer()
                Text("Adjust Photo").font(.headline)
                Spacer()
                Button(action: handleDone) {
                    Text("Done").font(.body).fontWeight(.semibold)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Spacer()

            GeometryReader { geo in
                let canvasW = geo.size.width - 32
                let canvasH = canvasW / aspectRatio
                HStack {
                    Spacer()
                    cropCanvas(canvasW: canvasW, canvasH: canvasH)
                        .onAppear { canvasPixelWidth = canvasW }
                        .onChange(of: canvasW) { newValue in canvasPixelWidth = newValue }
                    Spacer()
                }
                .frame(width: geo.size.width, height: geo.size.height, alignment: .center)
            }

            Spacer()

            Text("Pinch to zoom · Drag to position\nWhite fills any gaps around the image")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
        }
        .background(Color(.systemBackground))
    }

    private func cropCanvas(canvasW: CGFloat, canvasH: CGFloat) -> some View {
        ZStack {
            Color.white
            Image(uiImage: source)
                .resizable()
                .scaledToFit()
                .scaleEffect(scale)
                .offset(offset)
        }
        .frame(width: canvasW, height: canvasH)
        .clipped()
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
        )
        .gesture(
            SimultaneousGesture(
                MagnificationGesture()
                    .onChanged { v in
                        scale = max(0.2, min(6.0, baseScale * v))
                    }
                    .onEnded { _ in baseScale = scale },
                DragGesture()
                    .onChanged { v in
                        offset = CGSize(
                            width: baseOffset.width + v.translation.width,
                            height: baseOffset.height + v.translation.height
                        )
                    }
                    .onEnded { _ in baseOffset = offset }
            )
        )
    }

    @MainActor
    private func handleDone() {
        guard canvasPixelWidth > 0 else {
            onConfirm(source)
            return
        }
        let canvasH = canvasPixelWidth / aspectRatio
        let canvas = ZStack {
            Color.white
            Image(uiImage: source)
                .resizable()
                .scaledToFit()
                .scaleEffect(scale)
                .offset(offset)
        }
        .frame(width: canvasPixelWidth, height: canvasH)
        .clipped()

        let targetWidth: CGFloat = 1600
        let renderer = ImageRenderer(content: canvas)
        renderer.scale = targetWidth / canvasPixelWidth
        if let image = renderer.uiImage {
            onConfirm(image)
        } else {
            onConfirm(source)
        }
    }
}
