package pe.edu.unap.traza.controlador;

import jakarta.servlet.annotation.WebServlet;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import pe.edu.unap.traza.modelo.CursoModelo;
import pe.edu.unap.traza.modelo.EvidenciaModelo;
import pe.edu.unap.traza.modelo.ReglaException;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.Evidencia;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * POST /api/evidencias/{id}/calificacion  → calificarEvidencia (RF-03, RNF-04)
 * GET  /api/evidencias/{id}/archivo       descarga el archivo entregado
 */
@WebServlet(urlPatterns = "/api/evidencias/*")
public class EvidenciaServlet extends BaseServlet {

    private final EvidenciaModelo evidencias = new EvidenciaModelo();
    private final CursoModelo cursos = new CursoModelo();

    @Override
    protected Object post(Peticion p) throws Exception {
        if (!"calificacion".equals(p.parte(1))) throw new ReglaException(404, "No existe");
        Evidencia ev = evidencias.calificarEvidencia(p.usuario(), p.numero(0), p.param("nota"));
        return json("ok", true, "evidencia", ev);
    }

    @Override
    protected Object get(Peticion p) throws Exception {
        if (!"archivo".equals(p.parte(1))) throw new ReglaException(404, "No existe");
        Usuario u = p.usuario();
        Evidencia ev = evidencias.obtener(p.numero(0));
        if (ev == null) throw new ReglaException(404, "La evidencia no existe");
        Curso c = cursos.obtener(ev.curso());
        boolean puede = ev.estudiante().equals(u.codigo()) || u.es("director") || (u.es("docente") && c.docente().equals(u.codigo()));
        if (!puede) throw new ReglaException(403, "No puedes ver este archivo"); // Ley 29733: cada uno ve solo lo suyo
        String ruta = evidencias.rutaArchivo(ev.id());
        if (ruta == null || !Files.exists(Path.of(ruta))) throw new ReglaException(404, "Esta evidencia es de los datos de prueba y no tiene archivo");

        p.resp().setContentType(ev.archivo().formato().equals("PDF") ? "application/pdf" : Files.probeContentType(Path.of(ruta)));
        p.resp().setHeader("Content-Disposition", "inline; filename*=UTF-8''" + URLEncoder.encode(ev.archivo().nombre(), StandardCharsets.UTF_8).replace("+", "%20"));
        p.resp().setContentLengthLong(Files.size(Path.of(ruta)));
        try (OutputStream out = p.resp().getOutputStream()) { Files.copy(Path.of(ruta), out); }
        return YA_RESPONDIDO;
    }
}
