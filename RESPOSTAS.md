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

# Parte E — Linha do tempo unificada

## 1. O que significa ver timestamps diferentes sem saber se os eventos se influenciaram?

Se sabemos que A aconteceu antes de B causalmente, o relógio de Lamport garante `L(A) < L(B)`. Porém, observar apenas `L(A) < L(B)` não permite concluir que A causou B. Dois eventos independentes podem receber valores diferentes apenas porque cada processo avançou seu contador de forma distinta. Portanto, a ordenação por Lamport fornece uma ordem lógica consistente, mas não prova causalidade no sentido inverso.

## 2. Lamport consegue distinguir com certeza concorrência de causalidade?

Não. O relógio de Lamport não consegue, sozinho, distinguir em todos os casos se dois eventos são concorrentes ou se existe uma relação causal entre eles. Essa limitação motiva o relógio vetorial: com vetores é possível comparar componentes e detectar quando dois eventos não são ordenáveis entre si, caracterizando concorrência.

## Observação do teste

Preencha este trecho depois de executar `MesclarLogs` e capturar a evidência real. Registre aqui quais dois eventos apresentaram o mesmo `timestampLamport`, de quais agências vieram, se eram independentes e como seus valores de `horaParede` se compararam.

---

# Parte F — Autenticação JWT

## Decisões de design

A implementação possui um cadastro inicial em `POST /auth/cadastro`, com nome, usuário e senha. A senha é transformada em hash BCrypt e o cadastro é armazenado em `agencia/data/usuarios.jsonl`, compartilhado pelas três instâncias locais. Depois, `POST /auth/login` valida usuário e senha e emite um JWT. O token expira em 600 segundos por padrão, configurável por `JWT_EXPIRATION_SECONDS`, e as três agências usam a mesma `JWT_SECRET`. O cadastro não substitui a criação da conta bancária: primeiro é criado o usuário de acesso; após o login, a rota protegida `POST /contas` cria a conta bancária.

A rota interna `creditar-remoto` não usa o mesmo JWT do usuário do frontend. Ela exige o cabeçalho `X-Internal-Key`, compartilhado apenas entre as agências. A decisão separa autenticação de usuário de autenticação de comunicação interna serviço-a-serviço. Para um sistema real, essa chave deveria estar em um cofre de segredos e uma solução mais robusta poderia usar mTLS ou tokens de serviço com rotação.

## 1. Diferença entre autenticação e autorização. O projeto verifica as duas?

Autenticação responde **quem é o usuário**; autorização responde **o que esse usuário pode fazer**. Esta implementação realiza autenticação via JWT, mas não implementa autorização por titularidade da conta. Portanto, qualquer usuário que possua um JWT válido consegue operar qualquer conta disponível na agência, desde que respeite as demais regras do sistema. Em um sistema bancário real, isso seria insuficiente e seria necessário relacionar o usuário às contas e verificar permissões a cada operação.

## 2. Por que não consultar banco para validar o JWT? Efeito na escalabilidade

O JWT é assinado. A agência consegue verificar a assinatura usando a chave secreta e também validar a expiração presente no próprio token. Assim, não é necessário consultar um banco de dados ou uma sessão central apenas para verificar a autenticidade do token em cada requisição. Isso reduz dependência de estado de sessão no servidor e facilita escalar horizontalmente várias instâncias. Sessões em memória exigem afinidade de sessão, replicação ou um armazenamento compartilhado para que todas as instâncias reconheçam o usuário.

## 3. O que acontece se a chave secreta vazar?

Quem obtiver a chave secreta poderá assinar tokens que serão aceitos como válidos pelas agências, permitindo falsificar identidades e contornar a autenticação. Nesse caso, a chave precisa ser revogada/trocada imediatamente e os tokens assinados com a chave comprometida devem deixar de ser aceitos. Em produção, segredos não devem ficar fixos no código-fonte.

---

# Parte G — Frontend

## 1. Como o frontend lembra de reenviar o token?

Depois do login, o frontend salva o JWT no `localStorage` com a chave `iceibank.jwt`. A função central de requisição lê esse valor e, nas chamadas protegidas, adiciona automaticamente o cabeçalho `Authorization: Bearer <token>`. Assim, as funções de consultar, depositar, sacar, criar conta, transferir e consultar status reutilizam o mesmo mecanismo.

## 2. O que acontece se o token expirar durante uma operação?

A API responde HTTP 401. O frontend detecta esse status, remove o token expirado do `localStorage`, atualiza a indicação de autenticação e exibe uma mensagem na própria tela informando que a sessão expirou ou que o token é inválido e que é necessário fazer login novamente. Portanto, o erro não fica restrito ao console do navegador.

## 3. Onde estão M, V e C no frontend?

No frontend simples deste Sprint, a separação MVC não é tão rígida quanto no backend. A **View** está principalmente no `index.html` e no `styles.css`. O papel de **Controller** aparece nas funções de `app.js` que respondem às ações do usuário, chamam a API e atualizam a tela. O **Model** é leve e aparece nos objetos JavaScript utilizados para representar os dados recebidos/enviados e no estado do token armazenado no navegador. Como foi escolhido JavaScript puro para manter o frontend pequeno, essas responsabilidades ficam menos isoladas do que em um framework com estrutura própria de componentes/estado.

## Funcionalidade adicional — Histórico de transações por conta
Foi implementado o endpoint `GET /contas/{id}/historico`. Ele retorna os eventos relacionados à conta consultada, incluindo criação, depósito, saque e transferências nas quais a conta aparece como origem ou destino. Os eventos são lidos do arquivo `eventos-agencia-X.jsonl` da agência responsável e ordenados pelo `timestampLamport`. Escolhi essa funcionalidade porque acrescenta comportamento observável ao ICEIBank e facilita a conferência das operações e do relógio lógico de Lamport.

