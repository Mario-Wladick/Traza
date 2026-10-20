package pe.edu.unap.traza.modelo;

import java.util.List;
import java.util.Map;

/**
 * Los objetos del modelo de contenido (Práctica 3), como records de Java.
 * Los nombres de los campos son los mismos que lee la vista en JavaScript.
 */
public final class Objetos {

    private Objetos() { }

    /** 1 · Usuario. Nunca lleva la contraseña. */
    public record Usuario(String codigo, String nombre, String rol) implements java.io.Serializable {
        public boolean es(String r) { return rol.equals(r); }
    }

    /** 2 · Curso. «portafolio» es el estado del portafolio del curso. */
    public record Curso(int id, String codigo, String nombre, String semestre, String ciclo,
                        String docente, String docenteNombre, String portafolio) { }

    /** Curso con sus números, para la página «Mis cursos». */
    public record CursoConDatos(int id, String codigo, String nombre, String semestre, String ciclo,
                                String docente, String docenteNombre, String portafolio,
                                int matriculados, int sesiones, int siguiente, int porCalificar) { }

    /** 3 · Conteo de una sesión de asistencia. */
    public record Conteo(int presente, int tardanza, int falta) {
        public String resumen() {
            return presente + (presente == 1 ? " presente, " : " presentes, ")
                 + tardanza + (tardanza == 1 ? " tardanza, " : " tardanzas, ")
                 + falta + (falta == 1 ? " falta" : " faltas");
        }
    }

    public record Sesion(int sesion, String fecha, boolean registrada) { }

    public record SesionConConteo(int sesion, String fecha, Conteo conteo) { }

    /** 4 · Evidencia. calificacion es null hasta que el docente califica. */
    public record Archivo(String nombre, double tamanoMB, String formato) { }

    public record Evidencia(int id, String estudiante, String nombreEstudiante, int curso, String unidad,
                            String tipo, Archivo archivo, String fecha, Integer calificacion, boolean tieneArchivo) { }

    /** 5 · Portafolio del estudiante en un curso. */
    public record ConteoAsistencia(int presente, int tardanza, int falta) { }

    public record PortafolioCursoEstudiante(Curso curso, ConteoAsistencia asistencia, int sesiones,
                                            List<Evidencia> evidencias, Double promedio, int porCalificar, int avance) { }

    public record PortafolioEstudiante(String tipo, Usuario estudiante, String semestre,
                                       List<PortafolioCursoEstudiante> cursos, int avance, int asistenciaPct,
                                       int evidenciasEntregadas, Double promedio) { }

    /** 5 · Portafolio del curso, fila por estudiante. */
    public record FilaEstudiante(Usuario estudiante, int faltas, int asistenciaPct, int entregadas,
                                 Double promedio, int avance) { }

    public record PortafolioCurso(String tipo, Curso curso, String docente, String semestre, int matriculados,
                                  List<SesionConConteo> sesiones, List<Evidencia> evidencias,
                                  List<FilaEstudiante> estudiantes, int asistenciaPct, int calificadas,
                                  int porCalificar, Double promedio, int avance) { }

    /** Resultado de matricular en un curso. estado: «matriculado» o «ya». */
    public record ResultadoMatricula(Curso curso, String estado, String mensaje) { }

    public record Matricula(Usuario estudiante, List<ResultadoMatricula> resultados) { }

    /** Estados marcados en una sesión: código del estudiante → presente | tardanza | falta. */
    public record EstadosSesion(Map<String, String> estados) { }
}
