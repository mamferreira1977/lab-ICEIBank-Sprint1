package br.pucminas.iceibank.controller;

import br.pucminas.iceibank.dto.CadastroUsuarioRequest;
import br.pucminas.iceibank.dto.LoginRequest;
import br.pucminas.iceibank.dto.LoginResponse;
import br.pucminas.iceibank.model.Usuario;
import br.pucminas.iceibank.service.JwtService;
import br.pucminas.iceibank.service.UsuarioService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthController(JwtService jwtService, UsuarioService usuarioService) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public LoginResponse cadastrar(@RequestBody CadastroUsuarioRequest request) {
        Usuario usuario = usuarioService.cadastrar(request.nome(), request.usuario(), request.senha());
        String token = jwtService.gerarToken(usuario.usuario());
        return new LoginResponse(token, "Bearer", jwtService.getExpiracaoSegundos());
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.usuario(), request.senha());
        String token = jwtService.gerarToken(usuario.usuario());
        return new LoginResponse(token, "Bearer", jwtService.getExpiracaoSegundos());
    }
}
