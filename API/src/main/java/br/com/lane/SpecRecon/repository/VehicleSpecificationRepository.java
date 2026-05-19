package br.com.lane.SpecRecon.repository;

import br.com.lane.SpecRecon.model.VehicleSpecificationModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface VehicleSpecificationRepository extends JpaRepository<VehicleSpecificationModel, Long> {

    // Busca todas as especificações de um veículo
    List<VehicleSpecificationModel> findByVehicleId(Long vehicleId);

    // Remove todas as especificações de um veículo
    void deleteByVehicleId(Long vehicleId);
}