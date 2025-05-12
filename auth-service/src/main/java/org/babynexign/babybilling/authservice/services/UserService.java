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
import org.babynexign.babybilling.authservice.exception.InvalidCredentialsException;
import org.babynexign.babybilling.authservice.exception.InvalidRoleException;
import org.babynexign.babybilling.authservice.exception.MsisdnAlreadyInUseException;
import org.babynexign.babybilling.authservice.exception.MsisdnVerificationException;
import org.babynexign.babybilling.authservice.exception.RoleNotFoundException;
import org.babynexign.babybilling.authservice.exception.UserNotFoundException;
import org.babynexign.babybilling.authservice.exception.UsernameAlreadyExistsException;
import org.babynexign.babybilling.authservice.repository.RoleRepository;
import org.babynexign.babybilling.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for user authentication and management operations.
 * Handles user registration, login, and token generation.
 */
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

    /**
     * Registers a new user in the system.
     * Validates credentials, assigns roles, and generates authentication tokens.
     *
     * @param request The registration request with user credentials
     * @return AuthResponse containing access and refresh tokens
     * @throws InvalidCredentialsException if neither username nor MSISDN is provided
     * @throws UsernameAlreadyExistsException if the username is already taken
     * @throws MsisdnAlreadyInUseException if the MSISDN is already in use
     * @throws MsisdnVerificationException if the MSISDN verification fails
     * @throws RoleNotFoundException if a requested role doesn't exist
     * @throws InvalidRoleException if an invalid role is specified
     */
    public AuthResponse register(RegisterRequest request) {
        if (request.username() == null && request.msisdn() == null) {
            throw new InvalidCredentialsException("Either username or msisdn must be provided");
        }

        if (request.username() != null && userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username is already taken");
        }

        if (request.msisdn() != null && userRepository.existsByMsisdn(request.msisdn())) {
            throw new MsisdnAlreadyInUseException("MSISDN is already in use");
        }

        Set<String> strRoles = request.roles() != null ? request.roles() : new HashSet<>();
        boolean isSubscriber = strRoles.isEmpty() || strRoles.stream()
                .anyMatch(role -> role.equalsIgnoreCase("subscriber"));

        if (isSubscriber && request.msisdn() != null) {
            try {
                ResponseEntity<PersonDTO> response = brtServiceClient.getPersonByMsisdn(request.msisdn());

                if (response.getBody() == null) {
                    throw new MsisdnVerificationException("Person with msisdn " + request.msisdn() + " not found in BRT");
                }

            } catch (FeignException e) {
                throw new MsisdnVerificationException("Failed to verify msisdn in BRT: " + e.getMessage(), e);
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
                    .orElseThrow(() -> new RoleNotFoundException("Error: Role SUBSCRIBER is not found."));
            roles.add(subscriberRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RoleNotFoundException("Error: Role ADMIN is not found."));
                        roles.add(adminRole);
                        break;
                    case "subscriber":
                        Role subscriberRole = roleRepository.findByName(ERole.ROLE_SUBSCRIBER)
                                .orElseThrow(() -> new RoleNotFoundException("Error: Role SUBSCRIBER is not found."));
                        roles.add(subscriberRole);
                        break;
                    default:
                        throw new InvalidRoleException("Error: Role " + role + " is not found.");
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

    /**
     * Authenticates a user and generates access tokens.
     *
     * @param request The login request containing credentials
     * @return AuthResponse containing access and refresh tokens
     * @throws UserNotFoundException if no user is found with the provided login
     * @throws InvalidCredentialsException if the password is incorrect
     */
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
                throw new UserNotFoundException("User not found with login: " + request.login());
            }
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
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
