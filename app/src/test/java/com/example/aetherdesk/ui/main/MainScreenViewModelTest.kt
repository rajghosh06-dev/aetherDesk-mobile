package com.example.aetherdesk.ui.main

import com.example.aetherdesk.data.DataRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    assertEquals(viewModel.uiState.first(), MainScreenUiState.Loading)
  }

  @Test
  fun uiState_onItemSaved_isDisplayed() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    // Note: StateFlow testing typically requires advanceUntilIdle() or similar, 
    // but we can check the flow behavior.
    val items = mutableListOf<MainScreenUiState>()
    val job = launch { viewModel.uiState.collect { items.add(it) } }
    
    // allow flow to emit
    kotlinx.coroutines.test.runCurrent()
    
    assert(items.any { it is MainScreenUiState.Success })
    job.cancel()
  }

  @Test
  fun uiState_onError_isDisplayed() = runTest {
    val viewModel = MainScreenViewModel(FakeErrorRepository())
    val items = mutableListOf<MainScreenUiState>()
    val job = launch { viewModel.uiState.collect { items.add(it) } }
    
    kotlinx.coroutines.test.runCurrent()
    
    assert(items.any { it is MainScreenUiState.Error })
    job.cancel()
  }
}

private class FakeMyModelRepository : DataRepository {
  override val data: Flow<List<String>> = flow { emit(listOf("Sample")) }
}

private class FakeErrorRepository : DataRepository {
  override val data: Flow<List<String>> = flow { throw RuntimeException("Fake error") }
}
