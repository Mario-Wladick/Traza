package pe.edu.unap.traza.controlador;

import jakarta.servlet.annotation.WebServlet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.unap.traza.modelo.AsistenciaModelo;
import pe.edu.unap.traza.modelo.CursoModelo;
import pe.edu.unap.traza.modelo.EvidenciaModelo;
import pe.edu.unap.traza.modelo.PortafolioModelo;
import pe.edu.unap.traza.modelo.ReglaException;
import pe.edu.unap.traza.modelo.Objetos.Conteo;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.Evidencia;
import pe.edu.unap.traza.modelo.Objetos.Sesion;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * Rutas de los cursos (Práctica 4):
 *   GET  /api/cursos                                  Mis cursos (o todos, para secretaría y director)
 *   GET  /api/cursos/publicos                         cursos para la página de inicio
 *   GET  /api/cursos/{id}/estudiantes                 Lista del curso
 *   GET  /api/cursos/{id}/asistencia/{sesion}         Registro de asistencia
 *   POST /api/cursos/{id}/asistencia/{sesion}         → registrarAsistenciaSesion (RF-01)
 *   GET  /api/cursos/{id}/asistencia/{sesion}/resumen Resumen de la sesión
 *   GET  /api/cursos/{id}/evidencias                  Evidencias del curso
 *   GET  /api/cursos/{id}/portafolio                  → armarPortafolio del curso (RF-05)
 * El controlador no cuenta ni valida reglas: eso lo hace el modelo.
 */
@WebServlet(urlPatterns = {"/api/cursos", "/api/cursos/*"})
public class CursoServlet extends BaseServlet {

    private final CursoModelo cursos = new CursoModelo();
    private final AsistenciaModelo asistencias = new AsistenciaModelo();
    private final EvidenciaModelo evidencias = new EvidenciaModelo();
    private final PortafolioModelo portafolios = new PortafolioModelo();

    /** Revisa que el usuario pueda ver este curso: su docente, o los roles extra indicados. */
    private Curso permitir(Peticion p, int id, String... rolesExtra) throws Exception {
        Usuario u = p.usuario();
        Curso c = cursos.obtener(id);
        if (c == null) throw new ReglaException(404, "El curso no existe");
        boolean esSuDocente = u.es("docente") && c.docente().equals(u.codigo());
        boolean rolPermitido = java.util.Arrays.stream(rolesExtra).anyMatch(u::es);
        if (!esSuDocente && !rolPermitido) throw new ReglaException(403, "Esta página es solo para el docente del curso");
        return c;
    }

    @Override
    protected Object get(Peticion p) throws Exception {
        if (p.partes().isEmpty()) return json("ok", true, "cursos", cursos.cursosConDatos(p.usuario()));
        if (p.parte(0).equals("publicos")) return json("ok", true, "cursos", cursos.cursosPublicos());

        int id = p.numero(0);
        String recurso = p.parte(1) == null ? "" : p.parte(1);
        switch (recurso) {
            case "estudiantes" -> {
                Curso c = permitir(p, id, "secretaria", "director");
                return json("ok", true, "curso", c, "estudiantes", cursos.listarMatriculados(id), "selector", selector(p.usuario()));
            }
            case "asistencia" -> {
                Curso c = permitir(p, id);
                int siguiente = asistencias.siguienteSesion(id);
                if (p.parte(2) == null) return json("ok", true, "siguiente", siguiente);
                int sesion = p.numero(2);
                if (sesion < 1 || sesion > siguiente) throw new ReglaException(404, "La sesión no existe").con("siguiente", siguiente);
                if ("resumen".equals(p.parte(3))) {
                    Conteo conteo = asistencias.resumenSesion(id, sesion);
                    if (conteo == null) throw new ReglaException(404, "La sesión todavía no tiene asistencia");
                    return json("ok", true, "curso", c, "sesion", sesion, "conteo", conteo, "resumen", conteo.resumen());
                }
                List<Sesion> sesiones = new ArrayList<>(asistencias.sesionesRegistradas(id));
                sesiones.add(new Sesion(siguiente, LocalDate.now().toString(), false)); // la sesión de hoy
                Map<String, String> estados = asistencias.estadosDeSesion(id, sesion);
                return json("ok", true, "curso", c, "sesion", sesion, "siguiente", siguiente, "sesiones", sesiones,
                        "estudiantes", cursos.listarMatriculados(id), "estados", estados, "yaRegistrada", !estados.isEmpty());
            }
            case "evidencias" -> {
                Curso c = permitir(p, id);
                List<Evidencia> todas = evidencias.deCurso(id);
                return json("ok", true, "curso", c, "porCalificar", evidencias.listarPorCalificar(id),
                        "calificadas", todas.stream().filter(e -> e.calificacion() != null).toList());
            }
            case "portafolio" -> {
                permitir(p, id, "director");
                return json("ok", true, "portafolio", portafolios.armarPortafolioCurso(id), "selector", selector(p.usuario()));
            }
            default -> throw new ReglaException(404, "No existe");
        }
    }

    @Override
    protected Object post(Peticion p) throws Exception {
        // POST /api/cursos/{id}/asistencia/{sesion} con parámetros  e.<codigo>=presente|tardanza|falta
        if (!"asistencia".equals(p.parte(1)) || p.parte(2) == null) throw new ReglaException(404, "No existe");
        Usuario docente = p.usuario();
        int id = p.numero(0), sesion = p.numero(2);
        Map<String, String> estados = new LinkedHashMap<>();
        p.req().getParameterMap().forEach((k, v) -> { if (k.startsWith("e.") && v.length > 0) estados.put(k.substring(2), v[0]); });
        Conteo conteo = asistencias.registrarAsistenciaSesion(docente, id, sesion, estados);
        return json("ok", true, "conteo", conteo, "resumen", conteo.resumen(), "destino", "#/cursos/" + id + "/asistencia/" + sesion + "/resumen");
    }

    /** Cursos para el selector de curso: el director y la secretaría ven todos; el docente, los suyos. */
    private List<Curso> selector(Usuario u) throws Exception {
        return cursos.cursosDe(u);
    }
}
