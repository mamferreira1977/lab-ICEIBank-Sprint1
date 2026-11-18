
import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'database_helper.dart';

void main() => runApp(const MaterialApp(
      home: BankPage(),
      debugShowCheckedModeBanner: false,
    ));

class BankPage extends StatefulWidget {
  const BankPage({super.key});

  @override
  State<BankPage> createState() => _BankPageState();
}

class _BankPageState extends State<BankPage> {
  // Android Emulator: 10.0.2.2 aponta para o localhost do computador.
  final urls = List.generate(3, (i) => 'http://10.0.2.2:${4000 + i}');

  final usuario = TextEditingController();
  final senha = TextEditingController();
  final conta = TextEditingController(text: '1002');
  final destino = TextEditingController();
  final valor = TextEditingController();

  String token = '';
  String status = 'Entre com suas credenciais';
  String lider = 'Aguardando';
  String saldo = '—';

  List<int> vetor = [0, 0, 0];
  List<Map<String, dynamic>> pendentes = [];

  Timer? timer;
  StreamSubscription<List<ConnectivityResult>>? conectividade;

  bool online = false;
  bool sincronizando = false;
  bool carregado = false;
  bool consultandoLider = false;

  int _versaoConsulta = 0;

  int? get contaAtual {
    final id = int.tryParse(conta.text.trim());
    return id == null || id < 0 ? null : id;
  }

  @override
  void initState() {
    super.initState();
    _carregar();

    timer = Timer.periodic(const Duration(seconds: 3), (_) {
      _atualizarLider();
      _sincronizar();
    });

    conectividade = Connectivity().onConnectivityChanged.listen((_) {
      _sincronizar();
      _atualizarLider();
    });
  }

  @override
  void dispose() {
    timer?.cancel();
    conectividade?.cancel();

    for (final c in [usuario, senha, conta, destino, valor]) {
      c.dispose();
    }

    super.dispose();
  }

  void _msg(String mensagem) {
    if (mounted) {
      setState(() => status = mensagem);
    }
  }

