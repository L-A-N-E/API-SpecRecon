package br.com.lane.SpecRecon.controller;

import br.com.lane.SpecRecon.dto.SpecificationType.SpecificationTypesRequestDTO;
import br.com.lane.SpecRecon.dto.SpecificationType.SpecificationTypesResponseDTO;
import br.com.lane.SpecRecon.model.SpecificationTypeModel;
import br.com.lane.SpecRecon.service.AuditService;
import br.com.lane.SpecRecon.service.SpecificationTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import br.com.lane.SpecRecon.config.XSignatureHeader;
import java.util.List;

/**
 * Controller de tipos de especificação com trilha de auditoria (Eixo 5).
 */
@RestController
@RequestMapping("/specification-types")
@Tag(name = "SpecificationTypeController", description = "Endpoints para gerenciamento de tipos de especificação")
public class SpecificationTypeController {

    private final SpecificationTypeService service;
    private final AuditService auditService;

    public SpecificationTypeController(SpecificationTypeService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @Operation(summary = "Lista todos os tipos de especificação", description = "Retorna uma lista de todos os tipos de especificação cadastrados.")
    @ApiResponse(responseCode = "200", description = "Lista de tipos de especificação retornada com sucesso",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = SpecificationTypesResponseDTO.class)),
                    examples = @ExampleObject(value = "[{\"id\": 1, \"name\": \"ENGINE\"}, {\"id\": 2, \"name\": \"COLOR\"}]")))
    @GetMapping
    public List<SpecificationTypesResponseDTO> findAll() {
        return service.findAll().stream()
                .map(SpecificationTypesResponseDTO::fromModel)
                .toList();
    }

    @Operation(summary = "Busca um tipo de especificação por ID", description = "Retorna um tipo de especificação específico pelo seu ID.")
    @ApiResponse(responseCode = "200", description = "Tipo de especificação encontrado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SpecificationTypesResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"name\": \"ENGINE\"}")))
    @ApiResponse(responseCode = "404", description = "Tipo de especificação não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Tipo de especificação não encontrado\"}")))
    @GetMapping("/{id}")
    public SpecificationTypesResponseDTO findById(@PathVariable Long id) {
        return SpecificationTypesResponseDTO.fromModel(service.findById(id));
    }

    @Operation(summary = "Cria um novo tipo de especificação", description = "Adiciona um novo tipo de especificação ao sistema.")
    @ApiResponse(responseCode = "201", description = "Tipo de especificação criado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SpecificationTypesResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 3, \"name\": \"TRANSMISSION\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Nome do tipo de especificação inválido\"}")))
    @XSignatureHeader
    @PostMapping
    @Transactional
    public SpecificationTypesResponseDTO create(
            @RequestBody(description = "Dados do tipo de especificação a ser criado", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SpecificationTypesRequestDTO.class),
                            examples = @ExampleObject(value = "{\"name\": \"TRANSMISSION\"}")))
            @Valid @org.springframework.web.bind.annotation.RequestBody SpecificationTypesRequestDTO spec, HttpServletRequest request) {
        SpecificationTypeModel model = spec.toModel();
        SpecificationTypesResponseDTO created = SpecificationTypesResponseDTO.fromModel(service.create(model));
        auditService.logCreate("SpecificationType", created.id(), currentUser(), "Tipo de especificação criado", clientIp(request));
        return created;
    }

    @Operation(summary = "Atualiza um tipo de especificação existente", description = "Atualiza os dados de um tipo de especificação pelo seu ID.")
    @ApiResponse(responseCode = "200", description = "Tipo de especificação atualizado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SpecificationTypesResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"name\": \"ENGINE_TYPE\"}")))
    @ApiResponse(responseCode = "404", description = "Tipo de especificação não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Tipo de especificação não encontrado\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Nome do tipo de especificação inválido\"}")))
    @XSignatureHeader
    @PutMapping("/{id}")
    @Transactional
    public SpecificationTypesResponseDTO update(@PathVariable Long id,
                                   @RequestBody(description = "Novos dados do tipo de especificação", required = true,
                                           content = @Content(
                                                   mediaType = "application/json",
                                                   schema = @Schema(implementation = SpecificationTypesRequestDTO.class),
                                                   examples = @ExampleObject(value = "{\"name\": \"ENGINE_TYPE\"}")))
                                   @Valid @org.springframework.web.bind.annotation.RequestBody SpecificationTypesRequestDTO spec, HttpServletRequest request) {
        SpecificationTypeModel model = spec.toModel();
        SpecificationTypesResponseDTO updated = SpecificationTypesResponseDTO.fromModel(service.update(id, model));
        auditService.logUpdate("SpecificationType", id, currentUser(), "Tipo de especificação atualizado", clientIp(request));
        return updated;
    }

    @Operation(summary = "Deleta um tipo de especificação", description = "Remove um tipo de especificação do sistema pelo seu ID.")
    @ApiResponse(responseCode = "204", description = "Tipo de especificação deletado com sucesso (No Content)")
    @ApiResponse(responseCode = "404", description = "Tipo de especificação não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Tipo de especificação não encontrado\"}")))
    @XSignatureHeader
    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        service.delete(id);
        auditService.logDelete("SpecificationType", id, currentUser(), "Tipo de especificação removido", clientIp(request));
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isEmpty()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANONYMOUS";
    }
}