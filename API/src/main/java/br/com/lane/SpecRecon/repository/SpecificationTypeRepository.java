package br.com.lane.SpecRecon.repository;

import br.com.lane.SpecRecon.model.SpecificationTypeModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecificationTypeRepository extends JpaRepository<SpecificationTypeModel, Long> {

    // Busca por nome (deve ser único)
    Optional<SpecificationTypeModel> findByName(String name);

}