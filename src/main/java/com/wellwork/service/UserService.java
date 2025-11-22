package com.wellwork.service;

import com.wellwork.dto.UserRequestDTO;
import com.wellwork.dto.UserResponseDTO;
import com.wellwork.model.entities.User;
import com.wellwork.repository.UserRepository;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RabbitTemplate rabbitTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate; // opcional
    }

    // ============================================
    // CREATE USER
    // ============================================
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponseDTO create(UserRequestDTO dto) {

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username já existe");
        }

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(u);

        // ENVIO OPCIONAL PARA RABBITMQ
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(
                        "user.exchange",
                        "user.welcome",
                        "Bem-vindo(a), " + u.getUsername() + "!"
                );
            } catch (AmqpException ignored) {
                System.out.println("⚠️ RabbitMQ indisponível. Ignorando.");
            }
        }

        return toResponse(u);
    }

    // ============================================
    // GET BY ID (CACHEABLE)
    // ============================================
    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getById(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + id));

        return toResponse(u);
    }

    // ============================================
    // LIST USERS (REQUIRED BY UserController)
    // ============================================
    public Page<UserResponseDTO> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    // ============================================
    // FIND USER ENTITY BY USERNAME (NEEDED BY CheckInController)
    // ============================================
    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + username));
    }

    // ============================================
    // GET BY USERNAME (CACHEABLE)
    // ============================================
    @Cacheable(value = "users", key = "#username")
    public UserResponseDTO findByUsernameResponse(String username) {
        return toResponse(findEntityByUsername(username));
    }

    // ============================================
    // UPDATE PASSWORD
    // ============================================
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void updatePassword(Long userId, String newPassword) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + userId));

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    // ============================================
    // DELETE USER
    // ============================================
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    // ============================================
    // CONVERTER
    // ============================================
    private UserResponseDTO toResponse(User u) {
        UserResponseDTO r = new UserResponseDTO();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        return r;
    }
}
