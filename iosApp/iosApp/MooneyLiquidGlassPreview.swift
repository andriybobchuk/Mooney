import SwiftUI

// MARK: - Root

@available(iOS 26.0, *)
struct MooneyLiquidGlassPreview: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab: PreviewTab = .transactions

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Transactions", systemImage: "list.bullet.rectangle", value: PreviewTab.transactions) {
                TransactionsPreviewScreen(onClose: { dismiss() })
            }
            Tab("Assets", systemImage: "wallet.pass", value: PreviewTab.assets) {
                AssetsPreviewScreen()
            }
            Tab("Analytics", systemImage: "chart.line.uptrend.xyaxis", value: PreviewTab.analytics) {
                AnalyticsPreviewScreen()
            }
            Tab("Goals", systemImage: "target", value: PreviewTab.goals) {
                GoalsPreviewScreen()
            }
            Tab("Settings", systemImage: "gearshape", value: PreviewTab.settings) {
                SettingsPreviewScreen()
            }
        }
        .tint(MooneyBrand.accent)
    }

    enum PreviewTab: Hashable {
        case transactions, assets, analytics, goals, settings
    }
}

// ============================================================================
// MARK: - Transactions Tab
// ============================================================================

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
                    Section {
                        BalanceHeaderView()
                            .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                    }
                    Section {
                        MonthSelectorRow(selected: $selectedMonth)
                            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 12, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                    }
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
                .background(MooneyBrand.background)
                .searchable(text: $searchText, prompt: "Search transactions")
                .navigationTitle("Transactions")
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            onClose()
                        } label: {
                            Label("Close preview", systemImage: "xmark")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {} label: {
                            Label("Filter", systemImage: "line.3.horizontal.decrease.circle")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
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

@available(iOS 26.0, *)
private struct MonthSelectorRow: View {
    @Binding var selected: String
    private let months = ["Jun", "Jul", "Aug", "September", "Oct"]

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

@available(iOS 26.0, *)
private struct TransactionRowView: View {
    let transaction: MockTransaction
    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(transaction.categoryColor.opacity(0.18))
                    .frame(width: 44, height: 44)
                Text(transaction.emoji).font(.title2)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(transaction.title).font(.body).fontWeight(.medium)
                Text(transaction.subtitle).font(.caption).foregroundStyle(.secondary)
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
                        Text("Amount").foregroundStyle(.secondary)
                        Spacer()
                        TextField("0.00", text: $amount)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .font(.title2)
                            .fontWeight(.semibold)
                        Text("zł").foregroundStyle(.secondary)
                    }
                }
                Section("Category") {
                    ForEach(["Groceries", "Transport", "Coffee", "Rent"], id: \.self) { cat in
                        Button {
                            selectedCategory = cat
                        } label: {
                            HStack {
                                Text(cat).foregroundStyle(.primary)
                                Spacer()
                                if selectedCategory == cat {
                                    Image(systemName: "checkmark").foregroundStyle(MooneyBrand.accent)
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
                    Button("Save") { dismiss() }.fontWeight(.semibold)
                }
            }
        }
    }
}

// ============================================================================
// MARK: - Assets Tab
// ============================================================================

@available(iOS 26.0, *)
private struct AssetsPreviewScreen: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    NetWorthHeaderCard()
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                    // Accounts section
                    VStack(alignment: .leading, spacing: 8) {
                        SectionHeader(title: "Accounts")
                        VStack(spacing: 10) {
                            ForEach(MockData.accounts) { account in
                                AccountRowView(account: account)
                            }
                        }
                    }
                    .padding(.horizontal, 16)

                    // Add account CTA
                    Button {} label: {
                        Label("Add account", systemImage: "plus.circle.fill")
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.roundedRectangle(radius: 14))
                    .tint(MooneyBrand.accent)
                    .padding(.horizontal, 16)
                    .padding(.top, 6)

                    Spacer(minLength: 40)
                }
            }
            .background(MooneyBrand.background)
            .navigationTitle("Assets")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("Manage categories") {}
                        Button("Reconcile balances") {}
                    } label: {
                        Label("More", systemImage: "ellipsis.circle")
                    }
                }
            }
        }
    }
}

