package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.PresetData;
import de.rapha149.armorstandeditor.Util;
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

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Messages.getMessage;
import static de.rapha149.armorstandeditor.Messages.getRawMessage;
import static de.rapha149.armorstandeditor.Util.*;

public class AdvancedPosePresetsPage extends Page {

    private final int PAGE_NUMBER = 3;
    private final String KEY = "armorstands.advanced_controls.pose.";

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        Gui gui = Gui.gui().title(getMessage("armorstands.advanced_controls.title",
                Map.of("%menu%", getRawMessage(KEY + "name"))).adventure()).rows(6).disableAllInteractions().create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui);

        gui.setItem(gui.getRows(), 1, applyNameAndLore(ItemBuilder.from(Material.ARROW), KEY + "back").asGuiItem(event -> {
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, PAGE_NUMBER, true));
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
        }));

        int row = 2, col = 3;
        Map<BodyPart, EulerAngle> currentPose = Arrays.stream(BodyPart.values()).collect(Collectors.toMap(part -> part, part -> roundAngle(part.get(armorStand))));

        Map<Integer, Entry<PresetData, Boolean>> presetItems = new HashMap<>();
        for (PresetData preset : Config.get().presets) {
            boolean applied = currentPose.entrySet().stream().allMatch(entry -> entry.getValue().equals(roundAngle(preset.pose.get(entry.getKey()))));

            int slot = col + (row - 1) * 9 - 1;
            presetItems.put(slot, new SimpleEntry<>(preset, applied));
            setPresetItem(player, armorStand, gui, preset, slot, applied, presetItems);

            col++;
            if (col > 7) {
                col = 3;
                row++;
                if (row > 4)
                    break;
            }
        }

        Util.addPageItems(player, armorStand, gui, PAGE_NUMBER, true);
        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status);
    }

    private void setPresetItem(Player player, ArmorStand armorStand, Gui gui, PresetData preset, int slot, boolean applied,
                               Map<Integer, Entry<PresetData, Boolean>> presetItems) {
        gui.updateItem(slot, applyNameAndLore(ItemBuilder.from(applied ? Material.WRITTEN_BOOK : Material.BOOK),
                KEY + "presets.preset", Map.of("%preset%", preset.name)).asGuiItem(event -> {
            if (applied) {
                playBassSound(player);
                return;
            }

            preset.pose.forEach((part, angle) -> part.set(armorStand, angle));
            playExperienceSound(player);

            presetItems.forEach((otherSlot, entry) -> {
                if (entry.getValue()) {
                    entry.setValue(false);
                    setPresetItem(player, armorStand, gui, entry.getKey(), otherSlot, false, presetItems);
                }
            });

            if (!presetItems.containsKey(slot))
                presetItems.put(slot, new SimpleEntry<>(preset, true));
            else
                presetItems.get(slot).setValue(true);
            setPresetItem(player, armorStand, gui, preset, slot, true, presetItems);
        }));
    }

    private EulerAngle roundAngle(EulerAngle angle) {
        return angle.setX(Math.round(angle.getX() * 100D) / 100D)
                .setY(Math.round(angle.getY() * 100D) / 100D)
                .setZ(Math.round(angle.getZ() * 100D) / 100D);
    }
}
