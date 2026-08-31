package com.balandman.liftlog.ui

import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.LogEntry
import java.time.LocalDate
import kotlin.random.Random

/**
 * What the motivational cat actually knows about the workout so far — enough
 * to occasionally say something that sounds like it's really talking about
 * *this* log, rather than a generic poster on a gym wall.
 */
data class MotivationStats(
    val totalLifts: Int,
    val totalWeight: Int,
    val streakDays: Int,
    val favoriteMachine: String?,
    val favoriteCount: Int,
    val newRecordsToday: List<Pair<String, Int>>,
) {
    companion object {
        fun from(log: List<LogEntry>, today: LocalDate): MotivationStats {
            val favorite = log.groupingBy { it.machineName }.eachCount().maxByOrNull { it.value }
            return MotivationStats(
                totalLifts = log.size,
                totalWeight = log.sumOf { it.weight },
                streakDays = streakThrough(log, today),
                favoriteMachine = favorite?.key,
                favoriteCount = favorite?.value ?: 0,
                newRecordsToday = newRecordsOn(log, today),
            )
        }

        private fun streakThrough(log: List<LogEntry>, today: LocalDate): Int {
            val loggedDays = log.map { GymDay.dayOf(it.loggedAt) }.toHashSet()
            if (loggedDays.isEmpty()) return 0
            var cursor = if (today in loggedDays) today else today.minusDays(1)
            if (cursor !in loggedDays) return 0
            var streak = 0
            while (cursor in loggedDays) {
                streak++
                cursor = cursor.minusDays(1)
            }
            return streak
        }

        /** Machines where today's logged weight beat every prior day's best. */
        private fun newRecordsOn(log: List<LogEntry>, today: LocalDate): List<Pair<String, Int>> {
            val todays = log.filter { GymDay.dayOf(it.loggedAt) == today }
            if (todays.isEmpty()) return emptyList()
            val priorMaxByMachine = log
                .filter { GymDay.dayOf(it.loggedAt) != today }
                .groupBy { it.machineId }
                .mapValues { (_, entries) -> entries.maxOf { it.weight } }
            return todays
                .mapNotNull { entry ->
                    val priorMax = priorMaxByMachine[entry.machineId]
                    if (priorMax != null && entry.weight > priorMax) {
                        entry.machineName to entry.weight
                    } else {
                        null
                    }
                }
                .distinctBy { it.first }
        }
    }
}

/**
 * The mascot's caption. Backed by a combinatorial pool of complete,
 * independently-grammatical opener + closer sentences — [OPENERS] × [CLOSERS]
 * alone is well over a thousand distinct pairings, so the same line is rare
 * even for a heavy user — plus a handful of stat-aware templates that get
 * mixed in whenever there's something specific to say (a new record, a
 * streak, a milestone, a favorite machine), so the cat sometimes sounds like
 * it's actually paying attention.
 */
object MotivationCatalog {

    fun randomSaying(stats: MotivationStats, random: Random = Random.Default): String {
        val personalized = personalizedPool(stats)
        // Personalized lines are only ever a fraction of the pool, so lean on
        // them when available, but not every single time — always saying the
        // "new record" line the instant one happens would get repetitive if
        // several machines PR in the same session.
        if (personalized.isNotEmpty() && random.nextFloat() < 0.6f) {
            return personalized.random(random)
        }
        return "${OPENERS.random(random)} ${CLOSERS.random(random)}"
    }

    /**
     * Not private: [CoachVoice] reuses this same stat-aware pool for every
     * coach rather than duplicating it — the personalized lines (new record,
     * streak, lifetime total, favorite machine) are shared, only the general
     * opener/closer voice differs per coach.
     */
    fun personalizedPool(stats: MotivationStats): List<String> = buildList {
        if (stats.newRecordsToday.isNotEmpty()) {
            val (name, weight) = stats.newRecordsToday.random()
            NEW_RECORD_TEMPLATES.forEach { add(it(name, weight)) }
        }
        if (stats.streakDays >= 3) {
            STREAK_TEMPLATES.forEach { add(it(stats.streakDays)) }
        }
        if (stats.totalWeight >= 1000) {
            TOTAL_WEIGHT_TEMPLATES.forEach { add(it(stats.totalWeight)) }
        }
        if (stats.favoriteMachine != null && stats.favoriteCount >= 3) {
            FAVORITE_TEMPLATES.forEach { add(it(stats.favoriteMachine, stats.favoriteCount)) }
        }
    }

    // ---------------------------------------------------------------- pools

