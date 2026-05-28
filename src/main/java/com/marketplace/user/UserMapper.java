package com.marketplace.user;

import com.marketplace.user.dto.UpdateUserRequest;
import com.marketplace.user.dto.UserResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toDTO(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

    User updateUser(UpdateUserRequest updateUserRequest, @MappingTarget User user);
}
