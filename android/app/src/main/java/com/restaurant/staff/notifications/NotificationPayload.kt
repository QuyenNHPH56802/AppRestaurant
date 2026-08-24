package com.restaurant.staff.notifications

/**
 * V2.3 / V18 — Tiny utility to read a {@code response} key out of the
 * payload JSON the server stamps on a notification when the user
 * Accepts / Declines.
 *
 * Extracted from [NotificationsViewModel] so it can be unit-tested without
 * spinning up a ViewModel. We deliberately use a regex rather than Moshi
 * here: the payload is small (≤ 4 KB per the server's PUT_DATA guard) and
 * we only ever need ONE field from it, so a JSON parser would be
 * overkill.
 */
object NotificationPayload {

    /**
     * Returns {@code "ACCEPTED"}, {@code "DECLINED"}, or {@code null}
     * if the payload doesn't carry a verdict yet.
     */
    fun parseVerdict(payloadJson: String?): String? {
        if (payloadJson.isNullOrBlank()) return null
        return VERDICT_REGEX.find(payloadJson)?.groupValues?.get(1)
    }

    /**
     * The whitelist of "answer values" we accept. Mirrors the server's
     * `@Pattern(regexp = "ACCEPTED|DECLINED")` on the request DTO.
     */
    val VALID_VERDICTS: Set<String> = setOf("ACCEPTED", "DECLINED")

    private val VERDICT_REGEX = Regex("\"response\"\\s*:\\s*\"(ACCEPTED|DECLINED)\"")
}