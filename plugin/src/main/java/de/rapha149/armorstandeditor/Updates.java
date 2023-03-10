package de.rapha149.armorstandeditor;

import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Updates {

    public static final String SPIGOT_URL = "https://www.spigotmc.org/resources/armorstandeditor.108120/";
    private static final int RESOURCE_ID = 108120;

    public static String getAvailableVersion() {
        ArmorStandEditor plugin = ArmorStandEditor.getInstance();
        try {
            String version;

            try {
                URLConnection conn = new URL("https://api.spiget.org/v2/resources/" + RESOURCE_ID + "/versions/latest").openConnection();
                conn.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:86.0) Gecko/20100101 Firefox/86.0");
                conn.connect();
                JSONObject root = new JSONObject(new BufferedReader(new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining()));
                if (root != null)
                    version = root.getString("name");
                else
                    throw new IllegalStateException("JSON could not be read.");
            } catch (ConnectException e) {
                plugin.getLogger().warning("Could not access https://spiget.org/, instead legacy spigot api is used to check for updates.");

                URLConnection conn = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID).openConnection();
                conn.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:86.0) Gecko/20100101 Firefox/86.0");
                conn.connect();
                version = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
            }

            String result = compare(plugin.getDescription().getVersion(), version) < 0 ? version : "";
            return result;
        } catch (ConnectException e) {
            plugin.getLogger().warning("Could not access https://spigotmc.org/ to check for updates.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isBukkitVersionAboveOrEqualTo(String version) {
        Matcher matcher = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(Bukkit.getVersion());
        return matcher.find() && compare(matcher.group(), version) >= 0;
    }

    public static int compare(String version1, String version2) {
        int result = 0;

        String[] split1 = version1.split("\\.");
        String[] split2 = version2.split("\\.");
        int max = Math.max(split1.length, split2.length);

        for (int i = 0; i < max; i++) {
            Integer v1 = i < split1.length ? Integer.parseInt(split1[i]) : 0;
            Integer v2 = i < split2.length ? Integer.parseInt(split2[i]) : 0;
            int compare = v1.compareTo(v2);
            if (compare != 0) {
                result = compare;
                break;
            }
        }
        return result;
    }
}