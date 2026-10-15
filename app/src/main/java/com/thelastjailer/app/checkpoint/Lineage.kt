package com.thelastjailer.app.checkpoint

import java.security.SecureRandom
import java.util.Random

/**
 * A lineage is the identity of one slot's timeline. Checkpoints are only valid for the lineage they
 * were written under, so records from an overwritten slot, a restarted game or a partial backup
 * restore can never be mistaken for the current timeline's history.
 *
 * It is an identifier, not a secret: 64 random bits rendered as 16 lowercase hex characters.
 */
object Lineage {
    private const val LENGTH = 16
    private val secureRandom: Random by lazy { SecureRandom() }

    fun generate(random: Random = secureRandom): String {
        val value = random.nextLong()
        return String.format(java.util.Locale.ROOT, "%016x", value)
    }

    fun isWellFormed(value: String): Boolean =
        value.length == LENGTH && value.all { it in '0'..'9' || it in 'a'..'f' }
}
