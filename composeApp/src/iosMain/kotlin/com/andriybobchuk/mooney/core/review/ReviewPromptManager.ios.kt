package com.andriybobchuk.mooney.core.review

import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication

actual class ReviewPromptManager {
    actual fun requestReview() {
        // Use the scene-based API on iOS 14+, falling back to the deprecated
        // method on older OS versions. iOS handles its own throttling — calling
        // this more often than allowed is a silent no-op, not an error.
        val scene = UIApplication.sharedApplication.connectedScenes
            .firstOrNull { (it as? platform.UIKit.UIWindowScene)?.activationState == platform.UIKit.UISceneActivationStateForegroundActive }
            as? platform.UIKit.UIWindowScene
        if (scene != null) {
            SKStoreReviewController.requestReviewInScene(scene)
        } else {
            @Suppress("DEPRECATION")
            SKStoreReviewController.requestReview()
        }
    }
}
