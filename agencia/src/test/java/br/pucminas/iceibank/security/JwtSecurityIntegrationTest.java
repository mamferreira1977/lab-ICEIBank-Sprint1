package br.pucminas.iceibank.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "agencia.id=0", "agencia.offset=0",
        "iceibank.data-dir=target/test-data",
        "jwt.secret=01234567890123456789012345678901",
        "jwt.expiration-seconds=600",
        "iceibank.internal-key=teste-interno"
})
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void cadastroLoginEProtecaoJwtFuncionam() throws Exception {
        String usuario = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String cadastroBody = mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuário Teste\",\"usuario\":\"" + usuario + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String tokenCadastro = mapper.readTree(cadastroBody).get("token").asText();
        mvc.perform(get("/status").header("Authorization", "Bearer " + tokenCadastro))
                .andExpect(status().isOk());

        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        String token = json.get("token").asText();

        mvc.perform(get("/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencia").value(0));
        mvc.perform(get("/status").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaInternaExigeChaveInterna() throws Exception {
        mvc.perform(post("/contas/1/creditar-remoto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10,\"timestampLamport\":1,\"origemAgencia\":0}"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void historicoDaContaRetornaEventosDaConta() throws Exception {
        String usuario = "h" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String cadastro = mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Histórico\",\"usuario\":\"" + usuario + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(cadastro).get("token").asText();

        mvc.perform(post("/contas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":0,\"nomeUsuario\":\"Ana\",\"saldoInicial\":100}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/contas/0/depositar").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":25}"))
                .andExpect(status().isOk());

        mvc.perform(get("/contas/0/historico").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").exists())
                .andExpect(jsonPath("$[0].timestampLamport").exists());
    }

}
