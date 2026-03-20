package com.example.TranThanhHai.controller;

import com.example.TranThanhHai.model.Category;
import com.example.TranThanhHai.service.CartService;
import com.example.TranThanhHai.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Inject danh muc menu, thong tin gio hang va role flags vao moi trang.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CartService cartService;

    @ModelAttribute("parentCategories")
    public List<Category> parentCategories() {
        return categoryService.getParentCategoriesForMenu();
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount() {
        return cartService.getTotalItems();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getAuthority()));
    }

    @ModelAttribute("isManager")
    public boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "MANAGER".equals(a.getAuthority()));
    }

    @ModelAttribute("isUser")
    public boolean isUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "USER".equals(a.getAuthority()));
    }
}
