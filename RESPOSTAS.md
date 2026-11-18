# ICEIBank — Sprint 3
## Respostas às questões do roteiro

### Parte A — Mensagens de eleição e coordenador

**1. Por que o algoritmo de Bully sempre elege o processo de maior ID, e não o de menor? O resultado seria diferente (mas ainda assim correto) se a regra fosse invertida?**

O algoritmo Bully utiliza o maior identificador como critério de prioridade para escolher o coordenador. Essa regra estabelece uma ordem determinística entre os processos e permite que todos identifiquem qual agência deve assumir a liderança.

A escolha do maior ID é uma convenção do algoritmo. Seria possível inverter a regra e eleger o processo de menor ID, desde que todos os participantes adotassem consistentemente esse critério e o procedimento de eleição fosse adaptado. O resultado seria um líder diferente, mas a eleição continuaria correta.

**2. No passo 4 da tarefa, você deve ter visto mais de uma agência declarar `TORNOU_LIDER` durante a subida, antes de convergir. Isso é um bug, ou um comportamento esperado do algoritmo? Justifique com base no que você observou nos logs.**

É um comportamento transitório possível durante a inicialização distribuída. Quando uma agência inicia sua eleição e não consegue contatar processos de ID maior, pode declarar-se líder temporariamente. Quando as agências de maior ID entram em funcionamento, novas eleições podem substituir essa liderança.

Nos registros disponíveis, a agência 0 iniciou uma eleição e registrou o evento `TORNOU_LIDER` em 16/11/2026. Isso comprova uma declaração de liderança da agência 0. Entretanto, os arquivos de eventos disponíveis não permitem reconstruir toda a sequência de convergência entre as três agências. Assim, não é possível afirmar, apenas com esses registros, quantas declarações ocorreram durante a subida inicial.

O comportamento é esperado quando há convergência posterior para o maior ID ativo. A permanência de líderes divergentes, por outro lado, indicaria um problema.

**3. O endpoint `/bully/eleicao` não tem nenhuma autenticação. Qualquer processo que descubra a URL de uma agência pode disparar uma eleição nela. Isso é um problema de segurança real para este projeto? Por quê, ou por que não?**

Sim, existe um risco de segurança. Sem autenticação e autorização específicas para as mensagens de eleição, um agente não autorizado que consiga acessar o endpoint pode provocar eleições indevidas, gerar tráfego e prejudicar a estabilidade da liderança.

No contexto acadêmico, com três agências executadas em ambiente controlado, esse risco pode ser aceito como simplificação didática. Entretanto, em um sistema bancário real, seria necessário proteger a comunicação entre agências, utilizando mecanismos como autenticação entre serviços, TLS, autorização e restrições de acesso à rede interna.

### Parte B — Heartbeat, aplicativo e sincronização

**1. O que aconteceria se o intervalo do heartbeat (`intervaloMs`) fosse muito curto (ex.: 100ms)? E se fosse muito longo (ex.: 60 segundos)?**

Com um intervalo de 100 ms, as agências verificariam a disponibilidade do líder muito frequentemente. Isso permitiria detectar falhas rapidamente, mas aumentaria o tráfego de rede, o processamento e a possibilidade de verificações sobrepostas ou falsos alarmes em momentos de lentidão.

Com um intervalo de 60 segundos, a sobrecarga seria menor, mas uma falha poderia demorar muito mais para ser identificada, deixando o sistema temporariamente sem uma liderança funcional.

Na implementação analisada, o intervalo padrão configurado é de 3 segundos, buscando equilibrar rapidez de detecção e custo das verificações.

**2. No passo 6 da tarefa, a agência que você derrubou e subiu de novo pode, dependendo do ID dela, "tomar" a liderança de volta mesmo que a eleição atual estivesse funcionando bem. Isso é desejável? Que problema prático isso pode causar em um sistema real?**

Esse comportamento é coerente com a prioridade do algoritmo Bully: uma agência recuperada com ID superior ao do líder atual pode iniciar uma eleição e assumir a coordenação.

