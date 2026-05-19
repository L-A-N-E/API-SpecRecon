package br.com.lane.SpecRecon.controller;

import br.com.lane.SpecRecon.dto.Unit.UnitsRequestDTO;
import br.com.lane.SpecRecon.dto.Unit.UnitsResponseDTO;
import br.com.lane.SpecRecon.model.UnitModel;
import br.com.lane.SpecRecon.service.AuditService;
import br.com.lane.SpecRecon.service.UnitService;
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
 * Controller de unidades com trilha de auditoria (Eixo 5).
 */
@RestController
@RequestMapping("/units")
@Tag(name = "Unidades", description = "Endpoints para gerenciamento de unidades de medida")
public class UnitController {

    private final UnitService service;
    private final AuditService auditService;

    public UnitController(UnitService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @Operation(summary = "Lista todas as unidades", description = "Retorna uma lista de todas as unidades de medida cadastradas.")
    @ApiResponse(responseCode = "200", description = "Lista de unidades retornada com sucesso",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UnitsResponseDTO.class)),
                    examples = @ExampleObject(value = "[{\"id\": 1, \"name\": \"Kilogram\", \"symbol\": \"kg\"}, {\"id\": 2, \"name\": \"Liter\", \"symbol\": \"L\"}]")))
    @GetMapping
    public List<UnitsResponseDTO> findAll() {
        return service.findAll().stream()
                .map(UnitsResponseDTO::fromModel)
                .toList();
    }

    @Operation(summary = "Busca uma unidade por ID", description = "Retorna uma unidade de medida específica pelo seu ID.")
    @ApiResponse(responseCode = "200", description = "Unidade encontrada com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UnitsResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"name\": \"Kilogram\", \"symbol\": \"kg\"}")))
    @ApiResponse(responseCode = "404", description = "Unidade não encontrada",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Unidade não encontrada\"}")))
    @GetMapping("/{id}")
    public UnitsResponseDTO findById(@PathVariable Long id) {
        return UnitsResponseDTO.fromModel(service.findById(id));
    }

    @Operation(summary = "Cria uma nova unidade", description = "Adiciona uma nova unidade de medida ao sistema.")
    @ApiResponse(responseCode = "201", description = "Unidade criada com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UnitsResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 3, \"name\": \"Meter\", \"symbol\": \"m\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Nome ou símbolo da unidade inválido\"}")))
    @XSignatureHeader
    @PostMapping
    @Transactional
    public UnitsResponseDTO create(
            @RequestBody(description = "Dados da unidade a ser criada", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UnitsRequestDTO.class),
                            examples = @ExampleObject(value = "{\"name\": \"Meter\", \"symbol\": \"m\"}")))
            @Valid @org.springframework.web.bind.annotation.RequestBody UnitsRequestDTO unit, HttpServletRequest request) {
        UnitModel model = unit.toModel();
        UnitsResponseDTO created = UnitsResponseDTO.fromModel(service.create(model));
        auditService.logCreate("Unit", created.id(), currentUser(), "Unidade criada", clientIp(request));
        return created;
    }

    @Operation(summary = "Atualiza uma unidade existente", description = "Atualiza os dados de uma unidade de medida pelo seu ID.")
    @ApiResponse(responseCode = "200", description = "Unidade atualizada com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UnitsResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"name\": \"Gram\", \"symbol\": \"g\"}")))
    @ApiResponse(responseCode = "404", description = "Unidade não encontrada",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Unidade não encontrada\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Nome ou símbolo da unidade inválido\"}")))
    @XSignatureHeader
    @PutMapping("/{id}")
    @Transactional
    public UnitsResponseDTO update(@PathVariable Long id,
                                   @RequestBody(description = "Novos dados da unidade", required = true,
                                           content = @Content(
                                                   mediaType = "application/json",
                                                   schema = @Schema(implementation = UnitsRequestDTO.class),
                                                   examples = @ExampleObject(value = "{\"name\": \"Gram\", \"symbol\": \"g\"}")))
                                   @Valid @org.springframework.web.bind.annotation.RequestBody UnitsRequestDTO unit, HttpServletRequest request) {
        UnitModel model = unit.toModel();
        UnitsResponseDTO updated = UnitsResponseDTO.fromModel(service.update(id, model));
        auditService.logUpdate("Unit", id, currentUser(), "Unidade atualizada", clientIp(request));
        return updated;
    }

    @Operation(summary = "Deleta uma unidade", description = "Remove uma unidade de medida do sistema pelo seu ID.")
    @ApiResponse(responseCode = "204", description = "Unidade deletada com sucesso (No Content)")
    @ApiResponse(responseCode = "404", description = "Unidade não encontrada",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Unidade não encontrada\"}")))
    @XSignatureHeader
    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        service.delete(id);
        auditService.logDelete("Unit", id, currentUser(), "Unidade removida", clientIp(request));
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