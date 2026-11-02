package com.thelastjailer.app.ending

/** The three ways a telling can be walked. */
internal enum class Axis { COURAGE, HONOUR, STEADFAST }

/**
 * Which way a telling was walked, read from the story flags the player's own choices left behind. Pure: the flag set is
 * the only input, nothing is stored and nothing is read from stats, trophies or items.
 *
 * **The evidence.** Fourteen decisions between Chapters 4 and 30, each a scene every road to every ending must pass and
 * each reset on a new telling (so a flag held at an ending belongs to the telling that is ending). Every option of every
 * decision sets exactly one flag. An option's axis comes from what the story itself rewards: the option that gives
 * Courage is [Axis.COURAGE], the one that gives Honour is [Axis.HONOUR], and the ungated "stay, wait, say nothing" option
 * of the last nine chapters is [Axis.STEADFAST]. The three early deferrals (Chapters 13, 15 and 18) and the Chapter 17
 * "do whatever stops it fastest" option postpone Kaelen's *own* decision rather than respect someone else's, so they are
 * **neutral evidence**: the decision counts as recorded, and scores for no axis.
 *
 * **The margin rule.** A decision is *determined* when exactly one of its flags is held. One that holds none (missing) or
 * several (contradictory, as on a save from a build that did not reset the later chapters) is *undetermined*. With nothing
 * undetermined the highest tally wins, and a tie goes to the axis of the latest decision that belongs to a tied axis. With
 * *U* undetermined decisions the leader is named only if it leads every other axis by **more than** *U*: then no way of
 * answering the unknown decisions could change the result, so a stale or partial flag set can leave a legacy unawarded but
 * can never award a wrong one. On every clean telling *U* is 0, so the margin rule is the plain rule.
 */
internal object LegacyRule {
    /** One answer to a decision: the story flag it sets, and the axis it scores for (null: recorded, scores nothing). */
    class Option(val flag: String, val axis: Axis?)

    class Decision(val id: String, val options: List<Option>)

    private fun c(flag: String) = Option(flag, Axis.COURAGE)
    private fun h(flag: String) = Option(flag, Axis.HONOUR)
    private fun s(flag: String) = Option(flag, Axis.STEADFAST)
    private fun n(flag: String) = Option(flag, null)

    /** In story order; the tie-break reads the order. */
    val decisions: List<Decision> = listOf(
        Decision("aid", listOf(c("accepted_dark_aid"), h("refused_dark_aid_again"))),
        Decision("wardens", listOf(c("began_training_new_wardens"), h("stood_watch_rather_than_delegate"), n("deferred_the_question"))),
        Decision("old_debts", listOf(c("resolved_despite_sympathy"), h("committed_to_proving_her_wrong"), n("deferred_the_question_of_ilsevet"))),
        Decision("frame", listOf(c("destroyed_the_frame_outright"), h("disrupted_carefully"), n("stopped_it_however_possible"))),
        Decision("fate", listOf(c("captured_ilsevet"), h("showed_ilsevet_mercy"), n("deferred_ilsevets_fate"))),
        Decision("sixth_vow", listOf(c("vowed_to_protect_the_prisoner"), h("affirmed_its_own_choice"), s("left_the_question_open"))),
        Decision("commit", listOf(c("vowed_constant_vigilance"), h("vowed_to_share_the_burden"), s("simply_stayed"))),
        Decision("emberlow", listOf(c("refused_to_leave_emberlow"), h("insisted_on_real_choice"), s("waited_at_emberlow"))),
        Decision("greymoor", listOf(c("reminded_greymoor_of_choice"), h("offered_presence_not_replacement"), s("sat_with_greymoors_silence"))),
        Decision("duskmere", listOf(c("vowed_to_hold_duskmere"), h("sought_to_remember_wren"), s("secured_duskmere_in_silence"))),
        Decision("forgotten", listOf(c("pressed_the_forgotten_to_remember"), h("respected_the_forgetting"), s("stepped_back_from_the_sundering"))),
        Decision("vigil", listOf(c("offered_the_vigil_alliance"), h("respected_the_vigils_pace"), s("simply_bore_witness_at_wraithspire"))),
        Decision("start", listOf(c("chose_to_seek_the_ending"), h("insisted_on_another_way"))),
        Decision("final", listOf(c("demanded_it_be_earned"), h("offered_connection_without_merging"), s("simply_stayed_at_the_end")))
    )

    /**
     * [axis] is the legacy, or null when it cannot be proven. [tally] counts the determined decisions per axis,
     * [undetermined] is how many decisions were missing or contradictory, and [tieBroken] says the latest-decision
     * tie-break chose the axis.
     */
    class Result(val axis: Axis?, val tally: Map<Axis, Int>, val undetermined: Int, val tieBroken: Boolean)

    fun evaluate(flags: Set<String>): Result {
        val tally = Axis.values().associateWith { 0 }.toMutableMap()
        val determined = ArrayList<Pair<Int, Axis?>>()
        var undetermined = 0
        decisions.forEachIndexed { index, decision ->
            val held = decision.options.filter { it.flag in flags }
            if (held.size != 1) {
                undetermined++
                return@forEachIndexed
            }
            val axis = held.single().axis
            determined += index to axis
            if (axis != null) tally[axis] = tally.getValue(axis) + 1
        }
        val ranked = Axis.values().sortedByDescending { tally.getValue(it) }
        val top = tally.getValue(ranked[0])
        val second = tally.getValue(ranked[1])
        if (undetermined == 0) {
            val tied = Axis.values().filter { tally.getValue(it) == top }
            if (tied.size == 1) return Result(tied.single(), tally, 0, false)
            val winner = determined.sortedByDescending { it.first }.firstOrNull { it.second in tied }?.second
            return Result(winner, tally, 0, winner != null)
        }
        return Result(if (top - second > undetermined) ranked[0] else null, tally, undetermined, false)
    }
}
