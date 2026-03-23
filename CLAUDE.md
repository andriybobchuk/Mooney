# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mooney is a Kotlin Multiplatform personal finance management app using Compose Multiplatform, targeting Android and iOS. The app helps users track transactions, manage accounts, and analyze spending patterns.

## Architecture Overview

The app uses **MVVM** pattern with **Jetpack Compose** for UI:

- **ViewModel**: Holds immutable `UiState`, processes user actions, emits one-time events
- **Screen (Composable)**: Pure UI that receives `state` and `onAction` callback
- **Repository**: Provides data access through repository interfaces
- **Use Cases**: Business logic encapsulated in dedicated use cases
- **Dependency Injection**: Using Koin for DI across all platforms

## Build Commands

```bash
# Build the project for all platforms
./gradlew build

# Build and run Android app
./gradlew :composeApp:assembleDebug

# Clean build
./gradlew clean

# Generate Room database schemas
./gradlew :composeApp:kspCommonMainKotlinMetadata
```

## Tech Debt Tracking

When you encounter tech debt while working (dead code, unused params, TODO patterns, etc.), add it to `TECH_DEBT.md` in the project root. Check this file before starting cleanup tasks.

## Git Workflow

### Commit Guidelines
- **Commit after every feature or logical piece of work** - Proactively commit completed features without waiting for explicit consent
- **NEVER use `git add .`** - Always add files explicitly to avoid committing sensitive or unnecessary files
- **Commit messages must be single-line** - Keep it concise and descriptive
- **No AI/Claude mentions** - Never reference AI assistance, Claude, or automated generation in commit messages

### Example Workflow
```bash
# Check status to see what changed
git status

# Add specific files (NEVER use git add .)
git add composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/mooney/presentation/assets/AssetsScreen.kt
git add composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/mooney/presentation/assets/AssetsViewModel.kt

# Commit with descriptive single-line message
git commit -m "Add collapsible asset categories with persistent state"
```

### Good Commit Messages
✅ "Add asset diversification calculation"
✅ "Fix text contrast in dark mode"
✅ "Implement collapsible category headers"
✅ "Update Room migration for asset categories"

### Bad Commit Messages
❌ "Updated files as requested by user"
❌ "AI-generated improvements to assets screen"
❌ "Claude helped implement this feature"
❌ Multi-line messages with detailed explanations

## File Structure & Naming

For a feature called "Assets":

```
commonMain/kotlin/com/andriybobchuk/mooney/
├── mooney/presentation/assets/
│   ├── AssetsScreen.kt                   # Main Compose UI
│   ├── AssetsViewModel.kt                # State/Actions/Events/Logic
│   └── components/                       # Feature-specific components
│       └── AssetCard.kt
├── mooney/domain/usecase/assets/
│   ├── CalculateAssetDiversificationUseCase.kt
│   └── ManageAssetCategoryOrderUseCase.kt
├── mooney/domain/
│   └── AssetCategory.kt                  # Domain models
└── mooney/data/
    └── AssetRepository.kt                # Data access

test/commonTest/kotlin/.../assets/
└── AssetsViewModelTest.kt               # ViewModel unit tests
```

**Naming Conventions:**
- ViewModel: `AssetsViewModel`
- UI State: `AssetsState` 
- Actions: `AssetsAction` (sealed interface)
- Events: `AssetsEvent` (sealed class)
- Screen: `AssetsScreen` (Composable function)
- Use Cases: `VerbNounUseCase` pattern

## Step-by-Step Implementation Guide

### 1. Create Domain Models

**File:** `mooney/domain/AssetCategory.kt`

```kotlin
enum class AssetCategory(
    val displayName: String,
    val emoji: String,
    val description: String,
    val color: Long = 0xFF6750A4,
    val riskLevel: RiskLevel = RiskLevel.MEDIUM
) {
    BANK_ACCOUNT("Bank Account", "🏦", "Traditional bank accounts"),
    CRYPTO("Cryptocurrency", "₿", "Digital assets"),
    // ... other categories
}

enum class RiskLevel { VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH }
```

### 2. Create Use Cases

