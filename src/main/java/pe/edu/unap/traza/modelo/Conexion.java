package pe.edu.unap.traza.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones a PostgreSQL con los datos de traza.properties
 * (o de las variables de entorno TRAZA_DB_URL, TRAZA_DB_USUARIO, TRAZA_DB_CLAVE).
 */
public final class Conexion {

    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream in = Conexion.class.getClassLoader().getResourceAsStream("traza.properties")) {
            if (in != null) CONFIG.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            Class.forName("org.postgresql.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError("No se pudo cargar la configuración de TRAZA: " + e.getMessage());
        }
    }

    private Conexion() { }

    private static String valor(String variableEntorno, String clave, String porDefecto) {
        String env = System.getenv(variableEntorno);
        if (env != null && !env.isBlank()) return env;
        String prop = CONFIG.getProperty(clave);
        return (prop == null || prop.isBlank()) ? porDefecto : prop.trim();
    }

    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(
                valor("TRAZA_DB_URL", "db.url", "jdbc:postgresql://localhost:5432/traza"),
                valor("TRAZA_DB_USUARIO", "db.usuario", "traza_app"),
                valor("TRAZA_DB_CLAVE", "db.clave", "traza2026"));
    }

    /** Carpeta donde se guardan los archivos de las evidencias. */
    public static Path carpetaEvidencias() {
        String carpeta = valor("TRAZA_EVIDENCIAS", "evidencias.carpeta", "");
        return carpeta.isEmpty() ? Path.of(System.getProperty("user.home"), "traza-evidencias") : Path.of(carpeta);
    }
}
