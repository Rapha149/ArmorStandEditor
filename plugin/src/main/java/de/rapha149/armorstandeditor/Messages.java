package de.rapha149.armorstandeditor;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Messages {

    private static File messageFile;
    private static FileConfiguration messageConfig;

    static {
        messageFile = new File(ArmorStandEditor.getInstance().getDataFolder(), "messages.yml");
        messageConfig = new YamlConfiguration();
        messageConfig.options().copyDefaults(true);

        messageConfig.addDefault("prefix", "&8[&6ArmorStandEditor&8] &r");
        messageConfig.addDefault("plugin.enable", "Plugin successfully enabled.");
        messageConfig.addDefault("plugin.disable", "Plugin disabled.");
        messageConfig.addDefault("plugin.up_to_date", "Your version of this plugin is up to date!");
        messageConfig.addDefault("plugin.outdated", "There's a new version available for this plugin: %version%" +
                                                    "\nYou can download it from: %url%");
        messageConfig.addDefault("no_permission", "%prefix%&cYou do not have permission to do this.");
        messageConfig.addDefault("error", "%prefix%&cAn error occurred.");
        messageConfig.addDefault("reload", "%prefix%&7The config has been reloaded.");
        messageConfig.addDefault("armorstands.no_permission", "%prefix%&cYou can't edit this armor stand.");
        messageConfig.addDefault("armorstands.already_open", "%prefix%&6This armor stand is already being edited at the moment.");
        messageConfig.addDefault("armorstands.features.deactivated", "&cNot available." +
                                                              "\n&cThis feature is disabled.");
        messageConfig.addDefault("armorstands.features.no_permission", "&cNot available." +
                                                              "\n&cYou can't use this feature.");
        messageConfig.addDefault("armorstands.title.normal", "&5Armor Stand");
        messageConfig.addDefault("armorstands.title.admin_bypass", "&5Armor Stand (Admin Bypass)");
        messageConfig.addDefault("armorstands.page.first", "&7» [1/2]");
        messageConfig.addDefault("armorstands.page.second", "&7« [2/2]");
        messageConfig.addDefault("armorstands.status.on", "&aOn&7/Off");
        messageConfig.addDefault("armorstands.status.off", "&7On/&cOff");
        messageConfig.addDefault("armorstands.items.lore", "&7» Click, to replace with item.");
        messageConfig.addDefault("armorstands.items.cooldown", "%prefix%&ePlease slow down a little bit.");
        messageConfig.addDefault("armorstands.items.helmet", "&dHead [Slot]");
        messageConfig.addDefault("armorstands.items.chestplate", "&dChestplate [Slot]");
        messageConfig.addDefault("armorstands.items.leggings", "&dLeggings [Slot]");
        messageConfig.addDefault("armorstands.items.boots", "&dBoots [Slot]");
        messageConfig.addDefault("armorstands.items.mainhand", "&dRight Hand [Slot]");
        messageConfig.addDefault("armorstands.items.offhand", "&dLeft Hand [Slot]");
        messageConfig.addDefault("armorstands.move.armorstand_already_moving", "%prefix%&cThe armor stand is already being moved.");
        messageConfig.addDefault("armorstands.move.player_already_moving", "%prefix%&cYou're already moving an armor stand at the moment.");
        messageConfig.addDefault("armorstands.move.title.color_activated", "&d&l");
        messageConfig.addDefault("armorstands.move.title.color_deactivated", "&7&l");
        messageConfig.addDefault("armorstands.move.title.text", "%color_normal%Normal: %normal% &7&l| %color_sneak%Sneaking: %sneak% &7&l| &7&lLeft click: Finish | Right click: Cancel");
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
        messageConfig.addDefault("armorstands.position.title", "&d&lLeft click: Set down | Right click: Cancel");
        messageConfig.addDefault("armorstands.position.name", "&dMove");
        messageConfig.addDefault("armorstands.position.lore", "&7» Click, to move the armor stand");
        messageConfig.addDefault("armorstands.position.x.name", "&dMove (X)");
        messageConfig.addDefault("armorstands.position.x.lore", """
                &7» Left click ➜ Move the armor stand by 0.05 blocks
                &7» Right click ➜ Move the armor stand by -0.05 blocks
                &7» Drop ➜ Move the armor stand with your head movements""");
        messageConfig.addDefault("armorstands.position.y.name", "&dMove (Y)");
        messageConfig.addDefault("armorstands.position.y.lore", """
                &7» Left click ➜ Move the armor stand by 0.05 blocks
                &7» Right click ➜ Move the armor stand by -0.05 blocks
                &7» Drop ➜ Move the armor stand with your head movements""");
        messageConfig.addDefault("armorstands.position.z.name", "&dMove (Z)");
        messageConfig.addDefault("armorstands.position.z.lore", """
                &7» Left click ➜ Move the armor stand by 0.05 blocks
                &7» Right click ➜ Move the armor stand by -0.05 blocks
                &7» Drop ➜ Move the armor stand with your head movements""");
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
        messageConfig.addDefault("armorstands.give_item.name", "&dGive as item");
        messageConfig.addDefault("armorstands.give_item.lore", "&7» Click to receive the armor stand as an item.");
        messageConfig.addDefault("armorstands.rename.name", "&dRename");
        messageConfig.addDefault("armorstands.rename.lore", """
                &7» Current name ➜ &f%name%
                &7» Left click ➜ Change name (Supports color codes)
                &7» Right click ➜ Remove name""");
        messageConfig.addDefault("armorstands.rename.not_paper.lore", """
                &cNot available.
                &cIn order to use this feature, you have
                &cto use Paper as your server software.""");
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

            messageConfig.save(messageFile);
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