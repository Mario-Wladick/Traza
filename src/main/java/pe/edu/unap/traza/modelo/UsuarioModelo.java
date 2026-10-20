package pe.edu.unap.traza.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/** Usuario · OPERACIÓN 1: iniciarSesion (RF-06). */
public class UsuarioModelo {

    public static final String MENSAJE_INGRESO = "Usuario o contraseña incorrectos";

    /** Comprueba código y contraseña. Devuelve el usuario o lanza «Usuario o contraseña incorrectos». */
    public Usuario iniciarSesion(String codigo, String clave) throws SQLException {
        String sql = "SELECT codigo, nombre, rol, clave_cifrada FROM usuario WHERE codigo = ?";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo == null ? "" : codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !Clave.comprobar(clave, rs.getString("clave_cifrada"))) {
                    throw new ReglaException(401, MENSAJE_INGRESO); // mismo mensaje si falla el código o la clave
                }
                return new Usuario(rs.getString("codigo"), rs.getString("nombre"), rs.getString("rol"));
            }
        }
    }

    /** Busca un usuario por su código (sin contraseña). Devuelve null si no existe. */
    public Usuario buscar(String codigo) throws SQLException {
        String sql = "SELECT codigo, nombre, rol FROM usuario WHERE codigo = ?";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo == null ? "" : codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Usuario(rs.getString(1), rs.getString(2), rs.getString(3)) : null;
            }
        }
    }
}
