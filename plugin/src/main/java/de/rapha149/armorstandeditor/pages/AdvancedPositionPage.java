package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.version.Axis;
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static de.rapha149.armorstandeditor.Messages.getMessage;
import static de.rapha149.armorstandeditor.Messages.getRawMessage;
import static de.rapha149.armorstandeditor.Util.*;

public class AdvancedPositionPage extends Page {

    private final String KEY = "armorstands.advanced_controls.position.";

    private final Map<Axis, Material> ALIGN = new LinkedHashMap<>() {{
        put(Axis.X, Material.RED_CONCRETE);
        put(Axis.Y, Material.LIME_CONCRETE);
        put(Axis.Z, Material.BLUE_CONCRETE);
    }};
    private final Map<Double, Material> MOVE_X = new LinkedHashMap<>() {{
        put(0.01D, Material.RED_CARPET);
        put(0.05D, Material.RED_DYE);
        put(0.1D, Material.REDSTONE);
        put(0.2D, Material.MOOSHROOM_SPAWN_EGG);
        put(0.5D, Material.RED_STAINED_GLASS);
        put(1D, Material.RED_WOOL);
        put(2D, Material.RED_CONCRETE_POWDER);
        put(5D, Material.RED_CONCRETE);
    }};
    private final Map<Double, Material> MOVE_Y = new LinkedHashMap<>() {{
        put(0.01D, Material.LIME_CARPET);
        put(0.05D, Material.LIME_DYE);
        put(0.1D, Material.SLIME_BALL);
        put(0.2D, Material.CREEPER_SPAWN_EGG);
        put(0.5D, Material.LIME_STAINED_GLASS);
        put(1D, Material.LIME_WOOL);
        put(2D, Material.LIME_CONCRETE_POWDER);
        put(5D, Material.LIME_CONCRETE);
    }};
    private final Map<Double, Material> MOVE_Z = new LinkedHashMap<>() {{
        put(0.01D, Material.BLUE_CARPET);
        put(0.05D, Material.BLUE_DYE);
        put(0.1D, Material.LAPIS_LAZULI);
        put(0.2D, Material.SQUID_SPAWN_EGG);
        put(0.5D, Material.BLUE_STAINED_GLASS);
        put(1D, Material.BLUE_WOOL);
        put(2D, Material.BLUE_CONCRETE_POWDER);
        put(5D, Material.BLUE_CONCRETE);
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

        double[] currentLoc = new double[3];
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            Location loc = armorStand.getLocation();
            double[] actual = new double[]{loc.getX(), loc.getY(), loc.getZ()};
            if (!Arrays.equals(currentLoc, actual)) {
                System.arraycopy(actual, 0, currentLoc, 0, actual.length);
                setCurrentPositionItem(gui, armorStand);
            }
        }, 40, 40);

        setCurrentPositionItem(gui, armorStand);

        gui.setItem(2, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "align.label").asGuiItem());
        AtomicInteger col = new AtomicInteger(2);
        ALIGN.forEach((axis, mat) -> gui.setItem(2, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat),
                KEY + "align.button", Map.of("%axis%", axis.toString())).asGuiItem(event -> {
            Location loc = armorStand.getLocation();
            double value = axis.getBlockValue(loc) + (axis == Axis.Y ? 0 : 0.5);
            axis.setValue(loc, value);
            if (teleportArmorStand(player, armorStand, loc)) {
                playStepSound(player);
                currentLoc[axis.ordinal()] = value;
                setCurrentPositionItem(gui, armorStand);
            } else {
                player.spigot().sendMessage(getMessage("armorstands.move_position.too_far").spigot());
                playBassSound(player);
            }
        })));

        for (Axis axis : Axis.values()) {
            int row = switch (axis) {
                case X -> 3;
                case Y -> 4;
                case Z -> 5;
            };

            gui.setItem(row, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "move.label",
                    Map.of("%axis%", axis.toString())).asGuiItem());
            col.set(2);
            (switch (axis) {
                case X -> MOVE_X;
                case Y -> MOVE_Y;
                case Z -> MOVE_Z;
            }).forEach((amount, mat) -> gui.setItem(row, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat), KEY + "move.button", Map.of(
                    "%axis%", axis.toString(),
                    "%amount%", String.valueOf(amount))
            ).asGuiItem(event -> {
                Location loc = armorStand.getLocation();
                double value = axis.getValue(loc) + amount * (event.isLeftClick() ? 1 : -1);
                axis.setValue(loc, value);
                if (teleportArmorStand(player, armorStand, loc)) {
                    playStepSound(player);
                    currentLoc[axis.ordinal()] = value;
                    setCurrentPositionItem(gui, armorStand);
                } else {
                    player.spigot().sendMessage(getMessage("armorstands.move_position.too_far").spigot());
                    playBassSound(player);
                }
            })));
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task::cancel);
    }

    private void setCurrentPositionItem(Gui gui, ArmorStand armorStand) {
        Location loc = armorStand.getLocation();
        Function<Double, Double> round = d -> Math.round(d * 100.0) / 100.0;

        gui.updateItem(1, 9, applyNameAndLore(ItemBuilder.from(Material.FEATHER), KEY + "current", Map.of(
                "%position_x%", String.valueOf(round.apply(loc.getX())),
                "%position_y%", String.valueOf(round.apply(loc.getY())),
                "%position_z%", String.valueOf(round.apply(loc.getZ()))
        )).asGuiItem());
    }
}
