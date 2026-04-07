package edu.hotel.auth.mapper;

import edu.hotel.auth.dto.user.UserResponse;
import edu.hotel.auth.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
