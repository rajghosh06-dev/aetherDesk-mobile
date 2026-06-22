package com.rajghosh.aetherdesk.ui.main

import com.rajghosh.aetherdesk.data.DataRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class MainScreenViewModelTest {

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    assertEquals(MainScreenUiState.Success(listOf("Sample")), viewModel.uiState.first())
  }

  @Test
  fun uiState_onItemSaved_isDisplayed() = runTest(UnconfinedTestDispatcher()) {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    
    val items = mutableListOf<MainScreenUiState>()
    val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { items.add(it) } }
    
    // allow flow to emit
    advanceUntilIdle()
    
    assert(items.any { it is MainScreenUiState.Success })
    job.cancel()
  }

  @Test
  fun uiState_onError_isDisplayed() = runTest(UnconfinedTestDispatcher()) {
    val viewModel = MainScreenViewModel(FakeErrorRepository())
    val items = mutableListOf<MainScreenUiState>()
    val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { items.add(it) } }
    
    advanceUntilIdle()
    
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
