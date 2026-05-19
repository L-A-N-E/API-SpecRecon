package br.com.lane.SpecRecon.dto.SpecificationType;

import br.com.lane.SpecRecon.dto.Unit.UnitSummaryDTO;
import br.com.lane.SpecRecon.model.DataType;
import br.com.lane.SpecRecon.model.SpecificationTypeModel;

/**
 * DTO resumido de tipo de especificação para respostas enriquecidas.
 *
 * @param id identificador do tipo.
 * @param name nome do tipo.
 * @param category categoria do tipo.
 * @param data_type tipo de dado.
 * @param default_unit_id id da unidade padrão (se existir).
 * @param default_unit unidade padrão resumida (se existir).
 */
public record SpecificationTypeSummaryDTO(
        Long id,
        String name,
        String category,
        DataType data_type,
        Long default_unit_id,
        UnitSummaryDTO default_unit
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.SpecificationTypeModel} para DTO resumido.
     *
     * @param spec entidade de tipo de especificação.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static SpecificationTypeSummaryDTO fromModel(SpecificationTypeModel spec) {
        if (spec == null) {
            return null;
        }
        Long defaultUnitId = spec.getDefaultUnit() != null ? spec.getDefaultUnit().getId() : null;
        UnitSummaryDTO defaultUnit = UnitSummaryDTO.fromModel(spec.getDefaultUnit());
        return new SpecificationTypeSummaryDTO(
                spec.getId(),
                spec.getName(),
                spec.getCategory(),
                spec.getData_type(),
                defaultUnitId,
                defaultUnit
        );
    }
}
