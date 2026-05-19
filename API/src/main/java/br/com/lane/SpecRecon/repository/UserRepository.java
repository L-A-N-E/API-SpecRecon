package br.com.lane.SpecRecon.repository;

import br.com.lane.SpecRecon.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<UserModel, Long> {

    // Busca usuário por email
    Optional<UserModel> findByEmail(String email);

}