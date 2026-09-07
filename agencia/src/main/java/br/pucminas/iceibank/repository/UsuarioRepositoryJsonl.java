package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.config.DataPathConfig;
import br.pucminas.iceibank.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepositoryJsonl implements UsuarioRepository {
    private final ObjectMapper objectMapper;
    private final Path arquivoUsuarios;

    public UsuarioRepositoryJsonl(ObjectMapper objectMapper, DataPathConfig dataPathConfig) {
        this.objectMapper = objectMapper;
        this.arquivoUsuarios = dataPathConfig.getPastaDados().resolve("usuarios.jsonl").toAbsolutePath().normalize();
        try {
            if (Files.notExists(this.arquivoUsuarios)) {
                Files.createFile(this.arquivoUsuarios);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o arquivo de usuários.", e);
        }
        System.out.println("[ICEIBank] Repositório de usuários: " + arquivoUsuarios);
    }

    @Override
    public synchronized Optional<Usuario> buscarPorUsuario(String usuario) {
        try {
            List<String> linhas = Files.readAllLines(arquivoUsuarios, StandardCharsets.UTF_8);
            for (String linha : linhas) {
                if (linha == null || linha.isBlank()) continue;
                Usuario u = objectMapper.readValue(linha, Usuario.class);
                if (u.usuario().equalsIgnoreCase(usuario)) return Optional.of(u);
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler cadastros de usuários.", e);
        }
    }

    @Override
    public synchronized void salvar(Usuario usuario) {
        try {
            String linha = objectMapper.writeValueAsString(usuario) + System.lineSeparator();
            Files.writeString(arquivoUsuarios, linha, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao persistir cadastro de usuário.", e);
        }
    }

    @Override
    public Path getArquivoUsuarios() {
        return arquivoUsuarios;
    }
}
