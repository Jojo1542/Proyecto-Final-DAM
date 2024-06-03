package es.iesmm.proyecto.drivehub.backend.util.filter;

import es.iesmm.proyecto.drivehub.backend.service.jwt.JwtService;
import es.iesmm.proyecto.drivehub.backend.service.user.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private JwtService jwtService;
    private UserService userService;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        // Si la respuesta ya ha sido enviada, no se hace nada.
        if (response.isCommitted()) {
            return;
        }

        String token = jwtService.extractTokenFromRequest(request);

        if (token != null) {
            try {
                Long userId = jwtService.extractUserId(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    userService
                            .findById(userId)
                            .filter(user -> jwtService.isValid(token, user))
                            .ifPresent(user -> {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        user.getAuthorities()
                                );

                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
                }
            } catch (ExpiredJwtException e) {
                // Se ha expirado el token, en este caso solamente se ignora.
                // Se registra por pantalla solamente en modo debug.
                logger.debug("Token " + token + " expirado", e);
            } catch (Exception e) {
                // Se avisa por consola siempre de que hubo un error y el mensaje de error.
                logger.error("Error al procesar el token " + token + " enviado por " + request.getRemoteAddr()
                        + " -> " + e.getMessage());

                // En modo debug se muestra el stacktrace del error.
                logger.debug("Error al procesar el token", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
