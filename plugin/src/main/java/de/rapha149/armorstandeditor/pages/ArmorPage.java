package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Events;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
import de.rapha149.armorstandeditor.version.BodyPart;
import de.rapha149.armorstandeditor.version.Axis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static de.rapha149.armorstandeditor.Messages.getMessage;
import static de.rapha149.armorstandeditor.Util.*;

public class ArmorPage extends Page {

    @Override
    public GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass) {
        FeaturesData features = Config.get().features;
        Gui gui = Gui.gui().title(Component.text(getMessage("armorstands.title." + (adminBypass ? "admin_bypass" : "normal")))).rows(6).create();
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui);

        setPrivateItem(player, gui, armorStand, adminBypass);

        gui.setItem(2, 2, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.GOLDEN_HELMET), "armorstands.equipment")
                .flags(ItemFlag.HIDE_ATTRIBUTES).asGuiItem(), features.replaceEquipment, player));
        EntityEquipment equipment = armorStand.getEquipment();
        ItemStack[] equipmentItems = {equipment.getHelmet(),
                equipment.getChestplate(),
                equipment.getLeggings(),
                equipment.getBoots(),
                equipment.getItemInMainHand(),
                equipment.getItemInOffHand()};

        if (isDeactivated(features.replaceEquipment, player)) {
            for (int i = 0; i < EQUIPMENT_SLOTS.size(); i++)
                gui.setItem(EQUIPMENT_SLOTS.get(i), ItemBuilder.from(equipmentItems[i]).asGuiItem(event -> playBassSound(player)));

            gui.disableAllInteractions();
        } else {
            EQUIPMENT_SLOTS.forEach(slot -> gui.setItem(slot, ItemBuilder.from(Material.AIR).asGuiItem()));
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> {
                Inventory inv = gui.getInventory();
                for (int i = 0; i < EQUIPMENT_SLOTS.size(); i++)
                    inv.setItem(EQUIPMENT_SLOTS.get(i), equipmentItems[i]);
                status.saveEquipment = true;
            });

            gui.setDragAction(event -> {
                if (!status.saveEquipment) {
                    event.setCancelled(true);
                } else {
                    for (Integer slot : event.getInventorySlots()) {
                        if (!EQUIPMENT_SLOTS.contains(slot)) {
                            event.setCancelled(true);
                            break;
                        }
                    }
                }
            });
            gui.setDefaultTopClickAction(event -> {
                if (!status.saveEquipment || !EQUIPMENT_SLOTS.contains(event.getRawSlot()))
                    event.setCancelled(true);
            });
            armorStand.getEquipment().clear();
        }

        gui.setItem(2, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.PLAYER_HEAD), "armorstands.move_body_parts.head").asGuiItem(event -> {
            if (event.isLeftClick()) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMoveBodyPart(player, armorStand, BodyPart.HEAD);
                });
            } else if (event.isRightClick()) {
                wrapper.resetArmorStandBodyPart(armorStand, BodyPart.HEAD);
                playArmorStandHitSound(player);
            }
        }), features.moveBodyParts, player));
        gui.setItem(3, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move_body_parts.right_arm").asGuiItem(event -> {
            if (event.isLeftClick()) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMoveBodyPart(player, armorStand, BodyPart.RIGHT_ARM);
                });
            } else if (event.isRightClick()) {
                wrapper.resetArmorStandBodyPart(armorStand, BodyPart.RIGHT_ARM);
                playArmorStandHitSound(player);
            }
        }), features.moveBodyParts, player));
        gui.setItem(3, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move_body_parts.body").asGuiItem(event -> {
            if (event.isLeftClick()) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMoveBodyPart(player, armorStand, BodyPart.BODY);
                });
            } else if (event.isRightClick()) {
                wrapper.resetArmorStandBodyPart(armorStand, BodyPart.BODY);
                playArmorStandHitSound(player);
            }
        }), features.moveBodyParts, player));
        gui.setItem(3, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move_body_parts.left_arm").asGuiItem(event -> {
            if (event.isLeftClick()) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMoveBodyPart(player, armorStand, BodyPart.LEFT_ARM);
                });
            } else if (event.isRightClick()) {
                wrapper.resetArmorStandBodyPart(armorStand, BodyPart.LEFT_ARM);
                playArmorStandHitSound(player);
            }
        }), features.moveBodyParts, player));
        gui.setItem(4, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move_body_parts.right_leg").asGuiItem(event -> {
            if (event.isLeftClick()) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMoveBodyPart(player, armorStand, BodyPart.RIGHT_LEG);
                });
            } else if (event.isRightClick()) {
                wrapper.resetArmorStandBodyPart(armorStand, BodyPart.RIGHT_LEG);
                playArmorStandHitSound(player);
            }
        }), features.moveBodyParts, player));
        gui.setItem(4, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move_body_parts.left_leg").asGuiItem(event -> {
            if (event.isLeftClick()) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMoveBodyPart(player, armorStand, BodyPart.LEFT_LEG);
                });
            } else if (event.isRightClick()) {
                wrapper.resetArmorStandBodyPart(armorStand, BodyPart.LEFT_LEG);
                playArmorStandHitSound(player);
            }
        }), features.moveBodyParts, player));

        gui.setItem(4, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.FEATHER), "armorstands.move_position").asGuiItem(event ->
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startMovePosition(player, armorStand, event.isRightClick());
                })), features.movePosition, player));

        gui.setItem(5, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.RED_DYE), "armorstands.move_position.x").asGuiItem(event -> {
            if (event.getClick() == ClickType.DROP) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startSnapInMovePosition(player, armorStand, Axis.X);
                });
            } else {
                armorStand.teleport(armorStand.getLocation().add(event.isLeftClick() ? 0.05 : -0.05, 0, 0));
                playStepSound(player);
            }
        }), features.movePosition, player));
        gui.setItem(5, 7, checkDeactivated(applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.LIME_DYE),
                getMessage("armorstands.move_position.y.name") + (armorStand.hasGravity() ? getMessage("armorstands.move_position.y.gravity_warning") : ""),
                getMessage("armorstands.move_position.y.lore")).asGuiItem(event -> {
            if (event.getClick() == ClickType.DROP) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startSnapInMovePosition(player, armorStand, Axis.Y);
                });
            } else {
                armorStand.teleport(armorStand.getLocation().add(0, event.isLeftClick() ? 0.05 : -0.05, 0));
                playStepSound(player);
            }
        }), features.movePosition, player));
        gui.setItem(5, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.BLUE_DYE), "armorstands.move_position.z").asGuiItem(event -> {
            if (event.getClick() == ClickType.DROP) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startSnapInMovePosition(player, armorStand, Axis.Z);
                });
            } else {
                armorStand.teleport(armorStand.getLocation().add(0, 0, event.isLeftClick() ? 0.05 : -0.05));
                playStepSound(player);
            }
        }), features.movePosition, player));

        gui.setItem(2, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.ENDER_EYE), "armorstands.rotate",
                Map.of("%rotation%", rotationToString(armorStand.getLocation().getYaw()))).asGuiItem(event -> {
            if (event.getClick() == ClickType.DROP) {
                Util.runOneTimeItemClickAction(status, () -> {
                    gui.close(player);
                    Events.startRotationMovement(player, armorStand);
                });
            } else {
                if (event.getClick() == ClickType.CONTROL_DROP) {
                    armorStand.setRotation(0, armorStand.getLocation().getPitch());
                    playExperienceSound(player);
                } else {
                    int amount = event.isShiftClick() ? 10 : 45;
                    if (event.isRightClick())
                        amount *= -1;
                    armorStand.setRotation(getRotation(armorStand.getLocation().getYaw() + amount), armorStand.getLocation().getPitch());
                    playStepSound(player);
                }

                gui.updateItem(2, 8, applyNameAndLore(ItemBuilder.from(Material.ENDER_EYE), "armorstands.rotate",
                        Map.of("%rotation%", rotationToString(armorStand.getLocation().getYaw()))).build());
            }
        }), features.rotate, player));

        {
            GuiItem item = applyNameAndLore(ItemBuilder.from(Material.ENDER_PEARL), "armorstands.advanced_controls.open").asGuiItem(event -> {
                int newPage;
                if (event.getClick() == ClickType.DROP)
                    newPage = 3;
                else
                    newPage = event.isLeftClick() ? 1 : 2;

                int originalPage = newPage;
                FeaturesData features1 = Config.get().features;
                while (isDeactivated(switch (newPage) {
                    case 1 -> features1.movePosition;
                    case 2 -> features1.rotate;
                    case 3 -> features1.moveBodyParts;
                    default -> throw new IllegalStateException("Unexpected value: " + newPage);
                }, player)) {
                    newPage++;
                    if (newPage > 3)
                        newPage = 1;
                    if (newPage == originalPage) {
                        playBassSound(player);
                        return;
                    }
                }

                int finalNewPage = newPage;
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, finalNewPage, true));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            });

            AtomicInteger deactivatedStatus = new AtomicInteger(2);
            List.of(features.movePosition, features.rotate, features.moveBodyParts).forEach(feature -> {
                int i = getDeactivatedStatus(feature, player);
                if (i < deactivatedStatus.get())
                    deactivatedStatus.set(i);
            });
            if (deactivatedStatus.get() != 0)
                gui.setItem(2, 6, replaceWithDeactivatedItem(item, player, deactivatedStatus.get(), false));
            else
                gui.setItem(2, 6, checkDeactivated(item, features.advancedControls, player));
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        return new GuiResult(gui, status, () -> saveEquipment(status));
    }

    private void setPrivateItem(Player player, Gui gui, ArmorStand armorStand, boolean adminBypass) {
        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        UUID uuid;
        try {
            uuid = pdc.has(PRIVATE_KEY, PersistentDataType.STRING) ? UUID.fromString(pdc.get(PRIVATE_KEY, PersistentDataType.STRING)) : null;
        } catch (IllegalArgumentException e) {
            uuid = null;
        }

        boolean locked = uuid != null;
        String name;
        if (locked) {
            Player onlineTarget = Bukkit.getPlayer(uuid);
            if (onlineTarget != null)
                name = onlineTarget.getName();
            else {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(uuid);
                name = offlineTarget.hasPlayedBefore() ? offlineTarget.getName() : uuid.toString();
            }
        } else
            name = null;

        gui.updateItem(1, 1, checkDeactivated(applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.SHULKER_SHELL), getMessage("armorstands.private.name"),
                getMessage("armorstands.private.lore." + (adminBypass ? "admin_bypass" : "normal")).replace("%player%", locked ?
                        getMessage("armorstands.private.player").replace("%player%", name) : ""), locked).glow(locked).asGuiItem(event -> {
            playSpyglassSound(player);
            if (locked)
                pdc.remove(PRIVATE_KEY);
            else
                pdc.set(PRIVATE_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

            setPrivateItem(player, gui, armorStand, adminBypass);
        }), Config.get().features.privateArmorstand, player));
    }

    private float getRotation(float rotation) {
        if (rotation > 180)
            return rotation - 360;
        if (rotation == -180)
            return 180;
        return rotation;
    }

    private String rotationToString(float rotation) {
        String str = String.valueOf(getRotation(Math.round(rotation * 100F) / 100F));
        return str.endsWith(".0") ? str.substring(0, str.length() - 2) : str;
    }
}
