package br.com.lane.SpecRecon.dto.SpecificationType;

import br.com.lane.SpecRecon.model.DataType;
import br.com.lane.SpecRecon.model.SpecificationTypeModel;
import br.com.lane.SpecRecon.model.UnitModel;
import br.com.lane.SpecRecon.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requisição para tipos de especificação com validações de segurança.
 * <p>
 * Validações:
 * - Name, Category, Description: obrigatórios, sanitizados
 * - Data Type: obrigatório e válido
 * - Default Unit ID: opcional, válido se presente
 * </p>
 *
 * @param name nome do tipo (sanitizado)
 * @param category categoria do tipo (sanitizada)
 * @param description descrição do tipo (sanitizada)
 * @param data_type tipo de dado
 * @param default_unit_id id da unidade padrão (opcional)
 */
public record SpecificationTypesRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @SafeString(message = "Nome contém caracteres não permitidos", maxLength = 255)
        String name,

        @NotBlank(message = "Categoria é obrigatória")
        @SafeString(message = "Categoria contém caracteres não permitidos", maxLength = 255)
        String category,

        @NotBlank(message = "Descrição é obrigatória")
        @SafeString(message = "Descrição contém caracteres não permitidos", maxLength = 1024)
        String description,

        @NotNull(message = "Tipo de dado é obrigatório")
        DataType data_type,

        Long default_unit_id
) {
    /**
     * Converte o DTO em entidade de domínio.
     * <p>
     * Quando {@code default_unit_id} estiver presente, o relacionamento é criado por ID.
     * </p>
     *
     * @return instância de {@link SpecificationTypeModel} preenchida.
     */
    public SpecificationTypeModel toModel() {
        SpecificationTypeModel spec = new SpecificationTypeModel();
        spec.setName(name.trim());
        spec.setCategory(category.trim());
        spec.setDescription(description.trim());
        spec.setData_type(data_type);
        if (default_unit_id != null) {
            UnitModel unit = new UnitModel();
            unit.setId(default_unit_id);
            spec.setDefaultUnit(unit);
        }
        return spec;
    }
}
