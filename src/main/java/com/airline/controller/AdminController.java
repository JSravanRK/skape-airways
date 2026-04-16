package com.airline.controller;

import com.airline.dto.AdminDto;
import com.airline.model.User;
import com.airline.service.AdminService;
import com.airline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService   adminService;
    private final UserRepository userRepository;

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    /**
     * FIX: Map to safe UserResponse DTO so that hashed passwords
     * are never sent over the wire to the frontend.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<AdminDto.UserResponse> users = userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(java.util.Map.of("message", "User deleted successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    private AdminDto.UserResponse toUserResponse(User u) {
        AdminDto.UserResponse r = new AdminDto.UserResponse();
        r.setId(u.getId());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole().name());
        r.setPhone(u.getPhone());
        r.setMilesBalance(u.getMilesBalance());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}
