package br.com.lane.SpecRecon.dto.Unit;

import br.com.lane.SpecRecon.model.Dimension;
import br.com.lane.SpecRecon.model.UnitModel;

import java.math.BigDecimal;

/**
 * DTO de resposta de unidades.
 *
 * @param id identificador da unidade.
 * @param name nome da unidade.
 * @param symbol símbolo da unidade.
 * @param dimension dimensão da unidade.
 * @param conversion_factor_to_base fator de conversão para unidade base.
 */
public record UnitsResponseDTO(
        Long id,
        String name,
        String symbol,
        Dimension dimension,
        BigDecimal conversion_factor_to_base
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.UnitModel} para DTO.
     *
     * @param unit entidade de unidade.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static UnitsResponseDTO fromModel(UnitModel unit) {
        if (unit == null) {
            return null;
        }
        return new UnitsResponseDTO(
                unit.getId(),
                unit.getName(),
                unit.getSymbol(),
                unit.getDimension(),
                unit.getConversion_factor_to_base()
        );
    }
}
