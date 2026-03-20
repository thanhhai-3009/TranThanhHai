package com.example.TranThanhHai.service;

import com.example.TranThanhHai.model.Product;
import com.example.TranThanhHai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    // Retrieve all products from the database
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Sản phẩm có khuyến mãi giảm giá
    public List<Product> getDiscountProducts() {
        return productRepository.findByPromotionType("DISCOUNT");
    }

    // Sản phẩm có quà tặng
    public List<Product> getGiftProducts() {
        return productRepository.findByPromotionType("GIFT");
    }

    // Sản phẩm không khuyến mãi
    public List<Product> getNormalProducts() {
        return productRepository.findByPromotionType("NONE");
    }

    // Retrieve a product by its id
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // Add a new product to the database
    public Product addProduct(Product product) {
        normalizePromotionData(product);
        return productRepository.save(product);
    }

    // Update an existing product
    public Product updateProduct(@NotNull Product product) {
        Product existingProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalStateException("Product with ID " +
                        product.getId() + " does not exist."));
        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setPromotionType(product.getPromotionType());
        existingProduct.setDiscountPercent(product.getDiscountPercent());
        existingProduct.setGiftDescription(product.getGiftDescription());
        existingProduct.setPromotionStockQuantity(product.getPromotionStockQuantity());
        normalizePromotionData(existingProduct);
        // Update image only if provided in incoming product; otherwise keep existing image
        if (product.getImage() != null && !product.getImage().isBlank()) {
            existingProduct.setImage(product.getImage());
        }
        return productRepository.save(existingProduct);
    }

    // Delete a product by its id
    public void deleteProductById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalStateException("Product with ID " + id + " does not exist.");
        }
        productRepository.deleteById(id);
    }

    private void normalizePromotionData(Product product) {
        String promotionType = product.getPromotionType();
        if (promotionType == null || promotionType.isBlank()) {
            promotionType = "NONE";
        }
        promotionType = promotionType.toUpperCase();
        product.setPromotionType(promotionType);

        Integer promotionStock = product.getPromotionStockQuantity();
        product.setPromotionStockQuantity(Math.max(promotionStock == null ? 0 : promotionStock, 0));

        if ("DISCOUNT".equals(promotionType)) {
            Double percent = product.getDiscountPercent();
            if (percent == null) {
                percent = 0.0;
            }
            product.setDiscountPercent(Math.max(0.0, Math.min(100.0, percent)));
            product.setGiftDescription(null);
            return;
        }

        if ("GIFT".equals(promotionType)) {
            product.setDiscountPercent(null);
            String giftDescription = product.getGiftDescription();
            if (giftDescription != null && giftDescription.isBlank()) {
                product.setGiftDescription(null);
            }
            return;
        }

        product.setPromotionType("NONE");
        product.setPromotionStockQuantity(0);
        product.setDiscountPercent(null);
        product.setGiftDescription(null);
    }
}
