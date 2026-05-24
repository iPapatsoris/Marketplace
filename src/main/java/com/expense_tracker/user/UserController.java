package com.expense_tracker.user;

import com.expense_tracker.user.dto.CreateUserRequest;
import com.expense_tracker.user.dto.UpdateUserRequest;
import com.expense_tracker.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService service;

    @GetMapping
    public List<UserResponse> getAll() {
        return service.getAll();
    }

    @PatchMapping("/{userID}")
    public UserResponse updateUser(@PathVariable Long userID, @RequestBody UpdateUserRequest updateUserRequest) {
        return service.update(userID, updateUserRequest);
    }

    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest createUserRequest) {
        return service.create(createUserRequest);
    }

    @DeleteMapping("/{userID}")
    // what should this return?
    public void deleteUser(@PathVariable Long userID) {
        service.delete(userID);
    }
}
