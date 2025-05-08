package org.babynexign.babybilling.authservice.services;

import feign.FeignException;
import org.babynexign.babybilling.authservice.client.BrtClient;
import org.babynexign.babybilling.authservice.dto.AuthResponse;
import org.babynexign.babybilling.authservice.dto.LoginRequest;
import org.babynexign.babybilling.authservice.dto.PersonDTO;
import org.babynexign.babybilling.authservice.dto.RegisterRequest;
import org.babynexign.babybilling.authservice.entity.Role;
import org.babynexign.babybilling.authservice.entity.User;
import org.babynexign.babybilling.authservice.entity.enums.ERole;
import org.babynexign.babybilling.authservice.repository.RoleRepository;
import org.babynexign.babybilling.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BrtClient brtServiceClient;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtService jwtService, BrtClient brtServiceClient) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.brtServiceClient = brtServiceClient;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.username() == null && request.msisdn() == null) {
            throw new RuntimeException("Either username or msisdn must be provided");
        }

        if (request.username() != null && userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username is already taken");
        }

        if (request.msisdn() != null && userRepository.existsByMsisdn(request.msisdn())) {
            throw new RuntimeException("MSISDN is already in use");
        }

        Set<String> strRoles = request.roles() != null ? request.roles() : new HashSet<>();
        boolean isSubscriber = strRoles.isEmpty() || strRoles.stream()
                .anyMatch(role -> role.equalsIgnoreCase("subscriber"));

        if (isSubscriber && request.msisdn() != null) {
            try {
                ResponseEntity<PersonDTO> response = brtServiceClient.getPersonByMsisdn(request.msisdn());

                if (response.getBody() == null) {
                    throw new RuntimeException("Person with msisdn " + request.msisdn() + " not found in BRT");
                }

            } catch (FeignException e) {
                throw new RuntimeException("Failed to verify msisdn in BRT: " + e.getMessage());
            }
        }

        User user = new User(
                request.username(),
                request.msisdn(),
                passwordEncoder.encode(request.password())
        );

        Set<Role> roles = new HashSet<>();

        if (strRoles.isEmpty()) {
            Role subscriberRole = roleRepository.findByName(ERole.ROLE_SUBSCRIBER)
                    .orElseThrow(() -> new RuntimeException("Error: Role SUBSCRIBER is not found."));
            roles.add(subscriberRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role ADMIN is not found."));
                        roles.add(adminRole);
                        break;
                    case "subscriber":
                        Role subscriberRole = roleRepository.findByName(ERole.ROLE_SUBSCRIBER)
                                .orElseThrow(() -> new RuntimeException("Error: Role SUBSCRIBER is not found."));
                        roles.add(subscriberRole);
                        break;
                    default:
                        throw new RuntimeException("Error: Role " + role + " is not found.");
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        String subject = String.valueOf(savedUser.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));

        if (savedUser.getMsisdn() != null) {
            claims.put("msisdn", savedUser.getMsisdn());
        }

        if (savedUser.getUsername() != null) {
            claims.put("username", savedUser.getUsername());
        }

        String accessToken = jwtService.generateToken(subject, claims);
        String refreshToken = jwtService.generateRefreshToken(subject);

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user;
        Optional<User> userByUsername = userRepository.findByUsername(request.login());

        if (userByUsername.isPresent()) {
            user = userByUsername.get();
        } else {
            Optional<User> userByMsisdn = userRepository.findByMsisdn(request.login());
            if (userByMsisdn.isPresent()) {
                user = userByMsisdn.get();
            } else {
                throw new RuntimeException("User not found with login: " + request.login());
            }
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String subject = String.valueOf(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));

        if (user.getMsisdn() != null) {
            claims.put("msisdn", user.getMsisdn());
        }

        String accessToken = jwtService.generateToken(subject, claims);
        String refreshToken = jwtService.generateRefreshToken(subject);

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }
}
