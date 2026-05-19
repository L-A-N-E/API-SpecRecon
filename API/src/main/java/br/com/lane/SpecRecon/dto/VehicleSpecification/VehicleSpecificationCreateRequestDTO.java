package br.com.lane.SpecRecon.dto.VehicleSpecification;

import br.com.lane.SpecRecon.model.VehicleSpecificationModel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO de requisição para criar especificações dentro do cadastro de veículo.
 * <p>
 * Estrutura:
 * </p>
 * <ul>
 *     <li>{@code specTypeId}: identificador do tipo de especificação (obrigatório).</li>
 *     <li>{@code unitId}: identificador da unidade (opcional).</li>
 *     <li>{@code value_numeric}: valor numérico (quando o tipo for NUMBER).</li>
 *     <li>{@code value_text}: valor em texto (quando o tipo for TEXT/BOOLEAN).</li>
 *     <li>{@code is_available}: indicador de disponibilidade.</li>
 * </ul>
 *
 * @param specTypeId id do tipo de especificação.
 * @param unitId id da unidade.
 * @param value_numeric valor numérico.
 * @param value_text valor em texto.
 * @param is_available disponibilidade.
 */
public record VehicleSpecificationCreateRequestDTO(
        @NotNull(message = "O tipo de especificação é obrigatório")
        Long specTypeId,
        Long unitId,
        @Positive(message = "O valor numérico deve ser positivo")
        @DecimalMin(value = "0.0", inclusive = false, message = "O valor numérico deve ser maior que zero")
        BigDecimal value_numeric,
        String value_text,
        Boolean is_available
) {
    /**
     * Converte o DTO em entidade de domínio.
     *
     * @return instância de {@link br.com.lane.SpecRecon.model.VehicleSpecificationModel} preenchida.
     */
    public VehicleSpecificationModel toModel() {
        VehicleSpecificationModel spec = new VehicleSpecificationModel();
        spec.setValue_numeric(value_numeric);
        spec.setValue_text(value_text);
        spec.setIs_available(is_available);
        return spec;
    }
}


