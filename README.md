# ICEIBank — Sprint 3 | Java + Flutter

## Visão geral

O Sprint 3 amplia o sistema bancário distribuído ICEIBank, preservando as funcionalidades desenvolvidas nos Sprints 1 e 2.

### Funcionalidades do Sprint 3

- Eleição de coordenador utilizando o algoritmo Bully.
- Monitoramento do líder por heartbeat e detecção de falhas.
- Reeleição e recuperação após indisponibilidade de agências.
- Registro de eventos e histórico de liderança.
- Aplicativo mobile desenvolvido em Flutter.
- Consulta periódica do líder atual pelo aplicativo.
- Armazenamento local SQLite com a biblioteca `sqflite`.
- Operações offline e sincronização posterior.
- Detecção de conflitos utilizando relógio vetorial e informações de versão.
- Tratamento de conflitos HTTP 409 sem reaplicação automática.

## Estrutura da entrega

- `agencia/` — Backend Java e Spring Boot.
- `app_mobile/` — Aplicativo Flutter.
- `frontend/` — Frontend web dos sprints anteriores.
- `evidencias/sprint3/` — Capturas dos testes do Sprint 3.
- `video/demonstracao-sprint3.mp4` — Demonstração do Sprint 3.
- `RESPOSTAS.md` — Respostas às 16 perguntas do roteiro.

## Execução

O backend utiliza três agências, nas portas 4000, 4001 e 4002, com a configuração padrão.

Antes da execução, configurar as variáveis de ambiente necessárias, incluindo `RABBITMQ_URL` para conexão com o RabbitMQ/CloudAMQP.

Para executar os testes do backend:

```powershell
cd agencia
mvn clean test
```

Para preparar o aplicativo Flutter:

```powershell
cd app_mobile
flutter pub get
flutter run
```

No emulador Android, o aplicativo utiliza `10.0.2.2` para acessar os serviços executados no computador.

## Documentação e evidências

As respostas do roteiro estão no arquivo `RESPOSTAS.md`. As capturas de tela estão em `evidencias/sprint3/`, e a demonstração está em `video/demonstracao-sprint3.mp4`.

---

## Documentação dos Sprints anteriores

A documentação original foi preservada abaixo.
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