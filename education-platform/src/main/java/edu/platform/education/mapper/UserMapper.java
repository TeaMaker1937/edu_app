package edu.platform.education.mapper;

import edu.platform.education.dto.UserRegistrationRequest;
import edu.platform.education.entity.User;
import edu.platform.education.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // пароль закодируем в сервисе
    User toEntity(UserRegistrationRequest request);

    UserResponse toResponse(User user);
}