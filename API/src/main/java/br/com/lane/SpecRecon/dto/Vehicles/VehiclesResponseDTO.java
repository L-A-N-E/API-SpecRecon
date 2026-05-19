package br.com.lane.SpecRecon.dto.Vehicles;

import br.com.lane.SpecRecon.dto.VehicleSpecification.VehicleSpecificationsResponseDTO;
import br.com.lane.SpecRecon.model.VehicleModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta de veículos.
 * <p>
 * Estrutura exposta no retorno da API, sem regras de validação.
 * </p>
 *
 * @param id identificador do veículo.
 * @param brand marca do veículo.
 * @param model modelo do veículo.
 * @param version versão do veículo.
 * @param created_at data/hora de criação.
 * @param specifications especificações do veículo (quando solicitadas).
 */
public record VehiclesResponseDTO(
        Long id,
        String brand,
        String model,
        String version,
        LocalDateTime created_at,
        List<VehicleSpecificationsResponseDTO> specifications
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.VehicleModel} para DTO.
     *
     * @param vehicle entidade de veículo.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static VehiclesResponseDTO fromModel(VehicleModel vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehiclesResponseDTO(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVersion(),
                vehicle.getCreated_at(),
                null
        );
    }

    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.VehicleModel} para DTO com especificações.
     *
     * @param vehicle entidade de veículo.
     * @param specifications especificações do veículo.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static VehiclesResponseDTO fromModelWithSpecifications(
            VehicleModel vehicle,
            List<VehicleSpecificationsResponseDTO> specifications
    ) {
        if (vehicle == null) {
            return null;
        }
        return new VehiclesResponseDTO(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVersion(),
                vehicle.getCreated_at(),
                specifications
        );
    }
}