**File:** `mooney/domain/usecase/assets/CalculateAssetDiversificationUseCase.kt`

```kotlin
class CalculateAssetDiversificationUseCase {
    operator fun invoke(
        accounts: List<Account>,
        exchangeRates: ExchangeRates?,
        targetCurrency: Currency
    ): AssetDiversification {
        // Business logic implementation
        return AssetDiversification(
            categoryBreakdown = categoryBreakdown,
            totalNetWorth = totalNetWorth,
            currency = targetCurrency
        )
    }
}
```

### 3. Define ViewModel Structure

**File:** `mooney/presentation/assets/AssetsViewModel.kt`

```kotlin
// UI State - immutable, data class
data class AssetsState(
    val assets: List<UiAsset> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val diversification: AssetDiversification? = null,
    val categoryOrder: List<AssetCategory> = AssetCategory.entries,
    val expandedCategories: Set<AssetCategory> = AssetCategory.entries.toSet(),
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

// Actions - sealed interface for all user interactions
sealed interface AssetsAction {
    data object Load : AssetsAction
    data object RefreshExchangeRates : AssetsAction
    data object OnNetWorthClick : AssetsAction
    data class ToggleCategoryExpansion(val category: AssetCategory) : AssetsAction
    data class UpsertAsset(
        val id: Int,
        val title: String,
        val emoji: String,
        val amount: Double,
        val currency: Currency,
        val category: AssetCategory
    ) : AssetsAction
    data class DeleteAsset(val id: Int) : AssetsAction
}

// Events - sealed class for one-time effects (navigation, dialogs)
sealed class AssetsEvent {
    data object NavigateToSettings : AssetsEvent
    data class ShowError(val message: String) : AssetsEvent
}

// ViewModel
class AssetsViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateAssetDiversificationUseCase: CalculateAssetDiversificationUseCase,
    private val manageCategoryExpansionUseCase: ManageCategoryExpansionUseCase,
    // ... other dependencies
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetsState())
    val state = _uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _uiState.value
    )

    private val _events = MutableSharedFlow<AssetsEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: AssetsAction) {
        when (action) {
            is AssetsAction.Load -> loadAssets()
            is AssetsAction.UpsertAsset -> upsertAsset(action)
            is AssetsAction.DeleteAsset -> deleteAsset(action.id)
            is AssetsAction.ToggleCategoryExpansion -> toggleCategory(action.category)
            // ... handle other actions
        }
    }

    private fun loadAssets() {
        // Implementation
    }

    private fun updateState(update: (AssetsState) -> AssetsState) {
        _uiState.update(update)
    }

    private fun sendEvent(event: AssetsEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}
```

**Key Patterns:**
- Use `updateState { it.copy(field = value) }` for state updates
- Use `viewModelScope.launch` for coroutines
- Chain operations with `onSuccess` and `onFailure` for response handling
- Use `sendEvent()` for navigation or one-time effects

### 4. Create Compose Screen

