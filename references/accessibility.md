# Accessibility — Semantics, TalkBack & Focus

Accessibility is a core engineering requirement. Complete native designs require compliant reading order, clear focus, and minimum touch target sizes.

---

## 🏷️ Semantic Properties
Every visual image or button must have an explicit declaration for accessibility support:
- **`contentDescription`**: Meaningful descriptive text. Leave as `null` only for purely decorative divider lines or background shapes.
- **`role`**: Specify component intent programmatically so TalkBack lists elements correctly (e.g. `Role.Button` or `Role.RadioButton`).
- **`stateDescription`**: Describe dynamic state transitions clearly (e.g. "Draft voucher" changing to "Posted voucher").

---

## 📏 Minimum Touch Targets (48dp Rule)
All interactive elements must measure **at least 48dp × 48dp** physically on screen to guarantee easy selection:

```kotlin
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.minimumInteractiveComponentSize() // Native 48dp target
) {
    Icon(Icons.Filled.Add, contentDescription = "Add Item")
}
```

---

## 🎯 Focus Traversal Control
In forms, use `FocusRequester` to move focus programmatically to the next text field when the user clicks 'Next' or 'Done':

```kotlin
val (focusName, focusPhone) = remember { FocusRequester.createRefs() }

TextField(
    value = name,
    onValueChange = { name = it },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions = KeyboardActions(onNext = { focusPhone.requestFocus() }),
    modifier = Modifier.focusRequester(focusName)
)
```

---

## 💬 Live Regions (Announcing Updates)
For asynchronous status changes (such as database sync completions or validation fails), request immediate TalkBack announcements using live regions:

```kotlin
Box(
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
    }
) {
    Text(syncStatusMessage)
}
```
