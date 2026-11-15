package com.foodbridge.donation;

import com.foodbridge.donation.dto.CategoryRequest;
import com.foodbridge.donation.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only category endpoints for authenticated users. Writes live under
 * {@code /api/admin/categories}.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Food categories (read access)")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List food categories")
    public List<CategoryResponse> list(@RequestParam(defaultValue = "true") boolean onlyActive) {
        return categoryService.list(onlyActive);
    }
}
