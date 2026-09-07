package br.pucminas.iceibank.service;

import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Usuario;
import br.pucminas.iceibank.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public synchronized Usuario cadastrar(String nome, String usuario, String senha) {
        String nomeNormalizado = nome == null ? "" : nome.trim();
        String usuarioNormalizado = usuario == null ? "" : usuario.trim().toLowerCase();

        if (nomeNormalizado.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Nome é obrigatório.");
        if (usuarioNormalizado.length() < 3) throw new ApiException(HttpStatus.BAD_REQUEST, "Usuário deve ter pelo menos 3 caracteres.");
        if (senha == null || senha.length() < 6) throw new ApiException(HttpStatus.BAD_REQUEST, "Senha deve ter pelo menos 6 caracteres.");
        if (usuarioRepository.buscarPorUsuario(usuarioNormalizado).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Usuário já cadastrado.");
        }

        Usuario novo = new Usuario(nomeNormalizado, usuarioNormalizado,
                passwordEncoder.encode(senha), Instant.now().toString());
        usuarioRepository.salvar(novo);

        return novo;
    }

    public Usuario autenticar(String usuario, String senha) {
        String usuarioNormalizado = usuario == null ? "" : usuario.trim().toLowerCase();
        Usuario encontrado = usuarioRepository.buscarPorUsuario(usuarioNormalizado)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
        if (senha == null || !passwordEncoder.matches(senha, encontrado.senhaHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        return encontrado;
    }
}
