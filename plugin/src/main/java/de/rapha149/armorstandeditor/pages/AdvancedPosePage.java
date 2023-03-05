package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class AdvancedPosePage extends Page {

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

    private final Map<UUID, BodyPart> selectedBodyPart = new HashMap<>();

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        FeaturesData features = Config.get().features;
        int deactivatedStatus = getDeactivatedStatus(features.advancedControls, player);
        if (deactivatedStatus == 0)
            deactivatedStatus = getDeactivatedStatus(features.rotate, player);

        Gui gui = Gui.gui().title(Component.text(getMessage("armorstands.advanced_controls.title")
                        .replace("%menu%", getMessage(KEY + "name"))))
                .rows(deactivatedStatus != 0 ? 5 : 6).disableAllInteractions().create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui, PAGE_NUMBER, true);

        gui.setItem(1, 1, applyNameAndLore(ItemBuilder.from(Material.ARROW), "armorstands.advanced_controls.leave").asGuiItem(event -> {
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> Util.openGUI(player, armorStand, 1, false));
            Util.playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
        }));

        BukkitTask task = null;
        if (deactivatedStatus != 0) {
            gui.setItem(3, 5, applyNameAndLoreWithoutKeys(ItemBuilder.from(getDeactivatedMaterial(Material.DIAMOND_CHESTPLATE)),
                    getMessage("armorstands.advanced_controls.deactivated").replace("%menu%", getMessage(KEY + "name")),
                    getMessage("armorstands.features." + (deactivatedStatus == 1 ? "deactivated" : "no_permission"))).asGuiItem());
        } else {
            UUID uuid = player.getUniqueId();
            selectedBodyPart.putIfAbsent(uuid, BodyPart.HEAD);

            Map<BodyPart, Long[]> currentPose = new HashMap<>();
            for (BodyPart bodyPart : BodyPart.values()) {
                EulerAngle angle = bodyPart.get(armorStand);
                currentPose.put(bodyPart, new Long[]{
                        Math.round(getRotation(angle.getX())),
                        Math.round(getRotation(angle.getY())),
                        Math.round(getRotation(angle.getZ()))
                });
                setBodyPartItem(player, gui, armorStand, bodyPart);
            }

            task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                for (BodyPart bodyPart : BodyPart.values()) {
                    EulerAngle angle = bodyPart.get(armorStand);
                    Long[] current = currentPose.get(bodyPart);
                    Long[] actual = {
                            Math.round(getRotation(angle.getX())),
                            Math.round(getRotation(angle.getY())),
                            Math.round(getRotation(angle.getZ()))
                    };
                    if (!Arrays.equals(current, actual)) {
                        currentPose.put(bodyPart, actual);
                        setBodyPartItem(player, gui, armorStand, bodyPart);
                    }
                }
            }, 40, 40);

            gui.setItem(2, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "reset.label").asGuiItem());
            AtomicInteger col = new AtomicInteger(2);
            RESET.forEach((entry, mat) -> {
                Axis axis = entry.getKey();
                boolean zero = entry.getValue();
                gui.setItem(2, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat),
                        KEY + "reset.button." + (zero ? "zero" : "default"), Map.of("%axis%", axis.toString())).asGuiItem(event -> {
                    BodyPart bodyPart = selectedBodyPart.get(uuid);
                    if (zero)
                        bodyPart.apply(armorStand, axis.setValueDegrees(bodyPart.get(armorStand), 0));
                    else
                        wrapper.resetArmorStandBodyPart(armorStand, bodyPart, axis);
                    Util.playSpyglassSound(player);

                    currentPose.get(bodyPart)[axis.ordinal()] = Math.round(getRotation(axis.getValueDegrees(bodyPart.get(armorStand))));
                    setBodyPartItem(player, gui, armorStand, bodyPart);
                }));
            });

            for (Axis axis : Axis.values()) {
                int row = switch (axis) {
                    case X -> 3;
                    case Y -> 4;
                    case Z -> 5;
                };

                gui.setItem(row, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "change.label",
                        Map.of("%axis%", axis.toString())).asGuiItem());
                col.set(2);
                (switch (axis) {
                    case X -> CHANGE_X;
                    case Y -> CHANGE_Y;
                    case Z -> CHANGE_Z;
                }).forEach((amount, mat) -> gui.setItem(row, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat), KEY + "change.button", Map.of(
                        "%axis%", axis.toString(),
                        "%amount%", String.valueOf(amount))
                ).asGuiItem(event -> {
                    BodyPart bodyPart = selectedBodyPart.get(uuid);
                    EulerAngle angle = bodyPart.get(armorStand);
                    angle = axis.setValueDegrees(angle, getRotation(axis.getValueDegrees(angle) + amount * (event.isLeftClick() ? 1 : -1)));
                    bodyPart.apply(armorStand, angle);
                    Util.playSpyglassSound(player);

                    currentPose.get(bodyPart)[axis.ordinal()] = Math.round(axis.getValueDegrees(angle));
                    setBodyPartItem(player, gui, armorStand, bodyPart);
                })));
            }
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task != null ? task::cancel : () -> {});
    }

    private void setBodyPartItem(Player player, Gui gui, ArmorStand armorStand, BodyPart bodyPart) {
        UUID uuid = player.getUniqueId();

        int col;
        Material mat;
        switch(bodyPart) {
            case HEAD:
                col = 4;
                mat = Material.DIAMOND_HELMET;
                break;
            case BODY:
                col = 5;
                mat = Material.DIAMOND_CHESTPLATE;
                break;
            case LEFT_LEG:
                col = 6;
                mat = Material.IRON_LEGGINGS;
                break;
            case RIGHT_LEG:
                col = 7;
                mat = Material.GOLDEN_LEGGINGS;
                break;
            case LEFT_ARM:
                col = 8;
                mat = Material.SHIELD;
                break;
            case RIGHT_ARM:
                col = 9;
                mat = Material.DIAMOND_SWORD;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + bodyPart);
        }

        EulerAngle angle = bodyPart.get(armorStand);
        gui.updateItem(1, col, applyNameAndLore(ItemBuilder.from(mat),
                KEY + "bodypart.name." + bodyPart.toString().toLowerCase(), KEY + "bodypart.lore", Map.of(
                        "%pose_x%", String.valueOf(getRotation(Math.round(Math.toDegrees(angle.getX())))),
                        "%pose_y%", String.valueOf(getRotation(Math.round(Math.toDegrees(angle.getY())))),
                        "%pose_z%", String.valueOf(getRotation(Math.round(Math.toDegrees(angle.getZ()))))
                )).glow(selectedBodyPart.get(uuid) == bodyPart).asGuiItem(event -> {
            BodyPart old = selectedBodyPart.get(uuid);
            if (old == bodyPart) {
                Util.playBassSound(player);
                return;
            }

            selectedBodyPart.put(uuid, bodyPart);
            setBodyPartItem(player, gui, armorStand, old);
            setBodyPartItem(player, gui, armorStand, bodyPart);
            Util.playExperienceSound(player);
        }));
    }

    private double getRotation(double rotation) {
        if (rotation > 180)
            return rotation - 360;
        if (rotation == -180)
            return 180;
        return rotation;
    }
}
