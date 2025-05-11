package org.babynexign.babybilling.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Pattern(regexp = "^[0-9]{11}$", message = "MSISDN must contain exactly 11 digits")
    @Column(unique = true)
    private String msisdn;

    @NotBlank(message = "Password must not be empty")
    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();

    public User(String username, String msisdn, String password) {
        this.username = username;
        this.msisdn = msisdn;
        this.password = password;
    }
}
