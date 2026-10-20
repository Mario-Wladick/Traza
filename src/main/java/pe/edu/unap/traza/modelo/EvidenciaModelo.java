package pe.edu.unap.traza.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import pe.edu.unap.traza.modelo.Objetos.Archivo;
import pe.edu.unap.traza.modelo.Objetos.Curso;
import pe.edu.unap.traza.modelo.Objetos.Evidencia;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/** Evidencia · OPERACIÓN 4: subirEvidencia (RF-04) y OPERACIÓN 5: calificarEvidencia (RF-03, RNF-04). */
public class EvidenciaModelo {

    public static final int MAX_MB = 10;
    public static final Set<String> UNIDADES = Set.of("I", "II", "III");
    public static final Set<String> TIPOS = Set.of("trabajo", "práctica", "proyecto", "examen");
    private static final Set<String> EXT_IMAGEN = Set.of("jpg", "jpeg", "png", "webp", "heic");

    private final CursoModelo cursos = new CursoModelo();

    static final String SELECT_EVIDENCIA =
            "SELECT e.id, e.estudiante_codigo, u.nombre, e.curso_id, e.unidad, e.tipo, e.archivo_nombre, e.tamano_mb, "
          + "e.archivo_formato, e.fecha_entrega, e.calificacion, (e.archivo_ruta IS NOT NULL) AS tiene_archivo "
          + "FROM evidencia e JOIN usuario u ON u.codigo = e.estudiante_codigo ";

    static Evidencia leer(ResultSet rs) throws SQLException {
        int nota = rs.getInt("calificacion");
        Integer calificacion = rs.wasNull() ? null : nota;
        return new Evidencia(rs.getInt("id"), rs.getString("estudiante_codigo"), rs.getString("nombre"), rs.getInt("curso_id"),
                rs.getString("unidad"), rs.getString("tipo"),
                new Archivo(rs.getString("archivo_nombre"), rs.getBigDecimal("tamano_mb").doubleValue(), rs.getString("archivo_formato")),
                rs.getDate("fecha_entrega").toString(), calificacion, rs.getBoolean("tiene_archivo"));
    }

