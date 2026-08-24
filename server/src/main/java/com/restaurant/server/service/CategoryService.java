package com.restaurant.server.service;

import com.restaurant.server.dto.ContentDtos;
import com.restaurant.server.entity.Category;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repo;
    private final TranslationService translator;
    private final MessageService messages;

    public CategoryService(CategoryRepository repo, TranslationService translator, MessageService messages) {
        this.repo = repo;
        this.translator = translator;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<ContentDtos.CategoryView> listActive() {
        return repo.findAllActiveWithTranslations().stream()
                .map(c -> translator.toView(c, translator.currentLang()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentDtos.CategoryView get(Long id) {
        Category c = repo.findByIdWithTranslations(id)
                .orElseThrow(() -> AppException.notFound(messages.get("category.not_found")));
        return translator.toView(c, translator.currentLang());
    }
}