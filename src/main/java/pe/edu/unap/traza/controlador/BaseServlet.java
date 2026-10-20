package pe.edu.unap.traza.controlador;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.edu.unap.traza.modelo.ReglaException;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * Base de todos los controladores. Recibe el pedido, llama a get() o post(),
 * y devuelve la respuesta en JSON. Si el modelo rechaza algo (ReglaException),
 * devuelve su mensaje; si la base de datos falla, «No se pudo guardar. Intente de nuevo».
 */
public abstract class BaseServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());
    static final String ATRIBUTO_USUARIO = "usuario";
    /** Marca que la respuesta ya se escribió (por ejemplo, al enviar un archivo). */
    static final Object YA_RESPONDIDO = new Object();

    /** Datos del pedido que usan los controladores. */
    protected record Peticion(HttpServletRequest req, HttpServletResponse resp, List<String> partes) {
        String parte(int i) { return i < partes.size() ? partes.get(i) : null; }
        int numero(int i) {
            try { return Integer.parseInt(parte(i)); }
            catch (NumberFormatException | NullPointerException e) { throw new ReglaException(404, "No existe"); }
        }
        String param(String nombre) { return req.getParameter(nombre); }
        Usuario usuario() {
            HttpSession s = req.getSession(false);
            Usuario u = s == null ? null : (Usuario) s.getAttribute(ATRIBUTO_USUARIO);
            if (u == null) throw new ReglaException(401, "Inicia sesión para continuar");
            return u;
        }
        /** RF-06: solo deja pasar a los roles indicados. */
        Usuario exigirRol(String... roles) {
            Usuario u = usuario();
            if (Arrays.stream(roles).noneMatch(u::es)) throw new ReglaException(403, "Esta página no es para tu rol");
            return u;
        }
    }

    protected Object get(Peticion p) throws Exception { throw new ReglaException(405, "Método no permitido"); }

    protected Object post(Peticion p) throws Exception { throw new ReglaException(405, "Método no permitido"); }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        String info = req.getPathInfo() == null ? "" : req.getPathInfo();
        List<String> partes = Arrays.stream(info.split("/")).filter(s -> !s.isEmpty()).toList();
        Peticion p = new Peticion(req, resp, partes);
        Object resultado;
        int estado = 200;
        try {
            resultado = switch (req.getMethod()) {
                case "GET" -> get(p);
                case "POST" -> post(p);
                default -> throw new ReglaException(405, "Método no permitido");
            };
        } catch (ReglaException e) {
            estado = e.estadoHttp();
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("mensaje", e.getMessage());
            error.putAll(e.datos());
            resultado = error;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error de base de datos", e);
            estado = 503;
            resultado = Map.of("ok", false, "mensaje", "No se pudo guardar. Intente de nuevo");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error inesperado", e);
            estado = 500;
            resultado = Map.of("ok", false, "mensaje", "Ocurrió un error inesperado. Intente de nuevo");
        }
        if (resultado == YA_RESPONDIDO) return;
        resp.setStatus(estado);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(Json.de(resultado));
    }

    /** Arma un objeto JSON con pares clave, valor. */
    protected static Map<String, Object> json(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) m.put(String.valueOf(pares[i]), pares[i + 1]);
        return m;
    }
}