@available(iOS 26.0, *)
private struct NetWorthHeaderCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Total net worth")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("438,220 zł")
                .font(.system(size: 38, weight: .bold, design: .rounded))
            HStack(spacing: 6) {
                Image(systemName: "arrow.up.right")
                    .font(.caption.weight(.bold))
                Text("+2.1% this month")
                    .font(.caption)
                    .fontWeight(.medium)
                Spacer()
                Text("6 accounts")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .foregroundStyle(.green)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(.regularMaterial)
        }
    }
}

@available(iOS 26.0, *)
private struct AccountRowView: View {
    let account: MockAccount
    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(account.tint.opacity(0.16))
                    .frame(width: 46, height: 46)
                Text(account.emoji).font(.title3)
            }
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(account.title).font(.body).fontWeight(.medium)
                    if account.isPrimary {
                        Text("Primary")
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Capsule().fill(MooneyBrand.accent.opacity(0.15)))
                            .foregroundStyle(MooneyBrand.accent)
                    }
                }
                Text(account.category).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text(account.balanceFormatted)
                .font(.body)
                .fontWeight(.semibold)
                .monospacedDigit()
        }
        .padding(14)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.regularMaterial)
        }
    }
}

// ============================================================================
// MARK: - Analytics Tab
// ============================================================================

@available(iOS 26.0, *)
private struct AnalyticsPreviewScreen: View {
    @State private var period = "6mo"
    @State private var selectedMonth = "Sep 2026"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Period pills + month chip
                    HStack {
                        Picker("Period", selection: $period) {
                            Text("6mo").tag("6mo")
                            Text("1y").tag("1y")
                            Text("Lifetime").tag("Lifetime")
                        }
                        .pickerStyle(.segmented)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)

                    // Chart placeholder
                    TrendChartMock()
                        .frame(height: 200)
                        .padding(.horizontal, 16)

                    // Metric cards
                    VStack(spacing: 12) {
                        MetricCardView(
                            title: "Revenue",
                            value: "6,850 zł",
                            delta: "+8%",
                            deltaIsPositive: true,
                            previousLabel: "vs. 6,340 zł last month",
                            accent: .green
                        )
                        MetricCardView(
                            title: "Expenses",
                            value: "2,847 zł",
                            delta: "+12%",
                            deltaIsPositive: false,
                            previousLabel: "vs. 2,540 zł last month",
                            accent: .red
                        )
                        MetricCardView(
                            title: "Taxes",
                            value: "1,301 zł",
                            delta: "+8%",
                            deltaIsPositive: false,
                            previousLabel: "vs. 1,205 zł last month",
                            accent: .orange
                        )
                        MetricCardView(
                            title: "Net Income",
                            value: "2,702 zł",
                            delta: "+3%",
                            deltaIsPositive: true,
                            previousLabel: "vs. 2,595 zł last month",
                            accent: .blue
                        )
                    }
                    .padding(.horizontal, 16)

                    Spacer(minLength: 40)
                }
            }
            .background(MooneyBrand.background)
            .navigationTitle("Analytics")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("Sep 2026") {}
                        Button("Aug 2026") {}
                        Button("Jul 2026") {}
                    } label: {
                        HStack(spacing: 4) {
                            Text(selectedMonth).font(.subheadline).fontWeight(.medium)
                            Image(systemName: "chevron.down").font(.caption)
                        }
                    }
                }
            }
        }
    }
}

@available(iOS 26.0, *)
private struct TrendChartMock: View {
    // A pretty line drawn with SwiftUI Canvas. Just visual — no real data.
    var body: some View {
        Canvas { context, size in
            // Grid lines
            for i in 0...4 {
                let y = size.height * CGFloat(i) / 4
                var path = Path()
                path.move(to: CGPoint(x: 0, y: y))
                path.addLine(to: CGPoint(x: size.width, y: y))
                context.stroke(path, with: .color(.gray.opacity(0.15)), lineWidth: 0.5)
            }
            // Trend line
            let points: [CGFloat] = [0.7, 0.55, 0.65, 0.42, 0.38, 0.30]
            var linePath = Path()
            for (i, p) in points.enumerated() {
                let x = size.width * CGFloat(i) / CGFloat(points.count - 1)
                let y = size.height * p
                if i == 0 {
                    linePath.move(to: CGPoint(x: x, y: y))
                } else {
                    linePath.addLine(to: CGPoint(x: x, y: y))
                }
            }
            context.stroke(
                linePath,
                with: .color(MooneyBrand.accent),
                style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round)
            )
            // Fill under line
            var fillPath = linePath
            fillPath.addLine(to: CGPoint(x: size.width, y: size.height))
            fillPath.addLine(to: CGPoint(x: 0, y: size.height))
            fillPath.closeSubpath()
            context.fill(
                fillPath,
                with: .linearGradient(
                    Gradient(colors: [MooneyBrand.accent.opacity(0.25), MooneyBrand.accent.opacity(0.0)]),
                    startPoint: CGPoint(x: 0, y: 0),
                    endPoint: CGPoint(x: 0, y: size.height)
                )
            )
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.regularMaterial)
        }
    }
}

