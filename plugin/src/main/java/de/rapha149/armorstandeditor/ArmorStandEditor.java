package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Metrics.DrilldownPie;
import de.rapha149.armorstandeditor.Metrics.SimplePie;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public final class ArmorStandEditor extends JavaPlugin {

    private static ArmorStandEditor instance;

    public VersionWrapper wrapper;

    @Override
    public void onEnable() {
        instance = this;

        String nmsVersion = getNMSVersion();
        try {
            wrapper = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException exception) {
            throw new IllegalStateException("Failed to load support for server version " + nmsVersion, exception);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("ArmorStandEditor does not support the server version \"" + nmsVersion + "\"", exception);
        }

        Messages.loadMessages();
        try {
            Config.load();
        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadMetrics();

        if (Config.get().checkForUpdates) {
            String version = Updates.getAvailableVersion();
            if (version != null) {
                if (version.isEmpty())
                    getLogger().info(getMessage("plugin.up_to_date"));
                else {
                    for (String line : getMessage("plugin.outdated").split("\n"))
                        getLogger().warning(line.replace("%version%", version).replace("%url%", Updates.SPIGOT_URL));
                }
            }
        }

        new ReloadCommand(getCommand("asereload"));
        getServer().getPluginManager().registerEvents(new Events(), this);
        getLogger().info(getMessage("plugin.enable"));
    }

    @Override
    public void onDisable() {
        Util.onDisable();
        getLogger().info(getMessage("plugin.disable"));
    }

    public static ArmorStandEditor getInstance() {
        return instance;
    }

    private String getNMSVersion() {
        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        if (craftBukkitPackage.contains("v"))
            return craftBukkitPackage.split("\\.")[3].substring(1);

        // Get NMS Version from the bukkit version
        String bukkitVersion = Bukkit.getBukkitVersion();

        // Try to get NMS Version from online list (https://github.com/Rapha149/NMSVersions)
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://raw.githubusercontent.com/Rapha149/NMSVersions/main/nms-versions.json"))
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2)
                throw new IOException("Failed to get NMS versions list: " + response.statusCode());

            JSONObject json = new JSONObject(response.body());
            if (json.has(bukkitVersion))
                return json.getString(bukkitVersion);
        } catch (IOException | InterruptedException e) {
            getLogger().warning("Can't access online NMS versions list, falling back to hardcoded NMS versions. These could be outdated.");
        }

        // separating major and minor versions, example: 1.20.4-R0.1-SNAPSHOT -> major = 20, minor = 4
        final String[] versionNumbers = bukkitVersion.split("-")[0].split("\\.");
        int major = Integer.parseInt(versionNumbers[1]);
        int minor = versionNumbers.length > 2 ? Integer.parseInt(versionNumbers[2]) : 0;

        if (major == 20 && minor >= 5) { // 1.20.5, 1.20.6
            return "1_20_R4";
        } else if (major == 21 && minor <= 1) { // 1.21, 1.21.1
            return "1_21_R1";
        } else if (major == 21 && (minor == 2 || minor == 3)) { // 1.21.2, 1.21.3
            return "1_21_R2";
        } else if (major == 21 && (minor == 4)) { // 1.21.4
            return "1_21_R4";
        }

        throw new IllegalStateException("ArmorStandEditor does not support bukkit server version \"" + bukkitVersion + "\"");
    }

    private void loadMetrics() {
        Metrics metrics = new Metrics(this, 17771);
        metrics.addCustomChart(new DrilldownPie("check_for_updates", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();
            entry.put(getDescription().getVersion(), 1);
            map.put(String.valueOf(Config.get().checkForUpdates), entry);
            return map;
        }));
        metrics.addCustomChart(new SimplePie("general_permission", () -> String.valueOf(Config.get().permissions.general != null)));
        metrics.addCustomChart(new SimplePie("deactivated_item", () -> Optional.ofNullable(Config.get().deactivatedItem).orElse("None")));
        metrics.addCustomChart(new DrilldownPie("features_7", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> replaceEquipment = new HashMap<>();
            Map<String, Integer> moveBodyParts = new HashMap<>();
            Map<String, Integer> movePosition = new HashMap<>();
            Map<String, Integer> rotate = new HashMap<>();

            FeaturesData features = Config.get().features;
            replaceEquipment.put(getFeatureStatus(features.replaceEquipment), 1);
            moveBodyParts.put(getFeatureStatus(features.moveBodyParts), 1);
            movePosition.put(getFeatureStatus(features.movePosition), 1);
            rotate.put(getFeatureStatus(features.rotate), 1);

            map.put("Replace equipment", replaceEquipment);
            map.put("Move body parts", moveBodyParts);
            map.put("Move position", movePosition);
            map.put("Rotate", rotate);
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("features_2", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> disabledSlots = new HashMap<>();
            Map<String, Integer> invisibility = new HashMap<>();
            Map<String, Integer> invulnerability = new HashMap<>();
            Map<String, Integer> showArms = new HashMap<>();

            FeaturesData features = Config.get().features;
            disabledSlots.put(getFeatureStatus(features.disabledSlots), 1);
            invisibility.put(getFeatureStatus(features.invisibility), 1);
            invulnerability.put(getFeatureStatus(features.invulnerability), 1);
            showArms.put(getFeatureStatus(features.showArms), 1);

            map.put("Disabled Slots", disabledSlots);
            map.put("Invisibility", invisibility);
            map.put("Invulnerability", invulnerability);
            map.put("Show Arms", showArms);
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("features_3", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> gravity = new HashMap<>();
            Map<String, Integer> basePlate = new HashMap<>();
            Map<String, Integer> small = new HashMap<>();
            Map<String, Integer> rename = new HashMap<>();

            FeaturesData features = Config.get().features;
            gravity.put(getFeatureStatus(features.gravity), 1);
            basePlate.put(getFeatureStatus(features.basePlate), 1);
            small.put(getFeatureStatus(features.small), 1);
            rename.put(getFeatureStatus(features.rename), 1);

            map.put("Gravity", gravity);
            map.put("Base plate", basePlate);
            map.put("Small", small);
            map.put("Rename", rename);
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("features_5", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> glowing = new HashMap<>();
            Map<String, Integer> fire = new HashMap<>();
            Map<String, Integer> passenger = new HashMap<>();
            Map<String, Integer> vehicle = new HashMap<>();

            FeaturesData features = Config.get().features;
            glowing.put(getFeatureStatus(features.glowing), 1);
            fire.put(getFeatureStatus(features.fire), 1);
            passenger.put(getFeatureStatus(features.passenger), 1);
            vehicle.put(getFeatureStatus(features.vehicle), 1);

            map.put("Glowing", glowing);
            map.put("Fire", fire);
            map.put("Armor stand as passenger", passenger);
            map.put("Armor stand as vehicle", vehicle);
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("features_8", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> giveItem = new HashMap<>();
            Map<String, Integer> copy = new HashMap<>();
            Map<String, Integer> privateArmorStand = new HashMap<>();

            FeaturesData features = Config.get().features;
            giveItem.put(getFeatureStatus(features.giveItem), 1);
            copy.put(getFeatureStatus(features.copy), 1);
            privateArmorStand.put(getFeatureStatus(features.privateArmorstand), 1);

            map.put("Give item", giveItem);
            map.put("Copy", copy);
            map.put("Private", privateArmorStand);
            return map;
        }));
    }

    private String getFeatureStatus(FeatureData feature) {
        if (!feature.enabled)
            return "Disabled";
        if (feature.permission != null)
            return "Enabled with permission";
        return "Enabled";
    }
}
