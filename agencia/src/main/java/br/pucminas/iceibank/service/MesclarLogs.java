package br.pucminas.iceibank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class MesclarLogs {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        Path pastaDados =
                Paths.get("data").toAbsolutePath().normalize();

        if (!Files.exists(pastaDados)) {
            System.out.println(
                    "Pasta de dados não encontrada: " + pastaDados);
            return;
        }

        List<JsonNode> eventos = new ArrayList<>();

        /*
         * Localiza os arquivos:
         *
         * eventos-agencia-0.jsonl
         * eventos-agencia-1.jsonl
         * eventos-agencia-2.jsonl
         */
        try (DirectoryStream<Path> arquivos =
                     Files.newDirectoryStream(
                             pastaDados,
                             "eventos-agencia-*.jsonl")) {

            for (Path arquivo : arquivos) {

                System.out.println(
                        "Lendo: " + arquivo.getFileName());

                List<String> linhas =
                        Files.readAllLines(arquivo);

                for (String linha : linhas) {

                    if (linha == null || linha.isBlank()) {
                        continue;
                    }

                    JsonNode evento =
                            mapper.readTree(linha);

                    /*
                     * Ignora logs antigos da Sprint 1
                     * que ainda utilizem timestampLamport.
                     */
                    if (evento.has("timestampVetorial")) {
                        eventos.add(evento);
                    }
                }
            }
        }

        if (eventos.isEmpty()) {
            System.out.println(
                    "Nenhum evento com timestampVetorial encontrado.");
            return;
        }

        /*
         * Ordenação apenas para apresentação.
         * A relação causal será determinada pelos vetores,
         * e não pela hora de parede.
         */
        eventos.sort(
                Comparator.comparing(
                        e -> e.path("horaParede").asText()));

        System.out.println();
        System.out.println(
                "==============================================");
        System.out.println(
                " LINHA DO TEMPO - RELÓGIO VETORIAL");
        System.out.println(
                "==============================================");

        for (JsonNode evento : eventos) {

            System.out.printf(
                    "%s | vetor=%s | %s | %s%n",
                    evento.path("agencia").asText(),
                    evento.path("timestampVetorial"),
                    evento.path("tipo").asText(),
                    evento.path("detalhes"));
        }

        System.out.println();
        System.out.println(
                "==============================================");
        System.out.println(
                " EVENTOS CONCORRENTES ENTRE AGÊNCIAS");
        System.out.println(
                "==============================================");

        boolean encontrouConcorrente = false;

        for (int i = 0; i < eventos.size(); i++) {

            for (int j = i + 1; j < eventos.size(); j++) {

                JsonNode e1 = eventos.get(i);
                JsonNode e2 = eventos.get(j);

                String agencia1 =
                        e1.path("agencia").asText();

                String agencia2 =
                        e2.path("agencia").asText();

                /*
                 * O exercício pede comparação entre
                 * eventos de agências diferentes.
                 */
                if (agencia1.equals(agencia2)) {
                    continue;
                }

                int[] v1 =
                        converterVetor(
                                e1.path("timestampVetorial"));

                int[] v2 =
                        converterVetor(
                                e2.path("timestampVetorial"));

                Relacao relacao =
                        compararVetores(v1, v2);

                if (relacao == Relacao.CONCORRENTES) {

                    encontrouConcorrente = true;

                    System.out.println();

                    System.out.printf(
                            "[%s] %s %s%n",
                            agencia1,
                            e1.path("tipo").asText(),
                            Arrays.toString(v1));

                    System.out.println("        X");

                    System.out.printf(
                            "[%s] %s %s%n",
                            agencia2,
                            e2.path("tipo").asText(),
                            Arrays.toString(v2));

                    System.out.println(
                            "=> CONCORRENTES");
                }
            }
        }

        if (!encontrouConcorrente) {

            System.out.println();
            System.out.println(
                    "Nenhum par concorrente encontrado.");

            System.out.println(
                    "Gere eventos independentes em agências " +
                            "diferentes e execute novamente.");
        }
        System.out.println();
        System.out.println(
                "==============================================");
        System.out.println(
                " RELAÇÕES CAUSAIS DE TRANSFERÊNCIA");
        System.out.println(
                "==============================================");

        boolean encontrouTransferenciaCausal = false;

        for (JsonNode debito : eventos) {

            if (!debito.path("tipo").asText()
                    .equals("TRANSFERENCIA_DEBITO")) {
                continue;
            }

            for (JsonNode credito : eventos) {

                if (!credito.path("tipo").asText()
                        .equals("TRANSFERENCIA_CREDITO_REMOTO")) {
                    continue;
                }

                int[] vetorDebito =
                        converterVetor(
                                debito.path("timestampVetorial"));

                int[] vetorCredito =
                        converterVetor(
                                credito.path("timestampVetorial"));

                Relacao relacao =
                        compararVetores(
                                vetorDebito,
                                vetorCredito);

                if (relacao == Relacao.ANTES) {

                    encontrouTransferenciaCausal = true;

                    System.out.println();

                    System.out.printf(
                            "[%s] TRANSFERENCIA_DEBITO %s%n",
                            debito.path("agencia").asText(),
                            Arrays.toString(vetorDebito));

                    System.out.println("        ->");

                    System.out.printf(
                            "[%s] TRANSFERENCIA_CREDITO_REMOTO %s%n",
                            credito.path("agencia").asText(),
                            Arrays.toString(vetorCredito));

                    System.out.println(
                            "=> RELAÇÃO CAUSAL (NÃO CONCORRENTES)");
                }
            }
        }

        if (!encontrouTransferenciaCausal) {
            System.out.println();
            System.out.println(
                    "Nenhuma transferência causal encontrada.");
        }
        System.out.println();
        System.out.println(
                "==============================================");
        System.out.println(
                " ANÁLISE CONCLUÍDA");
        System.out.println(
                "==============================================");
    }

    private static int[] converterVetor(JsonNode node) {

        int[] vetor = new int[node.size()];

        for (int i = 0; i < node.size(); i++) {
            vetor[i] = node.get(i).asInt();
        }

        return vetor;
    }

    private static Relacao compararVetores(
            int[] v1,
            int[] v2) {

        boolean v1MenorOuIgual = true;
        boolean v2MenorOuIgual = true;

        for (int i = 0; i < v1.length; i++) {

            if (v1[i] > v2[i]) {
                v1MenorOuIgual = false;
            }

            if (v2[i] > v1[i]) {
                v2MenorOuIgual = false;
            }
        }

        if (v1MenorOuIgual && v2MenorOuIgual) {
            return Relacao.IGUAIS;
        }

        if (v1MenorOuIgual) {
            return Relacao.ANTES;
        }

        if (v2MenorOuIgual) {
            return Relacao.DEPOIS;
        }

        return Relacao.CONCORRENTES;
    }

    private enum Relacao {
        ANTES,
        DEPOIS,
        IGUAIS,
        CONCORRENTES
    }
}