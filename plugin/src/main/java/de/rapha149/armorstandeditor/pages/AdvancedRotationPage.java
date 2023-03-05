package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class AdvancedRotationPage extends Page {

    private final int PAGE_NUMBER = 2;
    private final String KEY = "armorstands.advanced_controls.rotation.";

    private final Map<Integer, Material> SET = new LinkedHashMap<>() {{
        put(0, Material.RED_DYE);
        put(45, Material.ORANGE_DYE);
        put(90, Material.YELLOW_DYE);
        put(135, Material.LIME_DYE);
        put(180, Material.LIGHT_BLUE_DYE);
        put(-135, Material.BLUE_DYE);
        put(-90, Material.MAGENTA_DYE);
        put(-45, Material.PURPLE_DYE);
    }};
    private final Map<Integer, Material> CHANGE = new LinkedHashMap<>() {{
        put(1, Material.SUNFLOWER);
        put(2, Material.FIREWORK_STAR);
        put(5, Material.SNOWBALL);
        put(10, Material.SLIME_BALL);
        put(15, Material.ENDER_PEARL);
        put(30, Material.FIRE_CHARGE);
        put(45, Material.ENDER_EYE);
        put(90, Material.HEART_OF_THE_SEA);
    }};

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
            gui.setItem(3, 5, applyNameAndLoreWithoutKeys(ItemBuilder.from(getDeactivatedMaterial(Material.ENDER_EYE)),
                    getMessage("armorstands.advanced_controls.deactivated").replace("%menu%", getMessage(KEY + "name")),
                    getMessage("armorstands.features." + (deactivatedStatus == 1 ? "deactivated" : "no_permission"))).asGuiItem());
        } else {
            AtomicInteger currentRotation = new AtomicInteger(Math.round(armorStand.getLocation().getYaw()));
            task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                int rotation = Math.round(getRotation(armorStand.getLocation().getYaw()));
                if (currentRotation.get() != rotation) {
                    currentRotation.set(rotation);
                    setCurrentRotationItem(gui, armorStand);
                }
            }, 40, 40);

            setCurrentRotationItem(gui, armorStand);

            gui.setItem(3, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "set.label").asGuiItem());
            AtomicInteger col = new AtomicInteger(2);
            SET.forEach((rotation, mat) -> gui.setItem(3, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat), KEY + "set.button", Map.of(
                    "%value%", String.valueOf(rotation),
                    "%alternative_value%", String.valueOf(rotation == 0 ? 360 : rotation + 360 * (rotation > 0 ? -1 : 1))
            )).asGuiItem(event -> {
                Location loc = armorStand.getLocation();
                loc.setYaw(rotation);
                armorStand.teleport(loc);
                Util.playStepSound(player);

                currentRotation.set(rotation);
                setCurrentRotationItem(gui, armorStand);
            })));

            gui.setItem(4, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "change.label").asGuiItem());
            col.set(2);
            CHANGE.forEach((amount, mat) -> gui.setItem(4, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat),
                    KEY + "change.button", Map.of("%amount%", String.valueOf(amount))).asGuiItem(event -> {
                Location loc = armorStand.getLocation();
                loc.setYaw(getRotation(loc.getYaw() + amount * (event.isLeftClick() ? 1 : -1)));
                armorStand.teleport(loc);
                Util.playStepSound(player);

                currentRotation.set(Math.round(loc.getYaw()));
                setCurrentRotationItem(gui, armorStand);
            })));
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task != null ? task::cancel : () -> {});
    }

    private void setCurrentRotationItem(Gui gui, ArmorStand armorStand) {
        gui.updateItem(1, 9, applyNameAndLore(ItemBuilder.from(Material.ENDER_EYE), KEY + "current",
                Map.of("%rotation%", String.valueOf(Math.round(getRotation(armorStand.getLocation().getYaw()))))).asGuiItem());
    }

    private float getRotation(float rotation) {
        if (rotation > 180)
            return rotation - 360;
        if (rotation == -180)
            return 180;
        return rotation;
    }
}
