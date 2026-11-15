package com.foodbridge.donation;

import com.foodbridge.audit.AuditAction;
import com.foodbridge.audit.AuditService;
import com.foodbridge.donation.dto.CategoryRequest;
import com.foodbridge.donation.dto.CategoryResponse;
import com.foodbridge.exception.ApiException;
import com.foodbridge.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Food category management. Read operations are available to every
 * authenticated user; mutations are admin-only (enforced at the URL level) and
 * audited.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final FoodCategoryRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(boolean onlyActive) {
        List<FoodCategory> categories = onlyActive
                ? repository.findByActiveTrueOrderByDisplayOrderAscNameAsc()
                : repository.findAllByOrderByDisplayOrderAscNameAsc();
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(AuthenticatedUser actor, CategoryRequest request, String ip) {
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw ApiException.conflict("CATEGORY_EXISTS", "A category with this name already exists");
        }
        FoodCategory category = new FoodCategory();
        category.setName(request.name().trim());
        category.setSlug(slugify(request.name()));
        category.setDescription(request.description());
        category.setActive(request.active());
        category.setDisplayOrder(request.displayOrder());
        FoodCategory saved = repository.save(category);
        auditService.record(actor, AuditAction.CATEGORY_CREATED, "FoodCategory", saved.getId(),
                "Created category " + saved.getName(), ip);
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(AuthenticatedUser actor, Long id, CategoryRequest request, String ip) {
        FoodCategory category = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "Category not found"));
        category.setName(request.name().trim());
        category.setSlug(slugify(request.name()));
        category.setDescription(request.description());
        category.setActive(request.active());
        category.setDisplayOrder(request.displayOrder());
        FoodCategory saved = repository.save(category);
        auditService.record(actor, AuditAction.CATEGORY_UPDATED, "FoodCategory", saved.getId(),
                "Updated category " + saved.getName() + " (active=" + saved.isActive() + ")", ip);
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void deactivate(AuthenticatedUser actor, Long id, String ip) {
        FoodCategory category = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "Category not found"));
        // Categories are never hard-deleted because donations reference them.
        category.setActive(false);
        repository.save(category);
        auditService.record(actor, AuditAction.CATEGORY_DELETED, "FoodCategory", id,
                "Deactivated category " + category.getName(), ip);
    }

    public FoodCategory requireActive(Long id) {
        return repository.findById(id)
                .filter(FoodCategory::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Selected food category is not available"));
    }

    static String slugify(String name) {
        return name.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
