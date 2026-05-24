# 🤖 Project Coding Instructions & Standards

This file contains the persistent system instructions and rules that the AI coding agent must follow for all code generation and structural tasks. It builds on the `ayush016/android-lead-agent-skills` framework as the core architectural and visual foundation of this application.

---

## 🏛️ 1. Architecture & State Management Guidelines
- **MVVM Pattern with Single Source of Truth**: Organize all business logic under `LedgerViewModel` and use structured database repositories.
- **Unidirectional Data Flow**: Modeled screens must read state solely from lifecycle-aware flows (`collectAsStateWithLifecycle()`).
- **No Direct Variable Mutability in Compose**: Maintain state within ViewModel Flow targets, and signal user events via strict methods.
- **Asynchronous Coroutine Dispatching**: Thread heavy calculations via `withContext(Dispatchers.IO)` or `Dispatchers.Default`; suspend database transactions appropriately.

---

## 🎨 2. Extraordinary UI Mandate (Jetpack Compose & Material 3)
- **High-Contrast Dark Theme Compatibility**: Ensure all custom colors use native Material 3 theme roles (`primary`, `primaryContainer`, `surface`, `onSurfaceVariant`, etc.). 
- **The 8dp Visual Rhythm**: Distribute layout and padding in multiples of `8dp` (or `4dp` for half-measures) to keep spacious, balanced visual compositions.
- **Tactile Hover/Active Animations**: Apply `AnimatedVisibility` and state-aware sliding transitions during tab changes or item entry. Avoid static transitions.
- **Accessible Touch Surfaces**: Ensure conversational and structural components measure at least a standard `48dp × 48dp` target.

---

## 🏗️ 3. Core References Index
Refer to these markdown resource outlines under `/references/` before modifying code blocks:
- **`SKILL.md`**: Guide to core engineering principles and quick decision gates.
- **`architecture.md`**: Detailed representations of sealed UI states, viewmodels, and flow collections.
- **`compose-ui-system.md`**: Tokens, theme setups, grid spacing, responsive layout classes, and skeleton loaders.
- **`motion-and-animation.md`**: Staggered list entries, infinite transitions, and tab-sliding layout transformations.
- **`theming-and-color.md`**: Deep dive into Material 3's 29 color roles and dynamic contrast ratios.
- **`accessibility.md`**: Focus management, touch targets, and semantic TalkBack properties.
- **`testing.md`**: Fast JVM tests (Robolectric) and screenshot visual verification guides (Roborazzi).
