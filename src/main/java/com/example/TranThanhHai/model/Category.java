package com.example.TranThanhHai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên là bắt buộc")
    private String name;

    /** Icon / ảnh minh hoạ cho danh mục */
    private String icon;

    /** Danh mục cha (null = danh mục gốc) */
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** Danh sách danh mục con */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Category> children;
}