**File:** `mooney/presentation/assets/AssetsScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val events by viewModel.events.collectAsState(initial = null)
    
    // Handle one-time events
    LaunchedEffect(events) {
        when (events) {
            is AssetsEvent.NavigateToSettings -> onSettingsClick()
            is AssetsEvent.ShowError -> {
                // Show snackbar or dialog
            }
            null -> { /* No event */ }
        }
    }

    // Bottom sheets at top level (conditionally shown)
    if (state.showBottomSheet) {
        AssetBottomSheet(
            onDismiss = { viewModel.onAction(AssetsAction.HideBottomSheet) }
        )
    }

    Scaffold(
        topBar = {
            Toolbars.Primary(
                titleContent = {
                    NetWorthDisplay(
                        amount = state.totalNetWorth,
                        currency = state.totalNetWorthCurrency,
                        onClick = { viewModel.onAction(AssetsAction.OnNetWorthClick) }
                    )
                },
                actions = listOf(
                    Toolbars.ToolBarAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Refresh Exchange Rates",
                        onClick = { viewModel.onAction(AssetsAction.RefreshExchangeRates) }
                    )
                )
            )
        },
        bottomBar = bottomNavbar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(AssetsAction.ShowAddAssetSheet) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Asset")
            }
        }
    ) { paddingValues ->
        AssetsScreenContent(
            modifier = Modifier.padding(paddingValues),
            state = state,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun AssetsScreenContent(
    modifier: Modifier,
    state: AssetsState,
    onAction: (AssetsAction) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Group assets by category
        val groupedAssets = state.assets.groupBy { it.assetCategory }
        state.categoryOrder.forEach { category ->
            val categoryAssets = groupedAssets[category]
            if (categoryAssets != null) {
                item {
                    CategoryHeader(
                        category = category,
                        totalAmount = categoryAssets.sumOf { it.baseCurrencyAmount },
                        currency = state.totalNetWorthCurrency,
                        isExpanded = state.expandedCategories.contains(category),
                        onToggle = { onAction(AssetsAction.ToggleCategoryExpansion(category)) }
                    )
                }

                if (state.expandedCategories.contains(category)) {
                    items(categoryAssets) { asset ->
                        AssetCard(
                            asset = asset,
                            onEdit = { onAction(AssetsAction.EditAsset(asset)) },
                            onDelete = { onAction(AssetsAction.DeleteAsset(asset.id)) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
    }
}

// Extract complex components as private composables
@Composable
private fun CategoryHeader(
    category: AssetCategory,
    totalAmount: Double,
    currency: Currency,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    // Implementation with animations
}

@Composable
private fun AssetCard(
    asset: UiAsset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Card implementation
}

// Always include preview
@Preview(showBackground = true)
@Composable
private fun AssetsScreenPreview() {
    MaterialTheme {
        AssetsScreen(
            // Mock data for preview
        )
    }
}
```

**Key UI Patterns:**
- Use `MaterialTheme.colorScheme` for colors
- Extract reusable components as private `@Composable` functions
- Always provide `@Preview` composables
- Use `stringResource()` for all user-facing text
- Convert all user interactions to `onAction()` calls
- Use `LaunchedEffect` for handling one-time events

### 5. Navigation Integration

**File:** `app/NavigationHost.kt`

```kotlin
@Composable
fun NavigationHost(navController: NavHostController) {
    NavHost(navController = navController) {
        composable<Route.Assets> {
            val viewModel = koinViewModel<AssetsViewModel>()
            AssetsScreen(
                viewModel = viewModel,
                bottomNavbar = { BottomNavigationBar(navController, 1) },
                onSettingsClick = { navController.navigate(Route.Settings) }
            )
        }
    }
}
```

### 6. Write Tests

**File:** `test/commonTest/kotlin/.../assets/AssetsViewModelTest.kt`

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class AssetsViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val getAccountsUseCase = mockk<GetAccountsUseCase>(relaxed = true)
    private val addAccountUseCase = mockk<AddAccountUseCase>(relaxed = true)
    private lateinit var viewModel: AssetsViewModel

    @Before
    fun setup() {
        every { getAccountsUseCase() } returns flowOf(emptyList())
    }

    @Test
    fun `initial state should have empty assets list`() = runTest {
        createViewModel()
        
        assertEquals(AssetsState(), viewModel.state.value)
    }

    @Test
    fun `onAction Load should update assets list`() = runTest {
        val mockAssets = listOf(
            Account(1, "Test", 100.0, Currency.USD, "💰", AssetCategory.BANK_ACCOUNT)
        )
        every { getAccountsUseCase() } returns flowOf(mockAssets)
        
        createViewModel()
        viewModel.onAction(AssetsAction.Load)
        
        // Verify state updates
        assertTrue(viewModel.state.value.assets.isNotEmpty())
    }

    private fun createViewModel() {
        viewModel = AssetsViewModel(
            getAccountsUseCase = getAccountsUseCase,
            addAccountUseCase = addAccountUseCase,
            // ... other dependencies
        )
    }
}
```

**Testing Patterns:**
- Use `MainCoroutineRule` for coroutine testing
- Use `mockk(relaxed = true)` for minimal mocking
- Create factory method for ViewModel initialization
- Test both initial state and action handling

## Core Architecture Patterns

### Clean Architecture Layers
- **Presentation Layer**: ViewModels and Compose screens
- **Domain Layer**: Use cases and business models  
- **Data Layer**: Repositories and data sources

### Module Structure
- `commonMain/`: Shared code across all platforms
  - `app/`: Navigation and main app composable
  - `core/`: Shared utilities, database, networking, and presentation components
  - `mooney/`: Main feature module containing data, domain, and presentation layers
- `androidMain/`: Android-specific implementations
- `iosMain/`: iOS-specific implementations

### Key Technologies
- **UI**: Compose Multiplatform with Material3
- **Database**: Room with SQLite, using bundled SQLite driver
- **Networking**: Ktor client with content negotiation and logging
- **Navigation**: Jetpack Navigation Compose
- **DI**: Koin (v4.0.0)
- **Image Loading**: Coil3
- **Serialization**: kotlinx-serialization
- **State Management**: Kotlin StateFlow/SharedFlow
- **Persistence**: DataStore for preferences

### Dependency Injection Setup
- `sharedModule`: Contains common dependencies (repository, use cases, ViewModels, database)
- `platformModule`: Platform-specific implementations (database factory, HTTP client engines)
- Initialize with `initKoin()` in platform-specific entry points

## Available Reusable Components

### Core Components
**Location:** `core/presentation/`

```kotlin
// Toolbars
Toolbars.Primary(
    titleContent: @Composable () -> Unit,
    actions: List<Toolbars.ToolBarAction> = emptyList(),
    scrollBehavior: TopAppBarScrollBehavior? = null
)

