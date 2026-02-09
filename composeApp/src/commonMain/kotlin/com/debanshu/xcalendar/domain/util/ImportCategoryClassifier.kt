package com.debanshu.xcalendar.domain.util

enum class ImportCategory(
    val key: String,
    val label: String,
) {
    SCHOOL("school", "School"),
    PRACTICE("practice", "Practice"),
    APPOINTMENT("appointment", "Appointment"),
    OTHER("other", "Other"),
}

object ImportCategoryClassifier {
    fun classify(
        title: String,
        description: String? = null,
    ): ImportCategory {
        val haystack = "$title ${description.orEmpty()}".lowercase()
        if (SCHOOL_KEYWORDS.any { haystack.contains(it) }) return ImportCategory.SCHOOL
        if (PRACTICE_KEYWORDS.any { haystack.contains(it) }) return ImportCategory.PRACTICE
        if (APPOINTMENT_KEYWORDS.any { haystack.contains(it) }) return ImportCategory.APPOINTMENT
        return ImportCategory.OTHER
    }

    fun applyCategory(
        description: String?,
        category: ImportCategory,
    ): String {
        val stripped = stripCategoryPrefix(description)?.trim()
        return if (stripped.isNullOrBlank()) {
            "Category: ${category.key}"
        } else {
            "Category: ${category.key}\n$stripped"
        }
    }

    fun fromKey(key: String): ImportCategory =
        ImportCategory.entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ImportCategory.OTHER

    private fun stripCategoryPrefix(description: String?): String? {
        if (description.isNullOrBlank()) return null
        return description.replace(Regex("^Category:\\s*(school|practice|appointment|other)\\s*\\n?", RegexOption.IGNORE_CASE), "")
    }

    private val SCHOOL_KEYWORDS =
        listOf(
            "school",
            "preschool",
            "kindergarten",
            "class",
            "teacher",
            "field trip",
            "pta",
            "homework",
            "campus",
        )

    private val PRACTICE_KEYWORDS =
        listOf(
            "practice",
            "soccer",
            "football",
            "basketball",
            "swim",
            "dance",
            "rehearsal",
            "training",
            "drill",
        )

    private val APPOINTMENT_KEYWORDS =
        listOf(
            "appointment",
            "doctor",
            "dentist",
            "pediatric",
            "therapy",
            "checkup",
            "consult",
            "clinic",
            "visit",
        )
}
