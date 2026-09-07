package br.pucminas.iceibank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class IceibankApplication {

    /**
     * Inicia as três agências a partir de um único main().
     *
     * Cada chamada a SpringApplication.run cria um ApplicationContext Spring
     * independente, com seus próprios beans, estado de contas e relógio de Lamport.
     * As instâncias compartilham apenas o mesmo processo Java e os arquivos da pasta data.
     *
     * Agência 0 -> 4000 + OFFSET
     * Agência 1 -> 4001 + OFFSET
     * Agência 2 -> 4002 + OFFSET
     */
    public static void main(String[] args) {
        int offset = lerInteiroAmbiente("OFFSET", 0);
        List<ConfigurableApplicationContext> contextos = new ArrayList<>();

        try {
            for (int idAgencia = 0; idAgencia < 3; idAgencia++) {
                int porta = 4000 + offset + idAgencia;

                SpringApplication app = new SpringApplication(IceibankApplication.class);
                app.setRegisterShutdownHook(true);

                // Defaults explícitos para tornar a inicialização robusta mesmo quando
                // application.properties não for carregado por alguma configuração do IDE.
                Map<String, Object> defaults = new HashMap<>();
                defaults.put("jwt.secret",
                        System.getenv().getOrDefault(
                                "JWT_SECRET",
                                "ICEIBank-Sprint1-chave-secreta-local-com-mais-de-32-bytes"
                        ));
                defaults.put("jwt.expiration-seconds",
                        System.getenv().getOrDefault("JWT_EXPIRATION_SECONDS", "600"));
                defaults.put("iceibank.internal-key",
                        System.getenv().getOrDefault(
                                "INTERNAL_API_KEY",
                                "iceibank-internal-sprint1"
                        ));
                app.setDefaultProperties(defaults);

                String[] argsDaAgencia = montarArgsDaAgencia(args, idAgencia, offset, porta);
                ConfigurableApplicationContext contexto = app.run(argsDaAgencia);
                contextos.add(contexto);

                System.out.printf(
                        "[ICEIBank] Agência %d iniciada em http://localhost:%d%n",
                        idAgencia,
                        porta
                );
            }

            System.out.println("============================================================");
            System.out.println(" ICEIBank iniciado com as 3 agências no mesmo processo Java");
            System.out.println(" Agência 0: http://localhost:" + (4000 + offset));
            System.out.println(" Agência 1: http://localhost:" + (4001 + offset));
            System.out.println(" Agência 2: http://localhost:" + (4002 + offset));
            System.out.println(" Abra a aplicação web pela Agência 0.");
            System.out.println("============================================================");
        } catch (RuntimeException e) {
            // Se uma das portas não puder ser aberta, encerra também as instâncias
            // que já haviam iniciado para não deixar o sistema parcialmente ativo.
            for (ConfigurableApplicationContext contexto : contextos) {
                try {
                    contexto.close();
                } catch (Exception ignored) {
                    // nada a fazer
                }
            }
            throw e;
        }
    }

    private static int lerInteiroAmbiente(String nome, int padrao) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nome + " deve ser um número inteiro.", e);
        }
    }

    /**
     * Remove argumentos que poderiam fazer as três instâncias tentarem usar a
     * mesma porta/ID e acrescenta os valores específicos de cada agência.
     */
    private static String[] montarArgsDaAgencia(String[] args,
                                                int idAgencia,
                                                int offset,
                                                int porta) {
        List<String> resultado = new ArrayList<>();

        if (args != null) {
            Arrays.stream(args)
                    .filter(arg -> !arg.startsWith("--server.port="))
                    .filter(arg -> !arg.startsWith("--agencia.id="))
                    .filter(arg -> !arg.startsWith("--agencia.offset="))
                    .forEach(resultado::add);
        }

        resultado.add("--server.port=" + porta);
        resultado.add("--agencia.id=" + idAgencia);
        resultado.add("--agencia.offset=" + offset);
        resultado.add("--spring.application.name=iceibank-agencia-" + idAgencia);

        return resultado.toArray(String[]::new);
    }
}