@available(iOS 26.0, *)
private struct MetricCardView: View {
    let title: String
    let value: String
    let delta: String
    let deltaIsPositive: Bool
    let previousLabel: String
    let accent: Color

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Circle().fill(accent).frame(width: 8, height: 8)
                    Text(title).font(.subheadline).foregroundStyle(.secondary)
                }
                Text(value)
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .monospacedDigit()
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(delta)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(
                        Capsule().fill((deltaIsPositive ? Color.green : Color.red).opacity(0.15))
                    )
                    .foregroundStyle(deltaIsPositive ? Color.green : Color.red)
                Text(previousLabel)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(.regularMaterial)
        }
    }
}

// ============================================================================
// MARK: - Goals Tab
// ============================================================================

@available(iOS 26.0, *)
private struct GoalsPreviewScreen: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(MockData.goals) { goal in
                        GoalCardView(goal: goal)
                    }
                    Button {} label: {
                        Label("Add goal", systemImage: "plus.circle.fill")
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.roundedRectangle(radius: 14))
                    .tint(MooneyBrand.accent)
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(MooneyBrand.background)
            .navigationTitle("Goals")
        }
    }
}

@available(iOS 26.0, *)
private struct GoalCardView: View {
    let goal: MockGoal
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Text(goal.emoji).font(.title)
                VStack(alignment: .leading, spacing: 2) {
                    Text(goal.title).font(.headline)
                    Text(goal.subtitle).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Text("\(Int(goal.progress * 100))%")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(MooneyBrand.accent)
            }
            ProgressView(value: goal.progress)
                .tint(MooneyBrand.accent)
            HStack {
                Text("\(goal.savedFormatted) saved")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text("of \(goal.targetFormatted)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.regularMaterial)
        }
    }
}

// ============================================================================
// MARK: - Settings Tab
// ============================================================================

@available(iOS 26.0, *)
private struct SettingsPreviewScreen: View {
    @State private var notificationsEnabled = true
    @State private var excludeTaxes = true
    @State private var themeMode: ThemeMode = .system

    enum ThemeMode: String, CaseIterable, Identifiable {
        case light = "Light"
        case dark = "Dark"
        case system = "System"
        var id: String { rawValue }
    }

