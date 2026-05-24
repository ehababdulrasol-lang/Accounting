# Architecture — State, ViewModel & Navigation

An enterprise-grade Android application must follow clean separation of concerns, robust state preservation, and clear single sources of truth.

---

## 🏗️ Architectural Pattern: MVVM + UDF
We use **Model-View-ViewModel (MVVM)** powered by **Unidirectional Data Flow (UDF)**.
- **State flows down**: The `ViewModel` holds and produces the UI State as a single, immutable source.
- **Events flow up**: The Composables trigger actions (methods) on the `ViewModel`.

---

## 🐳 UI State Modelling
UI state must represent what the screen shows at any instant. Avoid exposing multiple isolated `MutableStateFlow`s. Group them into a sealed interface representing distinct screen states:

```kotlin
sealed interface AccountUiState {
    object Loading : AccountUiState
    data class Success(
        val accounts: List<Account>,
        val selectedAccount: Account? = null,
        val isSaving: Boolean = false
    ) : AccountUiState
    data class Error(val message: String) : AccountUiState
}
```

---

## ⚡ One-Time Events (Navigation/Toasts)
Do not store one-time events (e.g. showing a SnackBar or navigating) in persistent UI State. Use `Channel`s or `SharedFlow` with `replay = 0` to avoid repeated triggers on orientation changes:

```kotlin
private val _eventChannel = Channel<UiEvent>(Channel.BUFFERED)
val eventFlow = _eventChannel.receiveAsFlow()

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class Navigate(val route: String) : UiEvent
}
```

---

## 🧪 Actions Pattern
Instead of direct state mutation or complex action structures, expose descriptive, typed public functions on the ViewModel representing user intent:

```kotlin
class AccountViewModel(private val repository: LedgerRepository) : ViewModel() {
    
    fun onAddAccount(code: String, name: String, type: AccountType) {
        viewModelScope.launch {
            repository.insertAccount(code, name, type)
        }
    }
}
```

---

## 🔒 Safe Flow Collection in Compose
Never use `.collectAsState()` in Jetpack Compose, as it is not lifecycle-aware and continues collecting flows when the app is in the background, wasting battery and memory.
Always use **`collectAsStateWithLifecycle()`**:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```
*Note: This requires `androidx.lifecycle:lifecycle-runtime-compose` in dependencies.*
