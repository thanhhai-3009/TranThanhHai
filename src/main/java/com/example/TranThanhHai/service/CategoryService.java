package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.Category;
import com.example.TranThanhHai.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing categories.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    /** Tất cả danh mục */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /** Chỉ danh mục gốc (parent == null) */
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    /** Danh mục con theo parent id */
    public List<Category> getChildrenByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public void addCategory(Category category) {
        categoryRepository.save(category);
    }

    public void updateCategory(@NotNull Category category) {
        Category existingCategory = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new IllegalStateException("Category with ID " +
                        category.getId() + " does not exist."));
        existingCategory.setName(category.getName());
        existingCategory.setIcon(category.getIcon());
        existingCategory.setParent(category.getParent());
        categoryRepository.save(existingCategory);
    }

    public void deleteCategoryById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalStateException("Category with ID " + id + " does not exist.");
        }
        categoryRepository.deleteById(id);
    }

    /** Lấy tất cả danh mục parent (parent_id = null) để hiển thị trong dropdown menu */
    public List<Category> getParentCategoriesForMenu() {
        return categoryRepository.findByParentIsNull();
    }

    /** Lấy các leaf categories (danh mục không có con) để gán cho product */
    public List<Category> getLeafCategories() {
        List<Category> allCategories = getAllCategories();
        return allCategories.stream()
                .filter(cat -> cat.getChildren() == null || cat.getChildren().isEmpty())
                .toList();
    }
}