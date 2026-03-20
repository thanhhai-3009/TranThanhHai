package com.example.TranThanhHai;

import lombok.AllArgsConstructor;
@AllArgsConstructor
public enum Role {
    ADMIN(1), // Vai trò quản trị viên, có quyền cao nhất trong hệ thống.
    MANAGER(2), // Vai trò quản lý, có quyền hạn trung bình, thường dùng để quản lý sản phẩm, đơn hàng, v.v.
    USER(3); // Vai trò người dùng bình thường, có quyền hạn giới hạn.
    public final long value; //Biến này lưu giá trị số tương ứng với mỗi vai trò.
}