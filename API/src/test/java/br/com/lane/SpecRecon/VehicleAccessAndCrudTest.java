package br.com.lane.SpecRecon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes automatizados do CRUD de veículos
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VehicleAccessAndCrudTest extends BaseIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = registerAndLogin(uniqueEmail("vehicle-admin"), "ADMIN");
    }

    @Test
    void deveRecusarListagemSemToken() throws Exception {
        mockMvc.perform(authed(get("/vehicles"), null))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveListarVeiculosComTokenValido() throws Exception {
        mockMvc.perform(authed(get("/vehicles"), adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deveCriarVeiculoERetornar201Created() throws Exception {
        String body = "{\"brand\": \"Ford\", \"model\": \"Ranger\", \"version\": \"Raptor\", \"specifications\": []}";

        mockMvc.perform(signed(post("/vehicles"), body, adminToken))
                .andExpect(status().isCreated());
    }

    @Test
    void deveRecusarCriacaoComMarcaEmBranco() throws Exception {
        String body = "{\"brand\": \"\", \"model\": \"Ranger\", \"version\": \"Raptor\", \"specifications\": []}";

        mockMvc.perform(signed(post("/vehicles"), body, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404ParaVeiculoInexistente() throws Exception {
        mockMvc.perform(authed(get("/vehicles/999999"), adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarEDepoisDeletarVeiculoComSucesso() throws Exception {
        String body = "{\"brand\": \"Ford\", \"model\": \"Bronco\", \"version\": \"Base\", \"specifications\": []}";

        MvcResult created = mockMvc.perform(signed(post("/vehicles"), body, adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        long id = extractNumber(created.getResponse().getContentAsString(), "id");

        mockMvc.perform(authed(delete("/vehicles/" + id), adminToken))
                .andExpect(status().isNoContent());
    }
}
