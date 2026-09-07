# ICEIBank — Sprint 1 em Java / Spring Boot

## Execução atual: um único `IceibankApplication`

A versão atual inicia as três agências com **uma única execução** de `IceibankApplication`. O `main()` cria três contextos Spring Boot independentes: Agência 0 em 4000, Agência 1 em 4001 e Agência 2 em 4002. Cada contexto possui seus próprios beans, mapa de contas e relógio de Lamport. Não é necessário configurar `AGENCIA_ID` no IntelliJ.

Abra `http://localhost:4000` após o console confirmar que as três agências foram iniciadas.


Implementação em Java do exercício do Sprint 1: API REST/MVC, partição de contas, relógio lógico de Lamport, transferências locais e entre agências, autenticação JWT, linha do tempo unificada, frontend web e funcionalidade adicional.

## 1. Abrir no IntelliJ

Abra a pasta `iceibank/agencia` como projeto Maven e aguarde a importação das dependências do `pom.xml`.

## 2. Compilar

No terminal, dentro de `iceibank/agencia`:

```powershell
mvn clean package
```

O JAR será criado em `target/iceibank-agencia-1.0.0.jar`.

## 3. Executar o ICEIBank

Execute **somente uma vez** a classe:

```text
br.pucminas.iceibank.IceibankApplication
```

O próprio `main()` inicializa três contextos Spring Boot independentes. Com `OFFSET=0`:

- Agência 0 → `http://localhost:4000`
- Agência 1 → `http://localhost:4001`
- Agência 2 → `http://localhost:4002`

Não configure `AGENCIA_ID` e não crie três Run Configurations no IntelliJ. Se estiver em uma máquina compartilhada, defina apenas `OFFSET` antes de iniciar a aplicação.

Ao final da inicialização, o console deve mostrar explicitamente as três agências e as três portas. Abra o frontend em `http://localhost:4000`.