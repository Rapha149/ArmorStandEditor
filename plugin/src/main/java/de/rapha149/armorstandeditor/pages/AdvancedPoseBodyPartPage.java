package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.version.Axis;
import de.rapha149.armorstandeditor.version.BodyPart;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static de.rapha149.armorstandeditor.Messages.getMessage;
import static de.rapha149.armorstandeditor.Messages.getRawMessage;
import static de.rapha149.armorstandeditor.Util.*;

public class AdvancedPoseBodyPartPage extends Page {

    private final int PAGE_NUMBER = 3;
    private final String KEY = "armorstands.advanced_controls.pose.";

    private final Map<Entry<Axis, Boolean>, Material> RESET = new LinkedHashMap<>() {{
        put(Map.entry(Axis.X, false), Material.RED_DYE);
        put(Map.entry(Axis.Y, false), Material.LIME_DYE);
        put(Map.entry(Axis.Z, false), Material.BLUE_DYE);
        put(Map.entry(Axis.X, true), Material.RED_CONCRETE);
        put(Map.entry(Axis.Y, true), Material.LIME_CONCRETE);
        put(Map.entry(Axis.Z, true), Material.BLUE_CONCRETE);
    }};
    private final Map<Integer, Material> CHANGE_X = new LinkedHashMap<>() {{
        put(1, Material.RED_CARPET);
        put(2, Material.RED_DYE);
        put(5, Material.REDSTONE);
        put(10, Material.MOOSHROOM_SPAWN_EGG);
        put(15, Material.RED_STAINED_GLASS);
        put(30, Material.RED_WOOL);
        put(45, Material.RED_CONCRETE_POWDER);
        put(90, Material.RED_CONCRETE);
    }};
    private final Map<Integer, Material> CHANGE_Y = new LinkedHashMap<>() {{
        put(1, Material.LIME_CARPET);
        put(2, Material.LIME_DYE);
        put(5, Material.SLIME_BALL);
        put(10, Material.CREEPER_SPAWN_EGG);
        put(15, Material.LIME_STAINED_GLASS);
        put(30, Material.LIME_WOOL);
        put(45, Material.LIME_CONCRETE_POWDER);
        put(90, Material.LIME_CONCRETE);
    }};
    private final Map<Integer, Material> CHANGE_Z = new LinkedHashMap<>() {{
        put(1, Material.BLUE_CARPET);
        put(2, Material.BLUE_DYE);
        put(5, Material.LAPIS_LAZULI);
        put(10, Material.SQUID_SPAWN_EGG);
        put(15, Material.BLUE_STAINED_GLASS);
        put(30, Material.BLUE_WOOL);
        put(45, Material.BLUE_CONCRETE_POWDER);
        put(90, Material.BLUE_CONCRETE);
    }};

    private BodyPart bodyPart;

    public AdvancedPoseBodyPartPage(BodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        Gui gui = Gui.gui().title(getMessage("armorstands.advanced_controls.title",
                Map.of("%menu%", getRawMessage(KEY + "name"))).adventure()).rows(6).disableAllInteractions().create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui);

        gui.setItem(gui.getRows(), 1, applyNameAndLore(ItemBuilder.from(Material.ARROW), KEY + "back").asGuiItem(event -> {
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, PAGE_NUMBER, true));
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
        }));

        Long[] currentPose;
        {
            EulerAngle angle = bodyPart.get(armorStand);
            currentPose = new Long[]{
                    Math.round(getRotation(angle.getX())),
                    Math.round(getRotation(angle.getY())),
                    Math.round(getRotation(angle.getZ()))
            };
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            for (BodyPart bodyPart : BodyPart.values()) {
                EulerAngle angle = bodyPart.get(armorStand);
                Long[] actual = {
                        Math.round(getRotation(angle.getX())),
                        Math.round(getRotation(angle.getY())),
                        Math.round(getRotation(angle.getZ()))
                };
                if (!Arrays.equals(currentPose, actual)) {
                    System.arraycopy(actual, 0, currentPose, 0, actual.length);
                    setCurrentPoseItem(gui, armorStand);
                }
            }
        }, 40, 40);

        setCurrentPoseItem(gui, armorStand);

        gui.setItem(2, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "bodypart.reset.label").asGuiItem());
        AtomicInteger col = new AtomicInteger(2);
        RESET.forEach((entry, mat) -> {
            Axis axis = entry.getKey();
            boolean zero = entry.getValue();
            gui.setItem(2, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat),
                    KEY + "bodypart.reset.button." + (zero ? "zero" : "default"), Map.of("%axis%", axis.toString())).asGuiItem(event -> {
                if (zero)
                    bodyPart.set(armorStand, axis.setValue(bodyPart.get(armorStand), 0));
                else
                    wrapper.resetArmorStandBodyPart(armorStand, bodyPart, axis);
                playSpyglassSound(player);

                currentPose[axis.ordinal()] = Math.round(getRotation(axis.getValueDegrees(bodyPart.get(armorStand))));
                setCurrentPoseItem(gui, armorStand);
            }));
        });

        for (Axis axis : Axis.values()) {
            int row = switch (axis) {
                case X -> 3;
                case Y -> 4;
                case Z -> 5;
            };

            gui.setItem(row, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "bodypart.change.label",
                    Map.of("%axis%", axis.toString())).asGuiItem());
            col.set(2);
            (switch (axis) {
                case X -> CHANGE_X;
                case Y -> CHANGE_Y;
                case Z -> CHANGE_Z;
            }).forEach((amount, mat) -> gui.setItem(row, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat), KEY + "bodypart.change.button", Map.of(
                    "%axis%", axis.toString(),
                    "%amount%", String.valueOf(amount))
            ).asGuiItem(event -> {
                EulerAngle angle = bodyPart.get(armorStand);
                angle = axis.setValueDegrees(angle, getRotation(axis.getValueDegrees(angle) + amount * (event.isLeftClick() ? 1 : -1)));
                bodyPart.set(armorStand, angle);
                playSpyglassSound(player);

                currentPose[axis.ordinal()] = Math.round(axis.getValueDegrees(angle));
                setCurrentPoseItem(gui, armorStand);
            })));
        }

        Util.addPageItems(player, armorStand, gui, PAGE_NUMBER, true);
        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task::cancel);
    }

    private void setCurrentPoseItem(Gui gui, ArmorStand armorStand) {
        Material mat = switch (bodyPart) {
            case HEAD -> Material.PLAYER_HEAD;
            case BODY -> Material.DIAMOND_CHESTPLATE;
            case LEFT_ARM -> Material.SHIELD;
            case RIGHT_ARM -> Material.DIAMOND_SWORD;
            case LEFT_LEG, RIGHT_LEG -> Material.DIAMOND_LEGGINGS;
        };

        EulerAngle angle = bodyPart.get(armorStand);
        gui.updateItem(1, 9, applyNameAndLore(ItemBuilder.from(mat), KEY + "bodypart.current", Map.of(
                "%bodypart%", getRawMessage(KEY + "bodypart_names." + bodyPart.toString().toLowerCase()),
                "%pose_x%", angleToString(angle.getX()),
                "%pose_y%", angleToString(angle.getY()),
                "%pose_z%", angleToString(angle.getZ())
        )).flags(ItemFlag.HIDE_ATTRIBUTES).asGuiItem());
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
