package com.example.TranThanhHai.config;

import com.example.TranThanhHai.model.Category;
import com.example.TranThanhHai.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khởi tạo dữ liệu danh mục 3 cấp mẫu khi ứng dụng start
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(CategoryRepository categoryRepository) {
        return args -> {
            // Kiểm tra xem đã có dữ liệu chưa
            if (categoryRepository.count() > 0) {
                return; // Không tạo lại nếu đã có
            }

            // ===== CẤP 0: PHỤ KIỆN (danh mục gốc) =====
            Category level0Phukien = new Category();
            level0Phukien.setName("Phụ kiện");
            level0Phukien.setParent(null);
            categoryRepository.save(level0Phukien);

            // ===== CẤP 1: Con của PHỤ KIỆN =====

            // Phụ kiện di động
            Category level1PhukienDiDong = new Category();
            level1PhukienDiDong.setName("Phụ kiện di động");
            level1PhukienDiDong.setParent(level0Phukien);
            categoryRepository.save(level1PhukienDiDong);

            // Phụ kiện Laptop/PC
            Category level1PhukienLaptop = new Category();
            level1PhukienLaptop.setName("Phụ kiện Laptop, PC");
            level1PhukienLaptop.setParent(level0Phukien);
            categoryRepository.save(level1PhukienLaptop);

            // ===== CẤP 2: Con của PHỤ KIỆN DI ĐỘNG =====

            // Sạc dự phòng
            Category level2Sac = new Category();
            level2Sac.setName("Sạc dự phòng");
            level2Sac.setParent(level1PhukienDiDong);
            categoryRepository.save(level2Sac);

            // Cáp
            Category level2Cap = new Category();
            level2Cap.setName("Cáp");
            level2Cap.setParent(level1PhukienDiDong);
            categoryRepository.save(level2Cap);

            // Sạc
            Category level2SacThuong = new Category();
            level2SacThuong.setName("Sạc");
            level2SacThuong.setParent(level1PhukienDiDong);
            categoryRepository.save(level2SacThuong);

            // ===== CẤP 2: Con của PHỤ KIỆN LAPTOP, PC =====

            Category level2LaptopSac = new Category();
            level2LaptopSac.setName("Adapter sạc Laptop");
            level2LaptopSac.setParent(level1PhukienLaptop);
            categoryRepository.save(level2LaptopSac);

            Category level2LaptopChuot = new Category();
            level2LaptopChuot.setName("Chuột, Bàn phím");
            level2LaptopChuot.setParent(level1PhukienLaptop);
            categoryRepository.save(level2LaptopChuot);

            System.out.println("✅ Đã khởi tạo dữ liệu danh mục 3 cấp thành công!");
        };
    }
}



