import SwiftUI

// MARK: - Root demo view

/// Proof-of-concept SwiftUI preview that mimics Mooney's Transactions tab
/// using iOS 26 Liquid Glass materials throughout. NOT wired to real Kotlin
/// data — everything below the chrome uses mock fixtures so you can evaluate
/// the aesthetic without a full ViewModel bridge.
///
/// Launch path: mooney://liquid-glass-preview  (handled in iOSApp.swift)
///
/// Extension path if you like what you see:
///   1. Bump `IPHONEOS_DEPLOYMENT_TARGET` to 26.0 (or keep 15.3 with gates)
///   2. Bridge each Kotlin ViewModel → an `@Observable` Swift wrapper that
///      reads Kotlin Flows via Skie or a hand-rolled SharedFlow collector
///   3. Migrate one screen at a time; each Compose screen becomes a
///      SwiftUI equivalent inside this outer TabView shell
@available(iOS 26.0, *)
struct MooneyLiquidGlassPreview: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab: PreviewTab = .transactions

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Transactions", systemImage: "list.bullet", value: PreviewTab.transactions) {
                TransactionsPreviewScreen(onClose: { dismiss() })
            }
            Tab("Assets", systemImage: "wallet.pass", value: PreviewTab.assets) {
                StubScreen(title: "Assets", subtitle: "Preview only — not migrated yet")
            }
            Tab("Analytics", systemImage: "chart.bar", value: PreviewTab.analytics) {
                StubScreen(title: "Analytics", subtitle: "Preview only — not migrated yet")
            }
            Tab("Goals", systemImage: "target", value: PreviewTab.goals) {
                StubScreen(title: "Goals", subtitle: "Preview only — not migrated yet")
            }
            Tab("Settings", systemImage: "gearshape", value: PreviewTab.settings) {
                StubScreen(title: "Settings", subtitle: "Preview only — not migrated yet")
            }
        }
        .tint(MooneyBrand.accent)
    }

    enum PreviewTab: Hashable {
        case transactions, assets, analytics, goals, settings
    }
}

// MARK: - Transactions screen

@available(iOS 26.0, *)
private struct TransactionsPreviewScreen: View {
    let onClose: () -> Void
    @State private var showAddSheet = false
    @State private var selectedMonth = "September"
    @State private var searchText = ""

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                List {
                    // Balance header
                    Section {
                        BalanceHeaderView()
                            .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                    }

                    // Month selector chip row
                    Section {
                        MonthSelectorRow(selected: $selectedMonth)
                            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 12, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                    }

                    // Grouped transactions
                    ForEach(MockData.transactionsByDay, id: \.day) { group in
                        Section {
                            ForEach(group.items) { tx in
                                TransactionRowView(transaction: tx)
                            }
                        } header: {
                            Text(group.day)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundStyle(.secondary)
                                .textCase(nil)
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
                .background(
                    LinearGradient(
                        colors: [MooneyBrand.backgroundTop, MooneyBrand.backgroundBottom],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .automatic), prompt: "Search transactions")
                .navigationTitle("Transactions")
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    ToolbarItemGroup(placement: .topBarLeading) {
                        Button {
                            onClose()
                        } label: {
                            Label("Close preview", systemImage: "xmark")
                        }
                    }
                    ToolbarItemGroup(placement: .topBarTrailing) {
                        Button {} label: {
                            Label("Filter", systemImage: "line.3.horizontal.decrease.circle")
                        }
                        Menu {
                            Button("Sort by date") {}
                            Button("Sort by amount") {}
                            Divider()
                            Button("Export CSV") {}
                        } label: {
                            Label("More", systemImage: "ellipsis.circle")
                        }
                    }
                }

                // Floating action button — Liquid Glass native
                Button {
                    showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .frame(width: 56, height: 56)
                }
                .buttonStyle(.borderedProminent)
                .buttonBorderShape(.circle)
                .tint(MooneyBrand.accent)
                .padding(.trailing, 20)
                .padding(.bottom, 20)
                .shadow(color: MooneyBrand.accent.opacity(0.35), radius: 10, y: 4)
            }
            .sheet(isPresented: $showAddSheet) {
                AddTransactionPreviewSheet()
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
        }
    }
}

// MARK: - Balance header

@available(iOS 26.0, *)
private struct BalanceHeaderView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Spent this month")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
                Text("Sep 2026")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            Text("2,847.50 zł")
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)
            HStack(spacing: 4) {
                Image(systemName: "arrow.up.right")
                    .font(.caption)
                Text("+12% vs last month")
                    .font(.caption)
                    .fontWeight(.medium)
            }
            .foregroundStyle(.red.opacity(0.85))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.regularMaterial)
        }
    }
}

// MARK: - Month selector

