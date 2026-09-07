package br.pucminas.iceibank.security;

import br.pucminas.iceibank.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final String internalKey;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   @Value("${iceibank.internal-key}") String internalKey) {
        this.jwtService = jwtService;
        this.internalKey = internalKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri.matches("/contas/\\d+/creditar-remoto")) {
            String recebida = request.getHeader("X-Internal-Key");
            if (!internalKey.equals(recebida)) {
                escrever401(response, "Chave interna ausente ou inválida.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            try {
                String usuario = jwtService.validarEObterClaims(token).getSubject();
                if (usuario == null || usuario.isBlank()) throw new JwtException("subject ausente");
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        usuario, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
                request.setAttribute("usuarioAutenticado", usuario);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                escrever401(response, "Token JWT inválido ou expirado.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void escrever401(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
