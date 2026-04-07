package edu.hotel.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
    @NotBlank(message = "Пароль не может быть пустым")
    private String oldPassword;

    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
    @NotBlank(message = "Пароль не может быть пустым")
    private String newPassword;
}
