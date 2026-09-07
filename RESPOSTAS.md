# 6.4 - Parte B — Relógio de Lamport

## 1. Por que o relógio de Lamport usa max(contador_local, timestampRecebido) + 1 ao receber uma mensagem, em vez de simplesmente adotar o timestamp recebido diretamente?

Ao receber uma mensagem, o processo precisa garantir que o evento de recebimento fique logicamente depois tanto dos eventos que ele próprio já processou quanto do evento de envio da mensagem recebida. Por isso, ele escolhe o maior valor entre o contador local e o timestamp recebido e soma 1.

## 2. Se a Agência 0 está no evento de contador 10 e recebe uma mensagem com timestamp 3 (de uma agência mais “atrasada”), qual o novo valor do contador da Agência 0? O que isso implica sobre agências que processam muitos eventos rapidamente versus agências mais lentas?

O novo valor é **11**. Isso mostra que uma agência que já processou muitos eventos não volta no tempo lógico ao receber uma mensagem de uma agência com contador menor. 

---

# 8.3 Parte D 

## 1. No trecho agenciaDestino === idAgencia, por que a transferência local não precisa da lógica de aoEnviar()/aoReceber() do relógio de Lamport, enquanto a transferência entre agências precisa?

Na transferência local, débito e crédito acontecem dentro do mesmo processo da mesma agência.Cada operação é apenas um evento local e usa `eventoLocal()`. Já na transferência entre agências existe comunicação entre processos diferentes: a origem executa `aoEnviar()` e envia o timestamp; o destino executa `aoReceber(timestampRecebido)`. 

## 2. Reproduza a falha conhecida (tarefa 5) e observe o saldo da conta de origem depois do erro. Ele foi revertido? O que isso significa em termos de consistência do sistema bancário?

Não. Na implementação do Sprint 1, o débito ocorre antes da chamada REST à agência de destino. Se a agência de destino estiver indisponível, o sistema registra `TRANSFERENCIA_FALHOU` e retorna HTTP 502, mas o valor debitado não é devolvido automaticamente à conta de origem. Isso significa que o sistema pode ficar temporariamente inconsistente: a origem perdeu saldo, mas o destino não recebeu o crédito.

## 3. Pensando à frente para o Sprint 4: cite, em alto nível, duas formas possíveis de corrigir esse problema (não precisa implementar agora, só descrever a ideia).

Uma possibilidade é usar uma **Saga**, dividindo a transferência em etapas e definindo uma ação compensatória; se o crédito remoto falhar depois do débito, uma compensação pode devolver o valor à conta de origem.

---

# 10.3 Parte E 

## O relógio de Lamport garante que, se A aconteceu antes de B causalmente, timestamp(A) < timestamp(B). Ele não garante a volta. O que isso significa na prática quando você vê dois eventos com timestamps diferentes na linha do tempo, mas sem saber se um realmente influenciou o outro?

Se sabemos que A aconteceu antes de B causalmente, o relógio de Lamport garante `L(A) < L(B)`. A ordenação por Lamport fornece uma ordem lógica consistente, mas não prova causalidade no sentido inverso.

## Baseado no que você observou no passo 3 da tarefa: o relógio de Lamport, sozinho, seria suficiente para um sistema que precisa distinguir com certeza “A e B são concorrentes” de “A aconteceu antes de B”? Por que isso motiva o relógio vetorial do Sprint 2?

Não. O relógio de Lamport não consegue, sozinho, distinguir em todos os casos se dois eventos são concorrentes ou se existe uma relação causal entre eles. Essa limitação motiva o relógio vetorial: com vetores é possível comparar componentes e detectar quando dois eventos não são ordenáveis entre si, caracterizando concorrência.

---

# 11.3 Parte F 

## Qual a diferença entre autenticação e autorização? Sua implementação verifica só uma das duas, ou as duas? Por exemplo: um usuário autenticado consegue sacar de uma conta que não é dele, na sua implementação atual?

Autenticação responde **quem é o usuário**; autorização responde **o que esse usuário pode fazer**. Esta implementação realiza autenticação via JWT, mas não implementa autorização por titularidade da conta.

## Por que o servidor não precisa consultar um banco de dados para validar a assinatura de um JWT a cada requisição? O que isso implica sobre escalabilidade, comparado a guardar sessões em memória no servidor?

O JWT é assinado. A agência consegue verificar a assinatura usando a chave secreta e também validar a expiração presente no próprio token. Assim, não é necessário consultar um banco de dados ou uma sessão central apenas para verificar a autenticidade do token em cada requisição.

## O que aconteceria com a segurança do sistema se a chave secreta usada para assinar o JWT vazasse?

Quem obtiver a chave secreta poderá assinar tokens que serão aceitos como válidos pelas agências, permitindo falsificar identidades e contornar a autenticação.

---

# 12.3 Parte G

## 1.Como o frontend “lembra” de reenviar o token em cada requisição depois do login? Descreva, em alto nível, o mecanismo que você implementou.

Depois do login, o frontend salva o JWT no `localStorage` com a chave `iceibank.jwt`. A função central de requisição lê esse valor e, nas chamadas protegidas, adiciona automaticamente o cabeçalho `Authorization: Bearer <token>`. Assim, as funções de consultar, depositar, sacar, criar conta, transferir e consultar status reutilizam o mesmo mecanismo.

## Se o token expirar enquanto alguém está usando o frontend no meio de uma operação, o que acontece na sua implementação? A interface avisa a pessoa usuária, ou ela só vê um erro genérico?

A API responde HTTP 401. O frontend detecta esse status, remove o token expirado do `localStorage`, atualiza a indicação de autenticação e exibe uma mensagem na própria tela informando que a sessão expirou ou que o token é inválido e que é necessário fazer login novamente.

## 3. Esta unidade da disciplina trata de arquitetura MVC. No seu frontend, onde fica o “M” (Model), o “V” (View) e o “C” (Controller)? Eles existem de forma clara na sua implementação, ou o código ficou mais misturado do que o padrão sugere?

No frontend simples deste Sprint, a separação MVC não é tão rígida quanto no backend. A **View** está principalmente no `index.html` e no `styles.css`. O papel de **Controller** aparece nas funções de `app.js` que respondem às ações do usuário, chamam a API e atualizam a tela. O **Model**  aparece nos objetos JavaScript utilizados para representar os dados recebidos/enviados e no estado do token armazenado no navegador. 

## Funcionalidade adicional — Histórico de transações por conta
Foi implementado o endpoint `GET /contas/{id}/historico`. Ele retorna os eventos relacionados à conta consultada, incluindo criação, depósito, saque e transferências nas quais a conta aparece como origem ou destino. Os eventos são lidos do arquivo `eventos-agencia-X.jsonl` da agência responsável e ordenados pelo `timestampLamport`. Escolhi essa funcionalidade porque acrescenta comportamento observável ao ICEIBank e facilita a conferência das operações e do relógio lógico de Lamport.

