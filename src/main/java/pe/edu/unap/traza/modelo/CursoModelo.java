package pe.edu.unap.traza.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.CursoConDatos;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/** Curso: consultas de cursos y de sus matriculados. */
public class CursoModelo {

    /** Semestre en curso. */
    public static final String SEMESTRE_ACTUAL = "2026-II";

    static final String SELECT_CURSO =
            "SELECT c.id, c.codigo, c.nombre, c.semestre, c.ciclo, c.docente_codigo, u.nombre AS docente_nombre, c.estado_portafolio "
          + "FROM curso c JOIN usuario u ON u.codigo = c.docente_codigo ";

    static Curso leerCurso(ResultSet rs) throws SQLException {
        return new Curso(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"), rs.getString("semestre"),
                rs.getString("ciclo"), rs.getString("docente_codigo"), rs.getString("docente_nombre"), rs.getString("estado_portafolio"));
    }

    public Curso obtener(int id) throws SQLException {
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(SELECT_CURSO + "WHERE c.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? leerCurso(rs) : null; }
        }
    }

    /** Los cursos que le corresponden a cada rol en el semestre actual. */
    public List<Curso> cursosDe(Usuario u) throws SQLException {
        String sql;
        if (u.es("docente")) sql = SELECT_CURSO + "WHERE c.semestre = ? AND c.docente_codigo = ? ORDER BY c.codigo";
        else if (u.es("estudiante")) sql = SELECT_CURSO + "JOIN matricula m ON m.curso_id = c.id WHERE c.semestre = ? AND m.estudiante_codigo = ? ORDER BY c.codigo";
        else sql = SELECT_CURSO + "WHERE c.semestre = ? AND CAST(? AS TEXT) IS NOT NULL ORDER BY c.codigo"; // secretaría y director ven todos
        List<Curso> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, SEMESTRE_ACTUAL);
            ps.setString(2, u.codigo());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(leerCurso(rs)); }
        }
        return lista;
    }

    /** Cursos con sus números (matriculados, sesiones, siguiente sesión y evidencias por calificar). */
    public List<CursoConDatos> cursosConDatos(Usuario u) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM matricula WHERE curso_id = ?), "
                   + "(SELECT COUNT(DISTINCT sesion) FROM asistencia WHERE curso_id = ?), "
                   + "(SELECT COALESCE(MAX(sesion), 0) + 1 FROM asistencia WHERE curso_id = ?), "
                   + "(SELECT COUNT(*) FROM evidencia WHERE curso_id = ? AND calificacion IS NULL)";
        List<CursoConDatos> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (Curso c : cursosDe(u)) {
                for (int i = 1; i <= 4; i++) ps.setInt(i, c.id());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    lista.add(new CursoConDatos(c.id(), c.codigo(), c.nombre(), c.semestre(), c.ciclo(), c.docente(),
                            c.docenteNombre(), c.portafolio(), rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        }
        return lista;
    }

    /** Cursos del semestre para la página de inicio (sin iniciar sesión). */
    public List<CursoConDatos> cursosPublicos() throws SQLException {
        return cursosConDatos(new Usuario("000000", "", "director"));
    }

    /**
     * Agregada para cerrar el hueco de la Práctica 4 (paso 2 del caso de uso 1):
     * lista los estudiantes matriculados en un curso, por orden alfabético.
     */
    public List<Usuario> listarMatriculados(int cursoId) throws SQLException {
        String sql = "SELECT u.codigo, u.nombre, u.rol FROM matricula m JOIN usuario u ON u.codigo = m.estudiante_codigo "
                   + "WHERE m.curso_id = ? ORDER BY u.nombre";
        List<Usuario> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Usuario(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }
        return lista;
    }

    public boolean estaMatriculado(String estudiante, int cursoId) throws SQLException {
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM matricula WHERE estudiante_codigo = ? AND curso_id = ?")) {
            ps.setString(1, estudiante);
            ps.setInt(2, cursoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
