package pe.edu.unap.traza.controlador;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpSession;
import pe.edu.unap.traza.modelo.CursoModelo;
import pe.edu.unap.traza.modelo.UsuarioModelo;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * POST /api/ingresar  → iniciarSesion (RF-06) y elige la página según el rol
 * GET  /api/sesion    → quién tiene la sesión abierta
 * POST /api/salir     → cierra la sesión
 */
@WebServlet(urlPatterns = {"/api/ingresar", "/api/sesion", "/api/salir"})
public class IngresoServlet extends BaseServlet {

    private final UsuarioModelo usuarios = new UsuarioModelo();
    private final CursoModelo cursos = new CursoModelo();

    @Override
    protected Object get(Peticion p) throws Exception {
        Usuario u = p.usuario();
        return json("ok", true, "usuario", u, "cursos", cursos.cursosDe(u), "inicio", inicioDe(u));
    }

    @Override
    protected Object post(Peticion p) throws Exception {
        if (p.req().getServletPath().equals("/api/salir")) {
            HttpSession s = p.req().getSession(false);
            if (s != null) s.invalidate();
            return json("ok", true);
        }
        Usuario u = usuarios.iniciarSesion(p.param("codigo"), p.param("clave"));
        HttpSession anterior = p.req().getSession(false);
        if (anterior != null) anterior.invalidate();          // sesión nueva en cada ingreso
        HttpSession s = p.req().getSession(true);
        s.setAttribute(ATRIBUTO_USUARIO, u);
        s.setMaxInactiveInterval(60 * 60);                      // 1 hora sin uso
        return json("ok", true, "usuario", u, "cursos", cursos.cursosDe(u), "inicio", inicioDe(u));
    }

    /** La página a la que entra cada rol (Práctica 4, columna «Desde dónde se llega»). */
    private String inicioDe(Usuario u) {
        return switch (u.rol()) {
            case "docente" -> "#/cursos";
            case "estudiante" -> "#/portafolio";
            case "secretaria" -> "#/matriculas/nueva";
            default -> "#/portafolio-cursos";
        };
    }
}
