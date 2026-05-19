package br.com.lane.SpecRecon.dto.Vehicles;

import br.com.lane.SpecRecon.model.VehicleModel;

/**
 * DTO resumido de veículo para respostas enriquecidas.
 *
 * @param id identificador do veículo.
 * @param brand marca do veículo.
 * @param model modelo do veículo.
 * @param version versão do veículo.
 */
public record VehicleSummaryDTO(
        Long id,
        String brand,
        String model,
        String version
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.VehicleModel} para DTO resumido.
     *
     * @param vehicle entidade de veículo.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static VehicleSummaryDTO fromModel(VehicleModel vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleSummaryDTO(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVersion()
        );
    }
}

