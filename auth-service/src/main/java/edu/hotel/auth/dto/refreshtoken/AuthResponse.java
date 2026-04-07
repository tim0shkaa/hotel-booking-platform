package edu.hotel.auth.dto.refreshtoken;

import lombok.Data;

@Data
public class AuthResponse {

    private String accessToken;

    private String refreshToken;
}
