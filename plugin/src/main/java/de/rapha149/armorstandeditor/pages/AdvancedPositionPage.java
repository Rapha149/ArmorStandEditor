package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
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
import java.util.function.Consumer;
import java.util.function.Function;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class AdvancedPositionPage extends Page {

    private final int PAGE_NUMBER = 1;
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
        FeaturesData features = Config.get().features;
        int deactivatedStatus = getDeactivatedStatus(features.advancedControls, player);
        if (deactivatedStatus == 0)
            deactivatedStatus = getDeactivatedStatus(features.movePosition, player);

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
            gui.setItem(3, 5, applyNameAndLoreWithoutKeys(ItemBuilder.from(getDeactivatedMaterial(Material.ARMOR_STAND)),
                    getMessage("armorstands.advanced_controls.deactivated").replace("%menu%", getMessage(KEY + "name")),
                    getMessage("armorstands.features." + (deactivatedStatus == 1 ? "deactivated" : "no_permission"))).asGuiItem());
        } else {
            double[] currentLoc = new double[3];
            Consumer<Location> updateLoc = loc -> {
                currentLoc[0] = loc.getX();
                currentLoc[1] = loc.getY();
                currentLoc[2] = loc.getZ();
            };
            task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                Location loc = armorStand.getLocation();
                if (!Arrays.equals(currentLoc, new double[]{loc.getX(), loc.getY(), loc.getZ()})) {
                    updateLoc.accept(loc);
                    setCurrentPositionItem(gui, armorStand);
                }
            }, 40, 40);

            setCurrentPositionItem(gui, armorStand);

            gui.setItem(2, 1, applyNameAndLore(ItemBuilder.from(Material.PAPER), KEY + "align.label").asGuiItem());
            AtomicInteger col = new AtomicInteger(2);
            ALIGN.forEach((axis, mat) -> gui.setItem(2, col.getAndIncrement(), applyNameAndLore(ItemBuilder.from(mat),
                    KEY + "align.button", Map.of("%axis%", axis.toString())).asGuiItem(event -> {
                Location loc = armorStand.getLocation();
                axis.setValue(loc, axis.getBlockValue(loc) + (axis == Axis.Y ? 0 : 0.5));
                armorStand.teleport(loc);
                Util.playStepSound(player);

                updateLoc.accept(loc);
                setCurrentPositionItem(gui, armorStand);
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
                    axis.setValue(loc, axis.getValue(loc) + amount * (event.isLeftClick() ? 1 : -1));
                    armorStand.teleport(loc);
                    Util.playStepSound(player);

                    updateLoc.accept(loc);
                    setCurrentPositionItem(gui, armorStand);
                })));
            }
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task != null ? task::cancel : () -> {});
    }

    private void setCurrentPositionItem(Gui gui, ArmorStand armorStand) {
        Location loc = armorStand.getLocation();
        Function<Double, Double> round = d -> Math.round(d * 100.0) / 100.0;

        gui.updateItem(1, 9, applyNameAndLore(ItemBuilder.from(Material.ARMOR_STAND), KEY + "current", Map.of(
                "%position_x%", String.valueOf(round.apply(loc.getX())),
                "%position_y%", String.valueOf(round.apply(loc.getY())),
                "%position_z%", String.valueOf(round.apply(loc.getZ()))
        )).asGuiItem());
    }
}
