---
description: Scaffold a new feature with ViewModel, Screen, and tests
argument-hint: FeatureName
---

# New Feature Scaffolding

Creates the basic structure for a new feature in Mooney.

## Feature: $1

I'll help you create a new feature called **$1**. This will include:

### 📁 Files to Create:

1. **Domain Layer**
   - `mooney/domain/$1.kt` - Domain model
   - `mooney/domain/usecase/${1}UseCase.kt` - Business logic

2. **Data Layer**
   - `mooney/data/${1}Repository.kt` - Repository interface
   - `mooney/data/${1}RepositoryImpl.kt` - Repository implementation

3. **Presentation Layer**
   - `mooney/presentation/${1,,}/${1}Screen.kt` - Compose UI
   - `mooney/presentation/${1,,}/${1}ViewModel.kt` - State management
   - `mooney/presentation/${1,,}/${1}State.kt` - UI state
   - `mooney/presentation/${1,,}/${1}Action.kt` - User actions
   - `mooney/presentation/${1,,}/${1}Event.kt` - One-time events

4. **Tests**
   - `test/commonTest/kotlin/.../mooney/${1}ViewModelTest.kt`
   - `test/commonTest/kotlin/.../mooney/${1}UseCaseTest.kt`

### 🔧 Configuration Updates:

1. Register in `di/Modules.kt`:
   - Add ViewModel to `presentationModule`
   - Add UseCase to `domainModule`
   - Add Repository to `dataModule`

2. Update `app/NavigationHost.kt`:
   - Add new route
   - Add composable entry

3. Add string resources:
   - Screen title
   - Labels and messages

### 📋 Implementation Checklist:

- [ ] Create domain model
- [ ] Implement use case
- [ ] Create repository
- [ ] Build ViewModel with state management
- [ ] Design UI with Compose
- [ ] Add @Preview composables
- [ ] Write unit tests
- [ ] Register in DI
- [ ] Wire up navigation
- [ ] Test dark mode
- [ ] Add accessibility labels
- [ ] Verify error handling

Would you like me to start creating these files for the **$1** feature?