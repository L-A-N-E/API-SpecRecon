package br.com.lane.SpecRecon.repository;

import br.com.lane.SpecRecon.model.UnitModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<UnitModel, Long> {

     // Busca unidade pelo símbolo (ex: kg, m, °C)
    Optional<UnitModel> findBySymbol(String symbol);

}