@available(iOS 26.0, *)
private struct MonthSelectorRow: View {
    @Binding var selected: String
    private let months = ["July", "August", "September", "October"]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(months, id: \.self) { month in
                    Button {
                        selected = month
                    } label: {
                        Text(month)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .tint(selected == month ? MooneyBrand.accent : .secondary)
                    .foregroundStyle(selected == month ? Color.white : Color.primary)
                }
            }
            .padding(.horizontal, 4)
        }
    }
}

// MARK: - Transaction row

@available(iOS 26.0, *)
private struct TransactionRowView: View {
    let transaction: MockTransaction

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(transaction.categoryColor.opacity(0.18))
                    .frame(width: 44, height: 44)
                Text(transaction.emoji)
                    .font(.title2)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(transaction.title)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundStyle(.primary)
                Text(transaction.subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(transaction.amountFormatted)
                .font(.body)
                .fontWeight(.semibold)
                .foregroundStyle(transaction.amount < 0 ? Color.red.opacity(0.85) : Color.green.opacity(0.85))
                .monospacedDigit()
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Add transaction sheet

@available(iOS 26.0, *)
private struct AddTransactionPreviewSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var amount = ""
    @State private var type: TxType = .expense
    @State private var selectedCategory = "Groceries"

    enum TxType: String, CaseIterable {
        case expense = "Expense"
        case income = "Income"
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Type", selection: $type) {
                        ForEach(TxType.allCases, id: \.self) { t in
                            Text(t.rawValue).tag(t)
                        }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                }

                Section {
                    HStack {
                        Text("Amount")
                            .foregroundStyle(.secondary)
                        Spacer()
                        TextField("0.00", text: $amount)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .font(.title2)
                            .fontWeight(.semibold)
                        Text("zł")
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Category") {
                    ForEach(["Groceries", "Transport", "Coffee", "Rent"], id: \.self) { cat in
                        Button {
                            selectedCategory = cat
                        } label: {
                            HStack {
                                Text(cat)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if selectedCategory == cat {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(MooneyBrand.accent)
                                }
                            }
                        }
                    }
                }

                Section("Account") {
                    LabeledContent("Bank", value: "8,240 zł")
                    LabeledContent("Cash", value: "420 zł")
                }
            }
            .navigationTitle("Add Transaction")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
    }
}

// MARK: - Stub screen for other tabs

@available(iOS 26.0, *)
private struct StubScreen: View {
    let title: String
    let subtitle: String

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "sparkles")
                    .font(.system(size: 48))
                    .foregroundStyle(MooneyBrand.accent.gradient)
                Text(title)
                    .font(.largeTitle.bold())
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle(title)
        }
    }
}

// MARK: - Brand tokens

@available(iOS 26.0, *)
private enum MooneyBrand {
    /// Approximate match to Mooney's existing accent. Swap in the exact hex
    /// when the SwiftUI migration is greenlit and design-system tokens are
    /// bridged from Kotlin's AppDesignSystem object.
    static let accent = Color(red: 0.19, green: 0.63, blue: 0.94)
    static let backgroundTop = Color(red: 0.98, green: 0.98, blue: 1.0)
    static let backgroundBottom = Color(red: 0.94, green: 0.96, blue: 1.0)
}

// MARK: - Mock data

@available(iOS 26.0, *)
private struct MockTransaction: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let subtitle: String
    let amount: Double
    let categoryColor: Color

    var amountFormatted: String {
        let sign = amount < 0 ? "-" : "+"
        return "\(sign)\(String(format: "%.2f", abs(amount))) zł"
    }
}

@available(iOS 26.0, *)
private struct DayGroup {
    let day: String
    let items: [MockTransaction]
}

@available(iOS 26.0, *)
private enum MockData {
    static let transactionsByDay: [DayGroup] = [
        DayGroup(day: "Today", items: [
            MockTransaction(emoji: "☕", title: "Coffee", subtitle: "Cafes · Cash", amount: -14.50, categoryColor: .brown),
            MockTransaction(emoji: "🛒", title: "Biedronka", subtitle: "Groceries · Bank", amount: -87.20, categoryColor: .green),
            MockTransaction(emoji: "🚕", title: "Uber", subtitle: "Transport · Bank", amount: -22.00, categoryColor: .yellow),
        ]),
        DayGroup(day: "Yesterday", items: [
            MockTransaction(emoji: "💼", title: "Freelance project", subtitle: "Freelance · Bank", amount: 1_450.00, categoryColor: .blue),
            MockTransaction(emoji: "🍔", title: "Lunch out", subtitle: "Eating out · Bank", amount: -42.00, categoryColor: .orange),
        ]),
        DayGroup(day: "Monday, Sep 20", items: [
            MockTransaction(emoji: "🎬", title: "Netflix", subtitle: "Subscriptions · Bank", amount: -55.00, categoryColor: .red),
            MockTransaction(emoji: "🛒", title: "Auchan", subtitle: "Groceries · Bank", amount: -132.50, categoryColor: .green),
            MockTransaction(emoji: "🏠", title: "Rent", subtitle: "Housing · Bank", amount: -2_800.00, categoryColor: .indigo),
        ]),
    ]
}
