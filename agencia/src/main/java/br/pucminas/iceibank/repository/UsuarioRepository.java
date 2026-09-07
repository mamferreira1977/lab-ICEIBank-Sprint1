package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.model.Usuario;

import java.nio.file.Path;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorUsuario(String usuario);
    void salvar(Usuario usuario);
    Path getArquivoUsuarios();
}
