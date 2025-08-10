# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mooney is a Kotlin Multiplatform personal finance management app using Compose Multiplatform, targeting Android and iOS. The app helps users track transactions, manage accounts, and analyze spending patterns.

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

## Project Architecture

### Core Architecture Patterns
- **Clean Architecture**: Organized in layers (data, domain, presentation)
- **MVVM**: ViewModels with Compose UI following unidirectional data flow
- **Repository Pattern**: Data access through repository interfaces
- **Use Case Pattern**: Business logic encapsulated in dedicated use cases
- **Dependency Injection**: Using Koin for DI across all platforms

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

### Database Schema
The app uses Room database (`mooney.db`) with two main entities:
- `TransactionEntity`: Financial transactions with categories, amounts, dates
- `AccountEntity`: User accounts with balances and currency information

### Dependency Injection Setup
- `sharedModule`: Contains common dependencies (repository, use cases, ViewModels, database)
- `platformModule`: Platform-specific implementations (database factory, HTTP client engines)
- Initialize with `initKoin()` in platform-specific entry points

### Business Logic Organization
Use cases handle specific business operations:
- Transaction management (Add/Delete/Get)
- Account management (Add/Delete/Get) 
- Analytics calculations (monthly analytics, net worth, totals)
- Currency management and conversions
- UI data transformations

### Navigation Structure
Three main screens accessible via bottom navigation:
1. **Transactions**: Transaction list and management
2. **Accounts**: Account overview and management  
3. **Analytics**: Spending analysis and statistics

## Development Notes

### Package Structure Consistency
The codebase has some legacy package references (`com.plcoding.bookpedia`) in build configuration that should be updated to `com.andriybobchuk.mooney` when making changes to build files.

### Room Database Location
Room schema files are stored in `composeApp/schemas/` for database versioning and migration support.

### Platform-Specific Implementations
- Android: Uses OkHttp for networking, Android-specific database factory
- iOS: Uses Darwin HTTP client, iOS-specific database factory
- Both platforms share the same core business logic and UI components