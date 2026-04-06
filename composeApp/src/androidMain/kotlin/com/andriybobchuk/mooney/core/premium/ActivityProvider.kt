package com.andriybobchuk.mooney.core.premium

import android.app.Activity
import java.lang.ref.WeakReference

class ActivityProvider {
    private var activityRef: WeakReference<Activity>? = null

    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun getActivity(): Activity? = activityRef?.get()
}
