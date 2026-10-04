package com.helper.vavahelper.controllers;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helper.vavahelper.models.User.User;
import com.helper.vavahelper.models.User.UserRole;
import com.helper.vavahelper.models.User.body.AuthenticationDTO;
import com.helper.vavahelper.models.User.body.LoginResponseDTO;
import com.helper.vavahelper.models.User.body.ResetPasswordDTO;
import com.helper.vavahelper.models.User.body.UserRegisterDTO;
import com.helper.vavahelper.repositories.UserRepository;
import com.helper.vavahelper.service.PasswordResetService;
import com.helper.vavahelper.service.TokenService;
import com.helper.vavahelper.util.PasswordPolicy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication", description = "API for user registration and login")
@RestController
@RequestMapping("auth")
public class AuthenticationController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,}$");

    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final TokenService tokenService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final String urlResetPassword;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    UserRepository repository,
                                    TokenService tokenService,
                                    PasswordResetService passwordResetService,
                                    PasswordEncoder passwordEncoder,
                                    @Value("${vavahelper.urlresetpass}") String urlResetPassword) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.tokenService = tokenService;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
        this.urlResetPassword = urlResetPassword;
    }

    @Operation(
        summary = "Register a new user",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "New user registration data",
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserRegisterDTO.class))
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data or e-mail already taken"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<String> postMethodRegister(@RequestBody @Valid UserRegisterDTO data) {
        if (!EMAIL_PATTERN.matcher(data.login()).matches()) {
            return ResponseEntity.badRequest().body("E-mail is invalid!");
        }

        if (!PasswordPolicy.isValid(data.password())) {
            return ResponseEntity.badRequest().body("Password is invalid: " + PasswordPolicy.REQUIREMENTS);
        }

        if (repository.findByLogin(data.login()) != null) {
            return ResponseEntity.badRequest().body("E-mail already taken.");
        }

        User newUser = new User(data.login(), passwordEncoder.encode(data.password()), UserRole.USER);
        this.repository.save(newUser);
        return ResponseEntity.ok("User registered successfully.");
    }

    @Operation(
        summary = "Authenticate user and return JWT",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User login credentials",
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationDTO.class))
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "JWT token returned",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
        }
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> postMethodLogin(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @Operation(
        summary = "Request password reset link",
        description = "If the e-mail belongs to an account, a reset link is sent. The response is always 200 so that "
                + "the endpoint cannot be used to discover which e-mails are registered.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Request accepted"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid e-mail"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
        }
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestParam("email") String email) {
        if (email == null || email.isBlank() || email.length() > 254) {
            return ResponseEntity.badRequest().build();
        }
        passwordResetService.createPasswordResetToken(email.trim(), urlResetPassword);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Reset user password",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Token and new password",
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ResetPasswordDTO.class))
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token, or weak password"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
        }
    )
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        if (!PasswordPolicy.isValid(dto.newPassword())) {
            return ResponseEntity.badRequest().body("Password is invalid: " + PasswordPolicy.REQUIREMENTS);
        }
        passwordResetService.resetPassword(dto.token(), dto.newPassword());
        return ResponseEntity.ok().build();
    }
}
