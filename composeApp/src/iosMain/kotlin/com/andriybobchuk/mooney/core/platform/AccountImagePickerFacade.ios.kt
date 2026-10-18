package com.andriybobchuk.mooney.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AccountImagePickerFacade {
    actual suspend fun pickImage(): String? {
        val bridge = AccountImagePickerBridgeHolder.bridge ?: return null
        return suspendCancellableCoroutine { cont ->
            bridge.pickImage { path ->
                if (cont.isActive) cont.resume(path)
            }
        }
    }
}