    private val OPENERS = listOf(
        "You pounced on that workout like it owed you money.",
        "Paws off the couch — you showed up and lifted.",
        "That was one purr-fectly executed set.",
        "You've got the focus of a cat stalking a laser pointer.",
        "Nine lives, and you're spending this one at the gym.",
        "You just out-stubborned a cat at 3am.",
        "That set had some serious cat-titude.",
        "You're building muscle faster than a cat sheds fur.",
        "Claws out, weights up — that's the spirit.",
        "You stretched, you lifted, you conquered — very cat of you.",
        "Somewhere, a cat is nodding in approval.",
        "That was a full-send, whiskers-forward effort.",
        "You've got more grit than a scratching post.",
        "Consistency like yours doesn't happen by accident.",
        "You showed up when the couch was calling louder.",
        "That's the kind of rep that makes a difference.",
        "Small steps, sharp claws, steady progress.",
        "You're proving that discipline beats motivation every time.",
        "Every set today was a vote for the person you're becoming.",
        "That effort didn't go unnoticed — not even by the cat.",
        "You chased that goal down like it was a red dot.",
        "Today's workout was no cat-nap.",
        "You brought the same energy as a cat guarding its favorite box.",
        "That's a lift worth bragging about at the water bowl.",
        "You're stacking good days on top of good days.",
        "Not every day is easy, but you showed up on this one.",
        "That's the kind of effort that compounds.",
        "You treated that set like the main event, not a warm-up.",
        "Somebody's about to nap like they earned it — and they did.",
        "That was a strong, deliberate, no-nonsense effort.",
        "You're proof that steady beats flashy.",
        "The weights didn't stand a chance today.",
        "You clawed back a little more strength today.",
        "That's a rep count worth curling up proud about.",
        "You made today count, one set at a time.",
        "Whatever you're chasing, you just got a little closer.",
        "You've got the patience of a cat waiting by a mouse hole — and it's paying off.",
        "That was focused, controlled, and quietly impressive.",
        "You're the kind of consistent that sneaks up on people.",
        "Today's version of you outworked yesterday's.",
        "You didn't just show up — you showed up ready.",
        "That's a set that future-you will thank you for.",
        "You're proving the streak wasn't a fluke.",
        "Every lift logged is a little deposit in a big investment.",
        "That was a disciplined, no-excuses kind of session.",
        "You're quietly becoming someone stronger.",
        "That's the kind of effort that adds up to something real.",
        "You showed the weights who's boss today.",
        "Today you chose progress over comfort.",
        "That's a session worth being proud of.",
    )

    private val CLOSERS = listOf(
        "Keep clawing your way forward.",
        "Meow that's what I call dedication.",
        "Purr-sistence pays off.",
        "You're on a roll — don't stop kneading now.",
        "Stay pawsitive and keep lifting.",
        "That's paw-some work.",
        "Fur real, keep it up.",
        "You're the cat's meow today.",
        "Claw your way to the next goal.",
        "This cat approves.",
        "Nothing but good vibes and gains ahead.",
        "Keep stacking those wins, one paw at a time.",
        "You've earned a nap — but tomorrow, back at it.",
        "Onward, one purr-fect rep at a time.",
        "The couch can wait — you've got momentum.",
        "Whiskers up, chin up, keep going.",
        "You're proving nine lives isn't the limit.",
        "Stay hungry, like a cat by an empty bowl at 6am.",
        "Rome wasn't built in a day, and neither is a cat's trust — but you're getting there.",
        "Keep it up and the streak keeps purring along.",
        "That's a job well scratched.",
        "Don't let this momentum slip through your paws.",
        "You're pouncing on your goals one day at a time.",
        "Here's to the next set, the next day, the next win.",
    )

    // ------------------------------------------------------- personalized

    private val NEW_RECORD_TEMPLATES: List<(String, Int) -> String> = listOf(
        { name, weight -> "New record on $name — $weight lb! This cat is seriously impressed." },
        { name, weight -> "$weight lb on $name? That's a personal best. Somebody give this human a treat." },
        { name, weight -> "You just topped your best $name lift ever, at $weight lb. Paws up!" },
        { name, weight -> "$name just met its match: $weight lb, a brand-new high. Purr-fection." },
        { name, weight -> "That's the heaviest $name lift on record — $weight lb and climbing." },
        { name, weight -> "$weight lb on $name is a new high-water mark. The cat is taking notes." },
        { name, weight -> "Personal best alert: $name at $weight lb. Keep that momentum clawing forward." },
        { name, weight -> "You out-lifted your old self on $name today — $weight lb, new record." },
    )

    private val STREAK_TEMPLATES: List<(Int) -> String> = listOf(
        { days -> "$days days in a row — that's a streak with real claws." },
        { days -> "$days consecutive gym days. This cat has never been more proud." },
        { days -> "$days days straight — you're basically part of the furniture at the gym now." },
        { days -> "A $days-day streak like that doesn't happen by accident." },
        { days -> "$days days and counting — keep that streak purring." },
        { days -> "$days days in a row is no small feat. Whiskers up." },
    )

    private val TOTAL_WEIGHT_TEMPLATES: List<(Int) -> String> = listOf(
        { total -> "$total lb lifted all-time — that's a mountain of gains." },
        { total -> "You've hauled $total lb total. The cat is officially impressed." },
        { total -> "$total lb and counting — every pound logged adds up to something real." },
        { total -> "$total lb lifted so far. That's a lot of paw-power." },
        { total -> "All-time total: $total lb. Keep building on that." },
        { total -> "$total lb lifted lifetime — that's an impressive haul, no catnip required." },
    )

    private val FAVORITE_TEMPLATES: List<(String, Int) -> String> = listOf(
        { name, count -> "$name is clearly your favorite — $count sessions and counting." },
        { name, count -> "You keep coming back to $name. $count times now — must be a favorite for a reason." },
        { name, count -> "$name, $count times logged. That's loyalty a cat would respect." },
        { name, count -> "$count sessions on $name — you two have a thing going." },
        { name, count -> "$name is basically your signature move by now — $count logs and rising." },
        { name, count -> "You and $name have logged $count sessions together. A beautiful partnership." },
    )
}
