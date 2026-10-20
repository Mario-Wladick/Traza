package pe.edu.unap.traza.modelo;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cifra y comprueba contraseñas con PBKDF2-SHA256, que ya viene en el JDK.
 * Formato guardado: pbkdf2_sha256$iteraciones$sal$hash (sal y hash en Base64).
 */
public final class Clave {

    private static final int ITERACIONES = 120_000;
    private static final SecureRandom AZAR = new SecureRandom();

    private Clave() { }

    public static String cifrar(String clave) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        byte[] hash = derivar(clave, sal, ITERACIONES);
        return "pbkdf2_sha256$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal) + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean comprobar(String clave, String guardada) {
        if (clave == null || guardada == null) return false;
        String[] partes = guardada.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2_sha256")) return false;
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        byte[] calculado = derivar(clave, sal, Integer.parseInt(partes[1]));
        return MessageDigest.isEqual(esperado, calculado); // comparación en tiempo constante
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", e);
        }
    }
}
