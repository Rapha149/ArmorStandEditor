package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Events;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.wesjd.anvilgui.AnvilGUI.Builder;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ArmorStand.LockType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class SettingsPage extends Page {

    private final int PAGE_NUMBER = 2;

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        FeaturesData features = Config.get().features;
        Gui gui = Gui.gui().title(Component.text(getMessage("armorstands.title." + (adminBypass ? "admin_bypass" : "normal"))))
                .rows(6).disableAllInteractions().create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui, PAGE_NUMBER, false);

        List<EquipmentSlot> disabled = Arrays.stream(EquipmentSlot.values()).filter(slot -> Arrays.stream(LockType.values())
                .allMatch(type -> armorStand.hasEquipmentLock(slot, type))).collect(Collectors.toList());
        boolean[] settings = getSettings(armorStand);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            boolean update = false;
            List<EquipmentSlot> currentDisabled = Arrays.stream(EquipmentSlot.values()).filter(slot -> Arrays.stream(LockType.values())
                    .allMatch(type -> armorStand.hasEquipmentLock(slot, type))).toList();
            if (!currentDisabled.equals(disabled)) {
                setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.HEAD, currentDisabled.contains(EquipmentSlot.HEAD));
                setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.CHEST, currentDisabled.contains(EquipmentSlot.CHEST));
                setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.LEGS, currentDisabled.contains(EquipmentSlot.LEGS));
                setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.FEET, currentDisabled.contains(EquipmentSlot.FEET));
                setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.HAND, currentDisabled.contains(EquipmentSlot.HAND));
                setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.OFF_HAND, currentDisabled.contains(EquipmentSlot.OFF_HAND));

                disabled.clear();
                disabled.addAll(currentDisabled);
                update = true;
            }

            boolean[] currentSettings = getSettings(armorStand);
            if (!Arrays.equals(currentSettings, settings)) {
                for (int i = 0; i < currentSettings.length; i++)
                    setSettingsItem(player, gui, armorStand, i, currentSettings[i]);

                System.arraycopy(currentSettings, 0, settings, 0, currentSettings.length);
                update = true;
            }

            if (update)
                gui.update();
        }, 40, 40);

        setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.HEAD, disabled.contains(EquipmentSlot.HEAD));
        setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.CHEST, disabled.contains(EquipmentSlot.CHEST));
        setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.LEGS, disabled.contains(EquipmentSlot.LEGS));
        setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.FEET, disabled.contains(EquipmentSlot.FEET));
        setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.HAND, disabled.contains(EquipmentSlot.HAND));
        setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.OFF_HAND, disabled.contains(EquipmentSlot.OFF_HAND));

        for (int i = 0; i < settings.length; i++)
            setSettingsItem(player, gui, armorStand, i, settings[i]);

        gui.setItem(5, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.ARMOR_STAND), "armorstands.give_item").asGuiItem(event -> {
            PlayerInventory inv = player.getInventory();
            if (inv.firstEmpty() == -1) {
                Util.playAnvilSound(player);
                return;
            }

            gui.close(player);
            ItemStack item = wrapper.getArmorstandItem(armorStand, Util.PRIVATE_KEY);
            armorStand.remove();
            inv.addItem(item);
            Util.playArmorStandBreakSound(player);
        }), features.giveItem, player));

        setRenameItem(player, armorStand, gui);

        gui.setItem(4, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.LEAD), "armorstands.vehicle")
                .glow(!armorStand.getPassengers().isEmpty()).asGuiItem(event -> {
                    if (event.isLeftClick()) {
                        gui.close(player);
                        if (Events.isPlayerDoingSomethingOutsideOfInv(player)) {
                            player.sendMessage(getMessage("not_possible_now"));
                            return;
                        }

                        Events.vehicleSelection.put(player, Map.entry(armorStand, false));
                        Events.runTask();
                    } else if (event.isRightClick()) {
                        if (armorStand.eject()) {
                            Util.playExperienceSound(player);
                            gui.updateItem(4, 8, applyNameAndLore(ItemBuilder.from(Material.SADDLE), "armorstands.vehicle").glow(false).build());
                        } else
                            Util.playBassSound(player);
                    }
                }), features.vehicle, player));

        gui.setItem(5, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.SADDLE), "armorstands.passenger")
                .glow(armorStand.isInsideVehicle()).asGuiItem(event -> {
                    if (event.isLeftClick()) {
                        gui.close(player);
                        if (Events.isPlayerDoingSomethingOutsideOfInv(player)) {
                            player.sendMessage(getMessage("not_possible_now"));
                            return;
                        }

                        Events.vehicleSelection.put(player, Map.entry(armorStand, true));
                        Events.runTask();
                    } else if (event.isRightClick()) {
                        if (armorStand.leaveVehicle()) {
                            Util.playExperienceSound(player);
                            gui.updateItem(5, 8, applyNameAndLore(ItemBuilder.from(Material.MINECART), "armorstands.passenger").glow(false).build());
                        } else
                            Util.playBassSound(player);
                    }
                }), features.passenger, player));

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, task::cancel);
    }

    private void setDisabledSlotItem(Player player, Gui gui, ArmorStand armorStand, EquipmentSlot slot, boolean disabled) {
        {
            int row;
            int col;
            String key;
            switch (slot) {
                case HEAD:
                    row = 2;
                    col = 3;
                    key = "helmet";
                    break;
                case CHEST:
                    row = 3;
                    col = 3;
                    key = "chestplate";
                    break;
                case LEGS:
                    row = 4;
                    col = 3;
                    key = "leggings";
                    break;
                case FEET:
                    row = 5;
                    col = 3;
                    key = "boots";
                    break;
                case HAND:
                    row = 3;
                    col = 2;
                    key = "mainhand";
                    break;
                case OFF_HAND:
                    row = 3;
                    col = 4;
                    key = "offhand";
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + slot);
            }

            gui.updateItem(row, col, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.HONEYCOMB), "armorstands.lock." + key,
                    "armorstands.lock.lore", disabled).glow(disabled).asGuiItem(event -> {
                Util.playSpyglassSound(player);
                if (disabled)
                    for (LockType type : LockType.values())
                        armorStand.removeEquipmentLock(slot, type);
                else
                    for (LockType type : LockType.values())
                        armorStand.addEquipmentLock(slot, type);

                setDisabledSlotItem(player, gui, armorStand, slot, !disabled);
            }), Config.get().features.disabledSlots, player));
        }
    }

    private void setSettingsItem(Player player, Gui gui, ArmorStand armorStand, int index, boolean enabled) {
        int row, col;
        Material mat;
        String key;
        FeatureData feature;
        FeaturesData features = Config.get().features;
        switch (index) {
            case 0:
                row = 2;
                col = 6;
                mat = Material.GLASS;
                key = "invisible";
                feature = features.invisibility;
                break;
            case 1:
                row = 3;
                col = 6;
                mat = Material.STICK;
                key = "show_arms";
                feature = features.showArms;
                break;
            case 2:
                row = 4;
                col = 6;
                mat = Material.STONE_SLAB;
                key = "base_plate";
                feature = features.basePlate;
                break;
            case 3:
                row = 2;
                col = 7;
                mat = Material.GOLDEN_APPLE;
                key = "invulnerable";
                feature = features.invulnerability;
                break;
            case 4:
                row = 3;
                col = 7;
                mat = Material.PHANTOM_MEMBRANE;
                key = "gravity";
                feature = features.gravity;
                break;
            case 5:
                row = 4;
                col = 7;
                mat = Material.TOTEM_OF_UNDYING;
                key = "small";
                feature = features.small;
                break;
            case 6:
                row = 2;
                col = 8;
                mat = Material.GLOWSTONE_DUST;
                key = "glowing";
                feature = features.glowing;
                break;
            case 7:
                row = 3;
                col = 8;
                mat = Material.CAMPFIRE;
                key = "fire";
                feature = features.fire;
                break;
            default:
                throw new IllegalArgumentException("Parameter \"index\" out of range.");
        }

        gui.updateItem(row, col, checkDeactivated(applyNameAndLore(ItemBuilder.from(mat), "armorstands.settings." + key, enabled).glow(enabled).asGuiItem(event -> {
            Util.playSpyglassSound(player);

            boolean newEnabled = !enabled;
            switch (index) {
                case 0 -> armorStand.setInvisible(newEnabled);
                case 1 -> armorStand.setArms(newEnabled);
                case 2 -> armorStand.setBasePlate(newEnabled);
                case 3 -> armorStand.setInvulnerable(newEnabled);
                case 4 -> armorStand.setGravity(newEnabled);
                case 5 -> armorStand.setSmall(newEnabled);
                case 6 -> armorStand.setGlowing(newEnabled);
                case 7 -> armorStand.setVisualFire(newEnabled);
            }

            setSettingsItem(player, gui, armorStand, index, newEnabled);
        }), feature, player));
    }

    private void setRenameItem(Player player, ArmorStand armorStand, Gui gui) {
        Component customNameDisplay = wrapper.getCustomNameForDisplay(armorStand);
        if (customNameDisplay == null)
            customNameDisplay = Component.text("§c---");
        List<Component> lore = new ArrayList<>();
        for (String line : getMessage("armorstands.rename.lore").split("\n")) {
            if (!line.contains("%name%"))
                lore.add(Component.text(line));
            else {
                String[] split = line.split("%name%");
                net.kyori.adventure.text.TextComponent.Builder component = Component.text().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
                for (int i = 0; i < split.length; i++) {
                    component.append(Component.text(split[i]));
                    if (line.endsWith("%name%") || i != split.length - 1)
                        component.append(customNameDisplay);
                }
                lore.add(component.asComponent());
            }
        }

        gui.updateItem(5, 7, checkDeactivated(ItemBuilder.from(Material.NAME_TAG).name(Component.text(getMessage("armorstands.rename.name"))).lore(lore).asGuiItem(event -> {
            if (event.isLeftClick()) {
                String customNameEdit = wrapper.getCustomNameForEdit(armorStand);
                String name = customNameEdit != null && !customNameEdit.isEmpty() ? customNameEdit : "Name...";
                long time = System.currentTimeMillis();
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> Util.anvilInvs.put(time, new Builder().plugin(ArmorStandEditor.getInstance())
                        .title(getMessage("armorstands.rename.name"))
                        .text(name.substring(0, Math.min(50, name.length())))
                        .onClose(p -> {
                            if (!Util.disabling) {
                                Util.anvilInvs.remove(time);
                                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> Util.openGUI(player, armorStand, PAGE_NUMBER, false));
                            }
                        }).onComplete(completion -> {
                            wrapper.setCustomName(armorStand, completion.getText());
                            armorStand.setCustomNameVisible(true);
                            Util.anvilInvs.remove(time);
                            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> Util.openGUI(player, armorStand, PAGE_NUMBER, false));
                            return Arrays.asList(ResponseAction.run(() -> {}));
                        }).open(player)));
            } else if (event.isRightClick()) {
                wrapper.setCustomName(armorStand, null);
                armorStand.setCustomNameVisible(false);
                setRenameItem(player, armorStand, gui);
            }
        }), Config.get().features.rename, player));
    }

    private boolean[] getSettings(ArmorStand armorStand) {
        return new boolean[]{armorStand.isInvisible(),
                armorStand.hasArms(),
                armorStand.hasBasePlate(),
                armorStand.isInvulnerable(),
                armorStand.hasGravity(),
                armorStand.isSmall(),
                armorStand.isGlowing(),
                armorStand.isVisualFire()};
    }
}
