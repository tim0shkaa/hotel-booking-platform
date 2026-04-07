package edu.hotel.auth.dto.user;

import edu.hotel.auth.model.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {

    private Long id;

    private String email;

    private Role role;

    private boolean active;

    private LocalDateTime createdAt;
}
