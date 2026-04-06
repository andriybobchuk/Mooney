package com.andriybobchuk.mooney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.core.premium.ActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val activityProvider: ActivityProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)

        setContent {
            App()
        }
    }
}