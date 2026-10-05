const TOKEN_KEY = 'iceibank.jwt';
const OFFSET_KEY = 'iceibank.offset';
let lastHttpStatus = null;
const $ = id => document.getElementById(id);
const agenciaSelect = $('agencia');

function offset(){ return Number(localStorage.getItem(OFFSET_KEY) || 0); }
function agenciaAtual(){ return Number(agenciaSelect.value); }
function baseUrl(){ return `http://localhost:${4000 + offset() + agenciaAtual()}`; }
function authBaseUrl(){ return `http://localhost:${4000 + offset()}`; }
function token(){ return localStorage.getItem(TOKEN_KEY); }
function agenciaResponsavel(id){ return Number(id) % 3; }
function moeda(v){ return Number(v ?? 0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'}); }

function toast(message,type='success'){
  const el=document.createElement('div'); el.className=`toast ${type}`; el.textContent=message;
  $('toastContainer').appendChild(el); setTimeout(()=>el.remove(),3400);
}
function setResult(data,isError=false,status=null){
  $('resultado').textContent=typeof data==='string'?data:JSON.stringify(data,null,2);
  const badge=$('httpBadge'); badge.textContent=status?`HTTP ${status}`:(isError?'Erro':'OK');
  badge.className=`http-badge ${isError?'error':'ok'}`;
}
function showAuthenticatedApp(){
  $('authLanding').classList.add('hidden'); $('appShell').classList.remove('hidden');
  showView('contas'); updateAgencyUI();
}
function showAuthLanding(mode='register'){
  $('appShell').classList.add('hidden'); $('authLanding').classList.remove('hidden');
  const login=mode==='login'; $('registerPanel').classList.toggle('hidden',login); $('loginPanel').classList.toggle('hidden',!login);
}
function showView(id){
  document.querySelectorAll('.view').forEach(v=>v.classList.remove('active-view'));
  document.querySelectorAll('.nav-item').forEach(v=>v.classList.remove('active'));
  $(id)?.classList.add('active-view'); document.querySelector(`.nav-item[data-target="${id}"]`)?.classList.add('active');
  $('pageTitle').textContent={contas:'Contas',movimentacoes:'Movimentações',transferencias:'Transferências'}[id]||'ICEIBank';
}
function updateAgencyUI(){ updatePartitionPreview(); }

async function requestTo(base,path,options={},authenticated=true){
  const headers={'Content-Type':'application/json',...(options.headers||{})};
  if(authenticated && token()) headers.Authorization=`Bearer ${token()}`;
  let response;
  try{ response=await fetch(`${base}${path}`,{...options,headers}); }
  catch(e){ const port=Number(new URL(base).port); const ag=port-(4000+offset()); throw new Error(`Não foi possível conectar à Agência ${ag}. Confirme se o ICEIBank foi iniciado corretamente.`); }
  lastHttpStatus=response.status;
  let payload; try{ payload=await response.json(); }catch{ payload={erro:`Resposta HTTP ${response.status}`}; }
  if(!response.ok){
    if(response.status===401 && authenticated){ localStorage.removeItem(TOKEN_KEY); showAuthLanding('login'); }
    const err=new Error(payload.erro||`Erro HTTP ${response.status}`); err.status=response.status; throw err;
  }
  return payload;
}
function request(path,options={},authenticated=true){ return requestTo(baseUrl(),path,options,authenticated); }
function authRequest(path,options={}){ return requestTo(authBaseUrl(),path,options,false); }
async function runOperation(fn,msg='Operação concluída.'){
  try{ const data=await fn(); setResult(data,false,lastHttpStatus); toast(msg); return data; }
  catch(e){ setResult(e.message,true,e.status||lastHttpStatus); toast(e.message,'error'); throw e; }
}

async function cadastrarUsuario(e){
  e.preventDefault();
  const senha=$('cadastroSenha').value;
  if(senha!==$('cadastroConfirmarSenha').value){ toast('As senhas não coincidem.','error'); return; }
  try{
    const data=await authRequest('/auth/cadastro',{method:'POST',body:JSON.stringify({nome:$('cadastroNome').value.trim(),usuario:$('cadastroUsuario').value.trim(),senha})});
    localStorage.setItem(TOKEN_KEY,data.token);
    $('registerForm').reset();
    showAuthenticatedApp();
    setResult({mensagem:'Cadastro realizado e JWT emitido.',tipoToken:data.tipoToken,expiraEmSegundos:data.expiraEmSegundos},false,lastHttpStatus);
    toast('Cadastro concluído. Acesso liberado com JWT.');
  }catch(e){ toast(e.message,'error'); }
}
async function login(e){
  e.preventDefault();
  try{
    const data=await authRequest('/auth/login',{method:'POST',body:JSON.stringify({usuario:$('usuario').value.trim(),senha:$('senha').value})});
    localStorage.setItem(TOKEN_KEY,data.token); showAuthenticatedApp(); setResult({mensagem:'Login realizado com sucesso.',tipoToken:data.tipoToken},false,lastHttpStatus); toast('Login realizado.');
  }catch(e){ toast(e.message,'error'); }
}
function logout(){ localStorage.removeItem(TOKEN_KEY); showAuthLanding('login'); toast('Sessão encerrada.'); }

function updatePartitionPreview(){
  const input=$('criarId'), preview=$('partitionPreview'); if(!input||input.value==='') return;
  const id=Number(input.value), owner=agenciaResponsavel(id), matches=owner===agenciaAtual();
  preview.textContent=`Conta ${id} pertence à Agência ${owner}. ${matches?'A agência selecionada pode cadastrá-la.':'Selecione a agência responsável para cadastrar.'}`;
  preview.className=`partition-preview ${matches?'good':'bad'}`;
}

function formatarDetalhes(d){
  if(!d) return '—';
  const partes=[];
  if(d.valor!=null) partes.push(`Valor: ${moeda(d.valor)}`);
  if(d.novoSaldo!=null) partes.push(`Saldo: ${moeda(d.novoSaldo)}`);
  if(d.idOrigem!=null) partes.push(`Origem: ${d.idOrigem}`);
  if(d.idDestino!=null) partes.push(`Destino: ${d.idDestino}`);
  if(d.nomeUsuario) partes.push(`Titular: ${d.nomeUsuario}`);
  return partes.length?partes.join(' · '):JSON.stringify(d);
}
function renderHistory(eventos){
  const body=$('historyBody'); body.innerHTML='';
  if(!eventos.length){ $('historyEmpty').textContent='Nenhuma transação encontrada para esta conta.'; $('historyEmpty').classList.remove('hidden'); $('historyTableWrap').classList.add('hidden'); return; }
  $('historyEmpty').classList.add('hidden'); $('historyTableWrap').classList.remove('hidden');
  eventos.forEach(ev=>{
    const tr=document.createElement('tr');
    tr.innerHTML=`<td>${JSON.stringify(ev.timestampVetorial)}</td><td>${ev.tipo}</td><td>${new Date(ev.horaParede).toLocaleString('pt-BR')}</td><td>${formatarDetalhes(ev.detalhes)}</td>`;
    body.appendChild(tr);
  });
}

document.querySelectorAll('.nav-item').forEach(el=>el.addEventListener('click',()=>showView(el.dataset.target)));
agenciaSelect.addEventListener('change',updateAgencyUI);
$('showExistingLogin').addEventListener('click',()=>showAuthLanding('login'));
$('showRegistration').addEventListener('click',()=>showAuthLanding('register'));
$('registerForm').addEventListener('submit',cadastrarUsuario);
$('loginForm').addEventListener('submit',login);
$('logoutBtn').addEventListener('click',logout);
$('criarId').addEventListener('input',updatePartitionPreview);

$('createAccountForm').addEventListener('submit',async e=>{ e.preventDefault(); await runOperation(()=>request('/contas',{method:'POST',body:JSON.stringify({id:Number($('criarId').value),nomeUsuario:$('criarNome').value.trim(),saldoInicial:Number($('criarSaldo').value||0)})}),'Conta criada com sucesso.').catch(()=>{}); });
$('balanceForm').addEventListener('submit',async e=>{ e.preventDefault(); const id=Number($('saldoId').value); const owner=agenciaResponsavel(id); const data=await runOperation(()=>requestTo(`http://localhost:${4000+offset()+owner}`,`/contas/${id}`,{},true),'Conta consultada.').catch(()=>null); if(data){ const r=$('accountResult'); r.classList.remove('empty'); r.querySelector('strong').textContent=moeda(data.saldo); r.querySelector('span').textContent=`${data.nomeUsuario} · Conta ${data.id}`; } });
$('historyForm').addEventListener('submit',async e=>{ e.preventDefault(); const id=Number($('historicoId').value), owner=agenciaResponsavel(id); const data=await runOperation(()=>requestTo(`http://localhost:${4000+offset()+owner}`,`/contas/${id}/historico`,{},true),'Histórico carregado.').catch(()=>null); if(data) renderHistory(data); });
$('depositForm').addEventListener('submit',async e=>{ e.preventDefault(); const id=Number($('depositId').value), owner=agenciaResponsavel(id); await runOperation(()=>requestTo(`http://localhost:${4000+offset()+owner}`,`/contas/${id}/depositar`,{method:'POST',body:JSON.stringify({valor:Number($('depositValue').value)})},true),'Depósito realizado.').catch(()=>{}); });
$('withdrawForm').addEventListener('submit',async e=>{ e.preventDefault(); const id=Number($('withdrawId').value), owner=agenciaResponsavel(id); await runOperation(()=>requestTo(`http://localhost:${4000+offset()+owner}`,`/contas/${id}/sacar`,{method:'POST',body:JSON.stringify({valor:Number($('withdrawValue').value)})},true),'Saque realizado.').catch(()=>{}); });
$('transferForm').addEventListener('submit',async e=>{ e.preventDefault(); const idOrigem=Number($('origem').value),idDestino=Number($('destino').value),valor=Number($('transfValor').value),owner=agenciaResponsavel(idOrigem); await runOperation(()=>requestTo(`http://localhost:${4000+offset()+owner}`,'/transferencias',{method:'POST',body:JSON.stringify({idOrigem,idDestino,valor})},true),'Transferência processada.').catch(()=>{}); });

if(token()) showAuthenticatedApp(); else showAuthLanding('register');
