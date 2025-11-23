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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        this.rabbitTemplate = rabbitTemplate;
    }

    // ======================
    // CREATE USER
    // ======================
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

        // RabbitMQ
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(
                        "user.exchange",
                        "user.welcome",
                        "Bem-vindo(a), " + u.getUsername() + "!"
                );
            } catch (AmqpException ignored) {}
        }

        return toResponse(u);
    }

    // ======================
    // GET USER BY ID - liberado para QUALQUER autenticado
    // ======================
    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getById(Long id) {

        // Apenas garante que o usuário está autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Acesso negado. Token inválido.");
        }

        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + id));

        return toResponse(u);
    }

    // ======================
    // LIST USERS
    // ======================
    public Page<UserResponseDTO> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    // ======================
    // findEntityByUsername
    // ======================
    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + username));
    }

    // ======================
    // /me endpoint
    // ======================
    @Cacheable(value = "users", key = "#username")
    public UserResponseDTO findByUsernameResponse(String username) {
        return toResponse(findEntityByUsername(username));
    }

    // ======================
    // update password - RESTRITO ao próprio usuário
    // ======================
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void updatePassword(Long userId, String newPassword) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedUser = auth.getName();

        User me = findEntityByUsername(loggedUser);

        if (!me.getId().equals(userId)) {
            throw new SecurityException("Você não pode alterar a senha de outro usuário.");
        }

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User não encontrado: " + userId));

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    // ======================
    // delete - RESTRITO ao próprio usuário
    // ======================
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void delete(Long id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loggedUser = auth.getName();

        User me = findEntityByUsername(loggedUser);

        if (!me.getId().equals(id)) {
            throw new SecurityException("Você não pode excluir outro usuário.");
        }

        userRepository.deleteById(id);
    }

    // ======================
    // converter
    // ======================
    private UserResponseDTO toResponse(User u) {
        UserResponseDTO r = new UserResponseDTO();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        return r;
    }
}
