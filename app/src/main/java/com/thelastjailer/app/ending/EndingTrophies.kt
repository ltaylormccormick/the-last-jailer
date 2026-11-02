package com.thelastjailer.app.ending

/**
 * The nine trophies that belong to the ending of a telling rather than to a scene or a fight: one for reaching an
 * ending, one for the way the telling was walked, and one each for the two outcomes the story itself varies (the dark
 * aid at Stonebeard's gate and Ilsevet's fate).
 *
 * A trophy's name is also its id in every save, so these strings are permanent: renaming one later would orphan the
 * trophy a player already holds. They are deliberately **not** part of
 * [com.thelastjailer.app.data.StoryRepository.availableTrophies], which stays the trophies a scene or a fight can give
 * (and so the `x / 37` the existing screens print). Anything that bounds the size of a trophy set must add these
 * explicitly; the three sizing tests do.
 *
 * Which of them a telling earns is decided by [EndingAwards]; the legacy is chosen by [LegacyRule].
 */
internal object EndingTrophies {
    const val TALE_TOLD = "The Tale Told"
    const val UNYIELDING = "The Unyielding"
    const val HONOURBOUND = "The Honourbound"
    const val STEADFAST = "The Steadfast"
    const val BORROWED_STRENGTH = "Borrowed Strength"
    const val UNAIDED = "Unaided"
    const val HER_CHAINS = "Her Chains"
    const val HER_FREEDOM = "Her Freedom"
    const val HER_FATE_UNWRITTEN = "Her Fate Unwritten"

    val all: Set<String> = linkedSetOf(
        TALE_TOLD, UNYIELDING, HONOURBOUND, STEADFAST, BORROWED_STRENGTH, UNAIDED, HER_CHAINS, HER_FREEDOM, HER_FATE_UNWRITTEN
    )

    /** The legacy trophy for the way a telling was walked. */
    fun forAxis(axis: Axis): String = when (axis) {
        Axis.COURAGE -> UNYIELDING
        Axis.HONOUR -> HONOURBOUND
        Axis.STEADFAST -> STEADFAST
    }
}
