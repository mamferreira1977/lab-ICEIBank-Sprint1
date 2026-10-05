# ICEIBank — Sprint 2 (Java / Spring Boot / RabbitMQ)

Evolução do Sprint 1 para comunicação indireta com RabbitMQ e relógio vetorial. JWT, frontend e particionamento de contas foram preservados.

## Novidades da Sprint 2
- `spring-boot-starter-amqp` e exchange `iceibank.eventos` do tipo `topic`;
- fila durável `fila-agencia-<id>` por agência, routing key `agencia.<id>.creditar`;
- mensagens persistentes para transferências remotas;
- `RelogioVetorial` substitui Lamport;
- logs JSONL usam `timestampVetorial`;
- `MesclarLogs` identifica eventos concorrentes;
- funcionalidade adicional: `fila-auditoria`, vinculada com `#`, grava `data/auditoria-mensageria.jsonl`;
- cada processo inicia uma única agência, permitindo derrubar/reiniciar a Agência 1 no teste de resiliência.

## Pré-requisitos
Java 17+, Maven e uma instância RabbitMQ/CloudAMQP.

## Executar no PowerShell
Abra três terminais na pasta `agencia`. Em todos, defina sua URL do CloudAMQP:

```powershell
$env:RABBITMQ_URL="amqps://usuario:senha@host.cloudamqp.com/vhost"
```

Terminal 1:
```powershell
$env:AGENCIA_ID="0"
mvn spring-boot:run
```
Terminal 2:
```powershell
$env:AGENCIA_ID="1"
mvn spring-boot:run
```
Terminal 3:
```powershell
$env:AGENCIA_ID="2"
mvn spring-boot:run
```

Portas: 4000, 4001 e 4002, acrescidas de `OFFSET` se definido. Abra o frontend pela Agência 0.

## Teste de resiliência
Crie previamente uma conta pertencente à Agência 1. Derrube apenas o terminal da Agência 1, publique uma transferência para ela e depois reinicie-a. A fila permanece no RabbitMQ e entrega a mensagem quando a agência retorna. Como as contas são mantidas em memória, após reiniciar a conta deixa de existir; o consumidor registra `CREDITO_REMOTO_FALHOU`. Isso demonstra a diferença entre durabilidade da mensagem e persistência do estado da aplicação.

## Linha do tempo causal
Após gerar eventos em agências diferentes:
```powershell
mvn -q -DskipTests package
java -cp target/classes br.pucminas.iceibank.util.MesclarLogs
```

## Evidências esperadas
Coloque em `evidencias/sprint2/`: `transferencia-assincrona.png`, `resiliencia-fila.png`, `linha-do-tempo-causal.png` e um print da funcionalidade adicional de auditoria.
