package com.restaurant.server.service;

import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.StoreSettings;
import com.restaurant.server.repository.StoreSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {

    private final StoreSettingsRepository repo;
    private final TranslationService translator;

    public StoreService(StoreSettingsRepository repo, TranslationService translator) {
        this.repo = repo;
        this.translator = translator;
    }

    @Transactional(readOnly = true)
    public ContentDtos.StoreView get() {
        StoreSettings s = repo.findSingleton().orElseGet(() -> {
            StoreSettings x = new StoreSettings();
            x.setId(1L);
            return x;
        });
        return translator.toView(s, translator.currentLang());
    }
}