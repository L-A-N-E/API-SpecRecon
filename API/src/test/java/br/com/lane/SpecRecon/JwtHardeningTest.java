package br.com.lane.SpecRecon;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes das correções de hardening da Sprint 3 (JWT seguro + anti-escalada de privilégio).
 */
class JwtHardeningTest extends BaseIntegrationTest {

    /** Faz login e devolve o corpo JSON (token + refreshToken). */
    private String loginBody(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void deveBloquearAutoRegistroComoAdminQuandoJaExistemUsuarios() throws Exception {
        // garante que o banco não está vazio (fora do bootstrap)
        registerAndLogin(uniqueEmail("existente"), "USER");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail("atacante"), DEFAULT_PASSWORD, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarRefreshTokenUsadoComoAccessToken() throws Exception {
        String email = uniqueEmail("refresh-como-access");
        registerAndLogin(email, "USER");
        String refreshToken = extractString(loginBody(email), "refreshToken");

        mockMvc.perform(authed(get("/vehicles"), refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarAccessTokenNoEndpointDeRefresh() throws Exception {
        String accessToken = registerAndLogin(uniqueEmail("access-no-refresh"), "USER");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + accessToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRenovarSessaoComRefreshTokenMantendoOPerfil() throws Exception {
        String email = uniqueEmail("refresh-ok");
        registerAndLogin(email, "USER");
        String refreshToken = extractString(loginBody(email), "refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"role\":\"USER\"")));
    }
}
