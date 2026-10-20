package pe.edu.unap.traza.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.Matricula;
import pe.edu.unap.traza.modelo.Objetos.ResultadoMatricula;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/** OPERACIÓN 2: matricularEstudiante (RF-02). */
public class MatriculaModelo {

    private final UsuarioModelo usuarios = new UsuarioModelo();
    private final CursoModelo cursos = new CursoModelo();

    /**
     * Vincula al estudiante con los cursos elegidos del semestre.
     * Si ya estaba en un curso, ese curso devuelve «Ya está matriculado en este curso».
     */
    public Matricula matricularEstudiante(String codigoEstudiante, String semestre, List<Integer> cursosElegidos) throws SQLException {
        Usuario est = usuarios.buscar(codigoEstudiante);
        if (est == null || !est.es("estudiante")) throw new ReglaException("No hay un estudiante con ese código");
        if (cursosElegidos == null || cursosElegidos.isEmpty()) throw new ReglaException("Elige al menos un curso");

        List<ResultadoMatricula> resultados = new ArrayList<>();
        String sql = "INSERT INTO matricula (estudiante_codigo, curso_id) VALUES (?, ?) "
                   + "ON CONFLICT (estudiante_codigo, curso_id) DO NOTHING"; // la clave única evita el duplicado
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            cn.setAutoCommit(false);
            try {
                for (Integer id : cursosElegidos) {
                    Curso curso = cursos.obtener(id);
                    if (curso == null || !curso.semestre().equals(semestre)) throw new ReglaException("Uno de los cursos no es del semestre " + semestre);
                    ps.setString(1, est.codigo());
                    ps.setInt(2, id);
                    boolean nuevo = ps.executeUpdate() == 1;
                    resultados.add(new ResultadoMatricula(curso, nuevo ? "matriculado" : "ya",
                            nuevo ? "Matriculado" : "Ya está matriculado en este curso"));
                }
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        }
        return new Matricula(est, resultados);
    }
}
