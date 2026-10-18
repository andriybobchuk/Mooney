package com.andriybobchuk.mooney.core.platform

/**
 * One-shot photo picker for account hero images (Vehicle / Real Estate).
 *
 * iOS implementation (Swift) presents SwiftUI's PhotosPicker with
 * `selectionLimit = 1`, saves the chosen UIImage as a JPEG under the app's
 * Documents directory, and returns the absolute file path. Android is a
 * no-op placeholder for now — image hero is iOS-only in this round.
 *
 * The returned path is persisted on `Account.imagePath`; subsequent reads
 * go through [com.andriybobchuk.mooney.core.data.ImageStorage.loadImage]
 * which loads the bytes for Compose rendering.
 */
interface AccountImagePickerBridge {
    /**
     * Present the native picker, save the chosen image, invoke [onResult]
     * with its absolute file path. Null if the user cancelled or no image
     * could be loaded.
     */
    fun pickImage(onResult: (String?) -> Unit)
}

/**
 * Global holder for the Swift-provided impl. Set from iOSApp.swift during
 * AppDelegate init. Mirrors the pattern used by [IosBillingBridgeHolder]
 * and [ScreenshotOcrBridgeHolder].
 */
object AccountImagePickerBridgeHolder {
    @kotlin.concurrent.Volatile
    var bridge: AccountImagePickerBridge? = null
}

/** Setter callable from Swift. */
fun setAccountImagePickerBridge(bridge: AccountImagePickerBridge) {
    AccountImagePickerBridgeHolder.bridge = bridge
}
