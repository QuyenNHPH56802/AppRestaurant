package com.restaurant.server.service;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.StoreSettings;
import com.restaurant.server.entity.StoreTranslation;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.StoreSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminStoreService {

    private final StoreSettingsRepository repo;
    private final TranslationService translator;
    private final AuditLogRepository auditLogs;

    public AdminStoreService(StoreSettingsRepository repo, TranslationService translator,
                             AuditLogRepository auditLogs) {
        this.repo = repo;
        this.translator = translator;
        this.auditLogs = auditLogs;
    }

    @Transactional
    public ContentDtos.StoreView upsert(AdminDtos.StoreRequest req, Long actorId) {
        StoreSettings s = repo.findSingleton().orElseGet(() -> {
            StoreSettings x = new StoreSettings();
            x.setId(1L);
            return x;
        });
        s.setLogoUrl(req.logoUrl());
        s.setAddress(req.address());
        s.setPhone(req.phone());
        s.setOpeningHours(req.openingHours());
        s.getTranslations().clear();
        s.getTranslations().addAll(toTranslations(s, req.translations()));
        StoreSettings saved = repo.save(s);
        audit(actorId);
        return translator.toView(saved, translator.currentLang());
    }

    private List<StoreTranslation> toTranslations(StoreSettings s, List<AdminDtos.StoreTranslationInput> inputs) {
        List<StoreTranslation> out = new ArrayList<>();
        for (var in : inputs) {
            StoreTranslation t = new StoreTranslation();
            t.setStore(s);
            t.setLanguageCode(in.lang());
            t.setStoreName(in.storeName());
            t.setDescription(in.description());
            out.add(t);
        }
        return out;
    }

    private void audit(Long actorId) {
        try {
            AuditLog a = new AuditLog();
            a.setAction("STORE_UPDATE");
            a.setEntity("store");
            a.setEntityId("1");
            a.setUserId(actorId);
            auditLogs.save(a);
        } catch (Exception ignored) {}
    }
}