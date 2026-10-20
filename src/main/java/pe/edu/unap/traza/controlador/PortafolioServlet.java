package pe.edu.unap.traza.controlador;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Part;
import java.io.InputStream;
import pe.edu.unap.traza.modelo.EvidenciaModelo;
import pe.edu.unap.traza.modelo.PortafolioModelo;
import pe.edu.unap.traza.modelo.ReglaException;
import pe.edu.unap.traza.modelo.Objetos.Evidencia;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * GET  /api/portafolio            → armarPortafolio del estudiante (RF-04, RF-05)
 * POST /api/portafolio/evidencias → subirEvidencia (RF-04). No valida el archivo: eso lo hace el modelo.
 */
@WebServlet(urlPatterns = {"/api/portafolio", "/api/portafolio/*"})
@MultipartConfig(maxFileSize = 25L * 1024 * 1024, maxRequestSize = 26L * 1024 * 1024, fileSizeThreshold = 1024 * 1024)
public class PortafolioServlet extends BaseServlet {

    private final PortafolioModelo portafolios = new PortafolioModelo();
    private final EvidenciaModelo evidencias = new EvidenciaModelo();

    @Override
    protected Object get(Peticion p) throws Exception {
        Usuario est = p.exigirRol("estudiante");
        return json("ok", true, "portafolio", portafolios.armarPortafolioEstudiante(est));
    }

    @Override
    protected Object post(Peticion p) throws Exception {
        if (!"evidencias".equals(p.parte(0))) throw new ReglaException(404, "No existe");
        Usuario est = p.exigirRol("estudiante");
        Part archivo;
        try {
            archivo = p.req().getPart("archivo");
        } catch (IllegalStateException e) {
            throw new ReglaException(413, "El archivo pesa más de 10 MB, que es el máximo");
        }
        int curso;
        try { curso = Integer.parseInt(p.param("curso")); } catch (NumberFormatException e) { throw new ReglaException("Elige el curso"); }
        boolean reemplazar = "true".equals(p.param("reemplazar"));
        try (InputStream contenido = archivo == null || archivo.getSize() == 0 ? null : archivo.getInputStream()) {
            Evidencia ev = evidencias.subirEvidencia(est, curso, p.param("unidad"), p.param("tipo"),
                    archivo == null ? null : archivo.getSubmittedFileName(),
                    archivo == null ? null : archivo.getContentType(),
                    archivo == null ? 0 : archivo.getSize(), contenido, reemplazar);
            return json("ok", true, "evidencia", ev);
        }
    }
}
