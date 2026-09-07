package br.pucminas.iceibank.util;

import br.pucminas.iceibank.model.Evento;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class MesclarLogs {
    public static void main(String[] args) throws Exception {
        Path pasta = resolverPastaDados();
        ObjectMapper mapper = new ObjectMapper();
        List<Evento> eventos = new ArrayList<>();

        System.out.println("Lendo logs de: " + pasta.toAbsolutePath().normalize());

        if (!Files.exists(pasta)) {
            System.out.println("Pasta data ainda não existe.");
            return;
        }

        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.filter(p -> p.getFileName().toString().endsWith(".jsonl")).toList()) {
                for (String linha : Files.readAllLines(arquivo)) {
                    if (!linha.isBlank()) {
                        eventos.add(mapper.readValue(linha, Evento.class));
                    }
                }
            }
        }

        eventos.sort(Comparator.comparingInt(Evento::timestampLamport));

        System.out.println("=== Linha do tempo unificada (ordenada por relogio de Lamport) ===");
        for (Evento evento : eventos) {
            System.out.printf("[Lamport %d] (%s) %s - %s %s%n",
                    evento.timestampLamport(),
                    evento.horaParede(),
                    evento.agencia(),
                    evento.tipo(),
                    mapper.writeValueAsString(evento.detalhes()));
        }
    }

    private static Path resolverPastaDados() {
        String configurado = System.getenv("ICEIBANK_DATA_DIR");
        if (configurado != null && !configurado.isBlank()) {
            return Path.of(configurado).toAbsolutePath().normalize();
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
}
