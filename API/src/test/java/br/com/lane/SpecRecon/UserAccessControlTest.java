package br.com.lane.SpecRecon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes automatizados de RBAC
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserAccessControlTest extends BaseIntegrationTest {

    private String adminToken;
    private String userToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = registerAndLogin(uniqueEmail("users-admin"), "ADMIN");
        userToken = registerAndLogin(uniqueEmail("users-comum"), "USER");
    }

    @Test
    void deveRecusarListagemDeUsuariosSemToken() throws Exception {
        mockMvc.perform(authed(get("/users"), null))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarAcessoParaPerfilSemPermissao() throws Exception {
        mockMvc.perform(authed(get("/users"), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirAcessoParaPerfilAdmin() throws Exception {
        mockMvc.perform(authed(get("/users"), adminToken))
                .andExpect(status().isOk());
    }
}
