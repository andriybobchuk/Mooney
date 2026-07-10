package com.andriybobchuk.mooney.core.review

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import org.koin.core.context.GlobalContext

/**
 * Android implementation backed by the Google Play In-App Review API.
 *
 * The expect-class has a no-arg constructor so we pull the Application
 * context out of Koin lazily, mirroring how [com.andriybobchuk.mooney.core.ads.Ads]
 * stashes its application reference up-front. The current activity is fetched
 * via [com.andriybobchuk.mooney.core.premium.ActivityProvider] — without an
 * activity in the foreground the Play API can't show the dialog, so we
 * silently skip (matching Apple's throttled-no-op behaviour).
 */
actual class ReviewPromptManager actual constructor() {

    actual fun requestReview() {
        val koin = GlobalContext.getOrNull() ?: return
        val application = koin.get<Application>()
        val activityProvider =
            koin.get<com.andriybobchuk.mooney.core.premium.ActivityProvider>()
        val activity: Activity = activityProvider.getActivity() ?: return

        val manager = ReviewManagerFactory.create(application)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "requestReviewFlow failed: ${task.exception?.message}")
                return@addOnCompleteListener
            }
            manager.launchReviewFlow(activity, task.result).addOnCompleteListener {
                // Play silently throttles — success here doesn't mean the user
                // saw the dialog, just that the API accepted our request.
            }
        }
    }

    private companion object {
        const val TAG = "ReviewPromptManager"
    }
}
