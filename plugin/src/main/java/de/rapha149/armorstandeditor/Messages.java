package de.rapha149.armorstandeditor;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Messages {

    private static File messageFile;
    private static FileConfiguration messageConfig;

    static {
        messageFile = new File(ArmorStandEditor.getInstance().getDataFolder(), "messages.yml");
        messageConfig = new YamlConfiguration();
        messageConfig.options().copyDefaults(true);
        messageConfig.options().copyHeader(false);

        messageConfig.addDefault("prefix", "&dArmorStandEditor &7» §r");
        messageConfig.addDefault("plugin.enable", "Plugin successfully enabled.");
        messageConfig.addDefault("plugin.disable", "Plugin disabled.");
        messageConfig.addDefault("plugin.up_to_date", "Your version of this plugin is up to date!");
        messageConfig.addDefault("plugin.outdated", "There's a new version available for this plugin: %version%" +
                                                    "\nYou can download it from: %url%");
        messageConfig.addDefault("no_permission", "%prefix%&cYou do not have permission to do this.");
        messageConfig.addDefault("error", "%prefix%&cAn error occurred.");
        messageConfig.addDefault("reload", "%prefix%&7The config has been reloaded.");
        messageConfig.addDefault("not_possible_now", "%prefix%&cThis is not possible right now.");
        messageConfig.addDefault("armorstands.no_permission", "%prefix%&cYou can't edit this armor stand.");
        messageConfig.addDefault("armorstands.already_open", "%prefix%&cThis armor stand is already being edited at the moment.");
        messageConfig.addDefault("armorstands.features.deactivated", "&cNot available." +
                                                                     "\n&cThis feature is disabled.");
        messageConfig.addDefault("armorstands.features.no_permission", "&cNot available." +
                                                                       "\n&cYou can't use this feature.");
        messageConfig.addDefault("armorstands.title.normal", "&5Armor Stand");
        messageConfig.addDefault("armorstands.title.admin_bypass", "&5Armor Stand (Admin Bypass)");
        messageConfig.addDefault("armorstands.page.forward", "&7» [%current%/%max%]");
        messageConfig.addDefault("armorstands.page.back", "&7« [%current%/%max%]");
        messageConfig.addDefault("armorstands.status.on", "&aOn&7/Off");
        messageConfig.addDefault("armorstands.status.off", "&7On/&cOff");
        messageConfig.addDefault("armorstands.equipment.name", "&dEquipment ➜");
        messageConfig.addDefault("armorstands.equipment.lore", """
                &7» Change the equipment using the
                &7   slots to the right of this item.
                &7» Changes take effect as soon as
                &7   the inventory or the page is closed.
                &7» Slots:
                &#ff1e00   1. Helmet
                &#ff7300   2. Chestplate
                &#ffbf00   3. Leggings
                &#65ff06   4. Boots
                &#00ffd9   5. Right Hand
                &#0289ff   6. Left Hand
                &7» Layout:
                &#ff1e00       &l1
                &#00ffd9    &l5 &#ff7300&l2 &#0289ff&l6
                &#ffbf00       &l3
                &#65ff06       &l4""");
        messageConfig.addDefault("armorstands.equipment.invalid", "%prefix%&cYou tried to equip invalid armor items.");
        messageConfig.addDefault("armorstands.move.title.color_activated", "&d&l");
        messageConfig.addDefault("armorstands.move.title.color_deactivated", "&7");
        messageConfig.addDefault("armorstands.move.title.text", "%color_normal%Normal: %normal% &7| %color_sneak%Sneaking: %sneak% &7| Left click: Finish | Right click: Cancel");
        messageConfig.addDefault("armorstands.move.head.name", "&dMove head");
        messageConfig.addDefault("armorstands.move.head.lore", "&7» Left click ➜ Move\n&7» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move.body.name", "&dMove body");
        messageConfig.addDefault("armorstands.move.body.lore", "&7» Left click ➜ Move\n&7» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move.left_arm.name", "&dMove left arm");
        messageConfig.addDefault("armorstands.move.left_arm.lore", "&7» Left click ➜ Move\n&7» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move.right_arm.name", "&dMove right arm");
        messageConfig.addDefault("armorstands.move.right_arm.lore", "&7» Left click ➜ Move\n&7» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move.left_leg.name", "&dMove left leg");
        messageConfig.addDefault("armorstands.move.left_leg.lore", "&7» Left click ➜ Move\n&7» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move.right_leg.name", "&dMove right leg");
        messageConfig.addDefault("armorstands.move.right_leg.lore", "&7» Left click ➜ Move\n&7» Right click ➜ Reset");
        messageConfig.addDefault("armorstands.move_position.title.normal", "&dLeft click: Set down | Right click: Cancel");
        messageConfig.addDefault("armorstands.move_position.title.snapin_color_aligned_inactive", "&7");
        messageConfig.addDefault("armorstands.move_position.title.snapin_color_aligned_active", "&d");
        messageConfig.addDefault("armorstands.move_position.title.snapin", "&dScroll: Distance (%distance%) &7| %aligned_color%Sneaking: Aligned &7| Left click: Finish | Right click: Cancel");
        messageConfig.addDefault("armorstands.move_position.name", "&dMove");
        messageConfig.addDefault("armorstands.move_position.lore", """
                &7» Left click ➜ Move the armor stand
                &7» Right click ➜ Move the armor stand with snap-in positions
                &7   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.x.name", "&dMove (X)");
        messageConfig.addDefault("armorstands.move_position.x.lore", """
                &7» Left click ➜ Move the armor stand by 0.05 blocks
                &7» Right click ➜ Move the armor stand by -0.05 blocks
                &7» Drop ➜ Move the armor stand with snap-in positions
                &7   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.y.name", "&dMove (Y)");
        messageConfig.addDefault("armorstands.move_position.y.lore", """
                &7» Left click ➜ Move the armor stand by 0.05 blocks
                &7» Right click ➜ Move the armor stand by -0.05 blocks
                &7» Drop ➜ Move the armor stand with snap-in positions
                &7   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.move_position.z.name", "&dMove (Z)");
        messageConfig.addDefault("armorstands.move_position.z.lore", """
                &7» Left click ➜ Move the armor stand by 0.05 blocks
                &7» Right click ➜ Move the armor stand by -0.05 blocks
                &7» Drop ➜ Move the armor stand with snap-in positions
                &7   (look at particles to move the armor stand)""");
        messageConfig.addDefault("armorstands.rotate.title", "&dLeft click: Finish &7| &dRight click: Cancel");
        messageConfig.addDefault("armorstands.rotate.name", "&dRotate");
        messageConfig.addDefault("armorstands.rotate.lore", """
                &7» Current rotation ➜ &d%rotation%
                &7» Left click ➜ Rotate &d45° &7clockwise
                &7» Right click ➜ Rotate &d45° &7counterclockwise
                &7» Shift + Left click ➜ Rotate &d10° &7clockwise
                &7» Shift + Right click ➜ Rotate &d10° &7counterclockwise
                &7» Drop ➜ Match the armor stand's rotation to yours
                &7» Ctrl + Drop ➜ Reset rotation""");
        messageConfig.addDefault("armorstands.advanced_controls.open.name", "&dAdvanced Controls");
        messageConfig.addDefault("armorstands.advanced_controls.open.lore", """
                &7» Left click ➜ Open &oPosition &7Menu
                &7» Right click ➜ Open &oRotation &7Menu
                &7» Drop ➜ Open &oPose &7Menu""");
        messageConfig.addDefault("armorstands.advanced_controls.leave.name", "&dLeave &oAdvanced Controls");
        messageConfig.addDefault("armorstands.advanced_controls.leave.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.title", "&5Advanced Controls » %menu%");
        messageConfig.addDefault("armorstands.advanced_controls.deactivated", "&d%menu%");
        messageConfig.addDefault("armorstands.advanced_controls.position.name", "Position");
        messageConfig.addDefault("armorstands.advanced_controls.position.current.name", "&dCurrent position");
        messageConfig.addDefault("armorstands.advanced_controls.position.current.lore", """
                &7» X ➜ &d%position_x%
                &7» Y ➜ &d%position_y%
                &7» Z ➜ &d%position_z%""");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.label.name", "&dAlign »");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.button.name", "&dAlign on %axis% axis");
        messageConfig.addDefault("armorstands.advanced_controls.position.align.button.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.label.name", "&dMove on %axis% axis »");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.button.name", "&dMove on %axis% axis by %amount%");
        messageConfig.addDefault("armorstands.advanced_controls.position.move.button.lore", "&7» Left click ➜ +%amount%" +
                                                                                            "\n&7» Right click ➜ -%amount%");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.name", "Rotation");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.current.name", "&dCurrent rotation");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.current.lore", "&7» %rotation%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.label.name", "&dSet value »");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.button.name", "&dSet to %value%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.set.button.lore", "&7» %alternative_value%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.label.name", "&dChange value »");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.button.name", "&dChange by %amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.rotation.change.button.lore", "&7» Left click ➜ +%amount%°" +
                                                                                              "\n&7» Right click ➜ -%amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.name", "Pose");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.name.head", "&dModify » Head");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.name.body", "&dModify » Body");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.name.left_leg", "&dModify » Left Leg");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.name.right_leg", "&dModify » Right Leg");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.name.left_arm", "&dModify » Left Arm");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.name.right_arm", "&dModify » Right Arm");
        messageConfig.addDefault("armorstands.advanced_controls.pose.bodypart.lore", """
                &7» Current X ➜ %pose_x%
                &7» Current Y ➜ %pose_y%
                &7» Current Z ➜ %pose_z%""");
        messageConfig.addDefault("armorstands.advanced_controls.pose.reset.label.name", "&dReset »");
        messageConfig.addDefault("armorstands.advanced_controls.pose.reset.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.pose.reset.button.default.name", "&dReset %axis%");
        messageConfig.addDefault("armorstands.advanced_controls.pose.reset.button.default.lore", "&7» Default position");
        messageConfig.addDefault("armorstands.advanced_controls.pose.reset.button.zero.name", "&dReset %axis%");
        messageConfig.addDefault("armorstands.advanced_controls.pose.reset.button.zero.lore", "&7» 0°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.change.label.name", "&dChange %axis% »");
        messageConfig.addDefault("armorstands.advanced_controls.pose.change.label.lore", "");
        messageConfig.addDefault("armorstands.advanced_controls.pose.change.button.name", "&dChange %axis% by %amount%°");
        messageConfig.addDefault("armorstands.advanced_controls.pose.change.button.lore", "&7» Left click ➜ +%amount%°" +
                                                                                          "\n&7» Right click ➜ -%amount%°");
        messageConfig.addDefault("armorstands.private.name", "&dPrivate");
        messageConfig.addDefault("armorstands.private.lore.normal", "&7» Makes your armor stand accessible only to you\n&7» %status%");
        messageConfig.addDefault("armorstands.private.lore.admin_bypass", "&7» Makes your armor stand accessible only to you\n&7» %status%%player%");
        messageConfig.addDefault("armorstands.private.player", " &7(&6%player%&7)");
        messageConfig.addDefault("armorstands.lock.lore", "&7» Click to toggle the slot lock\n&7» %status%");
        messageConfig.addDefault("armorstands.lock.helmet", "&dHead [Lock slot]");
        messageConfig.addDefault("armorstands.lock.chestplate", "&dChestplate [Lock slot]");
        messageConfig.addDefault("armorstands.lock.leggings", "&dLeggings [Lock slot]");
        messageConfig.addDefault("armorstands.lock.boots", "&dBoots [Lock slot]");
        messageConfig.addDefault("armorstands.lock.mainhand", "&dRight Hand [Lock slot]");
        messageConfig.addDefault("armorstands.lock.offhand", "&dLeft Hand [Lock slot]");
        messageConfig.addDefault("armorstands.settings.invisible.name", "&dInvisibility");
        messageConfig.addDefault("armorstands.settings.invisible.lore", "&7» Click to toggle invisibility\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.invulnerable.name", "&dInvulnerability");
        messageConfig.addDefault("armorstands.settings.invulnerable.lore", "&7» Click to toggle invulnerability\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.show_arms.name", "&dArms");
        messageConfig.addDefault("armorstands.settings.show_arms.lore", "&7» Click to toggle the arms\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.gravity.name", "&dGravity");
        messageConfig.addDefault("armorstands.settings.gravity.lore", "&7» Click to toggle gravity\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.base_plate.name", "&dBase plate");
        messageConfig.addDefault("armorstands.settings.base_plate.lore", "&7» Click to toggle the base plate\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.small.name", "&dSmall armor stand");
        messageConfig.addDefault("armorstands.settings.small.lore", "&7» Click to toggle the small armor stand\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.glowing.name", "&dGlowing");
        messageConfig.addDefault("armorstands.settings.glowing.lore", "&7» Click to toggle the glowing effect\n&7» %status%");
        messageConfig.addDefault("armorstands.settings.fire.name", "&dOn fire");
        messageConfig.addDefault("armorstands.settings.fire.lore", "&7» Click to toggle visual fire\n&7» %status%");
        messageConfig.addDefault("armorstands.passenger.name", "&dArmor stand as passenger");
        messageConfig.addDefault("armorstands.passenger.lore", "&7» Left click ➜ Set on vehicle" +
                                                               "\n&7» Right click ➜ Remove from vehicle");
        messageConfig.addDefault("armorstands.passenger.choose.title", "&dChoose a vehicle &7| &7Left click: Choose | Right click: Cancel");
        messageConfig.addDefault("armorstands.passenger.choose.no_players", "%prefix%&cYou can't choose a player.");
        messageConfig.addDefault("armorstands.passenger.choose.not_itself", "%prefix%&cYou can't choose the armor stand itself.");
        messageConfig.addDefault("armorstands.vehicle.name", "&dArmor stand as vehicle");
        messageConfig.addDefault("armorstands.vehicle.lore", "&7» Left click ➜ Choose passenger" +
                                                             "\n&7» Right click ➜ Remove all passengers");
        messageConfig.addDefault("armorstands.vehicle.choose.title", "&dChoose a passenger &7| &7Left click: Choose | Right click: Cancel");
        messageConfig.addDefault("armorstands.vehicle.choose.no_players", "%prefix%&cYou can't choose a player.");
        messageConfig.addDefault("armorstands.vehicle.choose.not_itself", "%prefix%&cYou can't choose the armor stand itself.");
        messageConfig.addDefault("armorstands.give_item.name", "&dGive as item");
        messageConfig.addDefault("armorstands.give_item.lore", "&7» Click to receive the armor stand as an item.");
        messageConfig.addDefault("armorstands.rename.name", "&dRename");
        messageConfig.addDefault("armorstands.rename.lore", """
                &7» Current name ➜ &f%name%
                &7» Left click ➜ Change name (Supports color codes)
                &7» Right click ➜ Remove name""");
    }

    public static void loadMessages() {
        try {
            if (messageFile.exists())
                messageConfig.load(messageFile);
            else
                messageFile.getParentFile().mkdirs();

            messageConfig.getKeys(true).forEach(key -> {
                if (!messageConfig.getDefaults().isSet(key))
                    messageConfig.set(key, null);
            });

            try (FileWriter writer = new FileWriter(messageFile)) {
                writer.write("# ArmorStandEditor version " + ArmorStandEditor.getInstance().getDescription().getVersion() +
                             "\n\n" + messageConfig.saveToString());
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            ArmorStandEditor.getInstance().getLogger().severe("Failed to load message config.");
        }
    }

    public static String getMessage(String key) {
        if (messageConfig.contains(key)) {
            return ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key)
                    .replace("\\n", "\n")
                    .replace("%prefix%", messageConfig.getString("prefix")));
        } else
            throw new IllegalArgumentException("Message key \"" + key + "\" does not exist.");
    }
}
