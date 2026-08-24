package com.restaurant.server.service;

import com.restaurant.server.entity.Category;
import com.restaurant.server.entity.CategoryTranslation;
import com.restaurant.server.entity.Food;
import com.restaurant.server.entity.FoodTranslation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationServiceTest {

    @Test
    void koFallbackToViAddsViToFallback() {
        TranslationService svc = new TranslationService();
        // No current locale set; default is vi
        Category c = new Category();
        c.setId(7L);
        c.setStatus(Category.Status.ACTIVE);
        CategoryTranslation vi = new CategoryTranslation();
        vi.setCategory(c);
        vi.setLanguageCode("vi");
        vi.setName("Phở");
        vi.setDescription("Mì gạo");
        c.getTranslations().add(vi);
        // No ko translation -> fallback to vi
        var view = svc.toView(c, "ko");
        assertEquals("Phở", view.name());
        assertTrue(view.fallback().contains("vi"), "fallback should include vi");
        assertEquals("ko", view.lang());
    }

    @Test
    void koTranslationUsedWhenPresent() {
        TranslationService svc = new TranslationService();
        Category c = new Category();
        c.setId(8L);
        c.setStatus(Category.Status.ACTIVE);
        for (String[] pair : new String[][]{
            {"vi", "Phở", "Mì gạo"},
            {"ko", "쌀국수", "쌀국수"}
        }) {
            CategoryTranslation t = new CategoryTranslation();
            t.setCategory(c);
            t.setLanguageCode(pair[0]);
            t.setName(pair[1]);
            t.setDescription(pair[2]);
            c.getTranslations().add(t);
        }
        var view = svc.toView(c, "ko");
        assertEquals("쌀국수", view.name());
        assertTrue(view.fallback().isEmpty());
    }

    @Test
    void foodPriceIsSerializedAsPlainString() {
        TranslationService svc = new TranslationService();
        Food f = new Food();
        f.setId(99L);
        f.setStatus(Food.Status.AVAILABLE);
        f.setPrice(new BigDecimal("75000"));
        FoodTranslation t = new FoodTranslation();
        t.setFood(f);
        t.setLanguageCode("vi");
        t.setName("Phở");
        f.getTranslations().add(t);
        var view = svc.toView(f, "vi");
        assertNotNull(view);
        assertEquals("75000", view.price(), "Price must be a plain decimal string");
    }
}