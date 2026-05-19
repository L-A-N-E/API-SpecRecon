package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.model.*;
import br.com.lane.SpecRecon.exception.BadRequestException;
import br.com.lane.SpecRecon.exception.ConflictException;
import br.com.lane.SpecRecon.exception.NotFoundException;
import br.com.lane.SpecRecon.repository.SpecificationTypeRepository;
import br.com.lane.SpecRecon.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de tipos de especificação.
 * <p>
 * Centraliza regras de negócio e validações de tipos de especificação.
 * </p>
 */
@Service
public class SpecificationTypeService {

    private final SpecificationTypeRepository repository;
    private final UnitRepository unitRepository;

    public SpecificationTypeService(
            SpecificationTypeRepository repository,
            UnitRepository unitRepository
    ) {
        this.repository = repository;
        this.unitRepository = unitRepository;
    }

    /**
     * Lista todos os tipos de especificação.
     *
     * @return lista de tipos de especificação.
     */
    public List<SpecificationTypeModel> findAll() {
        return repository.findAll();
    }

    /**
     * Busca um tipo de especificação por ID.
     *
     * @param id identificador do tipo.
     * @return tipo encontrado.
     */
    public SpecificationTypeModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de especificação não encontrado"));
    }

    /**
     * Cria novo tipo de especificação.
     *
     * @param spec dados do tipo.
     * @return tipo persistido.
     */
    public SpecificationTypeModel create(SpecificationTypeModel spec) {

        // Nome único
        repository.findByName(spec.getName())
                .ifPresent(s -> {
                    throw new ConflictException("Já existe um tipo com esse nome");
                });

        validateBusinessRules(spec);

        return repository.save(spec);
    }

    /**
     * Atualiza tipo de especificação.
     *
     * @param id identificador do tipo.
     * @param updatedSpec dados atualizados.
     * @return tipo atualizado.
     */
    public SpecificationTypeModel update(Long id, SpecificationTypeModel updatedSpec) {

        SpecificationTypeModel spec = findById(id);

        // Valida nome único (exceto ele mesmo)
        repository.findByName(updatedSpec.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new ConflictException("Nome já está em uso");
                    }
                });

        spec.setName(updatedSpec.getName());
        spec.setCategory(updatedSpec.getCategory());
        spec.setDescription(updatedSpec.getDescription());
        spec.setData_type(updatedSpec.getData_type());
        spec.setDefaultUnit(updatedSpec.getDefaultUnit());

        validateBusinessRules(spec);

        return repository.save(spec);
    }

    /**
     * Remove um tipo de especificação.
     *
     * @param id identificador do tipo.
     */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Tipo de especificação não encontrado");
        }
        repository.deleteById(id);
    }

    /**
     * Valida regras de negócio do tipo de especificação.
     *
     * @param spec tipo de especificação a validar.
     */
    private void validateBusinessRules(SpecificationTypeModel spec) {

        DataType type = spec.getData_type();

        // NUMBER -> pode ter unidade
        if (type == DataType.NUMBER) {
            if (spec.getDefaultUnit() == null) {
                throw new BadRequestException("Unidade padrão é recomendada para NUMBER");
            }

            // Garante que a unidade existe no banco
            if (spec.getDefaultUnit().getId() != null) {
                unitRepository.findById(spec.getDefaultUnit().getId())
                        .orElseThrow(() -> new NotFoundException("Unidade padrão não encontrada"));
            }
        }

        // TEXT / BOOLEAN -> NÃO pode ter unidade
        if (type == DataType.TEXT || type == DataType.BOOLEAN) {
            if (spec.getDefaultUnit() != null) {
                throw new BadRequestException("TEXT/BOOLEAN não devem possuir unidade");
            }
        }
    }
}