package com.pizzaflow.kitchen.service;

import com.pizzaflow.common.PageResponse;
import com.pizzaflow.common.exception.ConflictException;
import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.kitchen.dto.*;
import com.pizzaflow.kitchen.entity.Category;
import com.pizzaflow.kitchen.entity.Ingredient;
import com.pizzaflow.kitchen.entity.Product;
import com.pizzaflow.kitchen.entity.RecipeItem;
import com.pizzaflow.kitchen.repository.CategoryRepository;
import com.pizzaflow.kitchen.repository.IngredientRepository;
import com.pizzaflow.kitchen.repository.ProductRepository;
import com.pizzaflow.kitchen.repository.RecipeItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeItemRepository recipeItemRepository;

    public MenuService(CategoryRepository categoryRepository,
                       ProductRepository productRepository,
                       IngredientRepository ingredientRepository,
                       RecipeItemRepository recipeItemRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeItemRepository = recipeItemRepository;
    }

    // Category CRUD
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categoryRepository.findByNameIgnoreCase(req.getName()).isPresent()) {
            throw new ConflictException("Категория с названием '" + req.getName() + "' уже существует");
        }
        Category category = new Category(req.getName(), req.getDescription(), req.getDisplayOrder());
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    // Product Catalog & Search
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(Long categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<Product> page = productRepository.searchProducts(categoryId, search, minPrice, maxPrice, pageable);
        List<ProductResponse> list = page.getContent().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
        return new PageResponse<>(list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Продукт с ID " + id + " не найден"));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        if (productRepository.findByNameIgnoreCase(req.getName()).isPresent()) {
            throw new ConflictException("Продукт с названием '" + req.getName() + "' уже существует");
        }
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Категория не найдена"));

        Product product = new Product(
                category,
                req.getName(),
                req.getDescription(),
                req.getPrice(),
                req.getImageUrl(),
                req.getPreparationMinutes()
        );

        if (req.getRecipe() != null) {
            for (RecipeItemDto itemDto : req.getRecipe()) {
                Ingredient ingredient = ingredientRepository.findById(itemDto.getIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ингредиент не найден: ID " + itemDto.getIngredientId()));
                RecipeItem recipeItem = new RecipeItem(product, ingredient, itemDto.getQuantityRequired());
                product.getRecipeItems().add(recipeItem);
            }
        }

        return ProductResponse.fromEntity(productRepository.save(product));
    }
}
