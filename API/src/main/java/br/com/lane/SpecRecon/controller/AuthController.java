package br.com.lane.SpecRecon.controller;

import br.com.lane.SpecRecon.dto.Users.UsersRequestDTO;
import br.com.lane.SpecRecon.model.UserModel;
import br.com.lane.SpecRecon.security.JwtTokenProvider;
import br.com.lane.SpecRecon.service.AuditService;
import br.com.lane.SpecRecon.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação com auditoria.
 * Endpoints para login, registro e renovação de token.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de login, registro e renovação de token")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    /**
     * Realiza login do usuário e retorna JWT.
     * Registra tentativa de login na auditoria.
     *
     * @param loginRequest email e senha
     * @return token JWT e refresh token
     */
    @Operation(summary = "Realiza login do usuário", description = "Autentica um usuário e retorna um token JWT e um refresh token.")
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class),
                    examples = @ExampleObject(value = "{\"token\": \"eyJhbGciOiJIUzI1Ni...\", \"refreshToken\": \"eyJhbGciOiJIUzI1Ni...\", \"userId\": 1, \"role\": \"ADMIN\"}")))
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"E-mail ou senha inválidos\"}")))
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(description = "Credenciais de login do usuário", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = "{\"email\": \"newuser@example.com\", \"password\": \"@Securepassword123\"}")))
            @org.springframework.web.bind.annotation.RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        try {
            // Autenticar usuário
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Buscar detalhes do usuário
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserModel user = userService.findByEmail(userDetails.getUsername());

            // Migração automática de senhas legacy (sem prefixo {bcrypt}) para BCrypt.
            String storedPassword = user.getPassword();
            if (storedPassword != null && !storedPassword.startsWith("{bcrypt}") && !storedPassword.startsWith("$2a$") && !storedPassword.startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
                userService.update(user.getId(), user);
                auditService.logAction("PASSWORD_MIGRATED", "User", user.getId(), user.getEmail(),
                        "Senha migrada de texto puro para BCrypt", clientIp, "SUCCESS");
            }

            // Gerar tokens
            String token = jwtTokenProvider.generateToken(userDetails, user.getId(), user.getRole().name());
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, user.getId());

            // Auditoria: login bem-sucedido
            auditService.logSuccessfulLogin(user.getEmail(), user.getId(), clientIp);

            return ResponseEntity.ok(new LoginResponse(token, refreshToken, user.getId(), user.getRole().name()));
        } catch (BadCredentialsException e) {
            // Auditoria: falha de autenticação
            auditService.logFailedLogin(loginRequest.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "E-mail ou senha inválidos"));
        } catch (Exception e) {
            auditService.logFailedLogin(loginRequest.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "E-mail ou senha inválidos"));
        }
    }

    /**
     * Realiza registro de novo usuário com auditoria.
     *
     * @param registerRequest dados do novo usuário
     * @return dados do usuário criado
     */
    @Operation(summary = "Registra um novo usuário", description = "Cria uma nova conta de usuário no sistema.")
    @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegisterResponse.class),
                    examples = @ExampleObject(value = "{\"id\": 2, \"email\": \"newuser@example.com\", \"role\": \"USER\"}")))
    @ApiResponse(responseCode = "409", description = "Conflito (e.g., e-mail já cadastrado)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 409, \"message\": \"E-mail já cadastrado\"}")))
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid
                                      @RequestBody(description = "Dados para registro de um novo usuário", required = true,
                                              content = @Content(
                                                      mediaType = "application/json",
                                                      schema = @Schema(implementation = UsersRequestDTO.class),
                                                      examples = @ExampleObject(value = "{\"email\": \"newuser@example.com\", \"password\": \"@Securepassword123\", \"role\": \"USER\"}")))
                                      @org.springframework.web.bind.annotation.RequestBody UsersRequestDTO registerRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        try {
            UserModel user = registerRequest.toModel();
            // Criptografar senha com BCrypt
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            UserModel createdUser = userService.create(user);

            // Auditoria: novo usuário criado
            auditService.logCreate("User", createdUser.getId(), "SYSTEM",
                    "Novo usuário registrado", clientIp);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse(createdUser.getId(), createdUser.getEmail(), createdUser.getRole().name()));
        } catch (br.com.lane.SpecRecon.exception.ConflictException e) {
            auditService.logAction("REGISTER_FAILED", "User", 0L, "SYSTEM",
                    e.getMessage(), clientIp, "FAILED");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(409, e.getMessage()));
        }
    }

    /**
     * Renova JWT usando refresh token com auditoria.
     *
     * @param refreshTokenRequest refresh token
     * @return novo JWT
     */
    @Operation(summary = "Renova o token JWT", description = "Utiliza um refresh token para obter um novo token de acesso JWT.")
    @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class),
                    examples = @ExampleObject(value = "{\"token\": \"eyJhbGciOiJIUzI1Ni...\", \"refreshToken\": \"eyJhbGciOiJIUzI1Ni...\", \"userId\": 1, \"role\": \"ADMIN\"}")))
    @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"Refresh token inválido\"}")))
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody(description = "Refresh token para renovação", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(value = "{\"refreshToken\": \"eyJhbGciOiJIUzI1Ni...\"}")))
            @org.springframework.web.bind.annotation.RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();

            // Validar refresh token
            String username = jwtTokenProvider.extractUsername(refreshToken);
            Long userId = jwtTokenProvider.extractUserId(refreshToken);
            String role = jwtTokenProvider.extractRole(refreshToken);

            if (jwtTokenProvider.isTokenExpired(refreshToken)) {
                auditService.logAction("TOKEN_REFRESH_FAILED", "User", userId, username,
                        "Refresh token expirado", clientIp, "FAILED");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(401, "Refresh token expirado"));
            }

            // Gerar novo token
            UserModel user = userService.findByEmail(username);
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    .build();

            String newToken = jwtTokenProvider.generateToken(userDetails, userId, role);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails, userId);

            return ResponseEntity.ok(new LoginResponse(newToken, newRefreshToken, userId, role));
        } catch (Exception e) {
            auditService.logAction("TOKEN_REFRESH_FAILED", "User", 0L, "SYSTEM",
                    "Refresh token inválido", clientIp, "FAILED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Refresh token inválido"));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // DTOs para requisição/resposta
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;
        private String refreshToken;
        private Long userId;
        private String role;

        public LoginResponse(String token, String refreshToken, Long userId, String role) {
            this.token = token;
            this.refreshToken = refreshToken;
            this.userId = userId;
            this.role = role;
        }

        public String getToken() { return token; }
        public String getRefreshToken() { return refreshToken; }
        public Long getUserId() { return userId; }
        public String getRole() { return role; }
    }

    public static class RegisterResponse {
        private Long id;
        private String email;
        private String role;

        public RegisterResponse(Long id, String email, String role) {
            this.id = id;
            this.email = email;
            this.role = role;
        }

        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class ErrorResponse {
        private int status;
        private String message;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
    }
}