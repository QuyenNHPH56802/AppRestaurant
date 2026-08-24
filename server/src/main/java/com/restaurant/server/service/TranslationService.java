package com.restaurant.server.service;

import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.Category;
import com.restaurant.server.entity.CategoryTranslation;
import com.restaurant.server.entity.Food;
import com.restaurant.server.entity.FoodImage;
import com.restaurant.server.entity.FoodTranslation;
import com.restaurant.server.entity.StoreSettings;
import com.restaurant.server.entity.StoreTranslation;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Resolves the current language for a request and applies fallback rules.
 * The active language is read from Spring's LocaleContextHolder (set by
 * LocaleConfig). When the active language has no translation row, the service
 * substitutes the vi translation and reports the substitution in `fallback`.
 */
@Service
public class TranslationService {

    public static final String FALLBACK_LANG = "vi";

    public String currentLang() {
        Locale l = LocaleContextHolder.getLocale();
        if (l == null || l.getLanguage() == null) return FALLBACK_LANG;
        String lang = l.getLanguage();
        if ("vi".equalsIgnoreCase(lang) || "ko".equalsIgnoreCase(lang)) return lang.toLowerCase();
        return FALLBACK_LANG;
    }

    public ContentDtos.CategoryView toView(Category c, String lang) {
        List<String> fallback = new ArrayList<>();
        CategoryTranslation tr = pick(c.getTranslations(), CategoryTranslation::getLanguageCode,
                CategoryTranslation::getName, t -> t.getName(), lang, fallback);
        return new ContentDtos.CategoryView(
                c.getId(),
                c.getSortOrder(),
                c.getStatus().name(),
                c.getImageUrl(),
                tr != null ? tr.getName() : null,
                tr != null ? tr.getDescription() : null,
                lang,
                fallback
        );
    }

    public ContentDtos.FoodView toView(Food f, String lang) {
        List<String> fallback = new ArrayList<>();
        FoodTranslation tr = pick(f.getTranslations(), FoodTranslation::getLanguageCode,
                FoodTranslation::getName, t -> t.getName(), lang, fallback);
        List<String> images = new ArrayList<>();
        if (f.getImageUrl() != null && !f.getImageUrl().isBlank()) images.add(f.getImageUrl());
        for (FoodImage im : f.getImages()) {
            if (im.getImageUrl() != null && !im.getImageUrl().isBlank()) images.add(im.getImageUrl());
        }
        String categoryName = null;
        if (f.getCategory() != null) {
            List<String> catFb = new ArrayList<>();
            CategoryTranslation ct = pick(f.getCategory().getTranslations(),
                    CategoryTranslation::getLanguageCode, CategoryTranslation::getName,
                    t -> t.getName(), lang, catFb);
            categoryName = ct != null ? ct.getName() : null;
        }
        return new ContentDtos.FoodView(
                f.getId(),
                f.getCategory() != null ? f.getCategory().getId() : null,
                categoryName,
                f.getPrice() == null ? "0" : f.getPrice().toPlainString(),
                f.getImageUrl(),
                f.getStatus().name(),
                f.isFeatured(),
                f.getSortOrder(),
                tr != null ? tr.getName() : null,
                tr != null ? tr.getDescription() : null,
                tr != null ? tr.getIngredients() : null,
                tr != null ? tr.getPortion() : null,
                lang,
                fallback,
                images
        );
    }

    public ContentDtos.StoreView toView(StoreSettings s, String lang) {
        List<String> fallback = new ArrayList<>();
        StoreTranslation tr = pick(s.getTranslations(), StoreTranslation::getLanguageCode,
                StoreTranslation::getStoreName, t -> t.getStoreName(), lang, fallback);
        return new ContentDtos.StoreView(
                s.getId(),
                s.getLogoUrl(),
                s.getAddress(),
                s.getPhone(),
                s.getOpeningHours(),
                tr != null ? tr.getStoreName() : null,
                tr != null ? tr.getDescription() : null,
                lang,
                fallback
        );
    }

    /**
     * Returns the translation for the requested language, falling back to vi when missing.
     * If vi is also missing, returns the first available translation.
     * Adds the substituted language to {@code fallbackOut} when vi was used.
     */
    private <T> T pick(List<T> translations,
                       Function<T, String> languageCode,
                       Function<T, String> primary,
                       Function<T, String> fallbackName,
                       String lang,
                       List<String> fallbackOut) {
        if (translations == null || translations.isEmpty()) return null;
        for (T t : translations) {
            if (lang.equalsIgnoreCase(languageCode.apply(t))) return t;
        }
        for (T t : translations) {
            if (FALLBACK_LANG.equalsIgnoreCase(languageCode.apply(t))) {
                if (!FALLBACK_LANG.equals(lang)) fallbackOut.add(FALLBACK_LANG);
                return t;
            }
        }
        return translations.get(0);
    }
}