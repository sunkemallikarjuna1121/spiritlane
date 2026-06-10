package com.spiritlane.util;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Component
public class AppUtils {

    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return slug;
    }

    public static String generateOrderNumber() {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "SL-" + timestamp + "-" + random;
    }

    public static int calculateAge(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }

    public static boolean isAdult(LocalDate dob) {
        return calculateAge(dob) >= 18;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "XXXXXX" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        String[] parts = email.split("@");
        String name = parts[0];
        String masked = name.length() > 2
                ? name.substring(0, 2) + "*".repeat(name.length() - 2)
                : name.charAt(0) + "*";
        return masked + "@" + parts[1];
    }
}
