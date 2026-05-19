package br.com.lane.SpecRecon.dto.VehicleSpecification;

import br.com.lane.SpecRecon.dto.SpecificationType.SpecificationTypeSummaryDTO;
import br.com.lane.SpecRecon.dto.Unit.UnitSummaryDTO;
import br.com.lane.SpecRecon.dto.Vehicles.VehicleSummaryDTO;
import br.com.lane.SpecRecon.model.VehicleSpecificationModel;

import java.math.BigDecimal;

/**
 * DTO de resposta de especificações de veículo.
 *
 * @param id identificador da especificação.
 * @param vehicle_id id do veículo.
 * @param vehicle veículo resumido.
 * @param specification_type_id id do tipo de especificação.
 * @param unit_id id da unidade (quando aplicável).
 * @param unit unidade resumida (quando aplicável).
 * @param specification_type tipo de especificação resumido.
 * @param value_numeric valor numérico.
 * @param value_text valor em texto.
 * @param is_available disponibilidade.
 */
public record VehicleSpecificationsResponseDTO(
        Long id,
        Long vehicle_id,
        VehicleSummaryDTO vehicle,
        Long specification_type_id,
        Long unit_id,
        UnitSummaryDTO unit,
        SpecificationTypeSummaryDTO specification_type,
        BigDecimal value_numeric,
        String value_text,
        Boolean is_available
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.VehicleSpecificationModel} para DTO.
     *
     * @param spec entidade de especificação.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static VehicleSpecificationsResponseDTO fromModel(VehicleSpecificationModel spec) {
        if (spec == null) {
            return null;
        }
        Long vehicleId = spec.getVehicle() != null ? spec.getVehicle().getId() : null;
        Long typeId = spec.getSpecificationType() != null ? spec.getSpecificationType().getId() : null;
        Long unitId = spec.getUnit() != null ? spec.getUnit().getId() : null;
        VehicleSummaryDTO vehicle = VehicleSummaryDTO.fromModel(spec.getVehicle());
        UnitSummaryDTO unit = UnitSummaryDTO.fromModel(spec.getUnit());
        SpecificationTypeSummaryDTO specType = SpecificationTypeSummaryDTO.fromModel(spec.getSpecificationType());
        return new VehicleSpecificationsResponseDTO(
                spec.getId(),
                vehicleId,
                vehicle,
                typeId,
                unitId,
                unit,
                specType,
                spec.getValue_numeric(),
                spec.getValue_text(),
                spec.getIs_available()
        );
    }
}
