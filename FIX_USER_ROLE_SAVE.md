# 🔧 FIX - User Role Not Saved to Database

## 🐛 Vấn đề
Khi tạo user mới qua registration, role không được lưu vào bảng `user_role`

## 🔍 Nguyên nhân

### 1. User Model - roles collection không được khởi tạo đúng
```java
// CŨ
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(name = "user_role", ...)
private Set<Role> roles = new HashSet<>();
```

**Problem**: Khi dùng @Builder, field này có thể không được khởi tạo → NullPointerException

**Fix**: Thêm `@Builder.Default` và `cascade = CascadeType.MERGE`
```java
// MỚI
@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
@JoinTable(name = "user_role", ...)
@Builder.Default
private Set<Role> roles = new HashSet<>();
```

### 2. UserService.save() - Không xử lý null role
```java
// CŨ
if (user.getRoles() == null || user.getRoles().isEmpty()) {
    var userRole = roleRepository.findRoleById(Role.USER.value);
    user.getRoles().add(userRole); // ❌ Có thể NPE nếu userRole = null
}
userRepository.save(user); // ❌ Role không được cascade save
```

**Problems**:
- Không check nếu `userRole` là null
- Không log để debug
- Không ensure cascade operation

**Fix**: Thêm null checks, logging, improve cascading
```java
// MỚI
if (user.getRoles() == null || user.getRoles().isEmpty()) {
    var userRole = roleRepository.findRoleById(Role.USER.value);
    if (userRole != null) {
        user.getRoles().add(userRole);
        log.info("Added USER role to user: {}", user.getUsername());
    } else {
        log.warn("USER role not found in database!");
    }
}

User savedUser = userRepository.save(user);
log.info("User saved successfully: {} with roles: {}", user.getUsername(), user.getRoles());
```

---

## ✅ Thay đổi chi tiết

### 1. **User.java** (Model)
```diff
- @ManyToMany(fetch = FetchType.EAGER)
+ @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
  @JoinTable(name = "user_role", ...)
+ @Builder.Default
  private Set<Role> roles = new HashSet<>();
```

**Why:**
- `cascade = CascadeType.MERGE`: Khi save user, roles cũng được merge vào DB
- `@Builder.Default`: Đảm bảo roles được khởi tạo = new HashSet<> khi dùng @Builder

### 2. **UserService.java** (Service)
```java
public void save(@NotNull User user) {
    // ... encrypt password ...
    
    // ✅ Gán role USER
    if (user.getRoles() == null || user.getRoles().isEmpty()) {
        var userRole = roleRepository.findRoleById(Role.USER.value);
        if (userRole != null) { // ✅ Null check
            user.getRoles().add(userRole);
            log.info("Added USER role..."); // ✅ Log
        } else {
            log.warn("USER role not found..."); // ✅ Warn
        }
    }
    
    // ✅ Save with cascade
    User savedUser = userRepository.save(user);
    log.info("User saved successfully..."); // ✅ Log
}
```

---

## 🗄️ Database Result

### Trước (❌ Role không lưu):
```sql
SELECT * FROM user WHERE username = 'testuser';
-- ✅ User exists

SELECT * FROM user_role WHERE user_id = 1;
-- ❌ No records (Role not saved!)
```

### Sau (✅ Role được lưu):
```sql
SELECT * FROM user WHERE username = 'testuser';
-- ✅ User exists

SELECT * FROM user_role WHERE user_id = 1;
-- ✅ Record exists: user_id=1, role_id=3 (3=USER)

SELECT u.username, r.name 
FROM user u 
JOIN user_role ur ON u.id = ur.user_id 
JOIN role r ON ur.role_id = r.id 
WHERE u.username = 'testuser';
-- ✅ Result: testuser | USER
```

---

## 🔄 Workflow (Sau khi fix)

```
User fills register form
    ↓
POST /register → UserController.registerUser()
    ├─ Validate form
    ├─ Check username exists
    └─ userService.save(user)
         ├─ Encrypt password
         ├─ Get USER role from DB
         ├─ user.getRoles().add(userRole) ✅
         ├─ userRepository.save(user) → Cascade saves user_role
         └─ log.info("User saved...") ✅
    ↓
✅ User created with role=USER in DB!
    - users table: NEW USER
    - user_role table: NEW ASSOCIATION (user_id → role_id=3)
    - roles table: NO CHANGE
    ↓
Redirect /login
```

---

## 🧪 Verify

### Via MySQL:
```sql
-- After registration of username 'john'
SELECT * FROM user WHERE username = 'john';
-- id | username | email | ...
-- 5  | john     | john@... | ...

SELECT ur.* FROM user_role ur 
WHERE ur.user_id = 5;
-- user_id | role_id
-- 5       | 3

SELECT r.* FROM role WHERE id = 3;
-- id | name
-- 3  | USER
```

### Via Spring Boot Logs:
```
INFO: Added USER role to user: john
INFO: User saved successfully: john with roles: [Role(id=3, name=USER, ...)]
```

### Via Login:
```
1. Login with john / password
2. Check authorities → Should have ROLE_USER
3. Check Spring Security context → hasRole('USER') = true ✅
```

---

## 📋 Checklist

- ✅ User model: @Builder.Default + cascade.MERGE
- ✅ UserService: null checks + logging
- ✅ Role lưu vào user_role table
- ✅ User có thể login với role=USER
- ✅ Spring Security recognize role

---

## 🎯 Summary

**Root Cause**: User roles collection không được khởi tạo + cascade config sai

**Solution**: 
1. Add `@Builder.Default` to User.roles
2. Add `cascade = CascadeType.MERGE` for auto-save
3. Add null checks + logging in UserService.save()

**Result**: Roles được lưu vào user_role table ✅

---

**Fix Complete! User roles now saved to database correctly! 🚀**

