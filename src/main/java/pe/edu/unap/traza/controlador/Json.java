package pe.edu.unap.traza.controlador;

import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;

/**
 * Convierte a JSON los objetos del modelo (records, listas, mapas, textos y números),
 * sin librerías externas.
 */
public final class Json {

    private Json() { }

    public static String de(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, valor);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object v) {
        if (v == null) sb.append("null");
        else if (v instanceof CharSequence || v instanceof Character || v instanceof TemporalAccessor || v instanceof Enum<?>) texto(sb, v.toString());
        else if (v instanceof Double d) sb.append(d.isNaN() || d.isInfinite() ? "null" : d.toString());
        else if (v instanceof Float f) sb.append(f.isNaN() || f.isInfinite() ? "null" : f.toString());
        else if (v instanceof Number || v instanceof Boolean) sb.append(v);
        else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escribir(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primero = true;
            for (Object x : c) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(sb, x);
            }
            sb.append(']');
        } else if (v.getClass().isRecord()) {
            sb.append('{');
            boolean primero = true;
            for (RecordComponent rc : v.getClass().getRecordComponents()) {
                if (!primero) sb.append(',');
                primero = false;
                texto(sb, rc.getName());
                sb.append(':');
                try {
                    rc.getAccessor().setAccessible(true);
                    escribir(sb, rc.getAccessor().invoke(v));
                } catch (ReflectiveOperationException e) {
                    sb.append("null");
                }
            }
            sb.append('}');
        } else texto(sb, v.toString());
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c"); // evita cerrar etiquetas si el JSON termina dentro de HTML
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
