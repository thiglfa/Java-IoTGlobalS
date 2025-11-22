package com.wellwork.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank
    @Getter @Setter
    private String username;

    @Column(nullable = false)
    @NotBlank
    @Getter @Setter
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter @Setter
    private List<CheckIn> checkIns = new ArrayList<>();

    public User() {}

    public User(Long id, String username, String password, List<CheckIn> checkIns) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.checkIns = checkIns;

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("username é obrigatório");

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("password é obrigatório");
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank())
            throw new IllegalArgumentException("Senha inválida");
        this.password = newPassword;
    }

    public void addCheckIn(CheckIn checkIn) {
        if (checkIn == null)
            throw new IllegalArgumentException("checkIn não pode ser nulo");
        this.checkIns.add(checkIn);
    }
}
