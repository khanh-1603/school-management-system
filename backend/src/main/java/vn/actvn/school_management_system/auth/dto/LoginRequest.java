package vn.actvn.school_management_system.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO nhận dữ liệu JSON cho API đăng nhập {@code POST /api/auth/login}.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * @PostMapping("/login")
 * public ApiResponse<?> login(@Valid @RequestBody LoginRequest body) { ... }
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Chỉ kiểm tra không rỗng; không dùng {@code @Email} chặt để tránh lộ thông tin định dạng tài khoản.</li>
 *   <li>Mật khẩu được giữ nguyên ký tự, không trim và có phân biệt hoa/thường.</li>
 * </ul>
 *
 * @param email    email đăng nhập của người dùng, không được để trống
 * @param password mật khẩu dạng rõ do người dùng nhập, không được để trống
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống.")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống.")
        String password
) {}