package com.example.opssync.ui.state

// ─── Generic UI State ─────────────────────────────────────────
// Wraps every async operation in the ViewModel.
// Compose screens observe these and render accordingly.
//
// Usage in ViewModel:
//   var incidentsState by mutableStateOf<UiState<List<IncidentDto>>>(UiState.Idle)
//
// Usage in Composable:
//   when (val state = viewModel.incidentsState) {
//       is UiState.Loading -> CircularProgressIndicator()
//       is UiState.Success -> IncidentList(state.data)
//       is UiState.Error   -> ErrorBanner(state.message)
//       is UiState.Idle    -> {} // initial — show nothing
//   }

sealed class UiState<out T> {

    // No operation in progress yet (initial state)
    object Idle : UiState<Nothing>()

    // Request is in flight — show loading spinner
    object Loading : UiState<Nothing>()

    // Request succeeded — data is ready
    data class Success<T>(val data: T) : UiState<T>()

    // Request failed — display message to user
    data class Error(val message: String) : UiState<Nothing>()
}

// ─── Convenience extensions ───────────────────────────────────

val <T> UiState<T>.isLoading: Boolean
    get() = this is UiState.Loading

val <T> UiState<T>.isSuccess: Boolean
    get() = this is UiState.Success

// Unwrap data safely — returns null if not in Success state
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data
