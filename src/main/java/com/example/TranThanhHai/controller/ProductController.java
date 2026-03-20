package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.model.Product;
import com.example.TranThanhHai.service.CategoryService;
import com.example.TranThanhHai.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Controller
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @GetMapping("/products")
    public String showProductList(Model model) {
        // Hiển thị 3 loại khuyến mãi riêng biệt
        model.addAttribute("discountProducts", productService.getDiscountProducts());
        model.addAttribute("giftProducts", productService.getGiftProducts());
        model.addAttribute("normalProducts", productService.getNormalProducts());
        return "/products/products-list";
    }
    // For adding a new product
    @GetMapping("/products/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getLeafCategories());
        return "/products/add-product";
    }
    // Process the form for adding a new product
    @PostMapping("/products/add")
    public String addProduct(@ModelAttribute("product") @Valid Product product, BindingResult result,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             @RequestParam(value = "categoryId", required = false) Long categoryId,
                             Model model, HttpServletRequest request) {
        logger.debug("POST /products/add Content-Type={}", request.getContentType());
        if (result.hasErrors()) {
            // ensure categories are present when re-rendering the form
            model.addAttribute("categories", categoryService.getLeafCategories());
            return "/products/add-product";
        }
        // Set category explicitly from categoryId (form submits the id)
        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(product::setCategory);
        }
        logger.debug("Received imageFile for add (MultipartFile) : {}", imageFile != null ? imageFile.getOriginalFilename() : "null");
        if ((imageFile == null || imageFile.isEmpty())) {
            // Fallback: try to read Part directly from request
            try {
                Part part = request.getPart("imageFile");
                if (part != null && part.getSize() > 0) {
                    String originalFileName = part.getSubmittedFileName();
                    logger.debug("Received imageFile for add via Part: {} (size={})", originalFileName, part.getSize());
                    String sanitized = java.nio.file.Paths.get(originalFileName).getFileName().toString();
                    String fileName = System.currentTimeMillis() + "_" + sanitized;
                    java.nio.file.Path imagePath = java.nio.file.Paths.get("src/main/resources/static/images/" + fileName);
                    if (!java.nio.file.Files.exists(imagePath.getParent())) {
                        java.nio.file.Files.createDirectories(imagePath.getParent());
                    }
                    try (java.io.InputStream is = part.getInputStream()) {
                        java.nio.file.Files.copy(is, imagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    product.setImage("/images/" + fileName);
                }
            } catch (Exception e) {
                logger.debug("No Part named 'imageFile' or failed to read Part", e);
            }
        } else {
            try {
                String originalFileName = imageFile.getOriginalFilename();
                if (originalFileName != null) {
                    String sanitized = java.nio.file.Paths.get(originalFileName).getFileName().toString();
                    String fileName = System.currentTimeMillis() + "_" + sanitized;
                    // Save file to src/main/resources/static/images so it's available under /images in dev
                    java.nio.file.Path imagePath = java.nio.file.Paths.get("src/main/resources/static/images/" + fileName);
                    if (!java.nio.file.Files.exists(imagePath.getParent())) {
                        java.nio.file.Files.createDirectories(imagePath.getParent());
                    }
                    java.nio.file.Files.copy(imageFile.getInputStream(), imagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    product.setImage("/images/" + fileName);
                }
            } catch (Exception e) {
                logger.error("Failed to save uploaded image for new product", e);
            }
        }
        productService.addProduct(product);
        return "redirect:/products";
    }
    // For editing a product
    @GetMapping("/products/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getLeafCategories());
        return "/products/update-product";
    }
    // Process the form for updating a product
    @PostMapping("/products/update/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute("product") @Valid Product product,
                                BindingResult result,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "categoryId", required = false) Long categoryId,
                                Model model, HttpServletRequest request) {
        logger.debug("POST /products/update/{} Content-Type={}", id, request.getContentType());
        if (result.hasErrors()) {
            product.setId(id);
            // ensure categories are present when re-rendering the form
            model.addAttribute("categories", categoryService.getLeafCategories());
            return "/products/update-product";
        }
        // Ensure the product id is set
        product.setId(id);
        // Set category explicitly from categoryId if present
        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(product::setCategory);
        }
        // Preserve existing image if no new file is uploaded
        Product existing = productService.getProductById(id).orElse(null);
        if (existing != null && (imageFile == null || imageFile.isEmpty())) {
            product.setImage(existing.getImage());
        }
        logger.debug("Received imageFile for update (MultipartFile) id={} : {}", id, imageFile != null ? imageFile.getOriginalFilename() : "null");
        if ((imageFile == null || imageFile.isEmpty())) {
            // Fallback to request Part
            try {
                Part part = request.getPart("imageFile");
                if (part != null && part.getSize() > 0) {
                    String originalFileName = part.getSubmittedFileName();
                    logger.debug("Received imageFile for update via Part: {} (size={})", originalFileName, part.getSize());
                    String sanitized = java.nio.file.Paths.get(originalFileName).getFileName().toString();
                    String fileName = System.currentTimeMillis() + "_" + sanitized;
                    java.nio.file.Path imagePath = java.nio.file.Paths.get("src/main/resources/static/images/" + fileName);
                    if (!java.nio.file.Files.exists(imagePath.getParent())) {
                        java.nio.file.Files.createDirectories(imagePath.getParent());
                    }
                    try (java.io.InputStream is = part.getInputStream()) {
                        java.nio.file.Files.copy(is, imagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    product.setImage("/images/" + fileName);
                }
            } catch (Exception e) {
                logger.debug("No Part named 'imageFile' or failed to read Part for update", e);
            }
        } else {
            try {
                String originalFileName = imageFile.getOriginalFilename();
                if (originalFileName != null) {
                    String sanitized = java.nio.file.Paths.get(originalFileName).getFileName().toString();
                    String fileName = System.currentTimeMillis() + "_" + sanitized;
                    java.nio.file.Path imagePath = java.nio.file.Paths.get("src/main/resources/static/images/" + fileName);
                    if (!java.nio.file.Files.exists(imagePath.getParent())) {
                        java.nio.file.Files.createDirectories(imagePath.getParent());
                    }
                    java.nio.file.Files.copy(imageFile.getInputStream(), imagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    product.setImage("/images/" + fileName);
                }
            } catch (Exception e) {
                logger.error("Failed to save uploaded image while updating product id=" + id, e);
            }
        }
        productService.updateProduct(product);
        return "redirect:/products";
    }
    // Handle request to delete a product
    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProductById(id);
        return "redirect:/products";
    }
}
