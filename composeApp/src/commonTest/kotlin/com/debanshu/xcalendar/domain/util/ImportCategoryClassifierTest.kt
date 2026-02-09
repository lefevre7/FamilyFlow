package com.debanshu.xcalendar.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportCategoryClassifierTest {

    @Test
    fun classify_detectsSchoolKeywords() {
        val category = ImportCategoryClassifier.classify("Preschool pickup reminder")
        assertEquals(ImportCategory.SCHOOL, category)
    }

    @Test
    fun classify_detectsPracticeKeywords() {
        val category = ImportCategoryClassifier.classify("Soccer practice", "Gym field")
        assertEquals(ImportCategory.PRACTICE, category)
    }

    @Test
    fun applyCategory_replacesExistingPrefix() {
        val value =
            ImportCategoryClassifier.applyCategory(
                description = "Category: school\nBring forms",
                category = ImportCategory.APPOINTMENT,
            )
        assertEquals("Category: appointment\nBring forms", value)
    }
}