Entretanto, nem sempre é desejável operacionalmente. Uma troca de liderança durante uma transferência bancária ou outra operação importante pode interromper o processamento, gerar novas tentativas e exigir mecanismos de recuperação.

Em um sistema real, seria necessário coordenar as transições, preservar operações em andamento e garantir idempotência, para evitar processamento duplicado.

**3. O `TIMEOUT_MS` de 1 segundo é usado tanto para detectar uma agência fora do ar quanto para decidir se alguém respondeu a uma eleição. O que aconteceria se a rede estivesse genuinamente lenta e isso ultrapassasse 1 segundo?**

Uma resposta que demorasse mais de um segundo poderia ser interpretada como falha, mesmo que a agência estivesse funcionando normalmente.

Isso poderia provocar eleições desnecessárias, declarações temporárias de liderança e instabilidade no sistema. Em produção, seria recomendável ajustar os timeouts às características reais da rede e considerar tentativas adicionais ou mecanismos de detecção mais tolerantes a atrasos.

**4. Observando a saída do script, quantas vezes a liderança mudou de mãos durante a "tempestade" de eleições da subida inicial, antes de estabilizar? Isso tem algum custo real para o sistema?**

O histórico disponível comprova uma declaração de liderança pela agência 0, mas não contém os eventos completos das três agências referentes à mesma inicialização. Portanto, não é possível determinar com segurança o número exato de mudanças de liderança durante a tempestade inicial.

Cada eleição possui custo operacional: envio de mensagens HTTP, processamento, atualização de estados e registros de eventos. Mesmo quando o resultado final é correto, eleições repetidas podem aumentar o tempo necessário para estabilização e consumir recursos de rede e processamento.

**5. Como o app decide com que frequência consultar `/bully/lider`? Que trade-off você considerou ao escolher esse intervalo?**

O aplicativo Flutter utiliza `Timer.periodic` com intervalo de 3 segundos para consultar a liderança. Esse intervalo também aciona a rotina de sincronização.

A escolha representa um equilíbrio entre atualizar rapidamente a informação exibida ao usuário e evitar consultas excessivas, que aumentariam o consumo de rede, processamento e bateria do celular.

**6. Se, num momento de transição, você consultasse duas agências diferentes e elas respondessem líderes diferentes, como seu app se comporta? Isso é um problema real, ou se resolve sozinho na próxima consulta?**

Durante uma transição, diferentes agências podem apresentar temporariamente informações divergentes sobre o líder.

O aplicativo consulta o endpoint `/bully/lider` periodicamente e atualiza a informação apresentada conforme as respostas recebidas. Portanto, uma divergência temporária pode ser corrigida em consultas posteriores, quando as agências convergirem para o mesmo coordenador.

Entretanto, isso não garante consistência imediata. Em um sistema real, a informação de liderança precisaria ser validada antes de operações críticas, para evitar decisões baseadas em um coordenador desatualizado.

**7. Compare a arquitetura do app Flutter com a do frontend web do Sprint 1: o que foi reaproveitável e o que precisou ser pensado de forma diferente por ser uma plataforma mobile?**

Foram reaproveitados conceitos como autenticação, consumo de APIs REST, operações bancárias, consulta de saldos e separação entre a interface e o backend.

No Flutter, foi necessário considerar características específicas de dispositivos móveis, como conectividade intermitente, armazenamento persistente no aparelho, atualização periódica de informações, consumo de bateria e sincronização após períodos offline.

A principal diferença foi a necessidade de manter operações locais e reconciliá-las posteriormente com o servidor, em vez de depender exclusivamente de respostas imediatas da rede.

**8. Por que a agência compara o vetor inteiro, e não só verifica se o saldo da conta mudou? O que a comparação de vetores detecta que uma simples comparação de saldo não detectaria?**

O relógio vetorial representa informações sobre a evolução causal dos eventos distribuídos. A comparação do vetor completo permite identificar mudanças no estado conhecido do sistema que não seriam percebidas apenas pela comparação de saldos.

Por exemplo, um depósito de R$ 100 seguido de um saque de R$ 100 pode deixar o saldo final igual ao anterior, embora duas operações tenham ocorrido. A comparação apenas do saldo não identificaria essa evolução.

