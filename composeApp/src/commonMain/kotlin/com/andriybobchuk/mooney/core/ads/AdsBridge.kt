package com.andriybobchuk.mooney.core.ads

// IosAdsBridge moved to iosMain — see AdsBridge.ios.kt.
//
// The interface must live in the iOS-only source set so Kotlin/Native can
// reference `platform.UIKit.UIView` directly as the `makeBannerView` return
// type. That forces the generated framework's module.modulemap to declare
// `use UIKit`, which in turn lets Swift bind the protocol's `UIView` to the
// real `UIKit.UIView` class — without that, Swift treats it as a phantom
// forward-declared type and protocol conformance fails for any Swift class
// trying to implement it. This file is intentionally empty (kept for git
// history); the interface is now an iOS-platform-only contract.
