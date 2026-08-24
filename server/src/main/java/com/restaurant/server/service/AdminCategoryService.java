package com.restaurant.server.service;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.Category;
import com.restaurant.server.entity.CategoryTranslation;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminCategoryService {

    private final CategoryRepository repo;
    private final TranslationService translator;
    private final MessageService messages;
    private final AuditLogRepository auditLogs;

    public AdminCategoryService(CategoryRepository repo, TranslationService translator,
                                MessageService messages, AuditLogRepository auditLogs) {
        this.repo = repo;
        this.translator = translator;
        this.messages = messages;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public Page<com.restaurant.server.dto.ContentDtos.CategoryView> list(int page, int size) {
        Page<Category> p = repo.findAllAdmin(PageRequest.of(page, size, Sort.by("sortOrder", "id").ascending()));
        String lang = translator.currentLang();
        return p.map(c -> translator.toView(c, lang));
    }

    @Transactional
    public com.restaurant.server.dto.ContentDtos.CategoryView create(AdminDtos.CategoryRequest req, Long actorId) {
        Category c = new Category();
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        c.setStatus(parseStatus(req.status()));
        c.setImageUrl(req.imageUrl());
        c.setTranslations(toTranslations(c, req.translations()));
        Category saved = repo.save(c);
        audit("CATEGORY_CREATE", saved.getId(), actorId, null);
        return translator.toView(saved, translator.currentLang());
    }

    @Transactional
    public com.restaurant.server.dto.ContentDtos.CategoryView update(Long id, AdminDtos.CategoryRequest req, Long actorId) {
        Category c = repo.findByIdWithTranslations(id)
                .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
        c.setSortOrder(req.sortOrder() == null ? c.getSortOrder() : req.sortOrder());
        c.setStatus(req.status() == null ? c.getStatus() : parseStatus(req.status()));
        if (req.imageUrl() != null) c.setImageUrl(req.imageUrl());
        c.getTranslations().clear();
        c.getTranslations().addAll(toTranslations(c, req.translations()));
        Category saved = repo.save(c);
        audit("CATEGORY_UPDATE", saved.getId(), actorId, null);
        return translator.toView(saved, translator.currentLang());
    }

    @Transactional
    public com.restaurant.server.dto.ContentDtos.CategoryView patch(Long id, AdminDtos.CategoryPatchRequest req, Long actorId) {
        Category c = repo.findByIdWithTranslations(id)
                .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
        if (req.sortOrder() != null) c.setSortOrder(req.sortOrder());
        if (req.status() != null) c.setStatus(parseStatus(req.status()));
        if (req.imageUrl() != null) c.setImageUrl(req.imageUrl());
        Category saved = repo.save(c);
        audit("CATEGORY_PATCH", saved.getId(), actorId, null);
        return translator.toView(saved, translator.currentLang());
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Category c = repo.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
        // Soft-disable: set status to HIDDEN. Hard delete is reserved for future if no foods reference it.
        c.setStatus(Category.Status.HIDDEN);
        repo.save(c);
        audit("CATEGORY_HIDE", id, actorId, null);
    }

    private List<CategoryTranslation> toTranslations(Category c, List<AdminDtos.TranslationInput> inputs) {
        List<CategoryTranslation> out = new ArrayList<>();
        for (var in : inputs) {
            CategoryTranslation t = new CategoryTranslation();
            t.setCategory(c);
            t.setLanguageCode(in.lang());
            t.setName(in.name());
            t.setDescription(in.description());
            out.add(t);
        }
        return out;
    }

    private Category.Status parseStatus(String s) {
        try { return Category.Status.valueOf(s); }
        catch (Exception e) { throw AppException.badRequest("INVALID_STATUS", "Invalid status: " + s); }
    }

    private void audit(String action, Long entityId, Long actorId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setAction(action);
            a.setEntity("category");
            a.setEntityId(entityId == null ? null : entityId.toString());
            a.setUserId(actorId);
            a.setDetails(details);
            auditLogs.save(a);
        } catch (Exception ignored) {}
    }
}