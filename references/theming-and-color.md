# Theming & Color Guide

Rely on Material Design 3 token roles for consistent dark themes and accessible color distribution.

---

## 🎨 All 29 Color Roles (Core Groups)
1. **Primary Group**: For primary focus elements. High contrast.
   - `primary` / `onPrimary`: Main background and text for primary buttons.
   - `primaryContainer` / `onPrimaryContainer`: Large containers like header blocks and selected menu items.
2. **Secondary Group**: Supporting visual layout items. Less prominent than primary.
   - `secondary` / `onSecondary`: Used for styling secondary highlights and filters.
   - `secondaryContainer`: Perfect background color for dynamic chips inside rows.
3. **Tertiary Group**: Contrast highlights. Complementary opposite of Primary.
   - `tertiary` / `onTertiary`: Used extensively to signal alternative calculations or special status badges.
4. **Surface Group**: The solid foundation of our application screens.
   - `surface` / `onSurface`: Main background and body text.
   - `surfaceVariant` / `onSurfaceVariant`: Form fields, text areas, search cards.
5. **Outline Group**: Layout containers and borders.
   - `outline`: Visible border borders around outlined cards, input fields, and tab dividers.

---

## 🌗 Dynamic Contrast Rules
- **NEVER** use raw hex strings inside screen composites. Use color containers with matching content roles:
  - Text on `primary` must use `onPrimary`.
  - Text on `primaryContainer` must use `onPrimaryContainer`.
  - Text on `surface` must use `onSurface`.
- When reducing body opacity, use `onSurface.copy(alpha = 0.6f)` or explicit variant tokens like `onSurfaceVariant` instead of hardcoding light grey colors.

---

## 🛠️ Theme Assembly File Pattern
Review class setups inside `ui/theme/Theme.kt`:
```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surface = Color(0xFF1C1B1F)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color(0xFFFFFBFE)
)
```
Ensure dark theme is calculated logically, keeping true colors clean, readable, and highly pleasing to view during late hours.