  Future<void> _guardarToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', token);
  }

  Future<void> _carregar() async {
    try {
      final prefs = await SharedPreferences.getInstance();

      token = prefs.getString('token') ?? '';
      pendentes = await DatabaseHelper.listarPendentes();

      debugPrint('ICEIBank - operações pendentes: $pendentes');

      final saldoCache = await DatabaseHelper.buscarSaldo(1002);

      debugPrint('ICEIBank - saldo em cache da conta 1002: $saldoCache');
      debugPrint('ICEIBank - versão no cache: ${saldoCache?['versao']}');

      carregado = true;

      await _mostrarCache(contaAtual);

      _atualizarLider();
      _sincronizar();
    } catch (e) {
      carregado = true;
      _msg('Falha ao abrir armazenamento local: $e');
    }
  }

  double _provisao(int id) {
    double total = 0;

    for (final op in pendentes) {
      if (op['conta'] == id && op['estado'] == 'PENDENTE') {
        final quantia = (op['valor'] as num).toDouble();

        total += op['tipo'] == 'DEPOSITO'
            ? quantia
            : -quantia;
      }
    }

    return total;
  }

  int _quantidadePendentes(int id) {
    return pendentes
        .where((op) =>
            op['conta'] == id &&
            op['estado'] == 'PENDENTE')
        .length;
  }

  Future<void> _mostrarCache(int? id) async {
    if (id == null) {
      if (mounted) {
        setState(() {
          saldo = '—';
          vetor = [0, 0, 0];
          online = false;
        });
      }
      return;
    }

    final registro = await DatabaseHelper.buscarSaldo(id);

    if (!mounted || contaAtual != id) return;

    if (registro == null) {
      setState(() {
        saldo = '—';
        vetor = [0, 0, 0];
        online = false;
      });
      return;
    }

    final base = double.parse(registro['saldo'] as String);

    setState(() {
      vetor = (jsonDecode(registro['vetor'] as String) as List).cast<int>();
      saldo = (base + _provisao(id)).toStringAsFixed(2);
      online = false;
    });
  }

  Future<http.Response> _request(
    String metodo,
    String rota, {
    Map<String, dynamic>? dados,
    int? id,
  }) async {
    final agencia = id ?? ((contaAtual ?? 0) % 3);
    final uri = Uri.parse('${urls[agencia]}$rota');

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };

    if (token.isNotEmpty && !rota.startsWith('/auth/')) {
      headers['Authorization'] = 'Bearer $token';
    }

    if (metodo == 'GET') {
      final client = HttpClient()
  ..findProxy = (_) => 'DIRECT';

final request = await client.getUrl(uri);
headers.forEach((nome, valor) {
  request.headers.set(nome, valor);
});

final response = await request.close()
    .timeout(const Duration(seconds: 10));

final body = await response.transform(utf8.decoder).join();
client.close();

return http.Response(body, response.statusCode);
    }

    return http
        .post(
          uri,
          headers: headers,
          body: jsonEncode(dados ?? {}),
        )
        .timeout(const Duration(seconds: 5));
  }

  Future<void> _login() async {
    try {
      final r = await _request(
        'POST',
        '/auth/login',
        id: 0,
        dados: {
          'usuario': usuario.text.trim(),
          'senha': senha.text,
        },
      );

      if (r.statusCode != 200) {
        throw Exception(r.body);
      }

      token = (jsonDecode(r.body) as Map<String, dynamic>)['token'] as String;

      await _guardarToken();

      _msg('Autenticado');

      await _saldo();
      _sincronizar();
    } catch (e) {
      _msg('Falha no login: $e');
    }
  }

  Future<void> _atualizarLider() async {
    if (consultandoLider) return;

    consultandoLider = true;

    try {
      // Consulta as agências em ordem decrescente.
      // Se a agência 2 estiver indisponível, tenta 1 e depois 0.
      for (int i = 2; i >= 0; i--) {
        try {
          final r = await http.get(
  Uri.parse('${urls[i]}/bully/lider'),
).timeout(const Duration(seconds: 10));


          debugPrint(
            'LIDERANCA Agencia $i: HTTP ${r.statusCode} - ${r.body}',
          );

          if (r.statusCode == 200) {
            final dados = jsonDecode(r.body) as Map<String, dynamic>;
            final atual = dados['liderAtual'];

            if (mounted) {
              setState(() {
                lider = atual == null || atual.toString().isEmpty
                    ? 'Em eleição'
                    : 'Agência $atual';
              });
            }

            return;
          }
        } catch (e) {
          debugPrint('ERRO LIDERANCA - Agencia $i: $e');
        }
      }

      if (mounted) {
        setState(() {
          lider = 'Sem conexão';
        });
      }
    } finally {
      consultandoLider = false;
    }
  }

  Future<void> _saldo() async {
    final id = contaAtual;

    if (id == null) {
      _msg('Informe uma conta válida');
      return;
    }

    final versao = ++_versaoConsulta;

    try {
      final r = await _request(
        'GET',
        '/contas/$id',
        id: id % 3,
      );

      if (r.statusCode != 200) {
        if (r.statusCode == 401 || r.statusCode == 403) {
          _msg('Sessão expirada ou sem autorização: faça login novamente');
          return;
        }

        _msg('Consulta recusada (${r.statusCode}): ${r.body}');
        await _mostrarCache(id);
        return;
      }

      final data = jsonDecode(r.body) as Map<String, dynamic>;

      final saldoReal = data['saldo'].toString();
      final vetorReal = (data['vetorAtual'] as List).cast<int>();
      final versaoReal = (data['versao'] as num).toInt();

      await DatabaseHelper.salvarSaldo(
        id,
        saldoReal,
        vetorReal,
        versaoReal,
      );

      if (!mounted || contaAtual != id || versao != _versaoConsulta) {
        return;
      }

      final provisao = _provisao(id);

      setState(() {
        vetor = vetorReal;
        saldo = (double.parse(saldoReal) + provisao).toStringAsFixed(2);
        online = _quantidadePendentes(id) == 0;

        status = online
            ? 'Saldo atualizado'
            : 'Saldo provisório: há operações pendentes';
      });
    } on TimeoutException {
      await _mostrarCache(id);
      _msg('Sem resposta da agência: saldo em cache/provisório');
    } on http.ClientException {
      await _mostrarCache(id);
      _msg('Sem conexão: saldo em cache/provisório');
    } on SocketException {
      await _mostrarCache(id);
      _msg('Sem conexão: saldo em cache/provisório');
    } catch (e) {
      await _mostrarCache(id);
      _msg('Falha na consulta: $e');
    }
  }

  Future<void> _operar(String tipo) async {
    if (!carregado) {
      _msg('Aguarde o armazenamento local carregar');
      return;
    }

    final id = contaAtual;

    if (id == null) {
      _msg('Informe uma conta válida');
      return;
    }

    final v = double.tryParse(
      valor.text.trim().replaceAll(',', '.'),
    );

    if (v == null || !v.isFinite || v <= 0) {
      _msg('Informe um valor positivo');
      return;
    }

    if (tipo == 'TRANSFERENCIA') {
      final idDestino = int.tryParse(destino.text.trim());

      if (idDestino == null || idDestino < 0) {
        _msg('Informe uma conta de destino válida');
        return;
      }

      try {
        final r = await _request(
          'POST',
          '/transferencias',
          id: id % 3,
          dados: {
            'idOrigem': id,
            'idDestino': idDestino,
            'valor': v,
          },
        );

        if (r.statusCode != 200) {
          throw Exception('${r.statusCode}: ${r.body}');
        }

        _msg('Transferência solicitada');
        await _saldo();
      } catch (e) {
        _msg('Transferência exige conexão: $e');
      }

      return;
    }

    if (_quantidadePendentes(id) > 0) {
      _msg(
        'Há operações pendentes nesta conta. '
        'Aguarde a sincronização antes de operar novamente online.',
      );
      return;
    }

    try {
      final rota = tipo == 'DEPOSITO' ? 'depositar' : 'sacar';

      final r = await _request(
        'POST',
        '/contas/$id/$rota',
        id: id % 3,
        dados: {'valor': v},
      );

      if (r.statusCode != 200) {
        _msg('Operação recusada (${r.statusCode}): ${r.body}');
        return;
      }

      await _saldo();
      _msg('$tipo confirmado');
    } on TimeoutException {
      await _registrarOffline(id, tipo, v);
    } on http.ClientException {
      await _registrarOffline(id, tipo, v);
    } on SocketException {
      await _registrarOffline(id, tipo, v);
    } catch (e) {
      _msg('Operação recusada: $e');
    }
  }

  Future<void> _registrarOffline(
    int id,
    String tipo,
    double quantia,
  ) async {
    final registro = await DatabaseHelper.buscarSaldo(id);

    if (registro == null) {
      _msg('Consulte esta conta online antes de operar offline');
      return;
    }

    final base = double.parse(registro['saldo'] as String);
    final saldoProvisorio = base + _provisao(id);

    if (tipo == 'SAQUE' && saldoProvisorio < quantia) {
      _msg('Saldo em cache insuficiente');
      return;
    }

    final vetorBase =
        (jsonDecode(registro['vetor'] as String) as List).cast<int>();

    final versaoBase = registro['versao'] as int?;

    final op = <String, dynamic>{
      'versaoConhecida': versaoBase,
      'id': DateTime.now().microsecondsSinceEpoch.toString(),
      'conta': id,
      'tipo': tipo,
      'valor': quantia,
      'vetorConhecido': List<int>.from(vetorBase),
      'estado': 'PENDENTE',
    };

    try {
      await DatabaseHelper.salvarPendente(op);
      pendentes.add(op);

      await _mostrarCache(id);

      _msg('$tipo registrado localmente; aguardando sincronização');
    } catch (e) {
      _msg('Não foi possível salvar operação offline: $e');
    }
  }

  Future<void> _sincronizar() async {
    if (!carregado ||
        sincronizando ||
        token.isEmpty ||
        !pendentes.any((e) => e['estado'] == 'PENDENTE')) {
      return;
    }

    sincronizando = true;

    try {
      for (final op
          in pendentes.where((e) => e['estado'] == 'PENDENTE').toList()) {
        final id = op['conta'] as int;

        try {
          final r = await _request(
            'POST',
            '/contas/$id/sincronizar',
            id: id % 3,
            dados: {
              'tipo': op['tipo'],
              'valor': op['valor'],
              'vetorConhecido': op['vetorConhecido'],
              'operacaoId': op['id'],
              'versaoConhecida': op['versaoConhecida'],
            },
          );

          if (r.statusCode == 409) {
            op['estado'] = 'CONFLITO';

            await DatabaseHelper.salvarPendente(op);
            await _saldo();

            _msg(
              'CONFLITO 409: operação não aplicada; '
              'confira o saldo real da conta $id',
            );

            continue;
          }

          if (r.statusCode == 401 || r.statusCode == 403) {
            _msg('Sessão expirada: faça login novamente');
            break;
          }

          if (r.statusCode != 200) {
            _msg('Sincronização recusada (${r.statusCode}): ${r.body}');
            break;
          }

          await DatabaseHelper.removerPendente(op['id'].toString());
          pendentes.remove(op);

          await _saldo();

          _msg('Operação da conta $id sincronizada');
        } on TimeoutException {
          break;
        } on http.ClientException {
          break;
        } on SocketException {
          break;
        }
      }
    } catch (e) {
      _msg('Erro na sincronização: $e');
    } finally {
      sincronizando = false;

      if (mounted) {
        setState(() {});
      }
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('ICEIBank'),
        ),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Card(
              child: ListTile(
                title: const Text('Liderança atual'),
                subtitle: Text(lider),
                trailing: const Icon(Icons.hub),
              ),
            ),
            TextField(
              controller: usuario,
              decoration: const InputDecoration(
                labelText: 'Usuário',
              ),
            ),
            TextField(
              controller: senha,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Senha',
              ),
            ),
            ElevatedButton(
              onPressed: _login,
              child: const Text('Login'),
            ),
            TextField(
              controller: conta,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Número da conta',
              ),
              onChanged: (_) async {
                ++_versaoConsulta;
                await _mostrarCache(contaAtual);
              },
            ),
            ElevatedButton(
              onPressed: _saldo,
              child: const Text('Consultar saldo'),
            ),
            Text(
              'Saldo ${online ? 'confirmado' : 'em cache/provisório'}: R\$ $saldo',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            TextField(
              controller: valor,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Valor',
              ),
            ),
            Wrap(
              spacing: 8,
              children: [
                ElevatedButton(
                  onPressed: () => _operar('DEPOSITO'),
                  child: const Text('Depósito'),
                ),
                ElevatedButton(
                  onPressed: () => _operar('SAQUE'),
                  child: const Text('Saque'),
                ),
              ],
            ),
            TextField(
              controller: destino,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Conta de destino',
              ),
            ),
            ElevatedButton(
              onPressed: () => _operar('TRANSFERENCIA'),
              child: const Text('Transferir (online)'),
            ),
            const Divider(),
            Text('Operações locais: ${pendentes.length}'),
            for (final op in pendentes)
              ListTile(
                title: Text('${op['tipo']} R\$ ${op['valor']}'),
                subtitle: Text(
                  '${op['estado']} • Conta ${op['conta']}',
                ),
              ),
            const Divider(),
            Text(status),
          ],
        ),
      );
}
