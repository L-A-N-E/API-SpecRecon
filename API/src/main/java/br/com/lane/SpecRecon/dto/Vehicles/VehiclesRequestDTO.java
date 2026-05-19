package br.com.lane.SpecRecon.dto.Vehicles;

import br.com.lane.SpecRecon.dto.VehicleSpecification.VehicleSpecificationCreateRequestDTO;
import br.com.lane.SpecRecon.model.VehicleModel;
import br.com.lane.SpecRecon.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.util.List;

/**
 * DTO de requisição para veículos com validações de segurança.
 * <p>
 * Validações:
 * - Brand, Model, Version: obrigatórios, sanitizados, máx 255 caracteres
 * - Specifications: lista validada de especificações
 * </p>
 *
 * @param brand marca do veículo (sanitizada)
 * @param model modelo do veículo (sanitizado)
 * @param version versão do veículo (sanitizada)
 * @param specifications especificações do veículo (validadas)
 */
public record VehiclesRequestDTO(
        @NotBlank(message = "Marca é obrigatória")
        @SafeString(message = "Marca contém caracteres não permitidos", maxLength = 255)
        String brand,

        @NotBlank(message = "Modelo é obrigatório")
        @SafeString(message = "Modelo contém caracteres não permitidos", maxLength = 255)
        String model,

        @NotBlank(message = "Versão é obrigatória")
        @SafeString(message = "Versão contém caracteres não permitidos", maxLength = 255)
        String version,

        @Valid
        List<VehicleSpecificationCreateRequestDTO> specifications
) {
    /**
     * Converte o DTO em entidade de domínio.
     *
     * @return instância de {@link VehicleModel} preenchida com os dados do DTO.
     */
    public VehicleModel toModel() {
        VehicleModel vehicle = new VehicleModel();
        vehicle.setBrand(brand.trim());
        vehicle.setModel(model.trim());
        vehicle.setVersion(version.trim());
        return vehicle;
    }
}