// Theme management
MaterialTheme.colorScheme.* // Use for colors
MaterialTheme.typography.* // Use for text styles
```

### Input Components
```kotlin
// Text fields
OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

### Cards and Layout
```kotlin
Card(
    modifier: Modifier = Modifier,
    colors: CardDefaults.cardColors(),
    shape: RoundedCornerShape(12.dp)
) { content }

LazyColumn(
    modifier: Modifier.fillMaxSize(),
    verticalArrangement: Arrangement.spacedBy(8.dp)
) { content }
```

## Common Patterns

### State Management Pattern

**Immutable State:**
```kotlin
data class FeatureState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Update with copy
private fun updateState(update: (FeatureState) -> FeatureState) {
    _uiState.update(update)
}
```

### Event Handling Pattern

**One-Time Events:**
```kotlin
sealed class FeatureEvent {
    data object NavigateBack : FeatureEvent()
    data class ShowMessage(val message: String) : FeatureEvent()
}

// In ViewModel
private fun sendEvent(event: FeatureEvent) {
    viewModelScope.launch {
        _events.emit(event)
    }
}

// In Composable
LaunchedEffect(events) {
    when (events) {
        is FeatureEvent.NavigateBack -> navController.popBackStack()
        is FeatureEvent.ShowMessage -> showSnackbar(events.message)
    }
}
```

### Form Validation Pattern

```kotlin
private fun updateValidation() {
    val isValid = with(uiState.value) {
        field1.isNotBlank() &&
        field2.isNotBlank() &&
        amount > 0
    }
    updateState { it.copy(isFormValid = isValid) }
}
```

### Persistence Pattern

**DataStore Usage:**
```kotlin
class ManagePreferencesUseCase(
    private val dataStore: DataStore<Preferences>
) {
    private val PREFERENCE_KEY = stringPreferencesKey("preference_key")
    
    fun getPreference(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[PREFERENCE_KEY] ?: "default_value"
        }
    }

    suspend fun savePreference(value: String) {
        dataStore.edit { preferences ->
            preferences[PREFERENCE_KEY] = value
        }
    }
}
```

### Error Handling Patterns

**Repository Pattern with Result:**
```kotlin
suspend fun fetchData(): Result<Data, Error> = try {
    val data = api.getData()
    Result.Success(data)
} catch (e: Exception) {
    Result.Error(NetworkError(e.message))
}

// In ViewModel
private fun loadData() {
    viewModelScope.launch {
        updateState { it.copy(isLoading = true) }
        
        when (val result = repository.fetchData()) {
            is Result.Success -> {
                updateState { 
                    it.copy(
                        data = result.data,
                        isLoading = false,
                        error = null
                    )
                }
            }
            is Result.Error -> {
                updateState { 
                    it.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            }
        }
    }
}
```

