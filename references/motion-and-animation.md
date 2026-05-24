# Motion & Animation Guide

Animations are not cosmetics; they are functional feedback indicating boundaries, spatial layouts, and interface changes.

---

## 🏃‍♀️ AnimatedVisibility (Staggered Entry)
Animations must stagger when entering a collection of items (such as standard lazy list rows) to show vertical movement:

```kotlin
@Composable
fun StaggeredItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L) // Staggered delays
        visible.value = true
    }
    
    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(350, easing = LinearOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}
```

---

## 🌊 Breathing & Pulse Effects
Use `rememberInfiniteTransition` to generate ongoing breathing or pulse animations on custom graphics, warnings, or interactive badges:

```kotlin
@Composable
fun BreathingBadge(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(12.dp)
            .background(color, shape = CircleShape)
    )
}
```

---

## 🔄 Tab Switching (AnimatedContent)
Switch between tabs using sliding transitions: swipe left when index increases, swipe right when it decreases:

```kotlin
AnimatedContent(
    targetState = activeTab,
    transitionSpec = {
        if (targetState > initialState) {
            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                slideOutHorizontally { width -> -width } + fadeOut()
            )
        } else {
            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                slideOutHorizontally { width -> width } + fadeOut()
            )
        }
    },
    label = "tab_switching"
) { tab ->
    // Render specific Screen Tab content
}
```

---

## 🍃 Spring Physics Setup
Avoid stiff, robotic movements by tuning physics using springs:
- `Spring.DampingRatioLowBouncy`: For standard visual entries where tactile bounce is useful.
- `Spring.StiffnessMediumLow`: Perfect balance of speed and organic movement.