    // =========================================================
    // OPERACIÓN 4 · Subir evidencia
    // =========================================================
    /**
     * Valida tipo y tamaño del archivo (PDF o imagen, hasta 10 MB), guarda el archivo,
     * crea la Evidencia «por calificar» y así actualiza el avance del portafolio.
     * Si ya entregó ese tipo en esa unidad y no confirmó, pide confirmación (flujo 3b).
     */
    public Evidencia subirEvidencia(Usuario estudiante, int cursoId, String unidad, String tipo,
                                    String nombreArchivo, String tipoMime, long tamanoBytes, InputStream contenido,
                                    boolean reemplazar) throws SQLException, IOException {
        Curso curso = cursos.obtener(cursoId);
        if (curso == null || !cursos.estaMatriculado(estudiante.codigo(), cursoId)) throw new ReglaException("No estás matriculado en ese curso");
        if (!curso.portafolio().equals("abierto")) throw new ReglaException("El portafolio del curso está cerrado"); // flujo 4a
        if (unidad == null || tipo == null || !UNIDADES.contains(unidad) || !TIPOS.contains(tipo)) throw new ReglaException("Elige la unidad y el tipo de evidencia");
        if (nombreArchivo == null || nombreArchivo.isBlank() || contenido == null) throw new ReglaException("Adjunta un archivo");

        String ext = nombreArchivo.contains(".") ? nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String mime = tipoMime == null ? "" : tipoMime.toLowerCase(Locale.ROOT);
        boolean esPdf = mime.equals("application/pdf") || ext.equals("pdf");
        boolean esImagen = mime.startsWith("image/") || EXT_IMAGEN.contains(ext);
        if (!esPdf && !esImagen) throw new ReglaException("El archivo debe ser PDF o imagen"); // flujo 3a
        double mb = tamanoBytes / (1024.0 * 1024.0);
        if (mb > MAX_MB) throw new ReglaException(String.format(Locale.ROOT, "El archivo pesa %.1f MB y el máximo es %d MB", mb, MAX_MB));

        Integer previa = idPrevia(estudiante.codigo(), cursoId, unidad, tipo);
        if (previa != null && !reemplazar) {
            throw new ReglaException(409, "Ya entregaste un " + tipo + " de la unidad " + unidad + ". ¿Quieres reemplazarlo?")
                    .con("requiereConfirmacion", true);
        }

        // Guardar el archivo en la carpeta de evidencias: <carpeta>/<curso>/<estudiante>/<uuid>.<ext>
        Path carpeta = Conexion.carpetaEvidencias().resolve(String.valueOf(cursoId)).resolve(estudiante.codigo());
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext));
        Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);

        String rutaAnterior = null;
        int nuevoId;
        try (Connection cn = Conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                if (previa != null) {
                    try (PreparedStatement ps = cn.prepareStatement("DELETE FROM evidencia WHERE id = ? RETURNING archivo_ruta")) {
                        ps.setInt(1, previa);
                        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) rutaAnterior = rs.getString(1); }
                    }
                }
                String sql = "INSERT INTO evidencia (estudiante_codigo, curso_id, unidad, tipo, archivo_nombre, archivo_ruta, archivo_formato, tamano_mb) "
                           + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, estudiante.codigo());
                    ps.setInt(2, cursoId);
                    ps.setString(3, unidad);
                    ps.setString(4, tipo);
                    ps.setString(5, Path.of(nombreArchivo).getFileName().toString());
                    ps.setString(6, destino.toString());
                    ps.setString(7, esPdf ? "PDF" : "Imagen");
                    ps.setBigDecimal(8, BigDecimal.valueOf(mb).setScale(1, RoundingMode.HALF_UP));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); nuevoId = rs.getInt(1); }
                }
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                Files.deleteIfExists(destino); // no deja archivos sueltos si falló
                throw e;
            }
        }
        if (rutaAnterior != null) Files.deleteIfExists(Path.of(rutaAnterior));
        return obtener(nuevoId);
    }

    private Integer idPrevia(String estudiante, int cursoId, String unidad, String tipo) throws SQLException {
        String sql = "SELECT id FROM evidencia WHERE estudiante_codigo = ? AND curso_id = ? AND unidad = ? AND tipo = ?";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estudiante);
            ps.setInt(2, cursoId);
            ps.setString(3, unidad);
            ps.setString(4, tipo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    // =========================================================
    // OPERACIÓN 5 · Calificar evidencia
    // =========================================================
    /** Guarda la calificación de 0 a 20. La rechaza si quien la envía no es el docente del curso (RNF-04). */
    public Evidencia calificarEvidencia(Usuario usuario, int evidenciaId, String nota) throws SQLException {
        Evidencia ev = obtener(evidenciaId);
        if (ev == null) throw new ReglaException(404, "La evidencia no existe");
        Curso curso = cursos.obtener(ev.curso());
        if (!usuario.es("docente") || !curso.docente().equals(usuario.codigo())) {
            throw new ReglaException(403, "Solo el docente del curso puede calificar");
        }
        int valor;
        try { valor = Integer.parseInt(nota == null ? "" : nota.trim()); }
        catch (NumberFormatException e) { throw new ReglaException("La calificación es un número entero de 0 a 20"); }
        if (valor < 0 || valor > 20) throw new ReglaException("La calificación es un número entero de 0 a 20");

        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement("UPDATE evidencia SET calificacion = ?, calificado_en = now() WHERE id = ?")) {
            ps.setInt(1, valor);
            ps.setInt(2, evidenciaId);
            ps.executeUpdate();
        }
        return obtener(evidenciaId);
    }

    // ---------- Consultas ----------
    public Evidencia obtener(int id) throws SQLException {
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(SELECT_EVIDENCIA + "WHERE e.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? leer(rs) : null; }
        }
    }

    public String rutaArchivo(int id) throws SQLException {
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement("SELECT archivo_ruta FROM evidencia WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    /** Todas las evidencias de un curso, por unidad y por estudiante. */
    public List<Evidencia> deCurso(int cursoId) throws SQLException {
        return consultar(SELECT_EVIDENCIA + "WHERE e.curso_id = ? ORDER BY e.unidad, u.nombre, e.tipo", cursoId, null);
    }

    /** Agregada para cerrar el hueco de la Práctica 4: evidencias por calificar de un curso. */
    public List<Evidencia> listarPorCalificar(int cursoId) throws SQLException {
        return consultar(SELECT_EVIDENCIA + "WHERE e.curso_id = ? AND e.calificacion IS NULL ORDER BY e.unidad, u.nombre, e.tipo", cursoId, null);
    }

    public List<Evidencia> deEstudianteEnCurso(String estudiante, int cursoId) throws SQLException {
        return consultar(SELECT_EVIDENCIA + "WHERE e.curso_id = ? AND e.estudiante_codigo = ? ORDER BY e.fecha_entrega DESC, e.id DESC", cursoId, estudiante);
    }

    private List<Evidencia> consultar(String sql, int cursoId, String estudiante) throws SQLException {
        List<Evidencia> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            if (estudiante != null) ps.setString(2, estudiante);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(leer(rs)); }
        }
        return lista;
    }
}
