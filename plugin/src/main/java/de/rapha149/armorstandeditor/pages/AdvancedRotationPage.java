package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
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
import static de.rapha149.armorstandeditor.Util.*;

public class AdvancedRotationPage extends Page {

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
        put(1, Material.RED_WOOL);
        put(2, Material.ORANGE_WOOL);
        put(5, Material.YELLOW_WOOL);
        put(10, Material.LIME_WOOL);
        put(15, Material.LIGHT_BLUE_WOOL);
        put(30, Material.BLUE_WOOL);
        put(45, Material.MAGENTA_WOOL);
        put(90, Material.PURPLE_WOOL);
    }};

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        Gui gui = Gui.gui().title(Component.text(getMessage("armorstands.advanced_controls.title")
                .replace("%menu%", getMessage(KEY + "name")))).rows(6).disableAllInteractions().create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui);

        gui.setItem(gui.getRows(), 1, applyNameAndLore(ItemBuilder.from(Material.ARROW), "armorstands.advanced_controls.leave").asGuiItem(event -> {
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, 1, false));
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
        }));

        AtomicInteger currentRotation = new AtomicInteger(Math.round(armorStand.getLocation().getYaw()));
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
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
            playStepSound(player);

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
            playStepSound(player);

            currentRotation.set(Math.round(loc.getYaw()));
            setCurrentRotationItem(gui, armorStand);
        })));

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task::cancel);
    }

    private void setCurrentRotationItem(Gui gui, ArmorStand armorStand) {
        String rotation = String.valueOf(Math.round(getRotation(armorStand.getLocation().getYaw()) * 100F) / 100F);
        if (rotation.endsWith(".0"))
            rotation = rotation.substring(0, rotation.length() - 2);

        gui.updateItem(1, 9, applyNameAndLore(ItemBuilder.from(Material.ENDER_EYE), KEY + "current",
                Map.of("%rotation%", rotation)).asGuiItem());
    }

    private float getRotation(float rotation) {
        if (rotation > 180)
            return rotation - 360;
        if (rotation == -180)
            return 180;
        return rotation;
    }
}
