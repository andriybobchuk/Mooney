import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Painting the SwiftUI window background white before Compose attaches
        // bridges the launch-screen → first-frame transition. Without this,
        // there can be a brief gray/transparent frame visible while
        // ComposeUIViewController initializes.
        ZStack {
            Color.white.ignoresSafeArea()
            ComposeView()
                    .ignoresSafeArea(.keyboard)
                    .ignoresSafeArea(.container) // Compose has own keyboard handler
        }
    }
}



