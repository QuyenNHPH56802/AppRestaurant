package com.restaurant.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationServiceFallbackTest {

    @Test
    void fallbackLanguageIsVi() {
        // The schema is open to en/ja/zh later; today only vi and ko are recognized.
        // When the request language is anything else, vi is used.
        assertEquals("vi", TranslationService.FALLBACK_LANG);
    }
}