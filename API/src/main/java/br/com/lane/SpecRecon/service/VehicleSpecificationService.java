package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.dto.VehicleSpecification.VehicleSpecificationCreateRequestDTO;
import br.com.lane.SpecRecon.exception.BadRequestException;
import br.com.lane.SpecRecon.exception.NotFoundException;
import br.com.lane.SpecRecon.model.*;
import br.com.lane.SpecRecon.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço de especificações de veículos.
 * <p>
 * Centraliza regras de negócio e persistência de especificações.
 * </p>
 */
@Service
public class VehicleSpecificationService {

    private final VehicleSpecificationRepository repository;
    private final SpecificationTypeRepository specRepository;
    private final UnitRepository unitRepository;

    public VehicleSpecificationService(
            VehicleSpecificationRepository repository,
            SpecificationTypeRepository specRepository,
            UnitRepository unitRepository
    ) {
        this.repository = repository;
        this.specRepository = specRepository;
        this.unitRepository = unitRepository;
    }

    /**
     * Lista especificações por veículo.
     *
     * @param vehicleId identificador do veículo.
     * @return lista de especificações do veículo.
     */
    public List<VehicleSpecificationModel> findByVehicle(Long vehicleId) {
        return repository.findByVehicleId(vehicleId);
    }

    /**
     * Cria especificações em lote para um veículo.
     *
     * @param vehicle veículo associado.
     * @param specifications dados de especificação.
     * @return lista de especificações persistidas.
     */
    public List<VehicleSpecificationModel> createBatch(
            VehicleModel vehicle,
            List<VehicleSpecificationCreateRequestDTO> specifications
    ) {
        if (specifications == null || specifications.isEmpty()) {
            return List.of();
        }

        Set<Long> specTypeIds = specifications.stream()
                .map(VehicleSpecificationCreateRequestDTO::specTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, SpecificationTypeModel> specTypes = specRepository.findAllById(specTypeIds)
                .stream()
                .collect(Collectors.toMap(SpecificationTypeModel::getId, Function.identity()));

        if (specTypes.size() != specTypeIds.size()) {
            throw new NotFoundException("Tipo de especificação não encontrado");
        }

        Set<Long> unitIds = specifications.stream()
                .map(VehicleSpecificationCreateRequestDTO::unitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UnitModel> units = unitRepository.findAllById(unitIds)
                .stream()
                .collect(Collectors.toMap(UnitModel::getId, Function.identity()));

        if (units.size() != unitIds.size()) {
            throw new NotFoundException("Unidade não encontrada");
        }

        List<VehicleSpecificationModel> models = specifications.stream().map(spec -> {
            SpecificationTypeModel specType = specTypes.get(spec.specTypeId());
            UnitModel unit = spec.unitId() != null ? units.get(spec.unitId()) : null;

            validateUnitForType(specType, unit);

            VehicleSpecificationModel model = spec.toModel();
            model.setVehicle(vehicle);
            model.setSpecificationType(specType);
            model.setUnit(unit);

            validateValuesByType(specType, model);

            return model;
        }).toList();

        return repository.saveAll(models);
    }

    /**
     * Substitui todas as especificações de um veículo.
     * <p>
     * Se a lista estiver vazia, remove todas as especificações existentes.
     * </p>
     *
     * @param vehicle veículo associado.
     * @param specifications novas especificações.
     * @return lista de especificações persistidas.
     */
    public List<VehicleSpecificationModel> replaceBatch(
            VehicleModel vehicle,
            List<VehicleSpecificationCreateRequestDTO> specifications
    ) {
        repository.deleteByVehicleId(vehicle.getId());
        return createBatch(vehicle, specifications);
    }


    /**
     * Valida valores de acordo com o tipo de dado da especificação.
     *
     * @param spec tipo de especificação.
     * @param data dados da especificação.
     */
    private void validateValuesByType(SpecificationTypeModel spec, VehicleSpecificationModel data) {
        DataType type = spec.getData_type();

        if (type == DataType.NUMBER) {
            if (data.getValue_numeric() == null) {
                throw new BadRequestException("Valor numérico é obrigatório para tipo NUMBER");
            }
            data.setValue_text(null);
            return;
        }

        if (type == DataType.TEXT || type == DataType.BOOLEAN) {
            if (data.getValue_text() == null || data.getValue_text().isBlank()) {
                throw new BadRequestException("Valor texto é obrigatório para tipo TEXT/BOOLEAN");
            }
            data.setValue_numeric(null);
            data.setUnit(null);
        }
    }

    private void validateUnitForType(SpecificationTypeModel spec, UnitModel unit) {
        if (spec.getData_type() == DataType.NUMBER) {
            if (unit == null) {
                throw new BadRequestException("Unidade é obrigatória para tipo NUMBER");
            }

            if (spec.getDefaultUnit() != null &&
                    !unit.getDimension().equals(spec.getDefaultUnit().getDimension())) {

                throw new BadRequestException("Dimensão da unidade incompatível com a especificação");
            }
        }
    }
}