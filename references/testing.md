# Testing — Strategy, Unit & Screenshot Tests

We run rapid unit tests on the JVM, avoiding heavy emulator overhead, to prevent UI and logic regressions.

---

## 🏔️ The Testing Pyramid
1. **Unit Tests (ViewModel & Data Entities)**: Validates state streams and data validation constraints. Excludes UI layout logic.
2. **Robolectric Integration Tests (Screens & Layouts)**: Runs local headless JVM tests representing Compose views.
3. **Roborazzi Screenshot Tests (Visual Checks)**: Validates pixels and exact design alignment, ensuring no UI elements clip or distort during updates.

---

## 🧪 ViewModel Unit Testing Example
Exposes state changes and checks outcomes of transactional actions:

```kotlin
@Test
fun testVoucherPostingSavesToLedger() = runTest {
    val repository = FakeLedgerRepository()
    val viewModel = LedgerViewModel(repository)
    
    viewModel.saveVoucher(sampleVoucher)
    viewModel.postActiveVoucher(sampleVoucher.id)
    
    val state = viewModel.uiState.value
    assert(state is VoucherUiState.Posted)
}
```

---

## 📸 Roborazzi Screenshot Testing Layouts
Write headless screenshot checks directly in your test suite:

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class MainScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDashboardScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen()
            }
        }
        captureRoboImage("screenshots/dashboard_screen.png")
    }
}
```

---

## ⚙️ Quick Gradle Test Commands
- **Run Standard Tests**: `gradle :app:testDebugUnitTest`
- **Verify Design Layout Pixels**: `gradle :app:verifyRoborazziDebug`
- **Record Reference Screenshots**: `gradle :app:recordRoborazziDebug`
