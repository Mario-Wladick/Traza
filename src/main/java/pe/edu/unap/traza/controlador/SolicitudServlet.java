package pe.edu.unap.traza.controlador;

import jakarta.servlet.annotation.WebServlet;
import pe.edu.unap.traza.modelo.ReglaException;
import pe.edu.unap.traza.modelo.SolicitudModelo;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * POST /api/solicitudes                 pedir una cuenta (sin iniciar sesión), desde «Regístrate»
 * GET  /api/solicitudes                 solicitudes pendientes (secretaría)
 * POST /api/solicitudes/{id}/aprobar    crea el usuario (secretaría)
 * POST /api/solicitudes/{id}/rechazar   la descarta (secretaría)
 */
@WebServlet(urlPatterns = {"/api/solicitudes", "/api/solicitudes/*"})
public class SolicitudServlet extends BaseServlet {

    private final SolicitudModelo solicitudes = new SolicitudModelo();

    @Override
    protected Object get(Peticion p) throws Exception {
        p.exigirRol("secretaria");
        if ("contar".equals(p.parte(0))) return json("ok", true, "pendientes", solicitudes.contarPendientes());
        return json("ok", true, "solicitudes", solicitudes.listarPendientes());
    }

    @Override
    protected Object post(Peticion p) throws Exception {
        if (p.partes().isEmpty()) {
            return json("ok", true, "solicitud",
                    solicitudes.solicitarCuenta(p.param("codigo"), p.param("nombre"), p.param("rol"), p.param("clave")));
        }
        Usuario secretaria = p.exigirRol("secretaria");
        int id = p.numero(0);
        switch (p.parte(1) == null ? "" : p.parte(1)) {
            case "aprobar" -> { return json("ok", true, "usuario", solicitudes.aprobar(secretaria, id)); }
            case "rechazar" -> { solicitudes.rechazar(secretaria, id); return json("ok", true); }
            default -> throw new ReglaException(404, "No existe");
        }
    }
}