## Implementation Checklist

When implementing a new feature:

- [ ] Create domain models in `mooney/domain/`
- [ ] Create use cases in `mooney/domain/usecase/`
- [ ] Create package in `mooney/presentation/`
- [ ] Define `UiState` (immutable data class)
- [ ] Define `Actions` (sealed interface)
- [ ] Define `Events` (sealed class for one-time effects)
- [ ] Implement `ViewModel` with `onAction()` method
- [ ] Create Compose `Screen` with state + onAction parameters
- [ ] Add `@Preview` composables
- [ ] Update navigation in `NavigationHost`
- [ ] Add dependency injection in `Modules.kt`
- [ ] Write unit tests for ViewModel
- [ ] Test navigation flow manually
- [ ] Verify error handling and loading states
- [ ] Check accessibility (content descriptions, labels)

## Key Dos and Don'ts

### ✅ DO:
- Use immutable `UiState` with `copy()` for updates
- Use `collectAsState()` for state observation
- Extract complex components as private `@Composable` functions
- Use `MaterialTheme` for all colors, typography, and spacing
- Use `stringResource()` for all user-facing text
- Use `viewModelScope.launch` for coroutines in ViewModel
- Write unit tests with proper state assertions
- Handle both success and error cases
- Use `LaunchedEffect` for one-time events in Composables
- Follow Clean Architecture principles

### ❌ DON'T:
- Don't mutate state directly (always use `updateState { }`)
- Don't use `var` in `UiState` (use `val` only)
- Don't perform side effects directly in Composables
- Don't forget to handle loading and error states
- Don't forget to add `@Preview` composables
- Don't use hardcoded strings (use string resources)
- Don't block main thread (use suspending functions)
- Don't forget dependency injection registration
- Don't skip unit tests for ViewModels
- Don't mix platform-specific code in commonMain

## Database Schema

The app uses Room database (`mooney.db`) with entities:
- `TransactionEntity`: Financial transactions with categories, amounts, dates
- `AccountEntity`: User accounts with balances, currency, and asset categories
- `CategoryUsageEntity`: Category usage tracking
- `GoalEntity`: Financial goals
- `GoalGroupEntity`: Goal groupings

### Migration Pattern
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ... ADD COLUMN ...")
    }
}
```

## Platform-Specific Implementations
- Android: Uses OkHttp for networking, Android-specific database factory
- iOS: Uses Darwin HTTP client, iOS-specific database factory
- Both platforms share the same core business logic and UI components

## Development Notes

### Package Structure
Follow the established pattern:
```
com.andriybobchuk.mooney/
├── app/                     # Navigation, main app
├── core/                    # Shared utilities
│   ├── data/               # Database, networking
│   ├── domain/             # Core business models
│   └── presentation/       # UI components, theme
└── mooney/                 # Feature modules
    ├── data/              # Repositories, data sources
    ├── domain/            # Use cases, business models
    └── presentation/      # ViewModels, screens
```

### Room Database Location
Room schema files are stored in `composeApp/schemas/` for database versioning and migration support.

### Debugging Tips

**State Not Updating:**
- Check that you're using `updateState { it.copy(field = value) }`
- Verify StateFlow is being collected with `collectAsState()`
- Ensure state class properties are immutable

**Navigation Not Working:**
- Verify route exists in `NavigationHost`
- Check that events are properly emitted and collected
- Use type-safe navigation with sealed classes

**DI Issues:**
- Check module registration in `Modules.kt`
- Verify all dependencies are provided
- Use `single` vs `factory` appropriately

## Examples in Codebase

Reference implementations:

**Simple Feature (single screen):**
- `mooney/presentation/analytics/` - Analytics screen with charts

**Complex Feature (multiple screens, persistence):**
- `mooney/presentation/assets/` - Asset management with categories, persistence

**State Management:**
- `mooney/presentation/transaction/` - Transaction form with validation

Study these examples when implementing similar patterns.