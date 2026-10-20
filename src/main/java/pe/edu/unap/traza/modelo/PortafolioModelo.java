package pe.edu.unap.traza.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unap.traza.modelo.Objetos.ConteoAsistencia;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.Evidencia;
import pe.edu.unap.traza.modelo.Objetos.FilaEstudiante;
import pe.edu.unap.traza.modelo.Objetos.PortafolioCurso;
import pe.edu.unap.traza.modelo.Objetos.PortafolioCursoEstudiante;
import pe.edu.unap.traza.modelo.Objetos.PortafolioEstudiante;
import pe.edu.unap.traza.modelo.Objetos.SesionConConteo;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * Portafolio · OPERACIÓN 6: armarPortafolio (RF-05).
 * No cambia datos: reúne lo registrado en las operaciones 3, 4 y 5
 * usando la vista v_portafolio_estudiante_curso.
 */
public class PortafolioModelo {

    private final CursoModelo cursos = new CursoModelo();
    private final AsistenciaModelo asistencias = new AsistenciaModelo();
    private final EvidenciaModelo evidencias = new EvidenciaModelo();

    private record Fila(int presentes, int tardanzas, int faltas, int sesiones, int entregadas, int porCalificar, Double promedio, int avance) { }

    private Fila fila(Connection cn, String estudiante, int cursoId) throws SQLException {
        String sql = "SELECT presentes, tardanzas, faltas, sesiones, entregadas, por_calificar, promedio, avance "
                   + "FROM v_portafolio_estudiante_curso WHERE estudiante_codigo = ? AND curso_id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estudiante);
            ps.setInt(2, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new Fila(0, 0, 0, 0, 0, 0, null, 0);
                double p = rs.getDouble(7);
                return new Fila(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.wasNull() ? null : p, rs.getInt(8));
            }
        }
    }

    private static int porcentaje(int parte, int total) { return total == 0 ? 0 : Math.round(parte * 100f / total); }

    private static Double promedio(List<Integer> notas) {
        List<Integer> validas = notas.stream().filter(n -> n != null).toList();
        if (validas.isEmpty()) return null;
        return Math.round(validas.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;
    }

    /** Portafolio del estudiante: todos sus cursos del semestre. */
    public PortafolioEstudiante armarPortafolioEstudiante(Usuario estudiante) throws SQLException {
        List<PortafolioCursoEstudiante> lista = new ArrayList<>();
        List<Integer> todasLasNotas = new ArrayList<>();
        int sesiones = 0, asistidas = 0, entregadas = 0, sumaAvance = 0;
        try (Connection cn = Conexion.obtener()) {
            for (Curso c : cursos.cursosDe(estudiante)) {
                Fila f = fila(cn, estudiante.codigo(), c.id());
                List<Evidencia> evs = evidencias.deEstudianteEnCurso(estudiante.codigo(), c.id());
                evs.forEach(e -> todasLasNotas.add(e.calificacion()));
                lista.add(new PortafolioCursoEstudiante(c, new ConteoAsistencia(f.presentes(), f.tardanzas(), f.faltas()),
                        f.sesiones(), evs, f.promedio(), f.porCalificar(), f.avance()));
                sesiones += f.sesiones();
                asistidas += f.presentes() + f.tardanzas();
                entregadas += f.entregadas();
                sumaAvance += f.avance();
            }
        }
        int avance = lista.isEmpty() ? 0 : Math.round((float) sumaAvance / lista.size());
        return new PortafolioEstudiante("estudiante", estudiante, CursoModelo.SEMESTRE_ACTUAL, lista, avance,
                porcentaje(asistidas, sesiones), entregadas, promedio(todasLasNotas));
    }

    /** Portafolio del curso: asistencias, evidencias, calificaciones y avance de sus estudiantes. */
    public PortafolioCurso armarPortafolioCurso(int cursoId) throws SQLException {
        Curso curso = cursos.obtener(cursoId);
        if (curso == null) throw new ReglaException(404, "El curso no existe");
        List<Usuario> matriculados = cursos.listarMatriculados(cursoId);
        List<SesionConConteo> sesiones = asistencias.sesionesConConteo(cursoId);
        List<Evidencia> evs = evidencias.deCurso(cursoId);

        List<FilaEstudiante> filas = new ArrayList<>();
        int sumaAvance = 0;
        try (Connection cn = Conexion.obtener()) {
            for (Usuario e : matriculados) {
                Fila f = fila(cn, e.codigo(), cursoId);
                filas.add(new FilaEstudiante(e, f.faltas(), porcentaje(f.presentes() + f.tardanzas(), f.sesiones()),
                        f.entregadas(), f.promedio(), f.avance()));
                sumaAvance += f.avance();
            }
        }
        int registros = 0, asistidos = 0;
        for (SesionConConteo s : sesiones) {
            registros += s.conteo().presente() + s.conteo().tardanza() + s.conteo().falta();
            asistidos += s.conteo().presente() + s.conteo().tardanza();
        }
        int calificadas = (int) evs.stream().filter(x -> x.calificacion() != null).count();
        return new PortafolioCurso("curso", curso, curso.docenteNombre(), curso.semestre(), matriculados.size(), sesiones, evs, filas,
                porcentaje(asistidos, registros), calificadas, evs.size() - calificadas,
                promedio(evs.stream().map(Evidencia::calificacion).toList()),
                filas.isEmpty() ? 0 : Math.round((float) sumaAvance / filas.size()));
    }
}
