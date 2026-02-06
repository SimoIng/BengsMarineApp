package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Classe per gestire la configurazione degli utenti di test
 */
public class UserConfig {

    private final String userId;
    private final String username;
    private final String password;
    private final int totaleTotem;
    private final String descrizione;
    private final boolean abilitato;

    private UserConfig(String userId, Properties props) {
        this.userId = userId;
        this.username = props.getProperty(userId + ".username");
        this.password = props.getProperty(userId + ".password");
        this.totaleTotem = Integer.parseInt(props.getProperty(userId + ".totale_totem", "7"));
        this.descrizione = props.getProperty(userId + ".descrizione", "Utente " + userId);
        this.abilitato = Boolean.parseBoolean(props.getProperty(userId + ".abilitato", "true"));
    }

    public static UserConfig loadDefault() {
        try (InputStream input = new FileInputStream("test.properties")) {
            Properties props = new Properties();
            props.load(input);
            String defaultUser = props.getProperty("default.user", "user1");
            return new UserConfig(defaultUser, props);
        } catch (IOException e) {
            System.out.println("⚠️ Impossibile caricare test.properties, uso configurazione di default");
            return getDefaultConfig();
        }
    }

    public static UserConfig loadUser(String userId) {
        try (InputStream input = new FileInputStream("test.properties")) {
            Properties props = new Properties();
            props.load(input);
            return new UserConfig(userId, props);
        } catch (IOException e) {
            System.out.println("⚠️ Impossibile caricare configurazione per " + userId);
            return getDefaultConfig();
        }
    }

    public static List<UserConfig> loadAllEnabledUsers() {
        List<UserConfig> users = new ArrayList<>();
        try (InputStream input = new FileInputStream("test.properties")) {
            Properties props = new Properties();
            props.load(input);
            String testUsers = props.getProperty("test.users", "user1");
            String[] userIds = testUsers.split(",");
            for (String userId : userIds) {
                userId = userId.trim();
                UserConfig user = new UserConfig(userId, props);
                if (user.isAbilitato()) {
                    users.add(user);
                    System.out.println("✅ Utente abilitato: " + user.getDescrizione());
                } else {
                    System.out.println("⏩ Utente disabilitato: " + user.getDescrizione());
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Impossibile caricare utenti, uso configurazione di default");
            users.add(getDefaultConfig());
        }
        return users;
    }

    public static List<UserConfig> loadAllUsers() {
        List<UserConfig> users = new ArrayList<>();
        try (InputStream input = new FileInputStream("test.properties")) {
            Properties props = new Properties();
            props.load(input);
            Set<String> userIds = new HashSet<>();
            for (String key : props.stringPropertyNames()) {
                if (key.matches("user\\d+\\.username")) {
                    String userId = key.split("\\.")[0];
                    userIds.add(userId);
                }
            }
            for (String userId : userIds) {
                UserConfig user = new UserConfig(userId, props);
                users.add(user);
            }
            users.sort(Comparator.comparing(UserConfig::getUserId));
        } catch (IOException e) {
            System.out.println("⚠️ Impossibile caricare utenti");
            users.add(getDefaultConfig());
        }
        return users;
    }

    private static UserConfig getDefaultConfig() {
        Properties props = new Properties();
        props.setProperty("default.username", "collaudolamaddalena");
        props.setProperty("default.password", "bth01User!");
        props.setProperty("default.totale_totem", "7");
        props.setProperty("default.descrizione", "Utente Default");
        props.setProperty("default.abilitato", "true");
        return new UserConfig("default", props);
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getTotaleTotem() { return totaleTotem; }
    public String getDescrizione() { return descrizione; }
    public boolean isAbilitato() { return abilitato; }

    public boolean isValid() {
        return username != null && !username.isEmpty()
                && password != null && !password.isEmpty()
                && totaleTotem > 0;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %d totem [%s]",
                descrizione, username, totaleTotem,
                abilitato ? "ABILITATO" : "DISABILITATO");
    }

    public String toStringSafe() {
        return String.format("%s - User: %s, Password: %s, Totem: %d [%s]",
                descrizione, username, maskPassword(password), totaleTotem,
                abilitato ? "✅" : "⏸️");
    }

    private String maskPassword(String password) {
        if (password == null || password.length() <= 2) {
            return "***";
        }
        return password.charAt(0) + "***" + password.charAt(password.length() - 1);
    }
}
