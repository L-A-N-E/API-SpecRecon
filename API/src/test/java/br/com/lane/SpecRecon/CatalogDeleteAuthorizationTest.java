package br.com.lane.SpecRecon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 3 (OWASP API5 - Broken Function Level Authorization):
 * exclusão de itens do catálogo restrita a ADMIN e ANALYST.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogDeleteAuthorizationTest extends BaseIntegrationTest {

    private String adminToken;
    private String analystToken;
    private String userToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = registerAndLogin(uniqueEmail("del-admin"), "ADMIN");
        analystToken = registerAndLogin(uniqueEmail("del-analyst"), "ANALYST");
        userToken = registerAndLogin(uniqueEmail("del-user"), "USER");
    }

    private long createVehicle() throws Exception {
        String body = "{\"brand\": \"Ford\", \"model\": \"Maverick\", \"version\": \"Lariat\", \"specifications\": []}";
        MvcResult created = mockMvc.perform(signed(post("/vehicles"), body, adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        return extractNumber(created.getResponse().getContentAsString(), "id");
    }

    @Test
    void deveNegarExclusaoDeVeiculoParaPerfilUser() throws Exception {
        long id = createVehicle();
        mockMvc.perform(authed(delete("/vehicles/" + id), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirExclusaoDeVeiculoParaPerfilAnalyst() throws Exception {
        long id = createVehicle();
        mockMvc.perform(authed(delete("/vehicles/" + id), analystToken))
                .andExpect(status().isNoContent());
    }
}
