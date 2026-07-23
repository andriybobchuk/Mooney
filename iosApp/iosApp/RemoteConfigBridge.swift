import ComposeApp
import FirebaseRemoteConfig

class RemoteConfigBridge: IosRemoteConfigBridge {

    init() {
        let remoteConfig = FirebaseRemoteConfig.RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        // 15 min — well above the 5-fetches-per-hour server cap, short
        // enough that a manual toggle-and-restart during release prep
        // actually reflects the console. Was 3600 (1h) which was too slow.
        settings.minimumFetchInterval = 900
        remoteConfig.configSettings = settings
        // Fetch in background — values available on next getString call.
        remoteConfig.fetchAndActivate { _, _ in }
        // Realtime updates — console pushes any change to devices with an
        // open socket. Sidesteps the fetch interval entirely for edits we
        // make right now during release prep. Best-effort.
        remoteConfig.addOnConfigUpdateListener { _, _ in
            remoteConfig.activate { _, _ in }
        }
    }

    func getString(key: String) -> String {
        return FirebaseRemoteConfig.RemoteConfig.remoteConfig().configValue(forKey: key).stringValue ?? ""
    }
}
