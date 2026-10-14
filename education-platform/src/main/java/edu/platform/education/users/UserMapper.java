package edu.platform.education.users;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // пароль закодируем в сервисе
    User toEntity(UserRegistrationRequest request);

    UserResponse toResponse(User user);
}