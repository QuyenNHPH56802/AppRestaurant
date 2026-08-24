package com.restaurant.server.notify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FcmNotificationProviderScrubTest {

    @Test
    void scrubRemovesLongBase64ShapedTokens() {
        // 64+ chars of base64url-like content (FCM tokens are ~140+)
        String token = "aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789-_abcdefghijklmnopqrstuvwxyz12345extra";
        String input = "send failed: token=" + token + " http 400";
        String out = FcmNotificationProvider.scrub(input);
        assertNotNull(out);
        assertTrue(out.contains("<token-redacted>"), "expected redaction marker, got: " + out);
        assertTrue(!out.contains(token), "raw token must be stripped");
    }

    @Test
    void scrubIsNullSafe() {
        assertNull(FcmNotificationProvider.scrub(null));
    }

    @Test
    void scrubDoesNotTouchShortStrings() {
        String safe = "QUOTA_EXCEEDED: retry after 60s";
        assertEquals(safe, FcmNotificationProvider.scrub(safe));
    }

    @Test
    void tokenResultHelpers() {
        TokenResult s = TokenResult.sent("t", "msg-1");
        assertEquals(TokenResult.Outcome.SENT, s.outcome());
        assertEquals("msg-1", s.providerMsgId());

        TokenResult r = TokenResult.retryable("t", "X", "msg");
        assertEquals(TokenResult.Outcome.RETRYABLE, r.outcome());
        assertEquals("X", r.errorCode());

        TokenResult p = TokenResult.permanent("t", "UNREGISTERED", "msg");
        assertEquals(TokenResult.Outcome.PERMANENT_FAILURE, p.outcome());

        TokenResult sk = TokenResult.skipped("t", "dryRun");
        assertEquals(TokenResult.Outcome.SKIPPED, sk.outcome());
    }
}
