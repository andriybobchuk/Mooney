import ComposeApp
import FirebaseAnalytics
import FirebaseCrashlytics

class FirebaseAnalyticsBridge: IosAnalyticsBridge {

    func logEvent(name: String, params: [String: String]) {
        FirebaseAnalytics.Analytics.logEvent(name, parameters: params as [String: Any])
    }

    func setUserId(id: String?) {
        FirebaseAnalytics.Analytics.setUserID(id)
        Crashlytics.crashlytics().setUserID(id ?? "")
    }

    func setUserProperty(name: String, value: String) {
        FirebaseAnalytics.Analytics.setUserProperty(value, forName: name)
    }

    func recordException(message: String, context: [String: String]) {
        let crashlytics = Crashlytics.crashlytics()
        for (key, value) in context {
            crashlytics.setCustomValue(value, forKey: key)
        }
        let error = NSError(
            domain: "com.andriybobchuk.mooney",
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
        crashlytics.record(error: error)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func setCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }
}
