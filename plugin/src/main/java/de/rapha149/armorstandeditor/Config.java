package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.PresetData.PresetBodyPartData;
import de.rapha149.armorstandeditor.version.BodyPart;
import org.bukkit.util.EulerAngle;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.*;
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
        comments.put("features.replaceEquipment.clearItemsOnOpen", """
                Whether or not to clear the armor stand's equipment when the gui is opened. This will prevent duplication bugs that players can cause using mods.
                If you're running a small server with friends, you can choose to disable this option if you don't want the equipment to vanish every time you open the gui.
                However, if you're running a server with a few more players, it is recommended to leave this option enabled.""");
        comments.put("features.moveBodyParts", "Moving the armor stand's body parts.");
        comments.put("features.movePosition", "Moving the armor stand's position.");
        comments.put("features.movePosition.maxDistance", """
                The maximum distance the player can move an armor stand away from himself.
                This option only affects the movement controlled by the buttons in the gui, not the movement controlled by going somewhere / looking around.
                This limit exists to prevent players from unloading armor stands by moving them far away and then duplicating items.
                You can set increase it or set it to 0 to allow an infinite distance, but this is not recommended.""");
        comments.put("features.rotate", "Changing the armor stand's rotation.");
        comments.put("features.advancedControls", "Using advanced controls to change position, rotation and pose." +
                                                  "\nThey can be individually enabled/disabled via the options above.");
        comments.put("features.privateArmorstand", "Making your armor stand private so that only you can open its ASE inventory.");
        comments.put("features.privateArmorstand.autoPrivate", "Whether or not armor stands should become private when first accessed.");
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
        comments.put("presets", "Here you can define presets which are shown on the Pose page of the Advanced Controls." +
                                "\nMaximum of 15 presets.");
    }

    public static void load() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setSplitLines(false);
        Representer representer = new Representer(options);
        representer.setPropertyUtils(new CustomPropertyUtils());
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(ArmorStandEditor.getInstance().getClass().getClassLoader(), new LoaderOptions()), representer, options);

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
    public List<PresetData> presets = List.of(
            new PresetData("Default", Map.of(
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-10, 0, -10),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-15, 0, 10),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(-1, 0, -1),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(1, 0, 1)
            )),
            new PresetData("0°", Collections.emptyMap()),
            new PresetData("Item", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(0.8944604, 9.055486, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, -2.3101969, 0),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-90, 0, 0)
            )),
            new PresetData("Block", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(2.739634, -5.375096, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, -0.29615206, 0),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-15, -45, 0)
            )),
            new PresetData("Walking", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(5.883482, -19.316841, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, -1.3423482, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-20, 0, -10),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(20, 0, 10),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(20, 0, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(-20, 0, 0)
            )),
            new PresetData("Running", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(5.747715, 1.8663449, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, 1.5624547, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(40, 0, -10),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-40, 0, 10),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(-40, 0, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(40, 0, 0)
            )),
            new PresetData("Pointing", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(1.7437577, 19.371622, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, 2.9660754, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(0, 0, -10),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-90, 18, 0),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(0, 0, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(0, 0, 0)
            )),
            new PresetData("Salute", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(3.0737185, 6.626904, 0),
                    BodyPart.BODY, new PresetBodyPartData(5, -2.25304, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(29, 0, 25),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-124, -51, -35),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(0, 4, 2),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(0, -4, -2)
            )),
            new PresetData("Blocking", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(1.2775335, -7.32031, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, -4.3447514, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-50, 50, 0),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-20, -20, 0),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(20, 0, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(-20, 0, 0)
            )),
            new PresetData("Sitting", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(3.2998314, -6.680886, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, 3.2538185, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-80, -20, 0),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-80, 20, 0),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(-90, -10, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(-90, 10, 0)
            )),
            new PresetData("Laying", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(-83.36189, 3.5046368, 0),
                    BodyPart.BODY, new PresetBodyPartData(-90, -3.1862168, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-90, -10, 0),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-90, 10, 0),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(0, 0, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(0, 0, 0)
            )),
            new PresetData("Confused", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(1.1013848, 38.299202, 0),
                    BodyPart.BODY, new PresetBodyPartData(0, 12.96918, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(145, 22, -49),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-22, 31, 10),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(-6, 0, 0),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(6, -20, 0)
            )),
            new PresetData("Facepalm", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(47.090084, 3.8555756, 0),
                    BodyPart.BODY, new PresetBodyPartData(10, -1.2397861, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-72, 24, 47),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(18, -14, 0),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(-4, -6, -2),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(25, -2, 0)
            )),
            new PresetData("Formal", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(7.1271815, 3.9160357, 0),
                    BodyPart.BODY, new PresetBodyPartData(4, 3.4252434, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(30, -20, 21),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(30, 22, -20),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(0, 0, -5),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(0, 0, 5)
            )),
            new PresetData("Sad", Map.of(
                    BodyPart.HEAD, new PresetBodyPartData(67.50782, 2.741272, 0),
                    BodyPart.BODY, new PresetBodyPartData(10, 3.969596, 0),
                    BodyPart.LEFT_ARM, new PresetBodyPartData(-5, 0, -5),
                    BodyPart.RIGHT_ARM, new PresetBodyPartData(-5, 0, 5),
                    BodyPart.LEFT_LEG, new PresetBodyPartData(-5, 16, -5),
                    BodyPart.RIGHT_LEG, new PresetBodyPartData(-5, -10, 5)
            ))
    );

    public static class PermissionsData {

        public String general = null;
        public String ignorePrivate = "armorstandeditor.ignoreprivate";
    }

    public static class FeaturesData {

        public ReplaceEquipmentFeatureData replaceEquipment = new ReplaceEquipmentFeatureData();
        public FeatureData moveBodyParts = new FeatureData();
        public MovePositionFeatureData movePosition = new MovePositionFeatureData();
        public FeatureData rotate = new FeatureData();
        public FeatureData advancedControls = new FeatureData();
        public PrivateArmorstandFeatureData privateArmorstand = new PrivateArmorstandFeatureData();
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
            public boolean clearItemsOnOpen = true;
        }

        public static class MovePositionFeatureData extends FeatureData {

            private int maxDistance = 100;
            public transient int maxDistanceSquared = 10000;

            public int getMaxDistance() {
                return maxDistance;
            }

            public void setMaxDistance(int maxDistance) {
                this.maxDistance = maxDistance;
                this.maxDistanceSquared = maxDistance * maxDistance;
            }
        }

        public static class PrivateArmorstandFeatureData extends FeatureData {

            public boolean autoPrivate = false;
        }

        public static class VehicleFeatureData extends FeatureData {

            public boolean players = false;
        }
    }

    public static class PresetData {

        public String name = "Preset";
        private Map<String, PresetBodyPartData> bodyParts = new HashMap<>();
        public transient Map<BodyPart, EulerAngle> pose = Collections.emptyMap();

        public PresetData() {
            for (BodyPart bodyPart : BodyPart.values()) {
                String key = bodyPart.toString().toLowerCase();
                if (!bodyParts.containsKey(key))
                    bodyParts.put(key, new PresetBodyPartData());
            }
        }

        public PresetData(String name, Map<BodyPart, PresetBodyPartData> bodyParts) {
            this.name = name;
            for (BodyPart bodyPart : BodyPart.values())
                this.bodyParts.put(bodyPart.toString().toLowerCase(), bodyParts.getOrDefault(bodyPart, new PresetBodyPartData()));
            updatePose();
        }

        public Map<String, PresetBodyPartData> getBodyParts() {
            return bodyParts;
        }

        public void setBodyParts(Map<String, PresetBodyPartData> bodyParts) {
            this.bodyParts = new HashMap<>();
            for (BodyPart bodyPart : BodyPart.values())
                this.bodyParts.put(bodyPart.toString().toLowerCase(), bodyParts.getOrDefault(bodyPart.toString().toLowerCase(), new PresetBodyPartData()));
            updatePose();
        }

        private void updatePose() {
            Map<BodyPart, EulerAngle> pose = new HashMap<>();
            bodyParts.forEach((key, data) -> {
                try {
                    pose.put(BodyPart.valueOf(key.toUpperCase()), new EulerAngle(
                            Math.toRadians(data.x),
                            Math.toRadians(data.y),
                            Math.toRadians(data.z)
                    ));
                } catch (IllegalArgumentException ignore) {
                }
            });

            for (BodyPart bodyPart : BodyPart.values()) {
                if (!pose.containsKey(bodyPart))
                    pose.put(bodyPart, new EulerAngle(0, 0, 0));
            }

            this.pose = pose;
        }

        public static class PresetBodyPartData {

            public double x;
            public double y;
            public double z;

            public PresetBodyPartData() {
            }

            public PresetBodyPartData(double x, double y, double z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }
    }
}
