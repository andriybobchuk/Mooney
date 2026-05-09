import ComposeApp
import FirebaseRemoteConfig

class RemoteConfigBridge: IosRemoteConfigBridge {

    init() {
        let remoteConfig = FirebaseRemoteConfig.RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600 // 1 hour
        remoteConfig.configSettings = settings
        // Fetch in background — values available on next getString call
        remoteConfig.fetchAndActivate { _, _ in }
    }

    func getString(key: String) -> String {
        return FirebaseRemoteConfig.RemoteConfig.remoteConfig().configValue(forKey: key).stringValue ?? ""
    }
}