    var body: some View {
        NavigationStack {
            List {
                Section("Preferences") {
                    Picker("Theme", selection: $themeMode) {
                        ForEach(ThemeMode.allCases) { m in
                            Text(m.rawValue).tag(m)
                        }
                    }
                    LabeledContent("Default currency") {
                        Text("PLN").foregroundStyle(.secondary)
                    }
                    LabeledContent("Language") {
                        Text("English").foregroundStyle(.secondary)
                    }
                    Toggle("Notifications", isOn: $notificationsEnabled)
                    Toggle("Exclude taxes from totals", isOn: $excludeTaxes)
                }

                Section("Categories") {
                    NavigationLink {
                        Text("Categories detail (preview stub)")
                    } label: {
                        Label("Manage transaction categories", systemImage: "square.grid.2x2")
                    }
                    NavigationLink {
                        Text("Asset categories detail (preview stub)")
                    } label: {
                        Label("Manage asset categories", systemImage: "wallet.pass")
                    }
                    NavigationLink {
                        Text("Pinned categories (preview stub)")
                    } label: {
                        Label("Pinned categories", systemImage: "pin")
                    }
                }

                Section("Data") {
                    Button {} label: {
                        Label("Export data", systemImage: "square.and.arrow.up")
                            .foregroundStyle(.primary)
                    }
                    Button {} label: {
                        Label("Import data", systemImage: "square.and.arrow.down")
                            .foregroundStyle(.primary)
                    }
                    Button {} label: {
                        Label("Import from CSV or Excel", systemImage: "tablecells")
                            .foregroundStyle(.primary)
                    }
                }

                Section("Automation") {
                    NavigationLink {
                        Text("Automate transactions (preview stub)")
                    } label: {
                        Label("Automate transactions", systemImage: "sparkles")
                    }
                }

                Section("Premium") {
                    NavigationLink {
                        Text("Paywall preview (stub)")
                    } label: {
                        Label("Get Mooney Pro", systemImage: "crown.fill")
                            .foregroundStyle(MooneyBrand.accent)
                    }
                    Button {} label: {
                        Label("Restore purchases", systemImage: "arrow.clockwise")
                            .foregroundStyle(.primary)
                    }
                }

                Section("About") {
                    LabeledContent("Version") {
                        Text("26.09.02").foregroundStyle(.secondary)
                    }
                    Link(destination: URL(string: "https://andriybobchuk.github.io/Mooney/privacy-policy.html")!) {
                        Label("Privacy policy", systemImage: "hand.raised")
                    }
                    Link(destination: URL(string: "https://apple.com/legal/internet-services/itunes/dev/stdeula/")!) {
                        Label("Terms of use", systemImage: "doc.text")
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(MooneyBrand.background)
            .navigationTitle("Settings")
        }
    }
}

// ============================================================================
// MARK: - Shared building blocks
// ============================================================================

@available(iOS 26.0, *)
private struct SectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(.subheadline)
            .fontWeight(.semibold)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 4)
    }
}

@available(iOS 26.0, *)
private enum MooneyBrand {
    static let accent = Color(red: 0.19, green: 0.63, blue: 0.94)
    static let background = LinearGradient(
        colors: [Color(red: 0.98, green: 0.98, blue: 1.0), Color(red: 0.94, green: 0.96, blue: 1.0)],
        startPoint: .top,
        endPoint: .bottom
    )
}

// ============================================================================
// MARK: - Mock data models
// ============================================================================

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
private struct MockAccount: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let category: String
    let balance: Double
    let tint: Color
    let isPrimary: Bool
    var balanceFormatted: String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.maximumFractionDigits = 0
        f.groupingSeparator = ","
        let n = f.string(from: NSNumber(value: balance)) ?? "\(Int(balance))"
        return "\(n) zł"
    }
}

@available(iOS 26.0, *)
private struct MockGoal: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let subtitle: String
    let saved: Double
    let target: Double
    var progress: Double { min(saved / target, 1.0) }
    private func fmt(_ v: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.maximumFractionDigits = 0
        f.groupingSeparator = ","
        return f.string(from: NSNumber(value: v)) ?? "\(Int(v))"
    }
    var savedFormatted: String { "\(fmt(saved)) zł" }
    var targetFormatted: String { "\(fmt(target)) zł" }
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

    static let accounts: [MockAccount] = [
        MockAccount(emoji: "🏦", title: "Bank", category: "Bank account", balance: 8_240, tint: .blue, isPrimary: true),
        MockAccount(emoji: "💵", title: "Cash", category: "Cash", balance: 420, tint: .green, isPrimary: false),
        MockAccount(emoji: "💰", title: "Savings", category: "Bank account", balance: 15_600, tint: .yellow, isPrimary: false),
        MockAccount(emoji: "📈", title: "Investments", category: "Stocks", balance: 9_800, tint: .purple, isPrimary: false),
        MockAccount(emoji: "🏠", title: "Flat", category: "Real estate", balance: 380_000, tint: .indigo, isPrimary: false),
        MockAccount(emoji: "🚗", title: "Car", category: "Vehicle", balance: 45_000, tint: .red, isPrimary: false),
    ]

    static let goals: [MockGoal] = [
        MockGoal(emoji: "🏝️", title: "Vacation to Japan", subtitle: "Two weeks in spring", saved: 8_400, target: 12_000),
        MockGoal(emoji: "🛡️", title: "Emergency Fund", subtitle: "6 months of expenses", saved: 18_500, target: 30_000),
        MockGoal(emoji: "🏡", title: "New Kitchen", subtitle: "Renovation savings", saved: 6_200, target: 25_000),
    ]
}
