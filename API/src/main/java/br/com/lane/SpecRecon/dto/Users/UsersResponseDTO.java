package br.com.lane.SpecRecon.dto.Users;

import br.com.lane.SpecRecon.model.Role;
import br.com.lane.SpecRecon.model.UserModel;

/**
 * DTO de resposta de usuários.
 * <p>
 * Estrutura de saída que não expõe senha.
 * </p>
 *
 * @param id identificador do usuário.
 * @param email e-mail do usuário.
 * @param role perfil do usuário.
 */
public record UsersResponseDTO(
        Long id,
        String email,
        Role role
) {
    /**
     * Mapeia uma entidade {@link br.com.lane.SpecRecon.model.UserModel} para DTO.
     *
     * @param user entidade de usuário.
     * @return DTO preenchido ou {@code null} se a entidade for nula.
     */
    public static UsersResponseDTO fromModel(UserModel user) {
        if (user == null) {
            return null;
        }
        return new UsersResponseDTO(user.getId(), user.getEmail(), user.getRole());
    }
}