O relógio vetorial permite detectar que houve novos eventos e verificar se a visão utilizada pelo aplicativo está desatualizada.

**9. Neste exemplo, qualquer mudança no vetor da agência (mesmo em uma operação de outra conta) é tratada como conflito. Explique por que essa simplificação existe e o que seria necessário para tornar a detecção mais precisa, por conta.**

Essa simplificação existe porque o relógio vetorial representa os eventos da agência, e não exclusivamente os eventos de uma conta.

Assim, uma operação em outra conta pode alterar o vetor e fazer com que uma sincronização seja considerada potencialmente conflitante, mesmo sem alteração direta na conta envolvida.

Para melhorar a precisão, seria necessário manter versões ou informações de causalidade específicas para cada conta. Isso permitiria distinguir alterações relevantes para a operação pendente de eventos independentes ocorridos em outras contas.

**10. Quando a sincronização é recusada (409), a agência devolve o estado atual da conta. O que o app deveria fazer com essa informação — descartar a operação pendente, tentar de novo automaticamente, ou perguntar à pessoa usuária? Justifique sua escolha de design na Parte F.**

A alternativa mais segura é informar o conflito e permitir que a pessoa usuária avalie a situação antes de realizar uma nova operação.

Na implementação analisada, quando o servidor retorna HTTP 409, o aplicativo altera o estado da operação para `CONFLITO`, mantém o registro no SQLite, solicita a atualização do saldo e apresenta uma mensagem informando que a operação não foi aplicada.

A operação em conflito não é reenviada automaticamente. Essa decisão evita executar uma movimentação bancária baseada em dados desatualizados e preserva o registro para consulta.

### Parte F — Offline-first e interface

**1. Qual biblioteca de armazenamento local você escolheu, e por quê? Quais alternativas você considerou?**

Foi utilizada a biblioteca `sqflite`, que permite armazenar dados em um banco SQLite no aplicativo Flutter.

Essa solução possibilita persistir saldos, relógios vetoriais, versões das contas e operações pendentes, mesmo quando o aplicativo está sem conexão.

Como alternativas técnicas, existem `shared_preferences`, mais adequada para pequenas configurações, e Hive, que permite armazenamento local orientado a objetos e chave-valor. O SQLite é apropriado para o projeto porque oferece tabelas, consultas estruturadas e persistência das operações pendentes.

**2. Na tarefa 5, o que exatamente aconteceu na tela do app quando a sincronização foi recusada? Descreva o comportamento que você implementou e explique por que escolheu essa abordagem em vez de outra.**

Quando o servidor retornou HTTP 409, o aplicativo marcou a operação como `CONFLITO` e preservou seu registro no SQLite.

Em seguida, solicitou a atualização das informações da conta e apresentou uma mensagem informando que a operação não havia sido aplicada e que o saldo real deveria ser conferido.

Na interface, a operação permaneceu visível na seção **Operações locais**, com seu tipo, valor, conta e estado `CONFLITO`.

Essa abordagem evita que uma operação bancária seja repetida automaticamente após uma divergência. Em vez de descartá-la silenciosamente, o aplicativo mantém o registro e informa o usuário sobre a necessidade de conferir a situação.

**3. UI otimista significa mostrar o resultado esperado antes de ter certeza de que ele vai acontecer. Que risco isso traz para um aplicativo bancário, especificamente? Como seu design tenta reduzir esse risco?**

Em um aplicativo bancário, a interface otimista pode fazer a pessoa usuária acreditar que uma movimentação foi concluída quando ela ainda não foi confirmada pelo servidor.

Isso pode gerar decisões baseadas em saldo incorreto, expectativa de disponibilidade de valores e confusão sobre depósitos, saques ou transferências.

O design implementado procura reduzir esse risco distinguindo informações provisórias de informações confirmadas. As operações offline são registradas como `PENDENTE`, o aplicativo informa quando existe saldo provisório e, em caso de conflito HTTP 409, identifica a operação como `CONFLITO` e atualiza os dados da conta.

Dessa forma, o aplicativo permite o registro local de operações sem apresentar sua execução no servidor como definitivamente confirmada.