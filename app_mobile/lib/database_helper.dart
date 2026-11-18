
import 'dart:convert';

import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

class DatabaseHelper {
  static Database? _database;

  static Future<Database> get database async {
    if (_database != null) return _database!;
    final file = join(await getDatabasesPath(), 'iceibank.db');
    _database = await openDatabase(
      file,
      version: 2,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE saldos (
            conta INTEGER PRIMARY KEY,
            saldo TEXT NOT NULL,
            vetor TEXT NOT NULL,
            versao INTEGER
          )
        ''');
        await db.execute('''
          CREATE TABLE pendentes (
            id TEXT PRIMARY KEY,
            conta INTEGER NOT NULL,
            tipo TEXT NOT NULL,
            valor REAL NOT NULL,
            vetorConhecido TEXT NOT NULL,
            estado TEXT NOT NULL,
            versaoConhecida INTEGER
          )
        ''');
      },
      onUpgrade: (db, oldVersion, newVersion) async {
  if (oldVersion < 2) {
    await db.execute(
      'ALTER TABLE saldos ADD COLUMN versao INTEGER'
    );

    await db.execute(
      'ALTER TABLE pendentes ADD COLUMN versaoConhecida INTEGER'
    );
  }
},
    );
    return _database!;
  }

static Future<void> salvarSaldo(
  int conta,
  String saldo,
  List<int> vetor,
  int versao,
) async {
  final db = await database;

  await db.insert(
    'saldos',
    {
      'conta': conta,
      'saldo': saldo,
      'vetor': jsonEncode(vetor),
      'versao': versao,
    },
    conflictAlgorithm: ConflictAlgorithm.replace,
  );
}


  static Future<Map<String, dynamic>?> buscarSaldo(int conta) async {
    final db = await database;
    final rows = await db.query('saldos', where: 'conta = ?', whereArgs: [conta], limit: 1);
    return rows.isEmpty ? null : rows.first;
  }

  static Future<void> salvarPendente(Map<String, dynamic> op) async {
    final db = await database;
    await db.insert(
      'pendentes',
      {
        'id': op['id'].toString(),
        'conta': op['conta'],
        'tipo': op['tipo'],
        'valor': op['valor'],
        'vetorConhecido': jsonEncode(op['vetorConhecido']),
        'estado': op['estado'],
         'versaoConhecida': op['versaoConhecida'],
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }
static Future<int> atualizarVersaoPendenteLegada(
  String operacaoId,
  int conta,
  int versao,
) async {
  final db = await database;

  return db.update(
    'pendentes',
    {'versaoConhecida': versao},
    where: '''
      id = ?
      AND conta = ?
      AND tipo = ?
      AND valor = ?
      AND estado = ?
      AND versaoConhecida IS NULL
    ''',
    whereArgs: [
      operacaoId,
      conta,
      'DEPOSITO',
      100.0,
      'PENDENTE',
    ],
  );
}
  static Future<List<Map<String, dynamic>>> listarPendentes() async {
    final db = await database;
    final rows = await db.query('pendentes', orderBy: 'id ASC');
    return rows.map((r) => <String, dynamic>{
      'id': r['id'] as String,
      'conta': r['conta'] as int,
      'tipo': r['tipo'] as String,
      'valor': (r['valor'] as num).toDouble(),
      'vetorConhecido': (jsonDecode(r['vetorConhecido'] as String) as List).cast<int>(),
      'estado': r['estado'] as String,
      'versaoConhecida': r['versaoConhecida'] as int?,
    }).toList();
  }

  static Future<void> removerPendente(String id) async {
    final db = await database;
    await db.delete('pendentes', where: 'id = ?', whereArgs: [id]);
  }
}
