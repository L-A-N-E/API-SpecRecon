package br.com.lane.SpecRecon.controller;

import br.com.lane.SpecRecon.dto.Vehicles.VehiclesRequestDTO;
import br.com.lane.SpecRecon.dto.Vehicles.VehiclesResponseDTO;
import br.com.lane.SpecRecon.service.AuditService;
import br.com.lane.SpecRecon.service.VehicleService;
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
 * Controller de veículos com trilha de auditoria (Eixo 5).
 */
@RestController
@RequestMapping("/vehicles")
@Tag(name = "Veículos", description = "Endpoints para gerenciamento de veículos")
public class VehicleController {

    private final VehicleService service;
    private final AuditService auditService;

    public VehicleController(VehicleService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @Operation(summary = "Lista todos os veículos", description = "Retorna uma lista de todos os veículos cadastrados, incluindo suas especificações.")
    @ApiResponse(responseCode = "200", description = "Lista de veículos retornada com sucesso",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = VehiclesResponseDTO.class)),
                    examples = @ExampleObject(value = "[{\"id\": 1, \"model\": \"Ford Focus\", \"year\": 2022, \"licensePlate\": \"ABC1234\", \"specifications\": [{\"id\": 101, \"type\": \"ENGINE\", \"value\": \"2.0L\"}]}, {\"id\": 2, \"model\": \"Ford Mustang\", \"year\": 2023, \"licensePlate\": \"DEF5678\", \"specifications\": []}]")))
    @GetMapping
    public List<VehiclesResponseDTO> findAll() {
        return service.findAllResponses();
    }

    @Operation(summary = "Busca um veículo por ID", description = "Retorna um veículo específico pelo seu ID, incluindo suas especificações.")
    @ApiResponse(responseCode = "200", description = "Veículo encontrado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehiclesResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"model\": \"Ford Focus\", \"year\": 2022, \"licensePlate\": \"ABC1234\", \"specifications\": [{\"id\": 101, \"type\": \"ENGINE\", \"value\": \"2.0L\"}]}")))
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Veículo não encontrado\"}")))
    @GetMapping("/{id}")
    public VehiclesResponseDTO findById(@PathVariable Long id) {
        return service.findByIdWithSpecifications(id);
    }

    @Operation(summary = "Cria um novo veículo", description = "Adiciona um novo veículo ao sistema com suas especificações.")
    @ApiResponse(responseCode = "201", description = "Veículo criado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehiclesResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 3, \"model\": \"Ford Bronco\", \"year\": 2024, \"licensePlate\": \"GHI9012\", \"specifications\": [{\"id\": 102, \"type\": \"COLOR\", \"value\": \"Black\"}]}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Dados do veículo inválidos\"}")))
    @XSignatureHeader
    @PostMapping
    @Transactional
    public VehiclesResponseDTO create(
            @RequestBody(description = "Dados do veículo a ser criado, incluindo especificações", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VehiclesRequestDTO.class),
                            examples = @ExampleObject(value = "{\"model\": \"Ford Bronco\", \"year\": 2024, \"licensePlate\": \"GHI9012\", \"specifications\": [{\"typeId\": 1, \"value\": \"Black\"}]}")))
            @Valid @org.springframework.web.bind.annotation.RequestBody VehiclesRequestDTO vehicle, HttpServletRequest request) {
        VehiclesResponseDTO created = service.createWithSpecifications(vehicle);
        auditService.logCreate("Vehicle", created.id(), currentUser(), "Veículo criado", clientIp(request));
        return created;
    }

    @Operation(summary = "Atualiza um veículo existente", description = "Atualiza os dados de um veículo pelo seu ID, incluindo suas especificações.")
    @ApiResponse(responseCode = "200", description = "Veículo atualizado com sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehiclesResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\": 1, \"model\": \"Ford Focus RS\", \"year\": 2022, \"licensePlate\": \"ABC1234\", \"specifications\": [{\"id\": 101, \"type\": \"ENGINE\", \"value\": \"2.3L EcoBoost\"}]}")))
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Veículo não encontrado\"}")))
    @ApiResponse(responseCode = "400", description = "Requisição inválida",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Dados do veículo inválidos\"}")))
    @XSignatureHeader
    @PutMapping("/{id}")
    @Transactional
    public VehiclesResponseDTO update(@PathVariable Long id,
                                   @RequestBody(description = "Novos dados do veículo, incluindo especificações", required = true,
                                           content = @Content(
                                                   mediaType = "application/json",
                                                   schema = @Schema(implementation = VehiclesRequestDTO.class),
                                                   examples = @ExampleObject(value = "{\"model\": \"Ford Focus RS\", \"year\": 2022, \"licensePlate\": \"ABC1234\", \"specifications\": [{\"typeId\": 1, \"value\": \"2.3L EcoBoost\"}]}")))
                                   @Valid @org.springframework.web.bind.annotation.RequestBody VehiclesRequestDTO vehicle, HttpServletRequest request) {
        VehiclesResponseDTO updated = service.updateWithSpecifications(id, vehicle);
        auditService.logUpdate("Vehicle", id, currentUser(), "Veículo atualizado", clientIp(request));
        return updated;
    }

    @Operation(summary = "Deleta um veículo", description = "Remove um veículo do sistema pelo seu ID.")
    @ApiResponse(responseCode = "204", description = "Veículo deletado com sucesso (No Content)")
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Veículo não encontrado\"}")))
    @XSignatureHeader
    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        service.delete(id);
        auditService.logDelete("Vehicle", id, currentUser(), "Veículo removido", clientIp(request));
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