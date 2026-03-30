package com.debanshu.xcalendar.ui.screen.planScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.KitchenPlannerMode
import com.debanshu.xcalendar.domain.repository.IKitchenPlannerRepository
import com.debanshu.xcalendar.domain.usecase.kitchen.GenerateGroceryListUseCase
import com.debanshu.xcalendar.domain.usecase.kitchen.GenerateMealPlanUseCase
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.launch
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KitchenPlannerSection(
    kitchenRepository: IKitchenPlannerRepository,
    generateMealPlanUseCase: GenerateMealPlanUseCase,
    generateGroceryListUseCase: GenerateGroceryListUseCase,
    isLlmAvailable: Boolean,
    notifier: PlatformNotifier,
    modifier: Modifier = Modifier,
) {
    val savedState by kitchenRepository.state.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var mode by rememberSaveable { mutableStateOf(KitchenPlannerMode.MEAL_PLAN) }
    var draftMealPlan by rememberSaveable { mutableStateOf("") }
    var draftGroceryList by rememberSaveable { mutableStateOf("") }
    var draftDietaryNotes by rememberSaveable { mutableStateOf("") }
    var hasInitialized by rememberSaveable { mutableStateOf(false) }

    var isGeneratingMealPlan by remember { mutableStateOf(false) }
    var isGeneratingGroceryList by remember { mutableStateOf(false) }
    var showDietaryNotes by rememberSaveable { mutableStateOf(false) }

    // Confirmation dialog state: stores the action to run after user confirms overwrite
    var showOverwriteConfirmation by remember { mutableStateOf(false) }
    var pendingConfirmedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // One-time initialization: populate drafts from DataStore on first load
    LaunchedEffect(savedState) {
        if (!hasInitialized && savedState != null) {
            val s = savedState!!
            draftMealPlan = s.mealPlanText
            draftGroceryList = s.groceryListText
            draftDietaryNotes = s.dietaryNotes
            hasInitialized = true
        }
    }

    // --- Reusable generation logic ---

    // Generates a meal plan via LLM, saves it, then auto-generates the grocery list
    fun executeMealPlanGeneration() {
        scope.launch {
            isGeneratingMealPlan = true
            val now = Clock.System.now().toEpochMilliseconds()
            scope.launch { kitchenRepository.saveDietaryNotes(draftDietaryNotes) }
            val generated = generateMealPlanUseCase(draftGroceryList, draftDietaryNotes)
            if (generated != null) {
                draftMealPlan = generated
                kitchenRepository.saveMealPlan(generated, now)
                isGeneratingMealPlan = false
                // Auto-derive grocery list from the new meal plan
                isGeneratingGroceryList = true
                val grocery = generateGroceryListUseCase(generated, draftDietaryNotes)
                if (grocery != null) {
                    draftGroceryList = grocery
                    kitchenRepository.saveGroceryList(grocery, now)
                    notifier.showToast("Meal plan generated! Grocery list updated.")
                } else {
                    notifier.showToast("Meal plan generated! (Grocery list generation failed — try saving manually.)")
                }
                isGeneratingGroceryList = false
            } else {
                isGeneratingMealPlan = false
                notifier.showToast("Couldn't generate meal plan. Check if the AI model is ready.")
            }
        }
    }

    // Saves the typed meal plan as-is, then auto-generates the grocery list
    fun executeSaveMealPlanAndDeriveGrocery() {
        scope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            kitchenRepository.saveDietaryNotes(draftDietaryNotes)
            kitchenRepository.saveMealPlan(draftMealPlan, now)
            isGeneratingGroceryList = true
            val grocery = generateGroceryListUseCase(draftMealPlan, draftDietaryNotes)
            if (grocery != null) {
                draftGroceryList = grocery
                kitchenRepository.saveGroceryList(grocery, now)
                notifier.showToast("Meal plan saved. Grocery list generated!")
            } else {
                notifier.showToast("Meal plan saved. (Grocery list generation failed — AI model may not be ready.)")
            }
            isGeneratingGroceryList = false
        }
    }

    // Generates a meal plan from the current grocery list draft, saves it
    fun executeMealPlanFromGroceryList() {
        scope.launch {
            isGeneratingMealPlan = true
            val now = Clock.System.now().toEpochMilliseconds()
            kitchenRepository.saveDietaryNotes(draftDietaryNotes)
            val generated = generateMealPlanUseCase(draftGroceryList, draftDietaryNotes)
            if (generated != null) {
                draftMealPlan = generated
                kitchenRepository.saveMealPlan(generated, now)
                notifier.showToast("Meal plan generated from your grocery list!")
            } else {
                notifier.showToast("Couldn't generate meal plan. Check if the AI model is ready.")
            }
            isGeneratingMealPlan = false
        }
    }

    // Guards an action that would overwrite an existing meal plan with a confirmation dialog
    fun guardMealPlanOverwrite(action: () -> Unit) {
        if (savedState?.mealPlanText?.isNotBlank() == true) {
            pendingConfirmedAction = action
            showOverwriteConfirmation = true
        } else {
            action()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().testTag("kitchen_planner_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Kitchen Planner",
                    style = XCalendarTheme.typography.titleLarge,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Meals from groceries, or groceries from meals.",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = { showDietaryNotes = !showDietaryNotes },
                modifier = Modifier.semantics {
                    contentDescription = if (showDietaryNotes) "Hide dietary notes" else "Show dietary notes"
                },
            ) {
                Text(if (showDietaryNotes) "Hide notes" else "Dietary notes")
            }
        }

        // Dietary notes (expandable)
        AnimatedVisibility(visible = showDietaryNotes) {
            OutlinedTextField(
                value = draftDietaryNotes,
                onValueChange = { draftDietaryNotes = it },
                modifier = Modifier.fillMaxWidth().testTag("kitchen_dietary_notes"),
                label = { Text("Dietary notes") },
                placeholder = { Text("e.g. nut-free, 2 adults 3 kids, no pork") },
                maxLines = 3,
            )
        }

        // Mode toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Kitchen planner mode selector" },
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { mode = KitchenPlannerMode.MEAL_PLAN },
                selected = mode == KitchenPlannerMode.MEAL_PLAN,
                modifier = Modifier.testTag("mode_meal_plan"),
            ) {
                Text("🍽 Meal Plan")
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { mode = KitchenPlannerMode.GROCERY_LIST },
                selected = mode == KitchenPlannerMode.GROCERY_LIST,
                modifier = Modifier.testTag("mode_grocery_list"),
            ) {
                Text("🛒 Grocery List")
            }
        }

        // Mode-specific content
        when (mode) {
            KitchenPlannerMode.MEAL_PLAN -> MealPlanContent(
                draft = draftMealPlan,
                onDraftChange = { draftMealPlan = it },
                isGeneratingMealPlan = isGeneratingMealPlan,
                isGeneratingGroceryList = isGeneratingGroceryList,
                isLlmAvailable = isLlmAvailable,
                onGenerateWithAi = { guardMealPlanOverwrite(::executeMealPlanGeneration) },
                onSaveAndDeriveGrocery = { executeSaveMealPlanAndDeriveGrocery() },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(draftMealPlan))
                    notifier.showToast("Meal plan copied")
                },
                onShare = {
                    notifier.shareText("Meal Plan", draftMealPlan)
                },
                onClear = {
                    draftMealPlan = ""
                    scope.launch {
                        kitchenRepository.saveMealPlan("", 0L)
                        notifier.showToast("Meal plan cleared")
                    }
                },
            )

            KitchenPlannerMode.GROCERY_LIST -> GroceryListContent(
                draft = draftGroceryList,
                onDraftChange = { draftGroceryList = it },
                isGeneratingMealPlan = isGeneratingMealPlan,
                isLlmAvailable = isLlmAvailable,
                onGenerateMealPlanFromList = { guardMealPlanOverwrite(::executeMealPlanFromGroceryList) },
                onSaveGroceryList = {
                    scope.launch {
                        val now = Clock.System.now().toEpochMilliseconds()
                        kitchenRepository.saveDietaryNotes(draftDietaryNotes)
                        kitchenRepository.saveGroceryList(draftGroceryList, now)
                        notifier.showToast("Grocery list saved")
                    }
                },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(draftGroceryList))
                    notifier.showToast("Grocery list copied")
                },
                onShare = {
                    notifier.shareText("Grocery List", draftGroceryList)
                },
                onClear = {
                    draftGroceryList = ""
                    scope.launch {
                        kitchenRepository.saveGroceryList("", 0L)
                        notifier.showToast("Grocery list cleared")
                    }
                },
            )
        }
    }

    // Meal plan overwrite confirmation dialog
    if (showOverwriteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showOverwriteConfirmation = false
                pendingConfirmedAction = null
            },
            title = { Text("Replace existing meal plan?") },
            text = {
                Text("A meal plan is already saved. The AI will generate a new one and save it, replacing the current plan. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteConfirmation = false
                        pendingConfirmedAction?.invoke()
                        pendingConfirmedAction = null
                    },
                    modifier = Modifier.semantics { contentDescription = "Confirm replace meal plan" },
                ) {
                    Text("Generate & Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showOverwriteConfirmation = false
                        pendingConfirmedAction = null
                    },
                    modifier = Modifier.semantics { contentDescription = "Cancel meal plan replacement" },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MealPlanContent(
    draft: String,
    onDraftChange: (String) -> Unit,
    isGeneratingMealPlan: Boolean,
    isGeneratingGroceryList: Boolean,
    isLlmAvailable: Boolean,
    onGenerateWithAi: () -> Unit,
    onSaveAndDeriveGrocery: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth().testTag("meal_plan_input"),
            label = { Text("Meal plan") },
            placeholder = { Text("Type your meal plan here, or generate one with AI") },
            minLines = 4,
            maxLines = 14,
            enabled = !isGeneratingMealPlan,
        )

        if (isGeneratingMealPlan) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Generating meal plan… this may take up to 30 s",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Generating meal plan, please wait" },
            )
        }

        if (!isLlmAvailable) {
            Text(
                text = "AI model required — download it in Settings to enable generation.",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.error,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onGenerateWithAi,
                enabled = isLlmAvailable && !isGeneratingMealPlan && !isGeneratingGroceryList,
                modifier = Modifier.semantics { contentDescription = "Generate meal plan with AI" },
            ) {
                Text("Generate with AI")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSaveAndDeriveGrocery,
                enabled = draft.isNotBlank() && !isGeneratingMealPlan && !isGeneratingGroceryList,
                modifier = Modifier.semantics { contentDescription = "Save meal plan and generate grocery list" },
            ) {
                Text("Save + Derive Grocery")
            }
        }

        if (isGeneratingGroceryList) {
            Text(
                text = "Generating grocery list…",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Generating grocery list, please wait" },
            )
        }

        if (draft.isNotBlank() && !isGeneratingMealPlan) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.semantics { contentDescription = "Copy meal plan to clipboard" },
                ) { Text("Copy") }
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.semantics { contentDescription = "Share meal plan" },
                ) { Text("Share") }
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.semantics { contentDescription = "Clear meal plan" },
                ) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun GroceryListContent(
    draft: String,
    onDraftChange: (String) -> Unit,
    isGeneratingMealPlan: Boolean,
    isLlmAvailable: Boolean,
    onGenerateMealPlanFromList: () -> Unit,
    onSaveGroceryList: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth().testTag("grocery_list_input"),
            label = { Text("Grocery list") },
            placeholder = { Text("Type your grocery list here, or generate one from your meal plan") },
            minLines = 4,
            maxLines = 14,
            enabled = !isGeneratingMealPlan,
        )

        if (isGeneratingMealPlan) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Generating meal plan… this may take up to 30 s",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Generating meal plan from grocery list, please wait" },
            )
        }

        if (!isLlmAvailable) {
            Text(
                text = "AI model required — download it in Settings to enable generation.",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.error,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onGenerateMealPlanFromList,
                enabled = isLlmAvailable && draft.isNotBlank() && !isGeneratingMealPlan,
                modifier = Modifier.semantics { contentDescription = "Generate meal plan from grocery list" },
            ) {
                Text("Generate Meal Plan")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSaveGroceryList,
                enabled = draft.isNotBlank() && !isGeneratingMealPlan,
                modifier = Modifier.semantics { contentDescription = "Save grocery list" },
            ) {
                Text("Save")
            }
        }

        if (draft.isNotBlank() && !isGeneratingMealPlan) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.semantics { contentDescription = "Copy grocery list to clipboard" },
                ) { Text("Copy") }
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.semantics { contentDescription = "Share grocery list" },
                ) { Text("Share") }
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.semantics { contentDescription = "Clear grocery list" },
                ) { Text("Clear") }
            }
        }
    }
}

// Spacer padding at the bottom of the section to visually separate from next section
@Composable
private fun SectionSpacer() = Spacer(modifier = Modifier.height(4.dp))
