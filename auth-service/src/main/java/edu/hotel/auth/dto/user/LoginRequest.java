package edu.hotel.auth.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email не может быть пустым")
    private String email;

    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
    @NotBlank(message = "Пароль не может быть пустым")
    private String password;
}
