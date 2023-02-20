package de.rapha149.armorstandeditor;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Config {

    private static Map<String, String> comments = new HashMap<>();
    private static Config config;

    static {
        comments.put("checkForUpdates", "Whether to check for updates on enabling.");
        comments.put("advancement", "The advancement to grant the player when he first accesses an armor stand. Set to \"null\" to disable.");
        comments.put("permissions.general", "The permission that is needed to interact with armor stands and open the menu." +
                                            "\nSet to \"null\" to disable the permission and allow everybody to use the plugin.");
        comments.put("permissions.ignorePrivate", "With this permission, players can open private armor stands even if they wouldn't have access to them." +
                                                  "\nYou can set this permission to \"null\" to disable it, but it's not recommended as private armor stands would be available to everyone.");
        comments.put("deactivatedItem", "The item that is displayed when a feature is disabled." +
                                     "\nSet to \"null\" to show the actual item of the feature even though it's disabled.");
        comments.put("features", "A list of features. You can enable/disable each feature or set a permission to use a certain feature." +
                                 "\nIf you want a feature to be enabled and everybody to be able to use it, set the permission to \"null\".");
        comments.put("features.replaceEquipment", "Replacing the armor stand's equipment (armor and hand items) in the ASE inventory.");
        comments.put("features.replaceEquipment.useDeactivatedItem", "Whether or not to replace the armor and hand items of the armor stand with the disabled item when this feature is disabled.");
        comments.put("features.moveBodyParts", "Moving the armor stand's body parts.");
        comments.put("features.movePosition", "Moving the armor stand's position.");
        comments.put("features.privateArmorstand", "Making your armor stand private so that only you can open its ASE inventory.");
        comments.put("features.disabledSlots", "Locking the equipment slots of your armor stand so that players can't take items directly.");
        comments.put("features.invisibility", "Making your armor stand invisible.");
        comments.put("features.invulnerability", "Making your armor stand invulnerable.");
        comments.put("features.showArms", "Making your armor stand's arms visible.");
        comments.put("features.gravity", "Making your armor stand not affected by gravity.");
        comments.put("features.basePlate", "Making your armor stand's base plate invisible.");
        comments.put("features.small", "Making your armor stand small.");
        comments.put("features.glowing", "Making your armor stand glow.");
        comments.put("features.fire", "Making your armor stand seeming to be on fire.");
        comments.put("features.passenger", "Set your armor stand as a passenger on a vehicle.");
        comments.put("features.passenger.players", "Whether or not players can be selected as vehicles.");
        comments.put("features.vehicle", "Set another entity as a passenger on your armor stand.");
        comments.put("features.vehicle.players", "Whether or not players can be selected as passengers.");
        comments.put("features.rename", "Renaming your armor stand.");
        comments.put("features.giveItem", "Receiving your armor stand as an item.");
        comments.put("features.copy", "Copying your armor stand settings by combining a modified armor stand item with a normal armor stand item in the crafting table." +
                                      "\nThis behavior is similar to the copying behavior of written books.");
    }

    public static void load() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setSplitLines(false);
        Representer representer = new Representer(options);
        representer.setPropertyUtils(new CustomPropertyUtils());
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(ArmorStandEditor.getInstance().getClass().getClassLoader()), representer, options);

        File file = new File(ArmorStandEditor.getInstance().getDataFolder(), "config.yml");
        if (file.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String content = br.lines().collect(Collectors.joining("\n"));
            br.close();
            config = yaml.loadAs(content, Config.class);
        } else {
            file.getParentFile().mkdirs();
            config = new Config();
        }

        try (FileWriter writer = new FileWriter(file)) {
            Pattern pattern = Pattern.compile("((\\s|-)*)(\\w+):( .+)?");
            Map<Integer, String> parents = new HashMap<>();
            int lastIndent = 0;
            String[] lines = yaml.dumpAsMap(config).split("\n");
            StringBuilder sb = new StringBuilder("# ArmorStandEditor version " + ArmorStandEditor.getInstance().getDescription().getVersion() +
                                                 "\n# Github: https://github.com/Rapha149/ArmorStandEditor" +
                                                 "\n# Spigot: " + Updates.SPIGOT_URL + "\n");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    int indent = matcher.group(1).length();
                    parents.put(indent, matcher.group(3));

                    List<String> tree = new ArrayList<>();
                    for (int j = 0; j <= indent; j += options.getIndent())
                        tree.add(parents.get(j));
                    String key = String.join(".", tree);
                    if (comments.containsKey(key)) {
                        if (lastIndent >= indent)
                            sb.append("\n");

                        String prefix = " ".repeat(indent) + "# ";
                        sb.append(prefix + String.join("\n" + prefix, comments.get(key).split("\n")) + "\n" + line + "\n");

                        lastIndent = indent;
                        continue;
                    } else if (matcher.group(4) == null)
                        sb.append("\n");
                    lastIndent = indent;
                }

                sb.append(line + "\n");
            }

            writer.write(sb.toString().replaceAll("\\[\\n\\s+\\]", "[]"));
        }
    }

    public static Config get() {
        return config;
    }

    public boolean checkForUpdates = true;
    public String advancement = null;
    public PermissionsData permissions = new PermissionsData();
    public String deactivatedItem = "minecraft:gray_dye";
    public FeaturesData features = new FeaturesData();

    public static class PermissionsData {

        public String general = null;
        public String ignorePrivate = "armorstandeditor.ignoreprivate";
    }

    public static class FeaturesData {

        public ReplaceEquipmentFeatureData replaceEquipment = new ReplaceEquipmentFeatureData();
        public FeatureData moveBodyParts = new FeatureData();
        public FeatureData movePosition = new FeatureData();
        public FeatureData privateArmorstand = new FeatureData();
        public FeatureData disabledSlots = new FeatureData();
        public FeatureData invisibility = new FeatureData();
        public FeatureData invulnerability = new FeatureData();
        public FeatureData showArms = new FeatureData();
        public FeatureData gravity = new FeatureData();
        public FeatureData basePlate = new FeatureData();
        public FeatureData small = new FeatureData();
        public FeatureData glowing = new FeatureData();
        public FeatureData movable = new FeatureData();
        public FeatureData fire = new FeatureData();
        public VehicleFeatureData passenger = new VehicleFeatureData();
        public VehicleFeatureData vehicle = new VehicleFeatureData();
        public FeatureData rename = new FeatureData();
        public FeatureData giveItem = new FeatureData();
        public FeatureData copy = new FeatureData();

        public static class FeatureData {

            public boolean enabled = true;
            public String permission = null;
        }

        public static class ReplaceEquipmentFeatureData extends FeatureData {

            public boolean useDeactivatedItem = false;
        }

        public static class VehicleFeatureData extends FeatureData {

            public boolean players = false;
        }
    }
}
