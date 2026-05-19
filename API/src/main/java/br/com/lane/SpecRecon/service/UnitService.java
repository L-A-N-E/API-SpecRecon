package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.exception.ConflictException;
import br.com.lane.SpecRecon.exception.NotFoundException;
import br.com.lane.SpecRecon.model.UnitModel;
import br.com.lane.SpecRecon.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Serviço de unidades.
 * <p>
 * Centraliza regras de negócio e persistência de unidades de medida.
 * </p>
 */
@Service
public class UnitService {

    private final UnitRepository repository;

    public UnitService(UnitRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista todas as unidades.
     *
     * @return lista de unidades.
     */
    public List<UnitModel> findAll() {
        return repository.findAll();
    }

    /**
     * Busca unidade por ID.
     *
     * @param id identificador da unidade.
     * @return unidade encontrada.
     */
    public UnitModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unidade não encontrada"));
    }

    /**
     * Cria uma nova unidade.
     *
     * @param unit dados da unidade.
     * @return unidade persistida.
     */
    public UnitModel create(UnitModel unit) {

        // Regra: símbolo deve ser único
        repository.findBySymbol(unit.getSymbol())
                .ifPresent(u -> {
                    throw new ConflictException("Já existe uma unidade com esse símbolo");
                });

        return repository.save(unit);
    }

    /**
     * Atualiza uma unidade existente.
     *
     * @param id identificador da unidade.
     * @param updatedUnit dados atualizados.
     * @return unidade atualizada.
     */
    public UnitModel update(Long id, UnitModel updatedUnit) {

        UnitModel unit = findById(id);

        // Valida símbolo duplicado (exceto ele mesmo)
        repository.findBySymbol(updatedUnit.getSymbol())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new ConflictException("Símbolo já está em uso");
                    }
                });

        unit.setName(updatedUnit.getName());
        unit.setSymbol(updatedUnit.getSymbol());
        unit.setDimension(updatedUnit.getDimension());
        unit.setConversion_factor_to_base(updatedUnit.getConversion_factor_to_base());

        return repository.save(unit);
    }

    /**
     * Remove uma unidade.
     *
     * @param id identificador da unidade.
     */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Unidade não encontrada");
        }
        repository.deleteById(id);
    }
}