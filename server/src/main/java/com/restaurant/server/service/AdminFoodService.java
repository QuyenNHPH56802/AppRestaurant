package com.restaurant.server.service;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.Category;
import com.restaurant.server.entity.Food;
import com.restaurant.server.entity.FoodTranslation;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.CategoryRepository;
import com.restaurant.server.repository.FoodRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminFoodService {

    private final FoodRepository foods;
    private final CategoryRepository categories;
    private final TranslationService translator;
    private final MessageService messages;
    private final AuditLogRepository auditLogs;

    public AdminFoodService(FoodRepository foods, CategoryRepository categories,
                            TranslationService translator, MessageService messages,
                            AuditLogRepository auditLogs) {
        this.foods = foods;
        this.categories = categories;
        this.translator = translator;
        this.messages = messages;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public Page<ContentDtos.FoodView> list(int page, int size) {
        Page<Food> p = foods.findAllAdmin(PageRequest.of(page, size, Sort.by("sortOrder", "id").ascending()));
        String lang = translator.currentLang();
        return p.map(f -> translator.toView(f, lang));
    }

    @Transactional
    public ContentDtos.FoodView create(AdminDtos.FoodRequest req, Long actorId) {
        Category cat = categories.findById(req.categoryId())
                .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
        Food f = new Food();
        f.setCategory(cat);
        f.setPrice(req.price());
        f.setImageUrl(req.imageUrl());
        f.setStatus(parseStatus(req.status()));
        f.setFeatured(Boolean.TRUE.equals(req.featured()));
        f.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        f.setTranslations(toTranslations(f, req.translations()));
        Food saved = foods.save(f);
        audit("FOOD_CREATE", saved.getId(), actorId, null);
        return translator.toView(saved, translator.currentLang());
    }

    @Transactional
    public ContentDtos.FoodView update(Long id, AdminDtos.FoodRequest req, Long actorId) {
        Food f = foods.findByIdWithTranslations(id)
                .orElseThrow(() -> AppException.notFound(messages.get("food.not_found")));
        Category cat = categories.findById(req.categoryId())
                .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
        f.setCategory(cat);
        f.setPrice(req.price());
        f.setImageUrl(req.imageUrl());
        f.setStatus(parseStatus(req.status()));
        f.setFeatured(Boolean.TRUE.equals(req.featured()));
        if (req.sortOrder() != null) f.setSortOrder(req.sortOrder());
        f.getTranslations().clear();
        f.getTranslations().addAll(toTranslations(f, req.translations()));
        Food saved = foods.save(f);
        audit("FOOD_UPDATE", saved.getId(), actorId, null);
        return translator.toView(saved, translator.currentLang());
    }

    @Transactional
    public ContentDtos.FoodView patch(Long id, AdminDtos.FoodPatchRequest req, Long actorId) {
        Food f = foods.findByIdWithTranslations(id)
                .orElseThrow(() -> AppException.notFound(messages.get("food.not_found")));
        if (req.categoryId() != null) {
            Category cat = categories.findById(req.categoryId())
                    .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
            f.setCategory(cat);
        }
        if (req.price() != null) f.setPrice(req.price());
        if (req.imageUrl() != null) f.setImageUrl(req.imageUrl());
        if (req.status() != null) f.setStatus(parseStatus(req.status()));
        if (req.featured() != null) f.setFeatured(req.featured());
        if (req.sortOrder() != null) f.setSortOrder(req.sortOrder());
        Food saved = foods.save(f);
        audit("FOOD_PATCH", saved.getId(), actorId, null);
        return translator.toView(saved, translator.currentLang());
    }

    @Transactional
    public ContentDtos.FoodView setStatus(Long id, String status, Long actorId) {
        Food f = foods.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("food.not_found")));
        f.setStatus(parseStatus(status));
        foods.save(f);
        audit("FOOD_STATUS", id, actorId, status);
        return translator.toView(foods.findByIdWithTranslations(id).get(), translator.currentLang());
    }

    @Transactional
    public ContentDtos.FoodView setFeatured(Long id, boolean featured, Long actorId) {
        Food f = foods.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("food.not_found")));
        f.setFeatured(featured);
        foods.save(f);
        audit("FOOD_FEATURED", id, actorId, Boolean.toString(featured));
        return translator.toView(foods.findByIdWithTranslations(id).get(), translator.currentLang());
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Food f = foods.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("food.not_found")));
        f.setStatus(Food.Status.HIDDEN);
        foods.save(f);
        audit("FOOD_HIDE", id, actorId, null);
    }

    private List<FoodTranslation> toTranslations(Food f, List<AdminDtos.FoodTranslationInput> inputs) {
        List<FoodTranslation> out = new ArrayList<>();
        for (var in : inputs) {
            FoodTranslation t = new FoodTranslation();
            t.setFood(f);
            t.setLanguageCode(in.lang());
            t.setName(in.name());
            t.setDescription(in.description());
            t.setIngredients(in.ingredients());
            t.setPortion(in.portion());
            out.add(t);
        }
        return out;
    }

    private Food.Status parseStatus(String s) {
        try { return Food.Status.valueOf(s); }
        catch (Exception e) { throw AppException.badRequest("INVALID_STATUS", "Invalid status: " + s); }
    }

    private void audit(String action, Long entityId, Long actorId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setAction(action);
            a.setEntity("food");
            a.setEntityId(entityId == null ? null : entityId.toString());
            a.setUserId(actorId);
            a.setDetails(details);
            auditLogs.save(a);
        } catch (Exception ignored) {}
    }
}