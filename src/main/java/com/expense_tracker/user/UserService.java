package com.expense_tracker.user;

import com.expense_tracker.user.dto.CreateUserRequest;
import com.expense_tracker.user.dto.UpdateUserRequest;
import com.expense_tracker.user.dto.UserResponse;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    public UserResponse create(@RequestBody CreateUserRequest createUserRequest) {
        User user = new User(createUserRequest.name(), createUserRequest.email());
        repository.save(user);
        return mapper.toDTO(user);
    }

    public void delete(Long userID) {
        repository.deleteById(userID);
    }

    public UserResponse update(Long userID, UpdateUserRequest updateUserRequest) {
        Optional<User> user = repository.findById(userID);
        if (user.isEmpty()) {
            // TODO: handle error response
            System.out.println("User doesn't exist");
            return null;
        }

        var updatedUser = mapper.updateUser(updateUserRequest, user.get());
        repository.save(updatedUser);
        return mapper.toDTO(updatedUser);
    }

    public List<UserResponse> getAll() {
        var users = repository.findAll();
        users.forEach(System.out::println);

        List<UserResponse> dtos = new ArrayList<>();
        users.forEach(user -> dtos.add(mapper.toDTO(user)));
        return dtos;
    }
}
