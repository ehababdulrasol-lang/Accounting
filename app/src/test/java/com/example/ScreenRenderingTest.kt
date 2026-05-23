package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel
import com.example.MainLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ScreenRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEveryScreenTabRendering() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LedgerViewModel(app)
        
        // Wait until DB is seeded
        var isSeeding = viewModel.isSeeding.value
        var count = 0
        while (isSeeding && count < 100) {
            Thread.sleep(100)
            isSeeding = viewModel.isSeeding.value
            count++
        }

        composeTestRule.setContent {
            MyApplicationTheme {
                MainLayout(viewModel)
            }
        }
        composeTestRule.waitForIdle()

        val allTabs = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19)
        for (tab in allTabs) {
            println("Testing rendering of tab: $tab")
            viewModel.navigateToTabFlow.tryEmit(tab)
            composeTestRule.waitForIdle()
        }
        println("All screen tabs rendered successfully without crash!")
    }
}
