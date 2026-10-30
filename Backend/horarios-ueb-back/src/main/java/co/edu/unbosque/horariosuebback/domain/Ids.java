package co.edu.unbosque.horariosuebback.domain;

import java.security.SecureRandom;

/** Identificadores cortos y no adivinables con prefijo legible: ses-k3j9x0a2mq7c. */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALFABETO = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    private Ids() {
    }

    public static String nuevo(String prefijo) {
        StringBuilder sb = new StringBuilder(prefijo).append('-');
        for (int i = 0; i < 14; i++) sb.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        return sb.toString();
    }

    /** Ids aceptados cuando los envía el cliente (importación, creación desde el front). */
    public static boolean valido(String id) {
        return id != null && id.matches("^[a-z]{2,8}-[a-z0-9-]{1,31}$");
    }
}
