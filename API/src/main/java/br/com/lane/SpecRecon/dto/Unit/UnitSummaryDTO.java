package br.com.lane.SpecRecon.dto.Unit;

import br.com.lane.SpecRecon.model.Dimension;
import br.com.lane.SpecRecon.model.UnitModel;

/**
 * DTO resumido de unidade para respostas enriquecidas.
 *
 * @param id identificador da unidade.
 * @param name nome da unidade.
 * @param symbol símbolo da unidade.
 * @param dimension dimensão da unidade.
 */
public record UnitSummaryDTO(
        Long id,
        String name,
        String symbol,
        Dimension dimension
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.UnitModel} para DTO resumido.
     *
     * @param unit entidade de unidade.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static UnitSummaryDTO fromModel(UnitModel unit) {
        if (unit == null) {
            return null;
        }
        return new UnitSummaryDTO(unit.getId(), unit.getName(), unit.getSymbol(), unit.getDimension());
    }
}

