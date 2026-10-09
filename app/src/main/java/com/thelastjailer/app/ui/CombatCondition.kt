package com.thelastjailer.app.ui

internal enum class CombatCondition(val portrait: String) {
    NORMAL("kaelen_combat_portrait_v2"),
    WOUNDED("kaelen_combat_wounded"),
    CRITICAL("kaelen_combat_critical");

    companion object {
        fun fromHealth(health: Int, maxHealth: Int): CombatCondition {
            val fraction = health.toFloat() / maxHealth.coerceAtLeast(1)
            return when {
                fraction <= .30f -> CRITICAL
                fraction <= .50f -> WOUNDED
                else -> NORMAL
            }
        }
    }
}
