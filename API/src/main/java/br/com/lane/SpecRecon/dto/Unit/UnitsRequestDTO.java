package br.com.lane.SpecRecon.dto.Unit;

import br.com.lane.SpecRecon.model.Dimension;
import br.com.lane.SpecRecon.model.UnitModel;
import br.com.lane.SpecRecon.validation.SafeString;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO de requisição para unidades com validações de segurança.
 * <p>
 * Validações:
 * - Name, Symbol: obrigatórios, sanitizados, máx 255 caracteres
 * - Dimension: obrigatório
 * - Conversion Factor: positivo e válido
 * </p>
 *
 * @param name nome da unidade (sanitizado)
 * @param symbol símbolo da unidade (sanitizado)
 * @param dimension dimensão da unidade
 * @param conversion_factor_to_base fator de conversão (deve ser positivo)
 */
public record UnitsRequestDTO(
        @NotBlank(message = "O nome da unidade é obrigatório")
        @SafeString(message = "Nome contém caracteres não permitidos", maxLength = 255)
        String name,

        @NotBlank(message = "O símbolo da unidade é obrigatório")
        @SafeString(message = "Símbolo contém caracteres não permitidos", maxLength = 64)
        String symbol,

        @NotNull(message = "A dimensão é obrigatória")
        Dimension dimension,

        @NotNull(message = "Fator de conversão é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Fator de conversão deve ser positivo")
        BigDecimal conversion_factor_to_base
) {
    /**
     * Converte o DTO em entidade de domínio.
     *
     * @return instância de {@link UnitModel} preenchida.
     */
    public UnitModel toModel() {
        UnitModel unit = new UnitModel();
        unit.setName(name.trim());
        unit.setSymbol(symbol.trim().toUpperCase());
        unit.setDimension(dimension);
        unit.setConversion_factor_to_base(conversion_factor_to_base);
        return unit;
    }
}
