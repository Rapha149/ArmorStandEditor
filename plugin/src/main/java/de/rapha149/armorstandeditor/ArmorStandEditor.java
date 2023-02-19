package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Metrics.DrilldownPie;
import de.rapha149.armorstandeditor.Metrics.SimplePie;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public final class ArmorStandEditor extends JavaPlugin {

    private static ArmorStandEditor instance;

    public VersionWrapper wrapper;
    public boolean isPaper;

    private Events events;

    @Override
    public void onEnable() {
        instance = this;

        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
        try {
            wrapper = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to load support for server version \"" + nmsVersion + "\"");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ArmorStandEditor does not support the server version \"" + nmsVersion + "\"");
        }

        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            isPaper = true;
        } catch (ClassNotFoundException ignored) {
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
        getServer().getPluginManager().registerEvents(events = new Events(), this);
        getLogger().info(getMessage("plugin.enable"));
    }

    @Override
    public void onDisable() {
        events.onDisable();
        getLogger().info(getMessage("plugin.disable"));
    }

    public static ArmorStandEditor getInstance() {
        return instance;
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
        metrics.addCustomChart(new DrilldownPie("features_1", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> replaceEquipment = new HashMap<>();
            Map<String, Integer> moveBodyParts = new HashMap<>();
            Map<String, Integer> movePosition = new HashMap<>();
            Map<String, Integer> privateArmorStand = new HashMap<>();

            FeaturesData features = Config.get().features;
            replaceEquipment.put(getFeatureStatus(features.replaceEquipment), 1);
            moveBodyParts.put(getFeatureStatus(features.moveBodyParts), 1);
            movePosition.put(getFeatureStatus(features.movePosition), 1);
            privateArmorStand.put(getFeatureStatus(features.privateArmorstand), 1);

            map.put("Replace equipment", replaceEquipment);
            map.put("Move body parts", moveBodyParts);
            map.put("Move position", movePosition);
            map.put("Private armor stand", privateArmorStand);
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
        metrics.addCustomChart(new DrilldownPie("features_4", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> giveItem = new HashMap<>();
            Map<String, Integer> copy = new HashMap<>();

            FeaturesData features = Config.get().features;
            giveItem.put(getFeatureStatus(features.giveItem), 1);
            copy.put(getFeatureStatus(features.copy), 1);

            map.put("Give item", giveItem);
            map.put("Copy", copy);
            return map;
        }));
    }

    private String getFeatureStatus(FeatureData feature) {
        if(!feature.enabled)
            return "Disabled";
        if(feature.permission != null)
            return "Enabled with permission";
        return "Enabled";
    }
}
