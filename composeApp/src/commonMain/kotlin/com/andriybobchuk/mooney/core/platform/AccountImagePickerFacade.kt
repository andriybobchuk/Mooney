package com.andriybobchuk.mooney.core.platform

/**
 * Thin suspend facade over [AccountImagePickerBridge] — hides the Swift
 * callback-indirection behind a coroutine-friendly entry point for domain
 * code. VMs inject this via Koin and call `pickImage()` without caring
 * which platform they're on.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class AccountImagePickerFacade() {
    /**
     * Returns an absolute file path to the saved image, or null if the
     * user cancelled / no image was produced. Caller persists the path
     * on the target account via a normal upsert.
     */
    suspend fun pickImage(): String?
}
