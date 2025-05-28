package de.rapha149.armorstandeditor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Util.MINI_MESSAGE;

public class Messages {

    private static File messageFile;
    private static FileConfiguration messageConfig;
    private static Map<String, Message> messages;

    static {
        messageFile = new File(ArmorStandEditor.getInstance().getDataFolder(), "messages.yml");
        messageConfig = new YamlConfiguration();
        messageConfig.options().copyDefaults(true);
        messageConfig.options().copyHeader(false);

        messageConfig.addDefault("prefix", "<light_purple>ArmorStandEditor <gray>» <reset>");
        messageConfig.addDefault("plugin.enable", "Plugin successfully enabled.");
        messageConfig.addDefault("plugin.disable", "Plugin disabled.");
        messageConfig.addDefault("plugin.up_to_date", "Your version of this plugin is up to date!");
        messageConfig.addDefault("plugin.outdated", "There's a new version available for this plugin: %version%" +
                                                    "\nYou can download it from: %url%");
        messageConfig.addDefault("no_permission", "%prefix%<red>You do not have permission to do this.");
        messageConfig.addDefault("error", "%prefix%<red>An error occurred.");
        messageConfig.addDefault("reload", "%prefix%<gray>The config has been reloaded.");
        messageConfig.addDefault("not_possible_now", "%prefix%<red>This is not possible right now.");
        messageConfig.addDefault("armorstands.no_permission", "%prefix%<red>You can't edit this armor stand.");
        messageConfig.addDefault("armorstands.already_open", "%prefix%<red>This armor stand is already being edited at the moment.");
        messageConfig.addDefault("armorstands.armor_page_cooldown", "%prefix%<gold>Please wait a moment.");
        messageConfig.addDefault("armorstands.features.deactivated", "<red>Not available." +
                                                                     "\n<red>This feature is disabled.");
        messageConfig.addDefault("armorstands.features.no_permission", "<red>Not available." +
                                                                       "\n<red>You can't use this feature.");
        messageConfig.addDefault("armorstands.title.normal", "<dark_purple>Armor Stand");
        messageConfig.addDefault("armorstands.title.admin_bypass", "<dark_purple>Armor Stand (Admin Bypass)");
        messageConfig.addDefault("armorstands.page.forward", "<gray>» [%current%/%max%]");
        messageConfig.addDefault("armorstands.page.back", "<gray>« [%current%/%max%]");
        messageConfig.addDefault("armorstands.status.on", "<green>On<gray>/Off");
        messageConfig.addDefault("armorstands.status.off", "<gray>On/<red>Off");
        messageConfig.addDefault("armorstands.equipment.name", "<light_purple>Equipment ➜");
        messageConfig.addDefault("armorstands.equipment.lore", """
                <gray>» Change the equipment using the
                <gray>   slots to the right of this item.
                <gray>» Changes take effect as soon as
                <gray>   the inventory or the page is closed.
                
                <gray>» Slots:
                <#ff1e00>   1. Helmet
                <#ff7300>   2. Chestplate
                <#ffbf00>   3. Leggings
                <#65ff06>   4. Boots
                <#00ffd9>   5. Right Hand
                <#0289ff>   6. Left Hand
                <gray>» Layout:
                <#ff1e00>       <bold>1
                <#00ffd9>    <bold>5 <#ff7300>2 <#0289ff>6
                <#ffbf00>       <bold>3
                <#65ff06>       <bold>4""");
        messageConfig.addDefault("armorstands.equipment.invalid", "%prefix%<red>You tried to equip invalid armor items.");
        messageConfig.addDefault("armorstands.move_body_parts.title.color_activated", "<light_purple><bold>");
        messageConfig.addDefault("armorstands.move_body_parts.title.color_deactivated", "<gray>");
        messageConfig.addDefault("armorstands.move_body_parts.title.text", "%color_normal%Normal: %normal% <reset><gray>| %color_sneak%Sneaking: %sneak% <reset><gray>| Left click: Finish | Right click: Cancel");
        messageConfig.addDefault("armorstands.move_body_parts.head.name", "<light_purple>Move head");
        messageConfig.addDefault("armorstands.move_body_parts.head.lore", "<gray>» Left click ➜ Move\n<gray>» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_body_parts.body.name", "<light_purple>Move body");
        messageConfig.addDefault("armorstands.move_body_parts.body.lore", "<gray>» Left click ➜ Move\n<gray>» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_body_parts.left_arm.name", "<light_purple>Move left arm");
        messageConfig.addDefault("armorstands.move_body_parts.left_arm.lore", "<gray>» Left click ➜ Move\n<gray>» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_body_parts.right_arm.name", "<light_purple>Move right arm");
        messageConfig.addDefault("armorstands.move_body_parts.right_arm.lore", "<gray>» Left click ➜ Move\n<gray>» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_body_parts.left_leg.name", "<light_purple>Move left leg");
        messageConfig.addDefault("armorstands.move_body_parts.left_leg.lore", "<gray>» Left click ➜ Move\n<gray>» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_body_parts.right_leg.name", "<light_purple>Move right leg");
        messageConfig.addDefault("armorstands.move_body_parts.right_leg.lore", "<gray>» Left click ➜ Move\n<gray>» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_position.title.color_aligned_inactive", "<gray>");
        messageConfig.addDefault("armorstands.move_position.title.color_aligned_active", "<light_purple>");
        messageConfig.addDefault("armorstands.move_position.title.normal", "%aligned_color%Sneaking: Aligned <gray>| Left click: Set down | Right click: Cancel");
        messageConfig.addDefault("armorstands.move_position.title.snapin", "<light_purple>Scroll: Distance (%distance%) <gray>| %aligned_color%Sneaking: Aligned <gray>| Left click: Finish | Right click: Cancel");
        messageConfig.addDefault("armorstands.move_position.name", "<light_purple>Move");
        messageConfig.addDefault("armorstands.move_position.lore", """
                <gray>» Left click ➜ Move the armor stand
                <gray>» Right click ➜ Move the armor stand with snap-in positions
                <gray>   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.x.name", "<light_purple>Move (X)");
        messageConfig.addDefault("armorstands.move_position.x.lore", """
                <gray>» Left click ➜ Move the armor stand by 0.05 blocks
                <gray>» Right click ➜ Move the armor stand by -0.05 blocks
                <gray>» Drop ➜ Move the armor stand with snap-in positions
                <gray>   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.y.name", "<light_purple>Move (Y)");
        messageConfig.addDefault("armorstands.move_position.y.lore", """
                <gray>» Left click ➜ Move the armor stand by 0.05 blocks
                <gray>» Right click ➜ Move the armor stand by -0.05 blocks
                <gray>» Drop ➜ Move the armor stand with snap-in positions
                <gray>   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.y.gravity_warning", "<red> - Gravity is enabled!");
        messageConfig.addDefault("armorstands.move_position.z.name", "<light_purple>Move (Z)");
        messageConfig.addDefault("armorstands.move_position.z.lore", """
                <gray>» Left click ➜ Move the armor stand by 0.05 blocks
                <gray>» Right click ➜ Move the armor stand by -0.05 blocks
                <gray>» Drop ➜ Move the armor stand with snap-in positions
                <gray>   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.too_far", "%prefix%<red>You can't move the armor stand that far away.");
        messageConfig.addDefault("armorstands.rotate.title", "<light_purple>Left click: Finish <gray>| <light_purple>Right click: Cancel");
        messageConfig.addDefault("armorstands.rotate.name", "<light_purple>Rotate");
        messageConfig.addDefault("armorstands.rotate.lore", """
                <gray>» Current rotation ➜ <light_purple>%rotation%°
                
                <gray>» Left click ➜ Rotate <light_purple>45° <gray>clockwise
                <gray>» Right click ➜ Rotate <light_purple>45° <gray>counterclockwise
                
                <gray>» Shift + Left click ➜ Rotate <light_purple>10° <gray>clockwise
                <gray>» Shift + Right click ➜ Rotate <light_purple>10° <gray>counterclockwise
                
                <gray>» Drop ➜ Match the armor stand's rotation to yours
                <gray>» Ctrl + Drop ➜ Reset rotation""");
        messageConfig.addDefault("armorstands.advanced_controls.open.name", "<light_purple>Advanced Controls");
        messageConfig.addDefault("armorstands.advanced_controls.open.lore", """
                <gray>» Left click ➜ Open <italic>Position <gray>Menu
                <gray>» Right click ➜ Open <italic>Rotation <gray>Menu
                <gray>» Drop ➜ Open <italic>Pose <gray>Menu""");
        messageConfig.addDefault("armorstands.advanced_controls.leave.name", "<light_purple>Leave <italic>Advanced Controls");
        messageConfig.addDefault("armorstands.advanced_controls.leave.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.title", "<dark_purple>Advanced Controls » %menu%");
        messageConfig.addDefault("armorstands.advanced_controls.page_item.name", "<light_purple>%menu%");
        messageConfig.addDefault("armorstands.advanced_controls.page_item.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.name", "Position");
        messageConfig.addDefault("armorstands.advanced_controls.position.current.name", "<light_purple>Current position");
        messageConfig.addDefault("armorstands.advanced_controls.position.current.lore", """
                <gray>» X ➜ <light_purple>%position_x%
                <gray>» Y ➜ <light_purple>%position_y%
                <gray>» Z ➜ <light_purple>%position_z%""");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.label.name", "<light_purple>Align »");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.button.name", "<light_purple>Align on %axis% axis");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.button.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.label.name", "<light_purple>Move on %axis% axis »");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.button.name", "<light_purple>Move on %axis% axis by %amount%");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.button.lore", "<gray>» Left click ➜ +%amount%" +
                                                                                            "\n<gray>» Right click ➜ -%amount%");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.name", "Rotation");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.current.name", "<light_purple>Current rotation");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.current.lore", "<gray>» %rotation%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.label.name", "<light_purple>Set value »");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.button.name", "<light_purple>Set to %value%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.button.lore", "<gray>» %alternative_value%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.label.name", "<light_purple>Change value »");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.button.name", "<light_purple>Change by %amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.button.lore", "<gray>» Left click ➜ +%amount%°" +
                                                                                              "\n<gray>» Right click ➜ -%amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.name", "Pose");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart_names.head", "Head");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart_names.body", "Body");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart_names.left_arm", "Left Arm");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart_names.right_arm", "Right Arm");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart_names.left_leg", "Left Leg");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart_names.right_leg", "Right Leg");
        messageConfig.addDefault("armorstands.advanced_controls.pose.back.name", "<light_purple>Back to Overview");
        messageConfig.addDefault("armorstands.advanced_controls.pose.back.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.pose.overview.choose_bodypart.name", "<light_purple>Modify %bodypart%°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.overview.choose_bodypart.lore", """
                <gray>» Current X ➜ %pose_x%°
                <gray>» Current Y ➜ %pose_y%°
                <gray>» Current Z ➜ %pose_z%°""");
        messageConfig.addDefault("armorstands.advanced_controls.pose.overview.presets.name", "<light_purple>Presets");
        messageConfig.addDefault("armorstands.advanced_controls.pose.overview.presets.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.current.name", "<light_purple>Modify » %bodypart%");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.current.lore", """
                <gray>» Current X ➜ %pose_x%°
                <gray>» Current Y ➜ %pose_y%°
                <gray>» Current Z ➜ %pose_z%°""");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.reset.label.name", "<light_purple>Reset »");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.reset.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.reset.button.default.name", "<light_purple>Reset %axis%");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.reset.button.default.lore", "<gray>» Default position");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.reset.button.zero.name", "<light_purple>Reset %axis%");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.reset.button.zero.lore", "<gray>» 0°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.change.label.name", "<light_purple>Change %axis% »");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.change.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.change.button.name", "<light_purple>Change %axis% by %amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.change.button.lore", "<gray>» Left click ➜ +%amount%°" +
                                                                                                   "\n<gray>» Right click ➜ -%amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.presets.preset.name", "<light_purple>Preset » %preset%");
        messageConfig.addDefault("armorstands.advanced_controls.pose.presets.preset.lore", "");
        messageConfig.addDefault("armorstands.private.name", "<light_purple>Private");
        messageConfig.addDefault("armorstands.private.lore.normal", "<gray>» Makes your armor stand accessible only to you\n<gray>» %status%");
        messageConfig.addDefault("armorstands.private.lore.admin_bypass", "<gray>» Makes your armor stand accessible only to you\n<gray>» %status%%player%");
        messageConfig.addDefault("armorstands.private.player", " <gray>(<gold>%player%<gray>)");
        messageConfig.addDefault("armorstands.lock.lore", "<gray>» Click to toggle the slot lock\n<gray>» %status%");
        messageConfig.addDefault("armorstands.lock.helmet", "<light_purple>Head [Lock slot]");
        messageConfig.addDefault("armorstands.lock.chestplate", "<light_purple>Chestplate [Lock slot]");
        messageConfig.addDefault("armorstands.lock.leggings", "<light_purple>Leggings [Lock slot]");
        messageConfig.addDefault("armorstands.lock.boots", "<light_purple>Boots [Lock slot]");
        messageConfig.addDefault("armorstands.lock.mainhand", "<light_purple>Right Hand [Lock slot]");
        messageConfig.addDefault("armorstands.lock.offhand", "<light_purple>Left Hand [Lock slot]");
        messageConfig.addDefault("armorstands.settings.invisible.name", "<light_purple>Invisibility");
        messageConfig.addDefault("armorstands.settings.invisible.lore", "<gray>» Click to toggle invisibility\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.invulnerable.name", "<light_purple>Invulnerability");
        messageConfig.addDefault("armorstands.settings.invulnerable.lore", "<gray>» Click to toggle invulnerability\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.show_arms.name", "<light_purple>Arms");
        messageConfig.addDefault("armorstands.settings.show_arms.lore", "<gray>» Click to toggle the arms\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.gravity.name", "<light_purple>Gravity");
        messageConfig.addDefault("armorstands.settings.gravity.lore", "<gray>» Click to toggle gravity\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.base_plate.name", "<light_purple>Base plate");
        messageConfig.addDefault("armorstands.settings.base_plate.lore", "<gray>» Click to toggle the base plate\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.small.name", "<light_purple>Small armor stand");
        messageConfig.addDefault("armorstands.settings.small.lore", "<gray>» Click to toggle the small armor stand\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.glowing.name", "<light_purple>Glowing");
        messageConfig.addDefault("armorstands.settings.glowing.lore", "<gray>» Click to toggle the glowing effect\n<gray>» %status%");
        messageConfig.addDefault("armorstands.settings.fire.name", "<light_purple>On fire");
        messageConfig.addDefault("armorstands.settings.fire.lore", "<gray>» Click to toggle visual fire\n<gray>» %status%");
        messageConfig.addDefault("armorstands.passenger.name", "<light_purple>Armor stand as passenger");
        messageConfig.addDefault("armorstands.passenger.lore", "<gray>» Left click ➜ Set on vehicle" +
                                                               "\n<gray>» Right click ➜ Remove from vehicle");
        messageConfig.addDefault("armorstands.passenger.choose.title", "<light_purple>Choose a vehicle <gray>| <gray>Left click: Choose | Right click: Cancel");
        messageConfig.addDefault("armorstands.passenger.choose.no_players", "%prefix%<red>You can't choose a player.");
        messageConfig.addDefault("armorstands.passenger.choose.not_itself", "%prefix%<red>You can't choose the armor stand itself.");
        messageConfig.addDefault("armorstands.vehicle.name", "<light_purple>Armor stand as vehicle");
        messageConfig.addDefault("armorstands.vehicle.lore", "<gray>» Left click ➜ Choose passenger" +
                                                             "\n<gray>» Right click ➜ Remove all passengers");
        messageConfig.addDefault("armorstands.vehicle.choose.title", "<light_purple>Choose a passenger <gray>| <gray>Left click: Choose | Right click: Cancel");
        messageConfig.addDefault("armorstands.vehicle.choose.no_players", "%prefix%<red>You can't choose a player.");
        messageConfig.addDefault("armorstands.vehicle.choose.not_itself", "%prefix%<red>You can't choose the armor stand itself.");
        messageConfig.addDefault("armorstands.give_item.name", "<light_purple>Give as item");
        messageConfig.addDefault("armorstands.give_item.lore", "<gray>» Click to receive the armor stand as an item.");
        messageConfig.addDefault("armorstands.rename.name", "<light_purple>Rename");
        messageConfig.addDefault("armorstands.rename.lore", """
                <gray>» Current name ➜ <white>%name%
                <gray>» Left click ➜ Change name (Supports color codes)
                <gray>» Right click ➜ Remove name""");
    }

    public static void loadMessages() {
        Logger logger = ArmorStandEditor.getInstance().getLogger();
        try {
            if (messageFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(messageFile));
                String content = br.lines().collect(Collectors.joining("\n"));
                br.close();

                messageConfig.loadFromString(content);

                String line = content.split("\n")[0];
                Matcher matcher = Pattern.compile("# ArmorStandEditor version ((\\d|\\.)+)").matcher(line);
                if (matcher.matches()) {
                    String version = matcher.group(1);
                    if (Updates.compare(version, "1.4") <= 0) {
                        messageConfig.set("armorstands.move_position.title.normal", messageConfig.getDefaults().get("armorstands.move_position.title.normal"));
                    }

                    if (Updates.compare(version, "1.5.14") <= 0) {
                        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().hexColors().build();
                        String endTagRegex = "</(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|" +
                                             "blue|green|aqua|red|light_purple|yellow|white|#[0-9a-fA-F]{6}|" +
                                             "bold|italic|underlined|strikethrough|obfuscated)>";
                        messageConfig.getKeys(true).forEach(key -> {
                            if (messageConfig.isString(key)) {
                                messageConfig.set(key, MINI_MESSAGE.serialize(serializer.deserialize(ChatColor.translateAlternateColorCodes('&',
                                                messageConfig.getString(key)).replaceAll("&#([0-9a-fA-F]{6})", "§#$1")))
                                        .replaceAll(endTagRegex, ""));
                            }
                        });

                        messageConfig.set("armorstands.move_body_parts.title.text", messageConfig.getString("armorstands.move_body_parts.title.text")
                                .replaceAll("<(\\w+)>\\|", "<reset><$1>|"));
                        logger.info("The messages in messages.yml were converted to MiniMessage format.");
                    }
                }
            } else
                messageFile.getParentFile().mkdirs();

            messages = new HashMap<>();
            messageConfig.getKeys(true).forEach(key -> {
                if (!messageConfig.getDefaults().isSet(key)) {
                    messageConfig.set(key, null);
                } else {
                    try {
                        messages.put(key, new Message(messageConfig.getString(key)));
                    } catch (ParsingException e) {
                        logger.severe("Failed to parse message for key: " + key + " (" + e.getMessage() + ")");
                    }
                }
            });

            try (FileWriter writer = new FileWriter(messageFile)) {
                writer.write("# ArmorStandEditor version " + ArmorStandEditor.getInstance().getDescription().getVersion() +
                             "\n\n" + messageConfig.saveToString());
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            logger.severe("Failed to load message config.");
        }
    }

    public static String getRawMessage(String key) {
        if (messages.containsKey(key)) {
            return messageConfig.getString(key);
        } else {
            return "Unknown message key: " + key;
        }
    }

    public static Message getMessage(String key) {
        if (messages.containsKey(key)) {
            return messages.get(key);
        } else {
            return new Message(Component.text("Unknown message key: " + key));
        }
    }

    public static Message getMessage(String key, Map<String, String> replacements) {
        return new Message(getRawMessage(key), replacements);
    }

    private static String applyReplacements(String message, Map<String, String> replacements) {
        for (Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }

    public record Message(String plain, Component adventure, BaseComponent[] spigot) {

        public Message(String message) {
            this(MINI_MESSAGE.deserialize("<!italic>" + message.replace("\\n", "\n")
                    .replace("%prefix%", messageConfig.getString("prefix"))));
        }

        public Message(String message, Map<String, String> replacements) {
            this(applyReplacements(message, replacements));
        }

        private Message(Component adventure) {
            this(
                    Util.PLAIN_SERIALIZER.serialize(adventure),
                    adventure,
                    ComponentSerializer.parse(Util.GSON_SERIALIZER.serialize(adventure))
            );
        }
    }
}
