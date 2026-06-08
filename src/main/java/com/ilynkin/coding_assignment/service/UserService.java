package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.dto.request.UserRequest;
import com.ilynkin.coding_assignment.dto.response.UserResponse;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.entity.UserRole;
import com.ilynkin.coding_assignment.exception.DuplicateResourceException;
import com.ilynkin.coding_assignment.exception.InvalidRoleException;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.mapper.UserMapper;
import com.ilynkin.coding_assignment.repository.UserRepository;
import com.ilynkin.coding_assignment.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User with email " + request.email() + " already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(request.password());
        user.setRole(getUserRole(request.role().name()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User with email " + request.email() + " already exists");
        }

        userMapper.updateEntity(request, user);
        user.setPassword(request.password());
        user.setRole(getUserRole(request.role().name()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (userRepository.deleteUserById(id) == 0) {
            throw new ResourceNotFoundException("User not found");
        }
    }


    private UserRole getUserRole(String code) {
        return userRoleRepository.findByCode(code)
                .orElseThrow(() -> new InvalidRoleException("Unknown role: " + code));
    }
}
