package com.expense_tracker.user;

import com.expense_tracker.user.dto.CreateUserRequest;
import com.expense_tracker.user.dto.UpdateUserRequest;
import com.expense_tracker.user.dto.UserResponse;
import org.apache.coyote.Response;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class UserController {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final UserMapper userMapper;

    public UserController(UserRepository repository, UserMapper mapper, UserMapper userMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/user")
    public List<UserResponse> getAll() {
        var users = repository.findAll();
        users.forEach(System.out::println);

        List<UserResponse> dtos = new ArrayList<>();
        users.forEach(user -> dtos.add(mapper.toDTO(user)));
        return dtos;
    }

    @PatchMapping("/user/{id}")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest updateUserRequest) {
        Optional<User> user = repository.findById(id);
        if (user.isEmpty()) {
            // TODO: handle error response
            System.out.println("User doesn't exist");
            return null;
        }

        var updatedUser = userMapper.updateUser(updateUserRequest, user.get());
        repository.save(updatedUser);
        return userMapper.toDTO(updatedUser);
    }

    @PostMapping("/user")
    public UserResponse createUser(@RequestBody CreateUserRequest createUserRequest) {
       User user = new User(createUserRequest.name(), createUserRequest.email());
        repository.save(user);
        return userMapper.toDTO(user);
    }

    @DeleteMapping("/user/{userID}")
    // what should this return?
    public void deleteUser(@PathVariable Long userID) {
        repository.deleteById(userID);
    }
}
