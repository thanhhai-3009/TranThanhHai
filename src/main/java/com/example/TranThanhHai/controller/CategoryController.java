package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.model.Category;
import com.example.TranThanhHai.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private final CategoryService categoryService;

    // ========== LIST ==========
    @GetMapping("/categories")
    public String listCategories(Model model) {
        // Chỉ lấy danh mục gốc, children đã được EAGER fetch
        List<Category> rootCategories = categoryService.getRootCategories();
        model.addAttribute("categories", rootCategories);
        model.addAttribute("allCategories", categoryService.getAllCategories());
        return "/categories/categories-list";
    }

    // ========== ADD ==========
    @GetMapping("/categories/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        // Cho phép chọn danh mục cha
        model.addAttribute("parentCategories", categoryService.getAllCategories());
        return "/categories/add-category";
    }

    @PostMapping("/categories/add")
    public String addCategory(@Valid @ModelAttribute("category") Category category,
                              BindingResult result,
                              @RequestParam(value = "iconFile", required = false) MultipartFile iconFile,
                              @RequestParam(value = "parentId", required = false) Long parentId,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", categoryService.getAllCategories());
            return "/categories/add-category";
        }
        // Set parent
        if (parentId != null) {
            categoryService.getCategoryById(parentId).ifPresent(category::setParent);
        }
        // Save icon image
        saveIconFile(iconFile, category);

        categoryService.addCategory(category);
        return "redirect:/categories";
    }

    // ========== EDIT ==========
    @GetMapping("/categories/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        model.addAttribute("category", category);
        // Loại bỏ chính nó ra khỏi danh sách cha để không chọn chính nó làm cha
        List<Category> allCats = categoryService.getAllCategories();
        allCats.removeIf(c -> c.getId().equals(id));
        model.addAttribute("parentCategories", allCats);
        return "/categories/update-category";
    }

    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("category") Category category,
                                 BindingResult result,
                                 @RequestParam(value = "iconFile", required = false) MultipartFile iconFile,
                                 @RequestParam(value = "parentId", required = false) Long parentId,
                                 Model model) {
        if (result.hasErrors()) {
            category.setId(id);
            List<Category> allCats = categoryService.getAllCategories();
            allCats.removeIf(c -> c.getId().equals(id));
            model.addAttribute("parentCategories", allCats);
            return "/categories/update-category";
        }
        category.setId(id);

        // Set parent
        if (parentId != null) {
            categoryService.getCategoryById(parentId).ifPresent(category::setParent);
        } else {
            category.setParent(null);
        }

        // If no new icon uploaded, keep the old one
        if (iconFile == null || iconFile.isEmpty()) {
            Category existing = categoryService.getCategoryById(id).orElse(null);
            if (existing != null) {
                category.setIcon(existing.getIcon());
            }
        } else {
            saveIconFile(iconFile, category);
        }

        categoryService.updateCategory(category);
        return "redirect:/categories";
    }

    // ========== DELETE ==========
    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id) {
        categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        categoryService.deleteCategoryById(id);
        return "redirect:/categories";
    }

    // ========== HELPER ==========
    private void saveIconFile(MultipartFile iconFile, Category category) {
        if (iconFile != null && !iconFile.isEmpty()) {
            try {
                String originalFileName = iconFile.getOriginalFilename();
                if (originalFileName != null) {
                    String sanitized = Paths.get(originalFileName).getFileName().toString();
                    String fileName = System.currentTimeMillis() + "_" + sanitized;
                    Path imagePath = Paths.get("src/main/resources/static/images/" + fileName);
                    if (!Files.exists(imagePath.getParent())) {
                        Files.createDirectories(imagePath.getParent());
                    }
                    Files.copy(iconFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
                    category.setIcon("/images/" + fileName);
                }
            } catch (Exception e) {
                logger.error("Failed to save category icon", e);
            }
        }
    }
}