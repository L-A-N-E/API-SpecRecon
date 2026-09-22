package br.com.lane.SpecRecon;

import br.com.lane.SpecRecon.security.PayloadSignatureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe base para os testes de integração automatizados da API SpecRecon
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PayloadSignatureManager signatureManager;

    protected static final String DEFAULT_PASSWORD = "@Securepassword123";

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    /**
     * Extrai um valor String de um JSON simples e plano (sem precisar de Jackson
     * ou de bibliotecas externas de parsing), ex: extractString(body, "token").
     */
    protected String extractString(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo \"" + field + "\" não encontrado em: " + json);
        }
        return m.group(1);
    }

    /**
     * Extrai um valor numérico de um JSON simples, ex: extractNumber(body, "id").
     */
    protected long extractNumber(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo \"" + field + "\" não encontrado em: " + json);
        }
        return Long.parseLong(m.group(1));
    }

    protected String registerJson(String email, String password, String role) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\", \"role\": \"" + role + "\"}";
    }

    protected String loginJson(String email, String password) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
    }

    /**
     * Registra um usuário com o perfil (role) informado e retorna o JWT já autenticado,
     * pronto para ser usado no header Authorization dos testes.
     */
    protected String registerAndLogin(String email, String role) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, DEFAULT_PASSWORD, role)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return extractString(loginResult.getResponse().getContentAsString(), "token");
    }

    /**
     * Adiciona o header Authorization (Bearer) a uma requisição sem corpo (GET/DELETE).
     * Passe {@code null} para simular uma chamada anônima (sem token).
     */
    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    /**
     * Monta uma requisição POST/PUT/PATCH com corpo JSON, Authorization e o header
     * X-Signature (HMAC-SHA256 do corpo exato), exigido pelo PayloadSignatureInterceptor
     * em /users, /vehicles, /units e /specification-types.
     */
    protected MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder builder, String json, String token) {
        builder.contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.header("X-Signature", signatureManager.generateSignature(json));
        return builder;
    }
}
