# Implementation Plan - SpendWise Android App

SpendWise is a comprehensive expense-splitting Android application built with Kotlin, Jetpack Compose, MVVM architecture, and Firebase (Auth, Firestore).

## User Review Required

> [!IMPORTANT]
> **Firebase Configuration**: To enable live authentication and Firestore backend sync, a `google-services.json` file from Firebase Console is required. We will also include a robust fallback/mock in-memory & local simulation mode if Firebase isn't configured yet, ensuring the app builds and runs successfully out of the box.

- **Theme**: Purple and Teal Material 3 theme supporting dynamic dark/light mode.
- **Architecture**: Clean architecture separating `data`, `domain`, and `ui` layers.

## Open Questions

- None. Proceeding with the comprehensive feature set requested.

## Proposed Changes

### Build Configuration & Dependencies
#### [MODIFY] [libs.versions.toml](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/gradle/libs.versions.toml)
- Add versions and library aliases for Compose, Lifecycle, Navigation Compose, Firebase BoM (Auth, Firestore), Coroutines, and Google Sign-In / Credential Manager.

#### [MODIFY] [build.gradle.kts (project)](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/build.gradle.kts)
- Add google-services plugin classpath if needed.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/build.gradle.kts)
- Enable Jetpack Compose, add dependencies for Compose, Material 3, Navigation, Firebase BoM, Coroutines, and Google Play Services.

---

### Data Layer (`com.example.spendwise.data`)
#### [NEW] [Models](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/data/model/Models.kt)
- Define data classes: `User`, `Group`, `Expense`, `Split` (equal, exact, percentage, shares), `Settlement`, `ActivityItem`, `ExpenseCategory`, `Currency`.

#### [NEW] [Repositories](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/data/repository/Repositories.kt)
- `AuthRepository`: Handle Firebase Auth (Email/Password, Google, logout).
- `UserRepository`: User profiles, friends management (add by email/phone).
- `GroupRepository`: Create groups, add/remove members, fetch user groups.
- `ExpenseRepository`: Add, edit, delete expenses with various split types.
- `ActivityRepository`: Activity feed of all expenses and settlements.

#### [NEW] [Firestore Rules](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/firestore.rules)
- Firestore security rules ensuring users only see their own groups and expenses.

---

### Domain Layer (`com.example.spendwise.domain`)
#### [NEW] [Debt Simplification & Calculations](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/domain/CalculationUtils.kt)
- Balance calculation algorithm (who owes whom per group and overall).
- Debt simplification algorithm (minimizing number of transactions).
- Split calculators (Equal, Exact, Percentage, Shares).
- Multi-currency conversion helpers.

---

### UI Layer (`com.example.spendwise.ui`)
#### [NEW] [Theme](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/ui/theme/Theme.kt)
- Material 3 Purple & Teal color schemes for light and dark modes.

#### [NEW] [Navigation & Scaffolding](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/ui/navigation/SpendWiseNavGraph.kt)
- Bottom navigation bar: Groups, Friends, Activity, Account.
- NavGraph routing for Auth, Group Detail, Add Expense, Settle Up, Friend Detail, etc.

#### [NEW] [Screens](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/ui/screens/)
- `AuthScreen`: Login / Sign Up with Email and Google.
- `GroupsScreen` & `GroupDetailScreen`: Groups list, create group, member management, balances, simplify debts.
- `FriendsScreen` & `FriendDetailScreen`: Friends list, add friend by email/phone, balances.
- `AddExpenseScreen` & `ExpenseDetailScreen`: Add/edit expense with title, amount, payer, categories, and split modes (Equal, Exact, Percentage, Shares).
- `SettleUpScreen`: Record payments between users.
- `ActivityScreen`: Feed of all expense and settlement history.
- `AccountScreen`: Profile management (name, photo, default currency), push notification settings, Firebase setup guide.

---

### Application & Manifest
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/AndroidManifest.xml)
- Add internet permissions and configure application class.

#### [NEW] [SpendWiseApp.kt](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/SpendWiseApp.kt)
- Application class for Firebase initialization.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/amrut/AndroidStudioProjects/spendwise/app/src/main/java/com/example/spendwise/MainActivity.kt)
- Set up Compose content and NavHost.

## Verification Plan

### Automated Tests
- Run `./gradlew test` and `./gradlew assembleDebug` to verify compilation and unit tests.

### Manual Verification
- Build and run app in emulator / device to verify UI navigation, auth, group creation, expense splitting, and balances.
