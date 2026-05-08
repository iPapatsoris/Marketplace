package com.expense_tracker.user;

import com.expense_tracker.user.dto.UpdateUserRequest;
import com.expense_tracker.user.dto.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toDTO(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User updateUser(UpdateUserRequest updateUserRequest, @MappingTarget User user);
}
