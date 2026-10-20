package pe.edu.unap.traza.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Una regla del negocio no se cumplió (por ejemplo «Ya está matriculado en este curso»).
 * El controlador la convierte en una respuesta con el mensaje para la vista.
 */
public class ReglaException extends RuntimeException {

    private final int estadoHttp;
    private final Map<String, Object> datos = new LinkedHashMap<>();

    public ReglaException(String mensaje) { this(400, mensaje); }

    public ReglaException(int estadoHttp, String mensaje) {
        super(mensaje);
        this.estadoHttp = estadoHttp;
    }

    public ReglaException con(String clave, Object valor) {
        datos.put(clave, valor);
        return this;
    }

    public int estadoHttp() { return estadoHttp; }

    public Map<String, Object> datos() { return datos; }
}
