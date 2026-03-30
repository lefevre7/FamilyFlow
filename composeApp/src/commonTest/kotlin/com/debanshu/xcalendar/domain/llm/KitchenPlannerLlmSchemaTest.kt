package com.debanshu.xcalendar.domain.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KitchenPlannerLlmSchemaTest {

    @Test
    fun extractJsonBlock_returnsNull_whenNoOpenBrace() {
        assertNull(KitchenPlannerLlmSchema.extractJsonBlock("no json here at all"))
    }

    @Test
    fun extractJsonBlock_extractsCleanJson() {
        val input = """{"days":[{"day":"Monday"}]}"""
        val result = KitchenPlannerLlmSchema.extractJsonBlock(input)
        assertEquals(input, result)
    }

    @Test
    fun extractJsonBlock_stripsLeadingMarkdownWrapper() {
        val input = """
        Here is the result:
        ```json
        {"categories":[{"name":"Produce","items":[]}]}
        ```
        """.trimIndent()
        val result = KitchenPlannerLlmSchema.extractJsonBlock(input)
        assertNotNull(result)
        assertTrue(result.startsWith("{"))
        assertTrue(result.contains("categories"))
    }

    @Test
    fun parseMealPlanJson_returnsNull_forInvalidJson() {
        assertNull(KitchenPlannerLlmSchema.parseMealPlanJson("totally not json"))
    }

    @Test
    fun parseMealPlanJson_parsesAllDayFields() {
        val json = """{"days":[
            {"day":"Monday","breakfast":"Oats","lunch":"Salad","dinner":"Pasta","note":"Light day"},
            {"day":"Tuesday","breakfast":"Eggs","lunch":"Wrap","dinner":"Stir fry"}
        ]}"""
        val result = KitchenPlannerLlmSchema.parseMealPlanJson(json)
        assertNotNull(result)
        assertEquals(2, result.days.size)
        assertEquals("Monday", result.days[0].day)
        assertEquals("Oats", result.days[0].breakfast)
        assertEquals("Light day", result.days[0].note)
        assertNull(result.days[1].note)
    }

    @Test
    fun parseMealPlanJson_filtersEmptyDays() {
        val json = """{"days":[{"day":"","breakfast":"x","lunch":"y","dinner":"z"},{"day":"Monday","breakfast":"a","lunch":"b","dinner":"c"}]}"""
        val result = KitchenPlannerLlmSchema.parseMealPlanJson(json)
        assertNotNull(result)
        assertEquals(1, result.days.size)
        assertEquals("Monday", result.days[0].day)
    }

    @Test
    fun parseGroceryListJson_parsesCategories() {
        val json = """{"categories":[
            {"name":"Produce","items":[{"name":"apples","quantity":"6"},{"name":"spinach"}]},
            {"name":"Dairy","items":[{"name":"milk","quantity":"1 gallon"}]}
        ]}"""
        val result = KitchenPlannerLlmSchema.parseGroceryListJson(json)
        assertNotNull(result)
        assertEquals(2, result.categories.size)
        val produce = result.categories.first { it.name == "Produce" }
        assertEquals(2, produce.items.size)
        assertEquals("6", produce.items.first { it.name == "apples" }.quantity)
        assertNull(produce.items.first { it.name == "spinach" }.quantity)
    }

    @Test
    fun parseGroceryListJson_returnsNull_forInvalidJson() {
        assertNull(KitchenPlannerLlmSchema.parseGroceryListJson("not json"))
    }

    @Test
    fun formatMealPlan_includesAllDaysAndMeals() {
        val result = KitchenPlannerLlmSchema.parseMealPlanJson(
            """{"days":[{"day":"Monday","breakfast":"Oats","lunch":"Salad","dinner":"Pasta"}]}"""
        )
        assertNotNull(result)
        val formatted = KitchenPlannerLlmSchema.formatMealPlan(result)
        assertTrue(formatted.contains("Monday"))
        assertTrue(formatted.contains("Breakfast: Oats"))
        assertTrue(formatted.contains("Lunch: Salad"))
        assertTrue(formatted.contains("Dinner: Pasta"))
    }

    @Test
    fun formatGroceryList_includesCategoriesAndItems() {
        val result = KitchenPlannerLlmSchema.parseGroceryListJson(
            """{"categories":[{"name":"Produce","items":[{"name":"tomatoes","quantity":"4"}]}]}"""
        )
        assertNotNull(result)
        val formatted = KitchenPlannerLlmSchema.formatGroceryList(result)
        assertTrue(formatted.contains("Produce"))
        assertTrue(formatted.contains("tomatoes"))
        assertTrue(formatted.contains("(4)"))
    }

    @Test
    fun buildMealPlanPrompt_includesGroceryItems() {
        val prompt = KitchenPlannerLlmSchema.buildMealPlanPrompt("milk, eggs, pasta", "nut-free")
        assertTrue(prompt.contains("milk, eggs, pasta"))
        assertTrue(prompt.contains("nut-free"))
        assertTrue(prompt.contains("7-day"))
    }

    @Test
    fun buildMealPlanPrompt_handlesEmptyGroceryList() {
        val prompt = KitchenPlannerLlmSchema.buildMealPlanPrompt("", "")
        assertTrue(prompt.contains("general") || prompt.contains("No specific"))
    }

    @Test
    fun buildGroceryListPrompt_includesMealPlan() {
        val prompt = KitchenPlannerLlmSchema.buildGroceryListPrompt("Monday: Pasta for dinner", "vegetarian")
        assertTrue(prompt.contains("Monday: Pasta for dinner"))
        assertTrue(prompt.contains("vegetarian"))
    }
}
