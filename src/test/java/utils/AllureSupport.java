package utils;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AllureSupport {

    private AllureSupport() {}

    // =========================
    //  ATTACHMENTS
    // =========================

    /**
     * Attach screenshot directly from driver (TakesScreenshot) as PNG.
     */
    public static void attachScreenshotFromDriver(String name, Object driver) {
        try {
            if (driver == null) return;
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            if (png == null || png.length == 0) return;

            Allure.addAttachment(
                    safeName(name),
                    "image/png",
                    new ByteArrayInputStream(png),
                    ".png"
            );
        } catch (Exception ignored) {
            // Non bloccare mai i test per un attachment
        }
    }

    /**
     * Attach a PNG/JPG file (or any file) to Allure.
     */
    public static void attachFile(String name, Path file, String mimeType) {
        try {
            if (file == null) return;
            if (!Files.exists(file)) return;

            try (InputStream is = Files.newInputStream(file)) {
                Allure.addAttachment(
                        safeName(name),
                        mimeType != null ? mimeType : "application/octet-stream",
                        is,
                        getExt(file)
                );
            }
        } catch (Exception ignored) {}
    }

    /**
     * Convenience: attach screenshot file (auto mime).
     */
    public static void attachScreenshotFile(String name, Path screenshotFile) {
        String mime = guessImageMime(screenshotFile);
        attachFile(name, screenshotFile, mime);
    }

    /**
     * Convenience: attach video mp4 file.
     */
    public static void attachVideoMp4(String name, Path mp4File) {
        attachFile(name, mp4File, "video/mp4");
    }

    /**
     * Attach plain text (es. log o info).
     */
    public static void attachText(String name, String text) {
        try {
            if (text == null) text = "";
            Allure.addAttachment(
                    safeName(name),
                    "text/plain",
                    new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                    ".txt"
            );
        } catch (Exception ignored) {}
    }

    // =========================
    //  ENVIRONMENT.PROPERTIES
    // =========================

    /**
     * Crea/aggiorna allure-results/environment.properties.
     * Passa una mappa con i valori che vuoi vedere in Allure (Jenkins build, branch, device, ecc.).
     */
    public static void writeEnvironmentProperties(Map<String, String> env) {
        try {
            Path dir = Paths.get("allure-results");
            Files.createDirectories(dir);

            Path out = dir.resolve("environment.properties");

            StringBuilder sb = new StringBuilder();
            if (env != null) {
                for (Map.Entry<String, String> e : env.entrySet()) {
                    String k = e.getKey();
                    String v = e.getValue();
                    if (k == null || k.isBlank()) continue;
                    if (v == null) v = "";
                    sb.append(k.trim()).append("=").append(v.replace("\n", " ").trim()).append("\n");
                }
            }

            Files.writeString(out, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }

    /**
     * Helper pronto: costruisce una mappa "standard" per Jenkins + Java.
     * Tu puoi aggiungere device/appPackage/appActivity/udid ecc.
     */
    public static Map<String, String> defaultEnv(String udid, String appPackage, String appActivity) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("java", System.getProperty("java.version"));
        m.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        m.put("user.language", System.getProperty("user.language"));

        // Jenkins
        m.put("jenkins.build", getenv("BUILD_NUMBER"));
        m.put("jenkins.job", getenv("JOB_NAME"));
        m.put("git.branch", firstNonBlank(getenv("GIT_BRANCH"), getenv("BRANCH_NAME")));

        // Mobile
        if (udid != null) m.put("device.udid", udid);
        if (appPackage != null) m.put("app.package", appPackage);
        if (appActivity != null) m.put("app.activity", appActivity);

        return m;
    }

    // =========================
    //  INTERNAL UTILS
    // =========================

    private static String getenv(String k) {
        try { return System.getenv(k); } catch (Exception e) { return ""; }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "attachment";
        return name.trim();
    }

    private static String getExt(Path p) {
        try {
            String s = p.getFileName().toString();
            int i = s.lastIndexOf('.');
            if (i >= 0) return s.substring(i);
        } catch (Exception ignored) {}
        return "";
    }

    private static String guessImageMime(Path p) {
        String ext = getExt(p).toLowerCase();
        if (ext.equals(".png")) return "image/png";
        if (ext.equals(".jpg") || ext.equals(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
