import ComposeApp
import Foundation
import StoreKit

// Swift StoreKit 2 bridge — replaces the Kotlin/Native StoreKit 1
// (SKPaymentQueue/SKProductsRequest) flow that was producing stuck spinners
// and one App Store crash (Submission 1f7ed921, iPad Air M3).
//
// Why Swift not Kotlin: StoreKit 2's Product.purchase() is async/await + a
// single Result return value. The KMP bridging from Kotlin suspend → ObjC
// SKPaymentTransactionObserverProtocol → CompletableDeferred has multiple
// failure points (observer not firing, thread visibility, deferred dropped
// on cancellation). One Swift await call eliminates every one of those.
//
// Wired up in iOSApp.swift:
//   IosBillingManagerCompanion.shared.<unused> // ensures class loads
//   Koin's IosBillingManager singleton gets setBridge(IosBillingBridgeImpl())
@available(iOS 15.0, *)
class IosBillingBridgeImpl: NSObject, IosBillingBridge {

    private var entitlementTask: Task<Void, Never>?

    override init() {
        super.init()
        // Listen for unfinished transactions on app launch (subscription
        // renewals, family-shared purchases, anything completed off-device).
        entitlementTask = listenForTransactions()
    }

    deinit {
        entitlementTask?.cancel()
    }

    func fetchPrice(productId: String, onResult: @escaping (String?) -> Void) {
        NSLog("[BillingBridge] fetchPrice productId=\(productId)")
        Task {
            do {
                let products = try await Product.products(for: [productId])
                guard let product = products.first else {
                    NSLog("[BillingBridge] fetchPrice: no products returned")
                    onResult(nil)
                    return
                }
                NSLog("[BillingBridge] fetchPrice: product=\(product.id) displayPrice=\(product.displayPrice)")
                onResult(product.displayPrice)
            } catch {
                NSLog("[BillingBridge] fetchPrice failed: \(error)")
                onResult(nil)
            }
        }
    }

    func purchase(productId: String, onResult: @escaping (BridgePurchaseResult) -> Void) {
        NSLog("[BillingBridge] purchase productId=\(productId)")
        Task {
            do {
                let products = try await Product.products(for: [productId])
                guard let product = products.first else {
                    NSLog("[BillingBridge] purchase: product not found")
                    onResult(BridgePurchaseResult(
                        status: "error",
                        message: "Product not found. Check subscription is approved in App Store Connect."
                    ))
                    return
                }
                NSLog("[BillingBridge] purchase: calling product.purchase()")
                let result = try await product.purchase()
                NSLog("[BillingBridge] purchase: result=\(result)")
                switch result {
                case .success(let verification):
                    switch verification {
                    case .verified(let transaction):
                        NSLog("[BillingBridge] purchase: verified — finishing")
                        await transaction.finish()
                        onResult(BridgePurchaseResult(status: "success", message: nil))
                    case .unverified(_, let error):
                        NSLog("[BillingBridge] purchase: unverified — \(error)")
                        onResult(BridgePurchaseResult(
                            status: "error",
                            message: "Purchase could not be verified: \(error.localizedDescription)"
                        ))
                    }
                case .userCancelled:
                    NSLog("[BillingBridge] purchase: user cancelled")
                    onResult(BridgePurchaseResult(status: "cancelled", message: nil))
                case .pending:
                    NSLog("[BillingBridge] purchase: pending (Ask to Buy?)")
                    onResult(BridgePurchaseResult(
                        status: "error",
                        message: "Purchase is pending approval (Ask to Buy)."
                    ))
                @unknown default:
                    NSLog("[BillingBridge] purchase: unknown result")
                    onResult(BridgePurchaseResult(status: "error", message: "Unknown purchase result"))
                }
            } catch let error as Product.PurchaseError {
                NSLog("[BillingBridge] purchase: PurchaseError \(error)")
                onResult(BridgePurchaseResult(status: "error", message: error.localizedDescription))
            } catch let error as StoreKitError {
                NSLog("[BillingBridge] purchase: StoreKitError \(error)")
                onResult(BridgePurchaseResult(status: "error", message: error.localizedDescription))
            } catch {
                NSLog("[BillingBridge] purchase: generic error \(error)")
                onResult(BridgePurchaseResult(status: "error", message: error.localizedDescription))
            }
        }
    }

    func checkEntitlement(productId: String, onResult: @escaping (KotlinBoolean) -> Void) {
        NSLog("[BillingBridge] checkEntitlement productId=\(productId)")
        Task {
            for await result in Transaction.currentEntitlements {
                guard case .verified(let transaction) = result else { continue }
                if transaction.productID == productId && transaction.revocationDate == nil {
                    NSLog("[BillingBridge] checkEntitlement: entitled")
                    onResult(KotlinBoolean(value: true))
                    return
                }
            }
            NSLog("[BillingBridge] checkEntitlement: not entitled")
            onResult(KotlinBoolean(value: false))
        }
    }

    private func listenForTransactions() -> Task<Void, Never> {
        Task.detached {
            for await result in Transaction.updates {
                guard case .verified(let transaction) = result else { continue }
                NSLog("[BillingBridge] Transaction.updates: \(transaction.productID)")
                await transaction.finish()
            }
        }
    }
}
