package br.com.lane.SpecRecon.controller;

import br.com.lane.SpecRecon.dto.Users.UsersRequestDTO;
import br.com.lane.SpecRecon.dto.Users.UsersResponseDTO;
import br.com.lane.SpecRecon.model.UserModel;
import br.com.lane.SpecRecon.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.lane.SpecRecon.config.XSignatureHeader;
import java.util.List;

/**
 * Controller de usuários com RBAC (Role-Based Access Control).
 * <p>
 * Permissões:
 * - GET /users: ADMIN, ANALYST
 * - POST /users: ADMIN
 * - PUT /users: ADMIN (ou próprio usuário)
 * - DELETE /users: ADMIN
 * </p>
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de usuários com RBAC")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    /**
     * Lista todos os usuários.
     * Requer: ADMIN ou ANALYST
     *
     * @return lista de usuários em formato de resposta.
     */
    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista de todos os usuários cadastrados. Requer papel ADMIN ou ANALYST.")
    @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UsersResponseDTO.class)),
                    examples = @ExampleObject(value = "[{\"id\": 1, \"email\": \"admin@example.com\", \"role\": \"ADMIN\"}, {\"id\": 2, \"email\": \"user@example.com\", \"role\": \"USER\"}]")))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"Acesso negado: Você não tem permissão para acessar este recurso.\"}")))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public List<UsersResponseDTO> findAll() {
        return service.findAll().stream()
                .map(UsersResponseDTO::fromModel)
                .toList();
    }

    /**
     * Busca um usuário por ID.
     * Requer: ADMIN ou ANALYST
     *
     * @param id identificador do usuário.
     * @return usuário encontrado.
     */
    @Operation(summary = "Busca um usuário por ID", description = "Retorna um usuário específico pelo seu ID. Requer papel ADMIN ou ANALYST.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UsersResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"email\": \"admin@example.com\", \"role\": \"ADMIN\"}")))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Usuário não encontrado\"}")))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"Acesso negado: Você não tem permissão para acessar este recurso.\"}")))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public UsersResponseDTO findById(@PathVariable Long id) {
        return UsersResponseDTO.fromModel(service.findById(id));
    }

    /**
     * Cria um novo usuário.
     * Requer: ADMIN
     *
     * @param user dados do usuário.
     * @return usuário criado.
     */
    @Operation(summary = "Cria um novo usuário", description = "Adiciona um novo usuário ao sistema. Requer papel ADMIN.")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UsersResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 3, \"email\": \"newuser@example.com\", \"role\": \"USER\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Dados do usuário inválidos\"}")))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"Acesso negado: Você não tem permissão para acessar este recurso.\"}")))
    @XSignatureHeader
    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsersResponseDTO create(
            @RequestBody(description = "Dados para criação de um novo usuário", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UsersRequestDTO.class),
                            examples = @ExampleObject(value = "{\"email\": \"newuser@example.com\", \"password\": \"securepassword\", \"role\": \"USER\"}")))
            @Valid @org.springframework.web.bind.annotation.RequestBody UsersRequestDTO user) {
        UserModel model = user.toModel();
        return UsersResponseDTO.fromModel(service.create(model));
    }

    /**
     * Atualiza um usuário existente.
     * Requer: ADMIN
     *
     * @param id identificador do usuário.
     * @param user dados atualizados.
     * @return usuário atualizado.
     */
    @Operation(summary = "Atualiza um usuário existente", description = "Atualiza os dados de um usuário pelo seu ID. Requer papel ADMIN.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UsersResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"email\": \"admin_updated@example.com\", \"role\": \"ADMIN\"}")))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Usuário não encontrado\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Dados do usuário inválidos\"}")))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"Acesso negado: Você não tem permissão para acessar este recurso.\"}")))
    @XSignatureHeader
    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsersResponseDTO update(@PathVariable Long id,
                                   @RequestBody(description = "Dados atualizados do usuário", required = true,
                                           content = @Content(
                                                   mediaType = "application/json",
                                                   schema = @Schema(implementation = UsersRequestDTO.class),
                                                   examples = @ExampleObject(value = "{\"email\": \"admin_updated@example.com\", \"password\": \"newsecurepassword\", \"role\": \"ADMIN\"}")))
                                   @Valid @org.springframework.web.bind.annotation.RequestBody UsersRequestDTO user) {
        UserModel model = user.toModel();
        return UsersResponseDTO.fromModel(service.update(id, model));
    }

    /**
     * Remove um usuário por ID.
     * Requer: ADMIN
     *
     * @param id identificador do usuário.
     */
    @Operation(summary = "Deleta um usuário", description = "Remove um usuário do sistema pelo seu ID. Requer papel ADMIN.")
    @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso (No Content)")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Usuário não encontrado\"}")))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 403, \"message\": \"Acesso negado: Você não tem permissão para acessar este recurso.\"}")))
    @XSignatureHeader
    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}