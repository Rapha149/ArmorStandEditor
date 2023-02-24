package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Config.FeaturesData.ReplaceEquipmentFeatureData;
import de.rapha149.armorstandeditor.Config.PermissionsData;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandBodyPartMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandPositionMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandPositionMovement.ArmorStandPositionSnapInMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandRotationMovement;
import de.rapha149.armorstandeditor.version.BodyPart;
import de.rapha149.armorstandeditor.version.Direction;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.Builder;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import org.bukkit.*;
import org.bukkit.Particle.DustOptions;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ArmorStand.LockType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class Events implements Listener, Runnable {

    private VersionWrapper wrapper = ArmorStandEditor.getInstance().wrapper;

    private final NamespacedKey PRIVATE_KEY = NamespacedKey.fromString("private", ArmorStandEditor.getInstance());
    private final String INVISIBLE_TAG = "ArmorStandEditor-Invisible";
    private final List<Double> SNAP_IN_DISTANCES = List.of(0.5D, 1.0D, 1.5D, 2D, 3D, 4D, 5D);

    private Map<UUID, Long> armorItemsCooldown = new HashMap<>();
    private Map<Player, ArmorStandMovement> moving = new HashMap<>();
    private Map<Player, Entry<ArmorStand, Boolean>> vehicleSelection = new HashMap<>();

    private Map<Player, ArmorStandStatus> invs = new HashMap<>();
    private Map<Long, AnvilGUI> anvilInvs = new HashMap<>();
    private boolean disabling = false;

    public Events() {
        Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), this, 0, 20);
    }

    public void onDisable() {
        disabling = true;
        invs.values().forEach(status -> status.player.closeInventory());
        invs.clear();
        anvilInvs.values().forEach(AnvilGUI::closeInventory);
        anvilInvs.clear();
        moving.values().forEach(this::cancelMovement);
        moving.clear();
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand))
            return;

        if (armorStand.removeScoreboardTag(INVISIBLE_TAG))
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> armorStand.setInvisible(true));
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        onInteraction(event);
        if (event.isCancelled())
            return;

        if (!(event.getRightClicked() instanceof ArmorStand armorStand))
            return;

        Player player = event.getPlayer();
        if (player.isSneaking()) {
            String key = Config.get().advancement;
            if (key != null) {
                Advancement advancement = Bukkit.getAdvancement(NamespacedKey.fromString(key));
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    if (!progress.isDone())
                        progress.getRemainingCriteria().forEach(progress::awardCriteria);
                }
            }

            event.setCancelled(true);
            openGUI(player, armorStand, true);
        } else if (player.getInventory().getItem(event.getHand()).getType() == Material.NAME_TAG)
            armorStand.setCustomNameVisible(true);
    }

    @EventHandler
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        invs.values().forEach(status -> {
            if (!status.pageWithArmor || !armorStand.getUniqueId().equals(status.armorStand.getUniqueId()))
                return;

            ItemStack[] equipment = getEquipment(armorStand, status.player);
            status.gui.setItem(2, 3, ItemBuilder.from(equipment[0]).asGuiItem());
            status.gui.setItem(3, 3, ItemBuilder.from(equipment[1]).asGuiItem());
            status.gui.setItem(4, 3, ItemBuilder.from(equipment[2]).asGuiItem());
            status.gui.setItem(5, 3, ItemBuilder.from(equipment[3]).asGuiItem());
            status.gui.setItem(3, 2, ItemBuilder.from(equipment[4]).asGuiItem());
            status.gui.setItem(3, 4, ItemBuilder.from(equipment[5]).asGuiItem());
            status.gui.update();
        });
    }

    private boolean isArmorStandUsed(Player exclude, ArmorStand armorStand) {
        UUID uuid = exclude.getUniqueId();
        UUID armorStandUuid = armorStand.getUniqueId();
        return invs.entrySet().stream().anyMatch(entry -> !entry.getKey().getUniqueId().equals(uuid) &&
                                                          entry.getValue().armorStand.getUniqueId().equals(armorStandUuid)) ||
               moving.entrySet().stream().anyMatch(entry -> !entry.getKey().getUniqueId().equals(uuid) &&
                                                            entry.getValue().armorStand.getUniqueId().equals(armorStandUuid)) ||
               vehicleSelection.entrySet().stream().anyMatch(entry -> !entry.getKey().getUniqueId().equals(uuid) &&
                                                                      entry.getValue().getKey().getUniqueId().equals(armorStandUuid));
    }

    private void openGUI(Player player, ArmorStand armorStand, boolean pageWithArmor) {
        PermissionsData permissions = Config.get().permissions;
        if (permissions.general != null && !player.hasPermission(permissions.general)) {
            player.closeInventory();
            player.sendMessage(getMessage("no_permission"));
            playBassSound(player);
            return;
        }

        boolean adminBypass = false;
        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        if (pdc.has(PRIVATE_KEY, PersistentDataType.STRING) && !pdc.get(PRIVATE_KEY, PersistentDataType.STRING).equals(player.getUniqueId().toString())) {
            if (permissions.ignorePrivate != null && !player.hasPermission(permissions.ignorePrivate)) {
                player.sendMessage(getMessage("armorstands.no_permission"));
                return;
            }
            adminBypass = true;
        }

        if (isArmorStandUsed(player, armorStand)) {
            player.closeInventory();
            player.sendMessage(getMessage("armorstands.already_open"));
            playBassSound(player);
            return;
        }

        Gui gui = Gui.gui().title(Component.text(getMessage("armorstands.title." + (adminBypass ? "admin_bypass" : "normal")))).rows(6).create();

        ItemStack[] equipment = getEquipment(armorStand, player);
        List<EquipmentSlot> disabled = Arrays.stream(EquipmentSlot.values()).filter(slot -> Arrays.stream(LockType.values())
                .allMatch(type -> armorStand.hasEquipmentLock(slot, type))).collect(Collectors.toList());
        boolean[] settings = getSettings(armorStand);
        ArmorStandStatus status = new ArmorStandStatus(player, armorStand, gui, pageWithArmor, equipment, disabled, settings);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            if (pageWithArmor) {
                ItemStack[] currentEquipment = getEquipment(armorStand, player);
                if (!Arrays.equals(currentEquipment, status.equipment)) {
                    gui.updateItem(2, 3, ItemBuilder.from(currentEquipment[0]).asGuiItem());
                    gui.updateItem(3, 3, ItemBuilder.from(currentEquipment[1]).asGuiItem());
                    gui.updateItem(4, 3, ItemBuilder.from(currentEquipment[2]).asGuiItem());
                    gui.updateItem(5, 3, ItemBuilder.from(currentEquipment[3]).asGuiItem());
                    gui.updateItem(3, 2, ItemBuilder.from(currentEquipment[4]).asGuiItem());
                    gui.updateItem(3, 4, ItemBuilder.from(currentEquipment[5]).asGuiItem());
                    status.equipment = currentEquipment;
                }
            } else {
                boolean update = false;
                List<EquipmentSlot> currentDisabled = Arrays.stream(EquipmentSlot.values()).filter(slot -> Arrays.stream(LockType.values())
                        .allMatch(type -> armorStand.hasEquipmentLock(slot, type))).collect(Collectors.toList());
                if (!currentDisabled.equals(status.disabled)) {
                    setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.HEAD, currentDisabled.contains(EquipmentSlot.HEAD));
                    setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.CHEST, currentDisabled.contains(EquipmentSlot.CHEST));
                    setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.LEGS, currentDisabled.contains(EquipmentSlot.LEGS));
                    setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.FEET, currentDisabled.contains(EquipmentSlot.FEET));
                    setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.HAND, currentDisabled.contains(EquipmentSlot.HAND));
                    setDisabledSlotItem(player, gui, armorStand, EquipmentSlot.OFF_HAND, currentDisabled.contains(EquipmentSlot.OFF_HAND));
                    status.disabled = currentDisabled;
                    update = true;
                }

                boolean[] currentSettings = getSettings(armorStand);
                if (!Arrays.equals(currentSettings, status.settings)) {
                    for (int i = 0; i < currentSettings.length; i++)
                        setSettingsItem(player, gui, armorStand, i, currentSettings[i]);
                    status.settings = currentSettings;
                    update = true;
                }

                if (update)
                    gui.update();
            }
        }, 40, 40);
        gui.setCloseGuiAction(event -> {
            task.cancel();
            if (invs.containsKey(player) && invs.get(player).time == status.time)
                invs.remove(player);
        });

        FeaturesData features = Config.get().features;
        if (pageWithArmor) {
            setPrivateItem(player, gui, armorStand, adminBypass);

            gui.setItem(2, 3, ItemBuilder.from(equipment[0]).asGuiItem());
            gui.setItem(3, 3, ItemBuilder.from(equipment[1]).asGuiItem());
            gui.setItem(4, 3, ItemBuilder.from(equipment[2]).asGuiItem());
            gui.setItem(5, 3, ItemBuilder.from(equipment[3]).asGuiItem());
            gui.setItem(3, 2, ItemBuilder.from(equipment[4]).asGuiItem());
            gui.setItem(3, 4, ItemBuilder.from(equipment[5]).asGuiItem());

            gui.setItem(2, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.PLAYER_HEAD), "armorstands.move.head").asGuiItem(event -> {
                if (event.isLeftClick()) {
                    gui.close(player);
                    startMoveBodyPart(player, armorStand, BodyPart.HEAD);
                } else if (event.isRightClick()) {
                    wrapper.resetArmorstandPosition(armorStand, BodyPart.HEAD);
                    playArmorStandHitSound(player);
                }
            }), features.moveBodyParts, player));
            gui.setItem(3, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move.right_arm").asGuiItem(event -> {
                if (event.isLeftClick()) {
                    gui.close(player);
                    startMoveBodyPart(player, armorStand, BodyPart.RIGHT_ARM);
                } else if (event.isRightClick()) {
                    wrapper.resetArmorstandPosition(armorStand, BodyPart.RIGHT_ARM);
                    playArmorStandHitSound(player);
                }
            }), features.moveBodyParts, player));
            gui.setItem(3, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move.body").asGuiItem(event -> {
                if (event.isLeftClick()) {
                    gui.close(player);
                    startMoveBodyPart(player, armorStand, BodyPart.BODY);
                } else if (event.isRightClick()) {
                    wrapper.resetArmorstandPosition(armorStand, BodyPart.BODY);
                    playArmorStandHitSound(player);
                }
            }), features.moveBodyParts, player));
            gui.setItem(3, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move.left_arm").asGuiItem(event -> {
                if (event.isLeftClick()) {
                    gui.close(player);
                    startMoveBodyPart(player, armorStand, BodyPart.LEFT_ARM);
                } else if (event.isRightClick()) {
                    wrapper.resetArmorstandPosition(armorStand, BodyPart.LEFT_ARM);
                    playArmorStandHitSound(player);
                }
            }), features.moveBodyParts, player));
            gui.setItem(4, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move.right_leg").asGuiItem(event -> {
                if (event.isLeftClick()) {
                    gui.close(player);
                    startMoveBodyPart(player, armorStand, BodyPart.RIGHT_LEG);
                } else if (event.isRightClick()) {
                    wrapper.resetArmorstandPosition(armorStand, BodyPart.RIGHT_LEG);
                    playArmorStandHitSound(player);
                }
            }), features.moveBodyParts, player));
            gui.setItem(4, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.STICK), "armorstands.move.left_leg").asGuiItem(event -> {
                if (event.isLeftClick()) {
                    gui.close(player);
                    startMoveBodyPart(player, armorStand, BodyPart.LEFT_LEG);
                } else if (event.isRightClick()) {
                    wrapper.resetArmorstandPosition(armorStand, BodyPart.LEFT_LEG);
                    playArmorStandHitSound(player);
                }
            }), features.moveBodyParts, player));

            gui.setItem(2, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.FEATHER), "armorstands.move_position").asGuiItem(event -> {
                gui.close(player);
                startMovePosition(player, armorStand, event.isRightClick());
            }), features.movePosition, player));

            gui.setItem(5, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.RED_DYE), "armorstands.move_position.x").asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startSnapInMovePosition(player, armorStand, Axis.X);
                } else {
                    armorStand.teleport(armorStand.getLocation().add(event.isLeftClick() ? 0.05 : -0.05, 0, 0));
                    playStepSound(player);
                }
            }), features.movePosition, player));
            gui.setItem(5, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.LIME_DYE), "armorstands.move_position.y").asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startSnapInMovePosition(player, armorStand, Axis.Y);
                } else {
                    armorStand.teleport(armorStand.getLocation().add(0, event.isLeftClick() ? 0.05 : -0.05, 0));
                    playStepSound(player);
                }
            }), features.movePosition, player));
            gui.setItem(5, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.BLUE_DYE), "armorstands.move_position.z").asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startSnapInMovePosition(player, armorStand, Axis.Z);
                } else {
                    armorStand.teleport(armorStand.getLocation().add(0, 0, event.isLeftClick() ? 0.05 : -0.05));
                    playStepSound(player);
                }
            }), features.movePosition, player));

            gui.setItem(2, 8, checkDeactivated(applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.ENDER_EYE),
                    getMessage("armorstands.rotate.name"), getMessage("armorstands.rotate.lore").replace("%rotation%",
                            String.valueOf(Math.round(armorStand.getLocation().getYaw() * 100F) / 100F)), false).asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startRotationMovement(player, armorStand);
                } else {
                    if (event.getClick() == ClickType.CONTROL_DROP) {
                        armorStand.setRotation(0, armorStand.getLocation().getPitch());
                        playExperienceSound(player);
                    } else {
                        int amount = event.isShiftClick() ? 10 : 45;
                        if (event.isRightClick())
                            amount *= -1;
                        armorStand.setRotation(armorStand.getLocation().getYaw() + amount, armorStand.getLocation().getPitch());
                        playStepSound(player);
                    }

                    gui.updateItem(2, 8, applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.ENDER_EYE),
                            getMessage("armorstands.rotate.name"), getMessage("armorstands.rotate.lore").replace("%rotation%",
                                    String.valueOf(Math.round(armorStand.getLocation().getYaw() * 100F) / 100F)), false).build());
                }
            }), features.rotate, player));

            gui.setItem(6, 9, ItemBuilder.from(Material.SPECTRAL_ARROW).name(Component.text(getMessage("armorstands.page.first"))).asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, false));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));

            gui.setDragAction(event -> {
                if (event.getRawSlots().stream().anyMatch(slot -> slot <= 53))
                    event.setCancelled(true);
            });
            gui.setDefaultClickAction(event -> {
                if (event.getRawSlot() >= 54 && event.isShiftClick())
                    event.setCancelled(true);
            });

            gui.setDefaultTopClickAction(event -> {
                boolean applyCooldown = true;
                Material type = event.getCursor().getType();
                ItemStack cursor;
                if (event.getClick() == ClickType.NUMBER_KEY) {
                    cursor = Optional.ofNullable(player.getInventory().getItem(event.getHotbarButton())).orElseGet(() -> new ItemStack(Material.AIR));
                } else if (event.getCurrentItem() != null && ItemNbt.removeTag(event.getCursor().clone(), "mf-gui")
                        .isSimilar(ItemNbt.removeTag(event.getCurrentItem().clone(), "mf-gui"))) {
                    cursor = event.getCurrentItem().clone();
                    cursor.setAmount(cursor.getAmount() + (event.isLeftClick() ? event.getCursor().getAmount() : 1));
                    applyCooldown = event.isLeftClick();
                } else if (event.isRightClick()) {
                    if (type == Material.AIR) {
                        if (event.getCurrentItem() != null) {
                            cursor = event.getCurrentItem().clone();
                            cursor.setAmount((int) (cursor.getAmount() / 2D));
                        } else
                            cursor = new ItemStack(Material.AIR);
                    } else if (event.getCurrentItem() == null || ItemNbt.getString(event.getCurrentItem(), "empty_slot") != null) {
                        cursor = event.getCursor().clone();
                        cursor.setAmount(1);
                    } else
                        cursor = event.getCursor();
                } else
                    cursor = event.getCursor();
                type = cursor.getType();

                UUID uuid = armorStand.getUniqueId();
                if (applyCooldown && armorItemsCooldown.containsKey(uuid)) {
                    event.setCancelled(true);
                    long time = System.currentTimeMillis();
                    armorItemsCooldown.put(uuid, time);
                    Bukkit.getScheduler().runTaskLater(ArmorStandEditor.getInstance(), () -> armorItemsCooldown.remove(uuid, time), 10);
                    player.sendMessage(getMessage("armorstands.items.cooldown"));
                    return;
                }

                if (!Arrays.asList(11, 19, 20, 21, 29, 38).contains(event.getRawSlot())) {
                    event.setCancelled(true);
                    return;
                }

                if (isDeactivated(features.replaceEquipment, player)) {
                    event.setCancelled(true);
                    playBassSound(player);
                    return;
                }

                AtomicInteger index = new AtomicInteger(-1);
                switch (event.getRawSlot()) {
                    case 11:
                        armorStand.getEquipment().setHelmet(cursor);
                        index.set(0);
                        status.equipment[0] = getEquipmentItem(EquipmentSlot.HEAD, cursor, player);
                        break;
                    case 19:
                        armorStand.getEquipment().setItemInMainHand(cursor);
                        index.set(4);
                        status.equipment[4] = getEquipmentItem(EquipmentSlot.HAND, cursor, player);
                        break;
                    case 20:
                        if (type == Material.AIR || type.toString().endsWith("_CHESTPLATE")) {
                            armorStand.getEquipment().setChestplate(cursor);
                            index.set(1);
                            status.equipment[1] = getEquipmentItem(EquipmentSlot.CHEST, cursor, player);
                        } else {
                            event.setCancelled(true);
                            playAnvilSound(player);
                        }
                        break;
                    case 21:
                        armorStand.getEquipment().setItemInOffHand(cursor);
                        index.set(5);
                        status.equipment[5] = getEquipmentItem(EquipmentSlot.OFF_HAND, cursor, player);
                        break;
                    case 29:
                        if (type == Material.AIR || type.toString().endsWith("_LEGGINGS")) {
                            armorStand.getEquipment().setLeggings(cursor);
                            index.set(2);
                            status.equipment[2] = getEquipmentItem(EquipmentSlot.LEGS, cursor, player);
                        } else {
                            event.setCancelled(true);
                            playAnvilSound(player);
                        }
                        break;
                    case 38:
                        if (type == Material.AIR || type.toString().endsWith("_BOOTS")) {
                            armorStand.getEquipment().setBoots(cursor);
                            index.set(3);
                            status.equipment[3] = getEquipmentItem(EquipmentSlot.FEET, cursor, player);
                        } else {
                            event.setCancelled(true);
                            playAnvilSound(player);
                        }
                        break;
                    default:
                        event.setCancelled(true);
                        break;
                }

                if (!event.isCancelled() && event.getCurrentItem() != null) {
                    long time = System.currentTimeMillis();
                    armorItemsCooldown.put(uuid, time);
                    Bukkit.getScheduler().runTaskLater(ArmorStandEditor.getInstance(), () -> armorItemsCooldown.remove(uuid, time), 10);

                    event.setCurrentItem(ItemNbt.getString(event.getCurrentItem(), "empty_slot") != null ?
                            new ItemStack(Material.AIR) : ItemNbt.removeTag(event.getCurrentItem(), "mf-gui"));
                    if (cursor.getType().isAir()) {
                        Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () ->
                                gui.updateItem(event.getRawSlot(), ItemBuilder.from(status.equipment[index.get()]).asGuiItem()));
                    }

                    invs.values().forEach(otherStatus -> {
                        if (!otherStatus.pageWithArmor || !uuid.equals(otherStatus.armorStand.getUniqueId()) ||
                            status.time == otherStatus.time) {
                            return;
                        }

                        otherStatus.gui.updateItem(2, 3, ItemBuilder.from(status.equipment[0]).asGuiItem());
                        otherStatus.gui.updateItem(3, 3, ItemBuilder.from(status.equipment[1]).asGuiItem());
                        otherStatus.gui.updateItem(4, 3, ItemBuilder.from(status.equipment[2]).asGuiItem());
                        otherStatus.gui.updateItem(5, 3, ItemBuilder.from(status.equipment[3]).asGuiItem());
                        otherStatus.gui.updateItem(3, 2, ItemBuilder.from(status.equipment[4]).asGuiItem());
                        otherStatus.gui.updateItem(3, 4, ItemBuilder.from(status.equipment[5]).asGuiItem());
                    });
                }
            });
        } else {
            gui.disableAllInteractions();

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
                    playAnvilSound(player);
                    return;
                }

                gui.close(player);
                ItemStack item = wrapper.getArmorstandItem(armorStand, PRIVATE_KEY);
                armorStand.remove();
                inv.addItem(item);
                playArmorStandBreakSound(player);
            }), features.giveItem, player));

            setRenameItem(player, armorStand, gui);

            gui.setItem(4, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.LEAD), "armorstands.vehicle")
                    .glow(!armorStand.getPassengers().isEmpty()).asGuiItem(event -> {
                        if (event.isLeftClick()) {
                            gui.close(player);
                            if (isPlayerDoingSomethingOutsideOfInv(player)) {
                                player.sendMessage(getMessage("not_possible_now"));
                                return;
                            }

                            vehicleSelection.put(player, Map.entry(armorStand, false));
                            run();
                        } else if (event.isRightClick()) {
                            if (armorStand.eject()) {
                                playExperienceSound(player);
                                gui.updateItem(5, 8, applyNameAndLore(ItemBuilder.from(Material.SADDLE), "armorstands.vehicle").glow(false).build());
                            } else
                                playBassSound(player);
                        }
                    }), features.vehicle, player));

            gui.setItem(5, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.SADDLE), "armorstands.passenger")
                    .glow(armorStand.isInsideVehicle()).asGuiItem(event -> {
                        if (event.isLeftClick()) {
                            gui.close(player);
                            if (isPlayerDoingSomethingOutsideOfInv(player)) {
                                player.sendMessage(getMessage("not_possible_now"));
                                return;
                            }

                            vehicleSelection.put(player, Map.entry(armorStand, true));
                            run();
                        } else if (event.isRightClick()) {
                            if (armorStand.leaveVehicle()) {
                                playExperienceSound(player);
                                gui.updateItem(4, 8, applyNameAndLore(ItemBuilder.from(Material.MINECART), "armorstands.passenger").glow(false).build());
                            } else
                                playBassSound(player);
                        }
                    }), features.passenger, player));

            gui.setItem(6, 1, ItemBuilder.from(Material.SPECTRAL_ARROW).name(Component.text(getMessage("armorstands.page.second"))).asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, true));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        gui.open(player);
        invs.put(player, status);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ArmorStand))
            return;

        UUID uuid = entity.getUniqueId();
        new HashMap<>(invs).values().stream().filter(status -> status.armorStand.getUniqueId().equals(uuid)).forEach(status -> status.gui.close(status.player));
    }

    private boolean isDeactivated(FeatureData feature, Player player) {
        return !feature.enabled || (feature.permission != null && !player.hasPermission(feature.permission));
    }

    @SuppressWarnings("deprecation")
    private GuiItem checkDeactivated(GuiItem item, FeatureData feature, Player player) {
        boolean noPermission = false;
        if (feature.enabled) {
            noPermission = true;
            if (feature.permission == null || player.hasPermission(feature.permission))
                return item;
        }

        ItemStack itemStack = item.getItemStack();
        String deactivatedItem = Config.get().deactivatedItem;
        Material mat;
        if (deactivatedItem == null)
            mat = itemStack.getType();
        else {
            mat = Material.matchMaterial(deactivatedItem);
            if (mat == null)
                mat = Material.GRAY_DYE;
        }

        ItemBuilder builder = ItemBuilder.from(mat);
        if (mat == Material.AIR)
            return builder.asGuiItem();

        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
            builder.setName(itemStack.getItemMeta().getDisplayName());
        builder.lore(Arrays.stream(getMessage("armorstands.features." + (noPermission ? "no_permission" : "deactivated")).split("\n"))
                .map(line -> (Component) Component.text(line)).collect(Collectors.toList()));

        return builder.setNbt("deactivated", true).asGuiItem(event -> playBassSound(player));
    }

    private ItemStack checkDeactivated(ItemStack item, FeatureData feature, Player player) {
        boolean noPermission = false;
        if (feature.enabled) {
            noPermission = true;
            if (feature.permission == null || player.hasPermission(feature.permission))
                return item;
        }

        String deactivatedItem = Config.get().deactivatedItem;
        Material mat;
        if (deactivatedItem == null)
            mat = item.getType();
        else {
            mat = Material.matchMaterial(deactivatedItem);
            if (mat == null)
                mat = Material.FIREWORK_STAR;
        }

        ItemStack newItem = new ItemStack(mat);
        if (mat == Material.AIR)
            return newItem;

        ItemMeta meta = newItem.getItemMeta();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            meta.setDisplayName(item.getItemMeta().getDisplayName());
        meta.setLore(Arrays.asList(getMessage("armorstands.features." + (noPermission ? "no_permission" : "deactivated")).split("\n")));

        newItem.setItemMeta(meta);
        return ItemNbt.setBoolean(newItem, "deactivated", true);
    }

    private boolean isPlayerDoingSomethingOutsideOfInv(Player player) {
        return moving.containsKey(player) || vehicleSelection.containsKey(player);
    }

    private void startMovePosition(Player player, ArmorStand armorStand, boolean snapIn) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        if (!snapIn) {
            moving.put(player, new ArmorStandPositionMovement(armorStand, Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                if (player.getWorld().getUID().equals(armorStand.getWorld().getUID())) {
                    Location playerLoc = player.getLocation();
                    Location loc = playerLoc.clone().add(playerLoc.getDirection().multiply(3));
                    loc.setYaw(armorStand.getLocation().getYaw());
                    loc.setPitch(armorStand.getLocation().getPitch());
                    loc.setY(playerLoc.getY());

                    Location aboveBlock = loc.clone();
                    for (int i = 0; i < 3; i++) {
                        Block block = aboveBlock.getBlock();
                        if (block.isPassable()) {
                            loc = aboveBlock;
                            break;
                        }

                        aboveBlock.setY(block.getRelative(BlockFace.UP).getLocation().getY());
                    }

                    armorStand.teleport(loc);
                }
            }, 0, 1)));
        } else {
            long time = System.currentTimeMillis();

            ArmorStandPositionSnapInMovement movement = new ArmorStandPositionSnapInMovement(armorStand, null);
            movement.task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()))
                    return;

                Location center = movement.getCurrentLocation();
                List<Location> locations = movement.locations;
                if (locations.isEmpty()) {
                    locations.add(center);
                    BiConsumer<Double, Double> addLocation = (x, z) -> {
                        Location loc = new Location(center.getWorld(), x, center.getY(), z, center.getYaw(), center.getPitch());
                        if (loc.distanceSquared(player.getLocation()) <= 2500)
                            locations.add(loc);
                    };

                    double distance = movement.distance;
                    int count = 0;
                    double coveredDistance = 0;
                    while (count < 3 || coveredDistance < 3) {
                        count++;
                        coveredDistance += distance;

                        double minX = center.getX() - coveredDistance, maxX = center.getX() + coveredDistance,
                                minZ = center.getZ() - coveredDistance, maxZ = center.getZ() + coveredDistance;

                        double x = minX, z = minZ;
                        for (; x <= maxX; x += distance)
                            addLocation.accept(x, z);
                        x -= distance;
                        for (; z <= maxZ; z += distance)
                            addLocation.accept(x, z);
                        z -= distance;
                        for (; x >= minX; x -= distance)
                            addLocation.accept(x, z);
                        x += distance;
                        for (; z > minZ; z -= distance)
                            addLocation.accept(x, z);
                    }
                }

                DustOptions options = new DustOptions(Color.fromRGB(255, 0, 0), 0.5F);
                for (Location loc : locations)
                    player.spawnParticle(Particle.REDSTONE, loc, 1, options);

                if (System.currentTimeMillis() > time + 1000) {
                    Location eyeLoc = player.getEyeLocation();
                    Vector eyeVec = eyeLoc.toVector();
                    double currentDot = 0.999;
                    Location closest = null;
                    for (Location loc : locations) {
                        double dot = loc.toVector().subtract(eyeVec).normalize().dot(eyeLoc.getDirection());
                        if (dot > currentDot) {
                            currentDot = dot;
                            closest = loc;
                        }
                    }

                    if (closest != null && !closest.equals(center)) {
                        armorStand.teleport(closest);
                        movement.updateOffset(closest);
                        locations.clear();
                    } else
                        armorStand.teleport(center);
                } else
                    armorStand.teleport(center);
            }, 0, 1);

            moving.put(player, movement);
        }

        run();
    }

    private void startSnapInMovePosition(Player player, ArmorStand armorStand, Axis axis) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        long time = System.currentTimeMillis();

        ArmorStandPositionSnapInMovement movement = new ArmorStandPositionSnapInMovement(armorStand, axis);
        movement.task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()))
                return;

            Location center = movement.getCurrentLocation();
            List<Location> locations = movement.locations;
            if (locations.isEmpty()) {
                locations.add(center);
                Consumer<Location> addLocation = loc -> {
                    if (loc.distanceSquared(player.getLocation()) <= 2500)
                        locations.add(loc.clone());
                };

                double distance = movement.distance;
                int count = 0;
                double coveredDistance = 0;
                Location plus = center.clone(), minus = center.clone();
                while (count < 3 || coveredDistance < 5) {
                    count++;
                    coveredDistance += distance;

                    axis.setValue(plus, axis.getValue(plus) + distance);
                    axis.setValue(minus, axis.getValue(minus) - distance);
                    addLocation.accept(plus);
                    addLocation.accept(minus);
                }
            }

            DustOptions options = new DustOptions(Color.fromRGB(255, 0, 0), 0.5F);
            for (Location loc : locations)
                player.spawnParticle(Particle.REDSTONE, loc, 1, options);

            if (System.currentTimeMillis() > time + 1000) {
                Location eyeLoc = player.getEyeLocation();
                Vector eyeVec = eyeLoc.toVector();
                double currentDot = 0.999;
                Location closest = null;
                for (Location loc : locations) {
                    double dot = loc.toVector().subtract(eyeVec).normalize().dot(eyeLoc.getDirection());
                    if (dot > currentDot) {
                        currentDot = dot;
                        closest = loc;
                    }
                }

                if (closest != null && !closest.equals(center)) {
                    armorStand.teleport(closest);
                    movement.updateOffset(closest);
                    locations.clear();
                } else
                    armorStand.teleport(center);
            } else
                armorStand.teleport(center);
        }, 0, 1);

        moving.put(player, movement);
        run();
    }

    private void startMoveBodyPart(Player player, ArmorStand armorStand, BodyPart bodyPart) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        ArmorStandBodyPartMovement movement = new ArmorStandBodyPartMovement(armorStand, bodyPart,
                player.getLocation().getYaw(), player.getLocation().getPitch());
        movement.task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            EulerAngle angle = bodyPart.get(armorStand);
            Location loc = player.getLocation();

            boolean sneaking = player.isSneaking();
            Direction yawDir = sneaking ? bodyPart.sneakYawDir : bodyPart.normalYawDir,
                    pitchDir = sneaking ? bodyPart.sneakPitchDir : bodyPart.normalPitchDir;

            float yaw = loc.getYaw(), pitch = loc.getPitch();
            while (yaw < 0)
                yaw += 360;
            while (pitch < 0)
                pitch += 360;

            double yawChange = Math.toRadians((yaw - movement.zeroYaw) * yawDir.factor),
                    pitchChange = Math.toRadians((pitch - movement.zeroPitch) * pitchDir.factor);
            angle = switch (yawDir) {
                case X -> angle.setX(movement.zeroAngle.getX() + yawChange);
                case Y -> angle.setY(movement.zeroAngle.getY() + yawChange);
                case Z -> angle.setZ(movement.zeroAngle.getZ() + yawChange);
            };
            angle = switch (pitchDir) {
                case X -> angle.setX(movement.zeroAngle.getX() + pitchChange);
                case Y -> angle.setY(movement.zeroAngle.getY() + pitchChange);
                case Z -> angle.setZ(movement.zeroAngle.getZ() + pitchChange);
            };
            bodyPart.apply(armorStand, angle);
        }, 1, 1);

        moving.put(player, movement);
        run();
    }

    private void startRotationMovement(Player player, ArmorStand armorStand) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        moving.put(player, new ArmorStandRotationMovement(armorStand, Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(),
                () -> armorStand.setRotation(player.getLocation().getYaw(), armorStand.getLocation().getPitch()), 0, 1)));
        run();
    }

    @Override
    public void run() {
        // titles
        moving.forEach((player, movement) -> {
            if (movement instanceof ArmorStandPositionMovement) {
                String message;
                if (movement instanceof ArmorStandPositionSnapInMovement snapInMovement) {
                    message = getMessage("armorstands.move_position.title.snapin")
                                      .replace("%distance%", String.valueOf(snapInMovement.distance))
                                      .replace("%aligned_color%", getMessage("armorstands.move_position.title.snapin_color_aligned_" +
                                                                             (player.isSneaking() ? "active" : "inactive"))) +
                              " " + Math.round(movement.armorStand.getLocation().distance(player.getLocation()) * 10D) / 10D;
                } else
                    message = getMessage("armorstands.move_position.title.normal");

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
                BodyPart bodyPart = bodyPartMovement.bodyPart;
                String activated = getMessage("armorstands.move.title.color_activated"),
                        deactivated = getMessage("armorstands.move.title.color_deactivated");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.move.title.text")
                        .replace("%normal%", bodyPart.normalYawDir.getString(bodyPart.normalPitchDir))
                        .replace("%sneak%", bodyPart.sneakYawDir.getString(bodyPart.sneakPitchDir))
                        .replace("%color_normal%", !player.isSneaking() ? activated : deactivated)
                        .replace("%color_sneak%", player.isSneaking() ? activated : deactivated)));
            } else if (movement instanceof ArmorStandRotationMovement) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.rotate.title")));
            }
        });

        vehicleSelection.forEach((player, entry) -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(getMessage("armorstands." + (entry.getValue() ? "passenger" : "vehicle") + ".choose.title"))));

        // remove when player is far away
        moving.entrySet().removeIf(entry -> {
            Player player = entry.getKey();
            ArmorStandMovement movement = entry.getValue();
            ArmorStand armorStand = movement.armorStand;
            if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()) || player.getLocation().distanceSquared(armorStand.getLocation()) > 2500) {
                movement.task.cancel();
                cancelMovement(movement);
                return true;
            }
            return false;
        });
        vehicleSelection.entrySet().removeIf(entry -> entry.getKey().getLocation().distanceSquared(entry.getValue().getKey().getLocation()) > 2500);
    }

    @EventHandler
    public void onInteract(EntityInteractEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (event.getEntityType() == EntityType.ARMOR_STAND &&
            moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarSlot(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!moving.containsKey(player) || !(moving.get(player) instanceof ArmorStandPositionSnapInMovement movement))
            return;

        int index = SNAP_IN_DISTANCES.indexOf(movement.distance);
        int previous = event.getPreviousSlot(), current = event.getNewSlot();
        if (previous == 8 && current == 0)
            index++;
        else if (previous == 0 && current == 8)
            index--;
        else if (current > previous)
            index++;
        else if (current < previous)
            index--;

        if (index < 0 || index > SNAP_IN_DISTANCES.size() - 1) {
            playBassSound(player);
            return;
        }

        playSound(player, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON);
        movement.distance = SNAP_IN_DISTANCES.get(index);
        movement.locations.clear();
        run();
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!moving.containsKey(player))
            return;

        Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), this);
        ArmorStandMovement movement = moving.get(player);
        if (movement instanceof ArmorStandPositionSnapInMovement snapInMovement) {
            snapInMovement.locations.clear();

            Location loc = snapInMovement.getCurrentLocation().getBlock().getLocation();
            if (event.isSneaking())
                loc.add(0.5, 0, 0.5);
            else if (snapInMovement.axis == null) {
                double[] alignmentXZ = snapInMovement.previousAlignmentXZ;
                loc.add(alignmentXZ[0], 0, alignmentXZ[1]);
            } else
                snapInMovement.axis.setValue(loc, snapInMovement.axis.getValue(loc) + snapInMovement.previousAlignmentSingle);
            snapInMovement.updateOffset(loc);
        } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
            Location loc = player.getLocation();
            bodyPartMovement.zeroYaw = loc.getYaw();
            bodyPartMovement.zeroPitch = loc.getPitch();
            bodyPartMovement.zeroAngle = bodyPartMovement.bodyPart.get(bodyPartMovement.armorStand);
        }
    }

    @EventHandler
    public void onInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action == Action.PHYSICAL)
            return;

        if (moving.containsKey(player)) {
            event.setCancelled(true);
            onMovementInteraction(player, action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        }

        if (vehicleSelection.containsKey(player)) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
                return;

            event.setCancelled(true);
            onVehicleSelectionInteraction(player, null, false);
        }
    }

    @EventHandler
    public void onInteraction(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (moving.containsKey(player)) {
            event.setCancelled(true);
            onMovementInteraction(player, false);
        }

        if (vehicleSelection.containsKey(player)) {
            event.setCancelled(true);
            onVehicleSelectionInteraction(player, null, false);
        }
    }

    @EventHandler
    public void onInteraction(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;

        if (moving.containsKey(player)) {
            event.setCancelled(true);
            onMovementInteraction(player, true);
        }

        if (vehicleSelection.containsKey(player)) {
            event.setCancelled(true);
            onVehicleSelectionInteraction(player, event.getEntity(), true);
        }
    }

    private void onMovementInteraction(Player player, boolean leftClick) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        ArmorStandMovement movement = moving.remove(player);
        if (movement.task != null)
            movement.task.cancel();

        if (!leftClick) {
            cancelMovement(movement);
            playArmorStandBreakSound(player);
        } else
            playExperienceSound(player);
    }

    private void cancelMovement(ArmorStandMovement movement) {
        if (movement instanceof ArmorStandPositionMovement positionMovement) {
            movement.armorStand.teleport(positionMovement.originalLocation);
        } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
            bodyPartMovement.bodyPart.apply(movement.armorStand, bodyPartMovement.cancelAngle);
        } else if (movement instanceof ArmorStandRotationMovement rotationMovement) {
            movement.armorStand.setRotation(rotationMovement.originalYaw, movement.armorStand.getLocation().getPitch());
        }
    }

    private void onVehicleSelectionInteraction(Player player, Entity entity, boolean leftClick) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        Entry<ArmorStand, Boolean> entry = vehicleSelection.remove(player);
        ArmorStand armorStand = entry.getKey();
        boolean asPassenger = entry.getValue();

        if (leftClick) {
            String key = "armorstands." + (asPassenger ? "passenger" : "vehicle") + ".choose.";
            FeaturesData features = Config.get().features;
            if (!(asPassenger ? features.passenger : features.vehicle).players && entity instanceof Player) {
                player.sendMessage(getMessage(key + ".no_players"));
                playBassSound(player);
                return;
            }
            if (entity.getUniqueId().equals(armorStand.getUniqueId())) {
                player.sendMessage(getMessage(key + ".not_itself"));
                playBassSound(player);
                return;
            }

            if (asPassenger)
                entity.addPassenger(armorStand);
            else
                armorStand.addPassenger(entity);
            playExperienceSound(player);
        } else
            playArmorStandBreakSound(player);
    }

    private ItemBuilder applyNameAndLore(ItemBuilder builder, String key) {
        return applyNameAndLore(builder, key, false);
    }

    private ItemBuilder applyNameAndLore(ItemBuilder builder, String key, boolean status) {
        return applyNameAndLore(builder, key + ".name", key + ".lore", status);
    }

    private ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore, boolean status) {
        return applyNameAndLoreWithoutKeys(builder, getMessage(name), getMessage(lore), status);
    }

    private ItemBuilder applyNameAndLoreWithoutKeys(ItemBuilder builder, String name, String lore, boolean status) {
        return builder.name(Component.text(name))
                .lore(Arrays.stream(lore.replace("%status%", getMessage("armorstands.status." + (status ? "on" : "off")))
                        .split("\n")).map(line -> (Component) Component.text(line)).collect(Collectors.toList()));
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

        gui.setItem(1, 1, checkDeactivated(applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.SHULKER_SHELL), getMessage("armorstands.private.name"),
                getMessage("armorstands.private.lore." + (adminBypass ? "admin_bypass" : "normal")).replace("%player%", locked ?
                        getMessage("armorstands.private.player").replace("%player%", name) : ""), locked)
                .glow(locked).asGuiItem(event -> {
                    playSpyglassSound(player);
                    if (locked)
                        pdc.remove(PRIVATE_KEY);
                    else
                        pdc.set(PRIVATE_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

                    invs.forEach((p, status) -> {
                        if (!status.pageWithArmor || !armorStand.getUniqueId().equals(status.armorStand.getUniqueId()))
                            return;
                        setPrivateItem(p, status.gui, armorStand, adminBypass);
                        status.gui.update();
                    });
                }), Config.get().features.privateArmorstand, player));
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

            gui.setItem(row, col, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.HONEYCOMB), "armorstands.lock." + key,
                    "armorstands.lock.lore", disabled).glow(disabled).asGuiItem(event -> {
                playSpyglassSound(player);
                if (disabled)
                    for (LockType type : LockType.values())
                        armorStand.removeEquipmentLock(slot, type);
                else
                    for (LockType type : LockType.values())
                        armorStand.addEquipmentLock(slot, type);

                if (Arrays.stream(LockType.values()).anyMatch(type -> armorStand.hasEquipmentLock(slot, type)) == disabled) {
                    playAnvilSound(player);
                    return;
                }

                invs.forEach((p, status) -> {
                    if (status.pageWithArmor || !armorStand.getUniqueId().equals(status.armorStand.getUniqueId()))
                        return;
                    setDisabledSlotItem(p, status.gui, armorStand, slot, !disabled);
                    status.gui.update();
                });
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

        gui.setItem(row, col, checkDeactivated(applyNameAndLore(ItemBuilder.from(mat), "armorstands.settings." + key, enabled).glow(enabled).asGuiItem(event -> {
            playSpyglassSound(player);

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

            boolean mainhandDisabled = index == 2 && Arrays.stream(LockType.values()).allMatch(type -> armorStand.hasEquipmentLock(EquipmentSlot.HAND, type)),
                    offhandDisabled = index == 2 && Arrays.stream(LockType.values()).allMatch(type -> armorStand.hasEquipmentLock(EquipmentSlot.OFF_HAND, type));
            invs.forEach((p, status) -> {
                if (status.pageWithArmor || !armorStand.getUniqueId().equals(status.armorStand.getUniqueId()))
                    return;
                if (index == 2) {
                    setDisabledSlotItem(p, status.gui, armorStand, EquipmentSlot.HAND, mainhandDisabled);
                    setDisabledSlotItem(p, status.gui, armorStand, EquipmentSlot.OFF_HAND, offhandDisabled);
                }
                setSettingsItem(p, status.gui, armorStand, index, newEnabled);
                status.gui.update();
            });
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
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> anvilInvs.put(time, new Builder().plugin(ArmorStandEditor.getInstance())
                        .title(getMessage("armorstands.rename.name"))
                        .text(name.substring(0, Math.min(50, name.length())))
                        .onClose(p -> {
                            if (!disabling) {
                                anvilInvs.remove(time);
                                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, false));
                            }
                        }).onComplete(completion -> {
                            wrapper.setCustomName(armorStand, completion.getText());
                            armorStand.setCustomNameVisible(true);
                            anvilInvs.remove(time);
                            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, false));
                            return Arrays.asList(ResponseAction.run(() -> {}));
                        }).open(player)));
            } else if (event.isRightClick()) {
                wrapper.setCustomName(armorStand, null);
                armorStand.setCustomNameVisible(false);
                setRenameItem(player, armorStand, gui);
            }
        }), Config.get().features.rename, player));
    }

    private ItemStack[] getEquipment(ArmorStand armorStand, Player player) {
        EntityEquipment equipment = armorStand.getEquipment();
        return new ItemStack[]{getEquipmentItem(EquipmentSlot.HEAD, equipment.getHelmet(), player),
                getEquipmentItem(EquipmentSlot.CHEST, equipment.getChestplate(), player),
                getEquipmentItem(EquipmentSlot.LEGS, equipment.getLeggings(), player),
                getEquipmentItem(EquipmentSlot.FEET, equipment.getBoots(), player),
                getEquipmentItem(EquipmentSlot.HAND, equipment.getItemInMainHand(), player),
                getEquipmentItem(EquipmentSlot.OFF_HAND, equipment.getItemInOffHand(), player)};
    }

    private ItemStack getEquipmentItem(EquipmentSlot slot, ItemStack current, Player player) {
        ReplaceEquipmentFeatureData feature = Config.get().features.replaceEquipment;
        boolean deactivated = isDeactivated(feature, player);

        if (current != null && !current.getType().isAir() && (!deactivated || !feature.useDeactivatedItem))
            return current;

        ItemStack item = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        String key = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            case HAND -> "mainhand";
            case OFF_HAND -> "offhand";
        };

        meta.setDisplayName(getMessage("armorstands.items." + key));
        meta.setLore(Arrays.asList(getMessage("armorstands.items.lore").split("\n")));
        item.setItemMeta(meta);

        return ItemNbt.setString(checkDeactivated(item, feature, player), "empty_slot", "");
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

    private void playAnvilSound(Player player) {
        playSound(player, Sound.BLOCK_ANVIL_PLACE);
    }

    private void playBassSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1F);
    }

    private void playArmorStandHitSound(Player player) {
        playSound(player, Sound.ENTITY_ARMOR_STAND_HIT, 1F);
    }

    private void playArmorStandBreakSound(Player player) {
        playSound(player, Sound.ENTITY_ARMOR_STAND_BREAK);
    }

    private void playSpyglassSound(Player player) {
        playSound(player, Sound.ITEM_SPYGLASS_USE, 1F);
    }

    private void playExperienceSound(Player player) {
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    private void playStepSound(Player player) {
        playSound(player, Sound.BLOCK_ANVIL_STEP);
    }

    private void playSound(Player player, Sound sound) {
        playSound(player, sound, 0.75F);
    }

    private void playSound(Player player, Sound sound, float volume) {
        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, 1F);
    }

    private enum Axis {
        X, Y, Z;

        double getValue(Location loc) {
            return switch (this) {
                case X -> loc.getX();
                case Y -> loc.getY();
                case Z -> loc.getZ();
            };
        }

        double getBlockValue(Location loc) {
            return switch (this) {
                case X -> loc.getBlockX();
                case Y -> loc.getBlockY();
                case Z -> loc.getBlockZ();
            };
        }

        void setValue(Location loc, double value) {
            switch (this) {
                case X -> loc.setX(value);
                case Y -> loc.setY(value);
                case Z -> loc.setZ(value);
            }
        }
    }

    private class ArmorStandStatus {

        long time;
        Player player;
        ArmorStand armorStand;
        Gui gui;
        boolean pageWithArmor;
        ItemStack[] equipment;
        List<EquipmentSlot> disabled;
        boolean[] settings;

        ArmorStandStatus(Player player, ArmorStand armorStand, Gui gui, boolean pageWithArmor, ItemStack[] equipment, List<EquipmentSlot> disabled, boolean[] settings) {
            this.time = System.currentTimeMillis();
            this.player = player;
            this.armorStand = armorStand;
            this.gui = gui;
            this.pageWithArmor = pageWithArmor;
            this.equipment = equipment;
            this.disabled = disabled;
            this.settings = settings;
        }
    }

    static class ArmorStandMovement {

        ArmorStand armorStand;
        BukkitTask task;

        ArmorStandMovement(ArmorStand armorStand) {
            this.armorStand = armorStand;
        }

        static class ArmorStandPositionMovement extends ArmorStandMovement {

            Location originalLocation;

            public ArmorStandPositionMovement(ArmorStand armorStand, BukkitTask task) {
                super(armorStand);
                this.task = task;
                this.originalLocation = armorStand.getLocation();
            }

            static class ArmorStandPositionSnapInMovement extends ArmorStandPositionMovement {

                Axis axis;
                double[] previousAlignmentXZ;
                double previousAlignmentSingle;
                double[] offsetXZ;
                double offsetSingle;
                double distance = 1;
                List<Location> locations = new ArrayList<>();

                public ArmorStandPositionSnapInMovement(ArmorStand armorStand, Axis axis) {
                    super(armorStand, null);
                    this.axis = axis;

                    Location loc = armorStand.getLocation();

                    if (axis == null) {
                        previousAlignmentXZ = new double[]{loc.getX() - loc.getBlockX(), loc.getZ() - loc.getBlockZ()};
                        offsetXZ = new double[]{0, 0};
                    } else {
                        previousAlignmentSingle = axis.getValue(loc) - axis.getBlockValue(loc);
                        offsetSingle = 0;
                    }
                }

                public Location getCurrentLocation() {
                    Location loc = originalLocation.clone();
                    if (axis == null)
                        loc.add(offsetXZ[0], 0, offsetXZ[1]);
                    else
                        axis.setValue(loc, axis.getValue(loc) + offsetSingle);
                    return loc;
                }

                public void updateOffset(Location current) {
                    if (axis == null) {
                        offsetXZ[0] = current.getX() - originalLocation.getX();
                        offsetXZ[1] = current.getZ() - originalLocation.getZ();
                    } else
                        offsetSingle = axis.getValue(current) - axis.getValue(originalLocation);
                }
            }
        }

        static class ArmorStandBodyPartMovement extends ArmorStandMovement {

            BodyPart bodyPart;
            float zeroYaw, zeroPitch;
            EulerAngle zeroAngle, cancelAngle;

            public ArmorStandBodyPartMovement(ArmorStand armorStand, BodyPart bodyPart, float zeroYaw, float zeroPitch) {
                super(armorStand);
                this.bodyPart = bodyPart;

                this.zeroYaw = zeroYaw;
                while (this.zeroYaw < 0)
                    this.zeroYaw += 360;

                this.zeroPitch = zeroPitch;
                while (this.zeroPitch < 0)
                    this.zeroPitch += 360;

                zeroAngle = cancelAngle = bodyPart.get(armorStand);
            }
        }

        static class ArmorStandRotationMovement extends ArmorStandMovement {

            float originalYaw;

            public ArmorStandRotationMovement(ArmorStand armorStand, BukkitTask task) {
                super(armorStand);
                this.task = task;
                this.originalYaw = armorStand.getLocation().getYaw();
            }
        }
    }

    // recipe
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null)
            return;
        CraftingInventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof Player player))
            return;
        if (isDeactivated(Config.get().features.copy, player))
            return;

        ItemStack original = null;
        int originalSlot = -1;
        int count = 0;
        ItemStack[] matrix = inventory.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null)
                continue;
            if (item.getType() != Material.ARMOR_STAND)
                return;

            if (wrapper.isArmorstandItem(item)) {
                if (original != null)
                    return;
                original = item;
                originalSlot = i;
            } else
                count += item.getAmount();
        }

        if (count > Material.ARMOR_STAND.getMaxStackSize())
            return;

        if (original != null && count > 0) {
            ItemStack result = wrapper.prepareRecipeResult(original, originalSlot);
            result.setAmount(count);
            inventory.setResult(result);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() != SlotType.RESULT)
            return;
        if (!(event.getInventory() instanceof CraftingInventory inventory))
            return;

        Entry<ItemStack, Integer> entry = wrapper.getRecipeResultAndOriginalSlot(event.getCurrentItem());
        if (entry == null)
            return;

        inventory.setResult(entry.getKey());

        ItemStack[] matrix = inventory.getMatrix().clone();
        for (int i = 0; i < matrix.length; i++) {
            if (i != entry.getValue())
                matrix[i] = null;
            else if (matrix[i] != null)
                matrix[i] = matrix[i].clone();
        }
        Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> inventory.setMatrix(matrix));
    }
}
