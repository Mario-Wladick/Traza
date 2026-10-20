package pe.edu.unap.traza.modelo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pe.edu.unap.traza.modelo.Objetos.Conteo;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.Sesion;
import pe.edu.unap.traza.modelo.Objetos.SesionConConteo;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/** Asistencia · OPERACIÓN 3: registrarAsistenciaSesion (RF-01). */
public class AsistenciaModelo {

    private static final Set<String> ESTADOS = Set.of("presente", "tardanza", "falta");
    private final CursoModelo cursos = new CursoModelo();

    /** Sesiones ya registradas del curso, de la primera a la última. */
    public List<Sesion> sesionesRegistradas(int cursoId) throws SQLException {
        String sql = "SELECT sesion, MIN(fecha_sesion) FROM asistencia WHERE curso_id = ? GROUP BY sesion ORDER BY sesion";
        List<Sesion> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Sesion(rs.getInt(1), rs.getDate(2).toString(), true));
            }
        }
        return lista;
    }

    public int siguienteSesion(int cursoId) throws SQLException {
        List<Sesion> s = sesionesRegistradas(cursoId);
        return s.isEmpty() ? 1 : s.get(s.size() - 1).sesion() + 1;
    }

    /** Lo marcado en una sesión (vacío si todavía no se registra). */
    public Map<String, String> estadosDeSesion(int cursoId, int sesion) throws SQLException {
        String sql = "SELECT estudiante_codigo, estado FROM asistencia WHERE curso_id = ? AND sesion = ?";
        Map<String, String> mapa = new LinkedHashMap<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            ps.setInt(2, sesion);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) mapa.put(rs.getString(1), rs.getString(2)); }
        }
        return mapa;
    }

    /**
     * Guarda un registro por estudiante, TODO O NADA (una transacción).
     * Si la sesión ya existía, la reemplaza. Devuelve el conteo:
     * «36 presentes, 2 tardanzas, 2 faltas».
     */
    public Conteo registrarAsistenciaSesion(Usuario docente, int cursoId, int sesion, Map<String, String> estados) throws SQLException {
        Curso curso = cursos.obtener(cursoId);
        if (curso == null) throw new ReglaException(404, "El curso no existe");
        if (!docente.es("docente") || !curso.docente().equals(docente.codigo())) throw new ReglaException(403, "Este curso no es suyo");
        List<Usuario> lista = cursos.listarMatriculados(cursoId);
        if (lista.isEmpty()) throw new ReglaException("El curso no tiene estudiantes");
        int siguiente = siguienteSesion(cursoId);
        if (sesion < 1 || sesion > siguiente) throw new ReglaException("La sesión " + sesion + " no existe");

        List<String> sinMarcar = new ArrayList<>();
        for (Usuario e : lista) {
            String marcado = estados.get(e.codigo());
            if (marcado == null || !ESTADOS.contains(marcado)) sinMarcar.add(e.codigo());
        }
        if (!sinMarcar.isEmpty()) {
            throw new ReglaException("Falta marcar a " + sinMarcar.size() + (sinMarcar.size() == 1 ? " estudiante" : " estudiantes"))
                    .con("sinMarcar", sinMarcar);
        }

        LocalDate fecha = LocalDate.now();
        for (Sesion s : sesionesRegistradas(cursoId)) if (s.sesion() == sesion) fecha = LocalDate.parse(s.fecha());

        int presente = 0, tardanza = 0, falta = 0;
        try (Connection cn = Conexion.obtener()) {
            cn.setAutoCommit(false);
            try (PreparedStatement borrar = cn.prepareStatement("DELETE FROM asistencia WHERE curso_id = ? AND sesion = ?");
                 PreparedStatement insertar = cn.prepareStatement(
                         "INSERT INTO asistencia (curso_id, sesion, estudiante_codigo, fecha_sesion, estado) VALUES (?, ?, ?, ?, ?)")) {
                borrar.setInt(1, cursoId);
                borrar.setInt(2, sesion);
                borrar.executeUpdate();
                for (Usuario e : lista) {
                    String estado = estados.get(e.codigo());
                    insertar.setInt(1, cursoId);
                    insertar.setInt(2, sesion);
                    insertar.setString(3, e.codigo());
                    insertar.setDate(4, Date.valueOf(fecha));
                    insertar.setString(5, estado);
                    insertar.addBatch();
                    switch (estado) {
                        case "presente" -> presente++;
                        case "tardanza" -> tardanza++;
                        default -> falta++;
                    }
                }
                insertar.executeBatch();
                cn.commit();
            } catch (SQLException e) {
                cn.rollback(); // flujo 4a: no se guardan registros a medias
                throw e;
            }
        }
        return new Conteo(presente, tardanza, falta);
    }

    /** El conteo de una sesión ya guardada, o null si no existe. */
    public Conteo resumenSesion(int cursoId, int sesion) throws SQLException {
        String sql = "SELECT COUNT(*) FILTER (WHERE estado = 'presente'), COUNT(*) FILTER (WHERE estado = 'tardanza'), "
                   + "COUNT(*) FILTER (WHERE estado = 'falta'), COUNT(*) FROM asistencia WHERE curso_id = ? AND sesion = ?";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            ps.setInt(2, sesion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(4) == 0 ? null : new Conteo(rs.getInt(1), rs.getInt(2), rs.getInt(3));
            }
        }
    }

    /** Todas las sesiones del curso con su conteo (para el portafolio del curso). */
    public List<SesionConConteo> sesionesConConteo(int cursoId) throws SQLException {
        String sql = "SELECT sesion, MIN(fecha_sesion), COUNT(*) FILTER (WHERE estado = 'presente'), "
                   + "COUNT(*) FILTER (WHERE estado = 'tardanza'), COUNT(*) FILTER (WHERE estado = 'falta') "
                   + "FROM asistencia WHERE curso_id = ? GROUP BY sesion ORDER BY sesion";
        List<SesionConConteo> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new SesionConConteo(rs.getInt(1), rs.getDate(2).toString(),
                        new Conteo(rs.getInt(3), rs.getInt(4), rs.getInt(5))));
            }
        }
        return lista;
    }
}
