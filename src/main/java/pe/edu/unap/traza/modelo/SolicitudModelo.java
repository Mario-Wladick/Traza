package pe.edu.unap.traza.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import pe.edu.unap.traza.modelo.Objetos.Solicitud;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * Solicitudes de cuenta. Responde a la pregunta abierta «¿quién crea las cuentas?»:
 * la persona la pide y la secretaría académica la aprueba o la rechaza.
 */
public class SolicitudModelo {

    private static final Set<String> ROLES = Set.of("estudiante", "docente");
    private final UsuarioModelo usuarios = new UsuarioModelo();

    /** Guarda una solicitud nueva con la contraseña ya cifrada. */
    public Solicitud solicitarCuenta(String codigo, String nombre, String rol, String clave) throws SQLException {
        codigo = codigo == null ? "" : codigo.trim();
        nombre = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (nombre.length() < 5 || !nombre.contains(" ")) throw new ReglaException("Escribe tu nombre completo");
        if (!codigo.matches("[0-9]{6}")) throw new ReglaException("El código universitario tiene 6 dígitos");
        if (rol == null || !ROLES.contains(rol)) throw new ReglaException("Elige si eres estudiante o docente");
        if (clave == null || clave.length() < 6) throw new ReglaException("La contraseña debe tener al menos 6 caracteres");
        if (usuarios.buscar(codigo) != null) throw new ReglaException("Ya existe una cuenta con ese código. Ingresa con tu contraseña");

        String sql = "INSERT INTO solicitud_cuenta (codigo, nombre, rol, clave_cifrada) VALUES (?, ?, ?, ?) RETURNING id, creada_en";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setString(2, nombre);
            ps.setString(3, rol);
            ps.setString(4, Clave.cifrar(clave));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Solicitud(rs.getInt(1), codigo, nombre, rol, "pendiente", rs.getTimestamp(2).toLocalDateTime().toString());
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // índice único: ya hay una pendiente con ese código
                throw new ReglaException("Ya enviaste una solicitud con ese código. Espera a que la secretaría la revise");
            }
            throw e;
        }
    }

    /** Solicitudes que esperan respuesta, de la más antigua a la más nueva. */
    public List<Solicitud> listarPendientes() throws SQLException {
        String sql = "SELECT id, codigo, nombre, rol, estado, creada_en FROM solicitud_cuenta WHERE estado = 'pendiente' ORDER BY creada_en";
        List<Solicitud> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Solicitud(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getTimestamp(6).toLocalDateTime().toString()));
        }
        return lista;
    }

    public int contarPendientes() throws SQLException {
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM solicitud_cuenta WHERE estado = 'pendiente'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Aprueba: crea el usuario con la contraseña que eligió y marca la solicitud. Todo o nada. */
    public Usuario aprobar(Usuario secretaria, int id) throws SQLException {
        if (!secretaria.es("secretaria")) throw new ReglaException(403, "Solo la secretaría académica aprueba cuentas");
        try (Connection cn = Conexion.obtener()) {
            cn.setAutoCommit(false);
            try {
                String codigo, nombre, rol, clave;
                try (PreparedStatement ps = cn.prepareStatement(
                        "SELECT codigo, nombre, rol, clave_cifrada FROM solicitud_cuenta WHERE id = ? AND estado = 'pendiente' FOR UPDATE")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ReglaException(404, "Esa solicitud ya fue atendida o no existe");
                        codigo = rs.getString(1); nombre = rs.getString(2); rol = rs.getString(3); clave = rs.getString(4);
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO usuario (codigo, nombre, rol, clave_cifrada) VALUES (?, ?, ?, ?) ON CONFLICT (codigo) DO NOTHING")) {
                    ps.setString(1, codigo); ps.setString(2, nombre); ps.setString(3, rol); ps.setString(4, clave);
                    if (ps.executeUpdate() == 0) throw new ReglaException("Ya existe una cuenta con el código " + codigo);
                }
                marcar(cn, id, "aprobada", secretaria);
                cn.commit();
                return new Usuario(codigo, nombre, rol);
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    /** Rechaza la solicitud. No crea ningún usuario. */
    public void rechazar(Usuario secretaria, int id) throws SQLException {
        if (!secretaria.es("secretaria")) throw new ReglaException(403, "Solo la secretaría académica atiende solicitudes");
        try (Connection cn = Conexion.obtener()) {
            if (marcar(cn, id, "rechazada", secretaria) == 0) throw new ReglaException(404, "Esa solicitud ya fue atendida o no existe");
        }
    }

    private int marcar(Connection cn, int id, String estado, Usuario secretaria) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE solicitud_cuenta SET estado = ?, atendida_en = now(), atendida_por = ? WHERE id = ? AND estado = 'pendiente'")) {
            ps.setString(1, estado);
            ps.setString(2, secretaria.codigo());
            ps.setInt(3, id);
            return ps.executeUpdate();
        }
    }
}
