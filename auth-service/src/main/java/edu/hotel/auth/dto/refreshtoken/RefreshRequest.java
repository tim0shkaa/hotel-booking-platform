package edu.hotel.auth.dto.refreshtoken;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Токен не должен быть пустым")
    private String refreshToken;
}
