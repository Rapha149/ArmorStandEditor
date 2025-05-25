package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.version.BodyPart;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.EulerAngle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static de.rapha149.armorstandeditor.Messages.getMessage;
import static de.rapha149.armorstandeditor.Messages.getRawMessage;
import static de.rapha149.armorstandeditor.Util.*;

public class AdvancedPosePage extends Page {

    private final String KEY = "armorstands.advanced_controls.pose.";

    private final Map<Integer, Entry<BodyPart, Material>> BODYPARTS = new LinkedHashMap<>() {{
        put(11, Map.entry(BodyPart.HEAD, Material.PLAYER_HEAD));
        put(19, Map.entry(BodyPart.RIGHT_ARM, Material.STICK));
        put(20, Map.entry(BodyPart.BODY, Material.STICK));
        put(21, Map.entry(BodyPart.LEFT_ARM, Material.STICK));
        put(28, Map.entry(BodyPart.RIGHT_LEG, Material.STICK));
        put(30, Map.entry(BodyPart.LEFT_LEG, Material.STICK));
    }};

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        Gui gui = Gui.gui().title(getMessage("armorstands.advanced_controls.title",
                Map.of("%menu%", getRawMessage(KEY + "name"))).adventure()).rows(6).disableAllInteractions().create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui);

        gui.setItem(gui.getRows(), 1, applyNameAndLore(ItemBuilder.from(Material.ARROW), "armorstands.advanced_controls.leave").asGuiItem(event -> {
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, 1, false));
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
        }));

        BODYPARTS.forEach((slot, entry) -> {
            BodyPart bodyPart = entry.getKey();
            Material mat = entry.getValue();
            EulerAngle angle = bodyPart.get(armorStand);
            gui.setItem(slot, applyNameAndLore(ItemBuilder.from(mat), KEY + "overview.choose_bodypart", Map.of(
                    "%bodypart%", getRawMessage(KEY + "bodypart_names." + bodyPart.toString().toLowerCase()),
                    "%pose_x%", angleToString(angle.getX()),
                    "%pose_y%", angleToString(angle.getY()),
                    "%pose_z%", angleToString(angle.getZ())
                )).asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, new AdvancedPoseBodyPartPage(bodyPart)));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));
        });

        gui.setItem(3, 7, applyNameAndLore(ItemBuilder.from(Material.WRITTEN_BOOK), KEY + "overview.presets").asGuiItem(event -> {
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, ADVANCED_POSE_PRESETS_PAGE));
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
        }));

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status);
    }

    private double getRotation(double rotation) {
        if (rotation > 180)
            return rotation - 360;
        if (rotation == -180)
            return 180;
        return rotation;
    }

    private String angleToString(double angle) {
        String str = String.valueOf(getRotation(Math.round(Math.toDegrees(angle) * 100D) / 100D));
        return str.endsWith(".0") ? str.substring(0, str.length() - 2) : str;
    }
}
