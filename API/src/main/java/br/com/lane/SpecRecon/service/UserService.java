package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.exception.ConflictException;
import br.com.lane.SpecRecon.exception.NotFoundException;
import br.com.lane.SpecRecon.model.UserModel;
import br.com.lane.SpecRecon.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de usuários.
 * Centraliza regras de negócio e persistência de usuários.
 * O email é criptografado automaticamente pelo EncryptedStringConverter (JPA).
 */
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<UserModel> findAll() {
        return repository.findAll();
    }

    public UserModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    public UserModel findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    public UserModel create(UserModel user) {
        repository.findByEmail(user.getEmail())
                .ifPresent(u -> {
                    throw new ConflictException("E-mail já cadastrado");
                });
        return repository.save(user);
    }

    public UserModel update(Long id, UserModel updatedUser) {
        UserModel user = findById(id);

        repository.findByEmail(updatedUser.getEmail())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new ConflictException("E-mail já está em uso");
                    }
                });

        user.setEmail(updatedUser.getEmail());
        user.setRole(updatedUser.getRole());

        if (updatedUser.getPassword() != null) {
            user.setPassword(updatedUser.getPassword());
        }

        return repository.save(user);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Usuário não encontrado");
        }
        repository.deleteById(id);
    }
}