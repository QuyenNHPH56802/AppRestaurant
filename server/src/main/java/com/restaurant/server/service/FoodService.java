package com.restaurant.server.service;

import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.Food;
import com.restaurant.server.entity.Food.Status;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.FoodRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FoodService {

    private final FoodRepository repo;
    private final TranslationService translator;
    private final MessageService messages;

    public FoodService(FoodRepository repo, TranslationService translator, MessageService messages) {
        this.repo = repo;
        this.translator = translator;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public ContentDtos.PagedResponse<ContentDtos.FoodView> list(
            String q, Long categoryId, Status status, Boolean featured,
            int page, int size) {

        String lang = translator.currentLang();
        Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder", "id").ascending());

        Page<Food> result;
        if (q != null && !q.isBlank()) {
            result = repo.searchByLanguage(q.trim(), lang, pageable);
        } else {
            result = repo.findAllVisible(pageable);
        }

        List<ContentDtos.FoodView> items = result.getContent().stream()
                .map(f -> translator.toView(f, lang))
                .filter(v -> categoryId == null || (v.categoryId() != null && v.categoryId().equals(categoryId)))
                .filter(v -> status == null || v.status().equals(status.name()))
                .filter(v -> featured == null || v.featured() == featured)
                .toList();
        return new ContentDtos.PagedResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ContentDtos.FoodView get(Long id) {
        Food f = repo.findByIdWithTranslations(id)
                .orElseThrow(() -> AppException.notFound(messages.get("food.not_found")));
        if (f.getStatus() == Status.HIDDEN) {
            // STAFF can never see HIDDEN. ADMIN endpoints use a different code path.
            throw AppException.notFound(messages.get("food.not_found"));
        }
        return translator.toView(f, translator.currentLang());
    }

    @Transactional(readOnly = true)
    public List<ContentDtos.FoodView> featured(int limit) {
        String lang = translator.currentLang();
        Page<Food> page = repo.findFeatured(PageRequest.of(0, limit));
        return page.getContent().stream()
                .map(f -> translator.toView(f, lang))
                .toList();
    }
}