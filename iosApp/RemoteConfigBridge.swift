import ComposeApp
import FirebaseRemoteConfig

class RemoteConfigBridge: IosRemoteConfigBridge {

    func fetchAndActivate() async -> Bool {
        do {
            let status = try await FirebaseRemoteConfig.RemoteConfig.remoteConfig().fetchAndActivate()
            return status == .successFetchedFromRemote || status == .successUsingPreFetchedData
        } catch {
            return false
        }
    }

    func getString(key: String) -> String {
        return FirebaseRemoteConfig.RemoteConfig.remoteConfig().configValue(forKey: key).stringValue ?? ""
    }
}
