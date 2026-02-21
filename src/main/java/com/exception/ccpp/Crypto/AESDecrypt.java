package com.exception.ccpp.Crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESDecrypt {

    private final static byte[] CCKEY;
    static {
        String keyString = makeKey("next-webpack", 16);
        CCKEY = keyString.getBytes(StandardCharsets.UTF_8);
    }

    public static String makeKey(String e, int t) {
        String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz" +
                "0123456789";

        int n = 11259375;

        for (int i = 0; i < e.length(); i++) {
            n = (n ^ e.charAt(i)) * 1540483477;
            n = (n << 4) | (n >>> 29);
        }

        StringBuilder a = new StringBuilder();

        for (int i = 0; i < t - 1; i++) {
            int idx = Math.abs((n ^ i) % chars.length());
            a.append(chars.charAt(idx));
        }

        int half = t / 2;

        return a.substring(0, half) + "_" + a.substring(half);
    }

    public static String decrypt(String base64Data) throws Exception { return decrypt(base64Data, CCKEY); }

    public static String decrypt(String base64Data, byte[] key) throws Exception {

        byte[] buf = Base64.getDecoder().decode(base64Data);

        byte[] iv = new byte[16];
        byte[] tag = new byte[16];
        byte[] encrypted = new byte[buf.length - 32];

        System.arraycopy(buf, 0, iv, 0, 16);
        System.arraycopy(buf, buf.length - 16, tag, 0, 16);
        System.arraycopy(buf, 16, encrypted, 0, buf.length - 32);

        byte[] cipherPlusTag = new byte[encrypted.length + tag.length];
        System.arraycopy(encrypted, 0, cipherPlusTag, 0, encrypted.length);
        System.arraycopy(tag, 0, cipherPlusTag, encrypted.length, tag.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(cipherPlusTag);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // DEMO
    public static void main(String[] args) {

        try {
            String encrypted = "KlL9hz9GWcC8y0XYAbw+EhzOdxxC6L3YiPgR+6ejC8/Dglv4WbvNjYhzXKAqWqPvzp6IqHaR2ke+VwSZn2fyPw==";

            String result = decrypt(encrypted, CCKEY);

            System.out.println("Decrypted:");
            System.out.println(result.trim());

        } catch (Exception e) {
            System.out.println("Decrypt failed: " + e.getMessage());
        }
    }
}
