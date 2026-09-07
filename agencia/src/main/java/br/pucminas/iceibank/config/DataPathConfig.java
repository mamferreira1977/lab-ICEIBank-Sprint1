package br.pucminas.iceibank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DataPathConfig {
    private final Path pastaDados;

    public DataPathConfig(@Value("${iceibank.data-dir:}") String configurado) {
        this.pastaDados = resolverPastaDados(configurado);
        try {
            Files.createDirectories(this.pastaDados);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível criar a pasta de dados: " + this.pastaDados, e);
        }
    }

    private Path resolverPastaDados(String configurado) {
        if (configurado != null && !configurado.isBlank()) {
            return Path.of(configurado).toAbsolutePath().normalize();
        }

        String ambiente = System.getenv("ICEIBANK_DATA_DIR");
        if (ambiente != null && !ambiente.isBlank()) {
            return Path.of(ambiente).toAbsolutePath().normalize();
        }

        Path atual = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(atual.resolve("pom.xml")) && Files.isDirectory(atual.resolve("src"))) {
            return atual.resolve("data");
        }
        if (Files.exists(atual.resolve("agencia").resolve("pom.xml"))) {
            return atual.resolve("agencia").resolve("data");
        }
        if (Files.exists(atual.resolve("iceibank").resolve("agencia").resolve("pom.xml"))) {
            return atual.resolve("iceibank").resolve("agencia").resolve("data");
        }
        return atual.resolve("data");
    }

    public Path getPastaDados() {
        return pastaDados;
    }
}
