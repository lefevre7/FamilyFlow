package com.debanshu.xcalendar.domain.model

enum class FamilyLens {
    FAMILY,
    MOM,
    PERSON,
}

data class FamilyLensSelection(
    val lens: FamilyLens = FamilyLens.MOM,
    val personId: String? = null,
)

fun FamilyLensSelection.effectivePersonId(momId: String?): String? =
    when (lens) {
        FamilyLens.FAMILY -> null
        FamilyLens.MOM -> momId
        FamilyLens.PERSON -> personId ?: momId
    }
