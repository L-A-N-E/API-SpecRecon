package br.com.lane.SpecRecon;

import br.com.lane.SpecRecon.model.UserModel;
import br.com.lane.SpecRecon.service.AuditService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes das correções no gerenciamento de usuários (Sprint 3):
 *  - senha criada/alterada por ADMIN é gravada com BCrypt (antes: texto puro);
 *  - alterações críticas em usuários geram trilha de auditoria (inclusive ROLE_CHANGED).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserAuditTest extends BaseIntegrationTest {

    @Autowired
    private AuditService auditService;

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = registerAndLogin(uniqueEmail("audit-admin"), "ADMIN");
    }

    private long createUserAsAdmin(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(signed(post("/users"),
                        registerJson(email, DEFAULT_PASSWORD, role), adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        return extractNumber(result.getResponse().getContentAsString(), "id");
    }

    @Test
    void deveGravarSenhaComBCryptQuandoAdminCriaUsuario() throws Exception {
        String email = uniqueEmail("criado-por-admin");
        createUserAsAdmin(email, "USER");

        UserModel saved = userRepository.findByEmail(email).orElseThrow();
        assertTrue(saved.getPassword().startsWith("$2"),
                "Senha deveria estar em BCrypt, mas foi gravada em texto puro");
    }

    @Test
    void deveAuditarTrocaDePerfilComoRoleChanged() throws Exception {
        String email = uniqueEmail("promovido");
        long id = createUserAsAdmin(email, "USER");

        mockMvc.perform(signed(put("/users/" + id),
                        registerJson(email, DEFAULT_PASSWORD, "ANALYST"), adminToken))
                .andExpect(status().isOk());

        assertTrue(auditService.findByAction("ROLE_CHANGED").stream()
                        .anyMatch(log -> Long.valueOf(id).equals(log.getEntityId())),
                "Troca de perfil deveria gerar evento ROLE_CHANGED na auditoria");
    }
}
