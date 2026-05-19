package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.dto.VehicleSpecification.VehicleSpecificationCreateRequestDTO;
import br.com.lane.SpecRecon.dto.VehicleSpecification.VehicleSpecificationsResponseDTO;
import br.com.lane.SpecRecon.dto.Vehicles.VehiclesRequestDTO;
import br.com.lane.SpecRecon.dto.Vehicles.VehiclesResponseDTO;
import br.com.lane.SpecRecon.exception.NotFoundException;
import br.com.lane.SpecRecon.model.VehicleModel;
import br.com.lane.SpecRecon.model.VehicleSpecificationModel;
import br.com.lane.SpecRecon.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de veículos.
 * <p>
 * Centraliza regras de negócio e persistência de veículos.
 * </p>
 */
@Service
public class VehicleService {
    private final VehicleRepository repository;
    private final VehicleSpecificationService specificationService;

    public VehicleService(VehicleRepository repository, VehicleSpecificationService specificationService) {
        this.repository = repository;
        this.specificationService = specificationService;
    }


    /**
     * Lista todos os veículos em formato de resposta.
     *
     * @return lista de veículos.
     */
    public List<VehiclesResponseDTO> findAllResponses() {
        return repository.findAll().stream()
                .map(VehiclesResponseDTO::fromModel)
                .toList();
    }

    /**
     * Busca veículo por ID.
     *
     * @param id identificador do veículo.
     * @return veículo encontrado.
     */
    public VehicleModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Veículo não encontrado"));
    }

    /**
     * Cria um novo veículo.
     *
     * @param vehicle dados do veículo.
     * @return veículo persistido.
     */
    public VehicleModel create(VehicleModel vehicle) {
        return repository.save(vehicle);
    }

    /**
     * Atualiza o veículo.
     *
     * @param id identificador do veículo.
     * @param updatedVehicle dados atualizados.
     * @return veículo atualizado.
     */
    public VehicleModel update(Long id, VehicleModel updatedVehicle) {
        VehicleModel vehicle = findById(id);

        vehicle.setBrand(updatedVehicle.getBrand());
        vehicle.setModel(updatedVehicle.getModel());
        vehicle.setVersion(updatedVehicle.getVersion());

        return repository.save(vehicle);
    }

    /**
     * Remove o veículo.
     *
     * @param id identificador do veículo.
     */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Veículo não encontrado");
        }
        repository.deleteById(id);
    }

    /**
     * Busca um veículo e suas especificações.
     *
     * @param id identificador do veículo.
     * @return veículo com especificações.
     */
    public VehiclesResponseDTO findByIdWithSpecifications(Long id) {
        VehicleModel vehicle = findById(id);
        List<VehicleSpecificationsResponseDTO> specResponses = specificationService.findByVehicle(id)
                .stream()
                .map(VehicleSpecificationsResponseDTO::fromModel)
                .toList();

        return VehiclesResponseDTO.fromModelWithSpecifications(vehicle, specResponses);
    }

    /**
     * Cria um veículo com especificações (quando informadas).
     *
     * @param vehicle dados do veículo.
     * @return veículo criado com especificações.
     */
    public VehiclesResponseDTO createWithSpecifications(VehiclesRequestDTO vehicle) {
        VehicleModel created = create(vehicle.toModel());

        List<VehicleSpecificationCreateRequestDTO> specifications = vehicle.specifications();
        List<VehicleSpecificationModel> createdSpecs = specificationService.createBatch(created, specifications);
        List<VehicleSpecificationsResponseDTO> specResponses = createdSpecs.stream()
                .map(VehicleSpecificationsResponseDTO::fromModel)
                .toList();

        return VehiclesResponseDTO.fromModelWithSpecifications(created, specResponses);
    }

    /**
     * Atualiza um veículo e retorna suas especificações atuais.
     *
     * @param id identificador do veículo.
     * @param vehicle dados atualizados.
     * @return veículo atualizado com especificações.
     */
    public VehiclesResponseDTO updateWithSpecifications(Long id, VehiclesRequestDTO vehicle) {
        VehicleModel updated = update(id, vehicle.toModel());
        List<VehicleSpecificationCreateRequestDTO> specifications = vehicle.specifications();
        List<VehicleSpecificationsResponseDTO> specResponses;

        if (specifications != null) {
            List<VehicleSpecificationModel> replaced = specificationService.replaceBatch(updated, specifications);
            specResponses = replaced.stream()
                    .map(VehicleSpecificationsResponseDTO::fromModel)
                    .toList();
        } else {
            specResponses = specificationService.findByVehicle(id)
                    .stream()
                    .map(VehicleSpecificationsResponseDTO::fromModel)
                    .toList();
        }

        return VehiclesResponseDTO.fromModelWithSpecifications(updated, specResponses);
    }

}