# ICEIBank Mobile — Sprint 3

1. Instale Flutter SDK e Android Studio.
2. Dentro de `app_mobile`, execute `flutter create . --platforms android` (gera os arquivos nativos ausentes).
3. Execute `flutter pub get` e `flutter run` em emulador Android.
4. Inicie as agências Java nas portas 4000, 4001, 4002.
5. O endereço `10.0.2.2` é para emulador Android; em dispositivo físico, altere para o IP da máquina.

**Limites:** transferências exigem conexão; a fila offline cobre depósitos e saques. Operações em conflito ficam retidas para decisão humana. O protótipo não é apropriado para uso bancário real sem idempotência persistente, autenticação entre agências, TLS e controles de autorização por conta.
