package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.service.CategoryService;
import com.example.TranThanhHai.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("discountProducts", productService.getDiscountProducts());
        model.addAttribute("giftProducts", productService.getGiftProducts());
        model.addAttribute("normalProducts", productService.getNormalProducts());
        // Truyền danh mục parent (cha) để hiển thị trong dropdown Phụ kiện
        model.addAttribute("parentCategories", categoryService.getParentCategoriesForMenu());
        return "home";
    }
}
