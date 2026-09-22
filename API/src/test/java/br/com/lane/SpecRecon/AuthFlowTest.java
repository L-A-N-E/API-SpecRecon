package br.com.lane.SpecRecon;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes automatizados do fluxo de autenticação
 */
class AuthFlowTest extends BaseIntegrationTest {

    @Test
    void deveRegistrarNovoUsuarioComSucesso() throws Exception {
        String email = uniqueEmail("novo-usuario");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, DEFAULT_PASSWORD, "USER")))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(email)))
                .andExpect(content().string(containsString("USER")));
    }

    @Test
    void deveRecusarRegistroComEmailDuplicado() throws Exception {
        String email = uniqueEmail("duplicado");
        String body = registerJson(email, DEFAULT_PASSWORD, "USER");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRecusarRegistroComSenhaFraca() throws Exception {
        String email = uniqueEmail("senha-fraca");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "123", "USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAutenticarComCredenciaisValidasERetornarToken() throws Exception {
        String email = uniqueEmail("login-ok");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, DEFAULT_PASSWORD, "USER")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("token")))
                .andExpect(content().string(containsString("refreshToken")));
    }

    @Test
    void deveRecusarLoginComSenhaIncorreta() throws Exception {
        String email = uniqueEmail("login-invalido");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, DEFAULT_PASSWORD, "USER")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "SenhaErrada@123")))
                .andExpect(status().isUnauthorized());
    }
}
