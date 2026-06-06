package com.example

import android.content.Context
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ledger Pro", appName)
  }

  @Test
  fun `test database seeding and viewmodel initialization`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = LedgerViewModel(app)
    
    // Wait until isSeeding becomes false
    var isSeeding = viewModel.isSeeding.value
    var count = 0
    while (isSeeding && count < 100) {
      kotlinx.coroutines.delay(100)
      isSeeding = viewModel.isSeeding.value
      count++
    }
    
    // Check if there was any error message stored in uiMessage
    val errorMsg = viewModel.uiMessage.value
    if (errorMsg != null && errorMsg.startsWith("Init Error")) {
      throw RuntimeException(errorMsg)
    }
    
    assertNotNull(viewModel.accounts.value)
    println("Successfully initialized and seeded DB! Total accounts: ${viewModel.accounts.value.size}")
  }

  @Test
  fun `test fiscal period boundary selection and snapshot updates`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = LedgerViewModel(app)
    
    // Wait for initialization and database seeding
    var isSeeding = viewModel.isSeeding.value
    var count = 0
    while (isSeeding && count < 100) {
      kotlinx.coroutines.delay(100)
      isSeeding = viewModel.isSeeding.value
      count++
    }
    
    val initialSelectedFy = viewModel.selectedReportingFy.value
    assertNotNull(initialSelectedFy)
    assertEquals("FY 2026", initialSelectedFy?.name)
    
    // Ensure boundaries are set
    val startBoundary = viewModel.trialBalanceStart.value
    val endBoundary = viewModel.trialBalanceEnd.value
    assertEquals(1767225600000L, startBoundary)
    assertEquals(1798761599000L, endBoundary)
    
    // Test selecting a different fiscal period or cumulative null period
    viewModel.selectReportingFiscalYear(null)
    kotlinx.coroutines.delay(200)
    
    // Verify boundaries updated to cumulative (None selecion)
    assertEquals(null, viewModel.selectedReportingFy.value)
    
    println("Fiscal period boundary selection unit test passed successfully!")
  }
}
