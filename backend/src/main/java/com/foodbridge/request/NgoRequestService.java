package com.foodbridge.request;

import com.foodbridge.common.PageRequestFactory;
import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.FoodCategory;
import com.foodbridge.donation.FoodCategoryRepository;
import com.foodbridge.exception.ApiException;
import com.foodbridge.request.dto.NgoRequestCreate;
import com.foodbridge.request.dto.NgoRequestResponse;
import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.Role;
import com.foodbridge.user.User;
import com.foodbridge.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * NGO food requirement requests - the demand side of the marketplace.
 */
@Service
@RequiredArgsConstructor
public class NgoRequestService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "neededBy", "status");

    private final NgoRequestRepository repository;
    private final FoodCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public NgoRequestResponse create(AuthenticatedUser actor, NgoRequestCreate request) {
        if (actor.role() != Role.NGO) {
            throw ApiException.forbidden("ROLE_REQUIRED", "Only NGO accounts can publish food requests");
        }
        User ngo = userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Authentication is required"));
        FoodCategory category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Unknown food category"));
        }
        NgoRequest entity = new NgoRequest();
        entity.setNgo(ngo);
        entity.setTitle(request.title().trim());
        entity.setDescription(trimToNull(request.description()));
        entity.setCategory(category);
        entity.setQuantityNeeded(request.quantityNeeded());
        entity.setQuantityUnit(request.quantityUnit().trim());
        entity.setCity(request.city().trim());
        entity.setNeededBy(request.neededBy());
        entity.setBeneficiaries(request.beneficiaries());
        entity.setStatus(NgoRequestStatus.OPEN);
        return NgoRequestResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public PageResponse<NgoRequestResponse> mine(AuthenticatedUser actor, NgoRequestStatus status,
                                                 int page, int size) {
        Pageable pageable = PageRequestFactory.of(page, size, "createdAt", "desc", SORTABLE);
        return PageResponse.of(repository.findByNgo(actor.id(), status, pageable), NgoRequestResponse::from);
    }

    /** Open requests visible to donors so they can target their donations. */
    @Transactional(readOnly = true)
    public PageResponse<NgoRequestResponse> open(String city, int page, int size) {
        Pageable pageable = PageRequestFactory.of(page, size, "neededBy", "asc", SORTABLE);
        return PageResponse.of(repository.search(NgoRequestStatus.OPEN, city, pageable), NgoRequestResponse::from);
    }

    @Transactional
    public NgoRequestResponse updateStatus(AuthenticatedUser actor, Long id, NgoRequestStatus status) {
        NgoRequest entity = repository.findByIdAndNgoId(id, actor.id())
                .orElseThrow(() -> ApiException.notFound("REQUEST_NOT_FOUND", "Food request not found"));
        if (entity.getStatus() == NgoRequestStatus.FULFILLED || entity.getStatus() == NgoRequestStatus.CANCELLED) {
            throw ApiException.conflict("REQUEST_CLOSED", "This request is already closed");
        }
        entity.setStatus(status);
        return NgoRequestResponse.from(repository.save(entity));
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
