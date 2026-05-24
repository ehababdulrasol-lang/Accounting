# Compose UI System & Design Tokens Guide

## 🎨 Three-Layer Token Architecture
1. **Layer 1: Primitive Tokens (Values)**: Raw assets such as Hex colors, specific pixel values, or font names.
2. **Layer 2: Semantic Tokens (Purpose)**: Defines what a primitive token is used for (e.g. `ColorPrimary`, `CardElevation`).
3. **Layer 3: Component Tokens (Scoped)**: Custom tokens specific to individual components (e.g. `AccountCardBackground = SemanticTokens.SurfaceVariant`).

---

## 📱 Material 3 Theme Integration
Always utilize `MaterialTheme` color schemas, typography assets, and shape containers.
- **Dynamic Light/Dark Color Schemes**: Supported natively on Android 12+.
- **Tonal Elevation**: In Material 3, surface elevation increases contrast automatically using overlay colors.

---

## 📏 Spacing & Layout Rhythm
- **The 8dp Rule**: Margin, padding, and spacing distances between visual blocks must follow the 8dp grid system (4dp, 8dp, 16dp, 24dp, 32dp).
- **Responsive Layout Classes**: Use WindowSizeClasses (`Compact`, `Medium`, `Expanded`) for adaptive UI scaling.
- **Width Insets & Contraints**: Keep a maximum width of `600.dp` centered horizontally on tablet screens to avoid stretching layouts.

---

## ✨ Skeleton Shimmer Transition
For loading states, use a custom linear gradient transition rather than static loaders to give a high-fidelity visual experience:

```kotlin
@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(10f, 10f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}
```

---

## 🧩 Premium UI Components (Material 3)
1. **Cards**: Use `OutlinedCard` for standard item rows, and `ElevatedCard` for highlighting major headers or actionable cards.
2. **FABs**: Align Floating Action Buttons at the bottom-right corner of a `Scaffold` container to anchor clean actions.
3. **Indicators**: Implement explicit, responsive animations like ripples on click with minimum 48dp touch targets.
