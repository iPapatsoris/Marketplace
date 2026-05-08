package com.expense_tracker.user;

import com.expense_tracker.user.dto.CreateUserRequest;
import com.expense_tracker.user.dto.UpdateUserRequest;
import com.expense_tracker.user.dto.UserResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final UserMapper userMapper;

    public UserController(UserRepository repository, UserMapper mapper, UserMapper userMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        var users = repository.findAll();
        users.forEach(System.out::println);

        List<UserResponse> dtos = new ArrayList<>();
        users.forEach(user -> dtos.add(mapper.toDTO(user)));
        return dtos;
    }

    @PatchMapping("/{userID}")
    public UserResponse updateUser(@PathVariable Long userID, @RequestBody UpdateUserRequest updateUserRequest) {
        Optional<User> user = repository.findById(userID);
        if (user.isEmpty()) {
            // TODO: handle error response
            System.out.println("User doesn't exist");
            return null;
        }

        var updatedUser = userMapper.updateUser(updateUserRequest, user.get());
        repository.save(updatedUser);
        return userMapper.toDTO(updatedUser);
    }

    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest createUserRequest) {
       User user = new User(createUserRequest.name(), createUserRequest.email());
        repository.save(user);
        return userMapper.toDTO(user);
    }

    @DeleteMapping("/{userID}")
    // what should this return?
    public void deleteUser(@PathVariable Long userID) {
        repository.deleteById(userID);
    }
}
