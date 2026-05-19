package br.com.lane.SpecRecon.repository;

import br.com.lane.SpecRecon.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<VehicleModel, Long> {
}