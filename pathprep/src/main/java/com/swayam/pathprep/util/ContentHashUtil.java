package com.swayam.pathprep.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility for computing deterministic content hashes of questions for duplicate detection.
 * Does NOT include subtopic, difficulty, or explanations.
 */
public final class ContentHashUtil {

    private ContentHashUtil() {}

    /**
     * Computes a SHA-256 hex hash from normalised question body and option texts.
     * Options are normalised and sorted so ordering variations produce identical hashes.
     */
    public static String computeHash(String body, List<String> optionTexts) {
        if (body == null) {
            body = "";
        }
        String normBody = normalise(body);

        List<String> normOptions = (optionTexts == null ? Collections.<String>emptyList() : optionTexts).stream()
                .filter(Objects::nonNull)
                .map(ContentHashUtil::normalise)
                .sorted()
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("b:").append(normBody).append(";o:");
        for (String opt : normOptions) {
            sb.append(opt).append("|");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String normalise(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
