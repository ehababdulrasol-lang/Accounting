# Shared Element Transitions Guide

Use Compose 1.7+ `SharedTransitionLayout` to coordinate spatial continuity when animating elements between list and detail layouts.

---

## 🛠️ Mental Model: The Three-Layer Scope
1. **SharedTransitionLayout (Coordinator)**: Outermost container element that coordinates the overlay and geometry animations for children.
2. **SharedTransitionScope (Receiver Scope)**: Provided to inner components where shared actions are applied.
3. **AnimatedVisibilityScope (Bound Handle)**: Scoped to specific screens within a transition (like Navigation compose backstacks) to match geometry targets.

---

## 🏎️ Core Functions: sharedElement() vs sharedBounds()
- **`sharedElement()`**: Used when animating exact matching elements (e.g. an image moving from a list item to its detail header).
- **`sharedBounds()`**: Used when animating differing content within the same spatial region (e.g. expanding a compact card row outward into a full-screen details box).

---

## 🧵 Threading Scopes Pattern
Use an elegant `CompositionLocal` setup to provide scopes across deeply nested Compose structures cleanly:

```kotlin
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
```

In your composables, apply transitions by referencing:
```kotlin
val sharedScope = LocalSharedTransitionScope.current ?: return
val animatedScope = LocalAnimatedVisibilityScope.current ?: return

Box(
    modifier = Modifier
        .sharedElement(
            rememberSharedContentState(key = "card_${item.id}"),
            animatedVisibilityScope = animatedScope
        )
)
```

---

## ⚠️ Important Pitfalls
1. **Key Mismatch**: Make sure keys match exactly in both screens (e.g. `"img_12"` on list screen, and `"img_12"` on detail screen).
2. **Missing Scopes**: Triggering bounds animations outside of detailed parent scopes will cause program crashes. Ensure scopes are threaded.
