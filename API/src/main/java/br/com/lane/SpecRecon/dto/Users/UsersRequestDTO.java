package br.com.lane.SpecRecon.dto.Users;

import br.com.lane.SpecRecon.model.Role;
import br.com.lane.SpecRecon.model.UserModel;
import br.com.lane.SpecRecon.validation.SafeString;
import br.com.lane.SpecRecon.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requisição para usuários.
 * <p>
 * Validações:
 * - Email: obrigatório, válido e sanitizado
 * - Password: obrigatório, mínimo 8 caracteres, 1 maiúscula, 1 minúscula, 1 dígito, 1 especial
 * - Role: obrigatório e válido
 * </p>
 *
 * @param email e-mail do usuário (validado)
 * @param password senha do usuário (validação de força)
 * @param role perfil do usuário (obrigatório)
 */
public record UsersRequestDTO(
        @Email(message = "O e-mail deve ser válido")
        @NotBlank(message = "O e-mail é obrigatório")
        @SafeString(message = "E-mail contém caracteres não permitidos", maxLength = 255)
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @StrongPassword
        String password,

        @NotNull(message = "O perfil (role) é obrigatório")
        Role role
) {
    /**
     * Converte o DTO em entidade de domínio.
     *
     * @return instância de {@link UserModel} preenchida.
     */
    public UserModel toModel() {
        UserModel user = new UserModel();
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
