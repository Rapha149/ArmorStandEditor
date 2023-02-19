package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Config.FeaturesData.ReplaceEquipmentFeatureData;
import de.rapha149.armorstandeditor.Config.PermissionsData;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandBodyPartMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandPositionMovement;
import de.rapha149.armorstandeditor.version.BodyPart;
import de.rapha149.armorstandeditor.version.Direction;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.util.ItemNbt;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.Builder;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ArmorStand.LockType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class Events implements Listener, Runnable {

    private VersionWrapper wrapper = ArmorStandEditor.getInstance().wrapper;

    private final NamespacedKey PRIVATE_KEY = NamespacedKey.fromString("private", ArmorStandEditor.getInstance());
    private final String INVISIBLE_TAG = "ArmorStandEditor-Invisible";
    private final LegacyComponentSerializer EDIT_SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();
    private final LegacyComponentSerializer SHOW_SERIALIZER = LegacyComponentSerializer.builder().useUnusualXRepeatedCharacterHexFormat().character('§').build();
    private Map<UUID, Long> armorItemsCooldown = new HashMap<>();
    private Map<Player, ArmorStandMovement> moving = new HashMap<>();

    private Map<Player, ArmorStandStatus> invs = new HashMap<>();
    private Map<Long, AnvilGUI> anvilInvs = new HashMap<>();
    private boolean closingAllInvs = false;

    public Events() {
        Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), this, 0, 20);
    }

    public void closeAllInvs() {
        closingAllInvs = true;
        invs.values().forEach(status -> status.gui.getInventory().close());
        invs.clear();
        anvilInvs.values().forEach(AnvilGUI::closeInventory);
        anvilInvs.clear();
        closingAllInvs = false;
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
        onMovementInteraction(event);
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
                                                            entry.getValue().armorStand.getUniqueId().equals(armorStandUuid));
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

            gui.setItem(4, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.FEATHER), "armorstands.position").asGuiItem(event -> {
                gui.close(player);
                startMovePosition(player, armorStand);
            }), features.movePosition, player));

            gui.setItem(5, 6, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.RED_DYE), "armorstands.position.x").asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startMovePosition(player, armorStand, Axis.X);
                } else {
                    armorStand.teleport(armorStand.getLocation().add(event.isLeftClick() ? 0.05 : -0.05, 0, 0));
                    playSound(player, Sound.BLOCK_ANVIL_STEP);
                }
            }), features.movePosition, player));
            gui.setItem(5, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.LIME_DYE), "armorstands.position.y").asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startMovePosition(player, armorStand, Axis.Y);
                } else {
                    armorStand.teleport(armorStand.getLocation().add(0, event.isLeftClick() ? 0.05 : -0.05, 0));
                    playSound(player, Sound.BLOCK_ANVIL_STEP);
                }
            }), features.movePosition, player));
            gui.setItem(5, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.BLUE_DYE), "armorstands.position.z").asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    gui.close(player);
                    startMovePosition(player, armorStand, Axis.Z);
                } else {
                    armorStand.teleport(armorStand.getLocation().add(0, 0, event.isLeftClick() ? 0.05 : -0.05));
                    playSound(player, Sound.BLOCK_ANVIL_STEP);
                }
            }), features.movePosition, player));

            setPrivateItem(player, gui, armorStand, adminBypass);

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
                    Bukkit.getScheduler().runTaskLater(ArmorStandEditor.getInstance(), () -> armorItemsCooldown.remove(uuid, time), 20);
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
                    Bukkit.getScheduler().runTaskLater(ArmorStandEditor.getInstance(), () -> armorItemsCooldown.remove(uuid, time), 20);

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

            gui.setItem(5, 7, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.ARMOR_STAND), "armorstands.give_item").asGuiItem(event -> {
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

            if (ArmorStandEditor.getInstance().isPaper) {
                Component customName = armorStand.customName();
                gui.setItem(5, 8, checkDeactivated(ItemBuilder.from(Material.NAME_TAG).name(Component.text(getMessage("armorstands.rename.name")))
                        .lore(Arrays.stream(getMessage("armorstands.rename.lore").replace("%name%",
                                        customName != null ? SHOW_SERIALIZER.serialize(customName) : "§c---")
                                .split("\n")).map(line -> (Component) Component.text(line)).collect(Collectors.toList())).asGuiItem(event -> {
                            if (event.isLeftClick()) {
                                Component currentCustomName = armorStand.customName();
                                String name = currentCustomName != null ? EDIT_SERIALIZER.serialize(currentCustomName)
                                        .replaceAll("§([a-fA-F\\dklmnorx])", "&$1") : "Name...";
                                long time = System.currentTimeMillis();
                                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> anvilInvs.put(time, new Builder().plugin(ArmorStandEditor.getInstance())
                                        .title(getMessage("armorstands.rename.name"))
                                        .text(name.isEmpty() ? "Name..." : name.substring(0, Math.min(50, name.length())))
                                        .onClose(p -> {
                                            if (!closingAllInvs) {
                                                anvilInvs.remove(time);
                                                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, false));
                                            }
                                        }).onComplete(completion -> {
                                            armorStand.customName(EDIT_SERIALIZER.deserialize(ChatColor.translateAlternateColorCodes('&', completion.getText())));
                                            armorStand.setCustomNameVisible(true);
                                            anvilInvs.remove(time);
                                            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, false));
                                            return Arrays.asList(ResponseAction.run(() -> {}));
                                        }).open(player)));
                            } else if (event.isRightClick()) {
                                armorStand.customName(null);
                                armorStand.setCustomNameVisible(false);
                                gui.updateItem(5, 8, ItemBuilder.from(Material.NAME_TAG).name(Component.text(getMessage("armorstands.rename.name")))
                                        .lore(Arrays.stream(getMessage("armorstands.rename.lore").replace("%name%", "§c---")
                                                .split("\n")).map(line -> (Component) Component.text(line)).collect(Collectors.toList())).build());
                            }
                        }), features.rename, player));
            } else {
                gui.setItem(5, 8, checkDeactivated(applyNameAndLore(ItemBuilder.from(Material.NAME_TAG),
                        "armorstands.rename.name", "armorstands.rename.not_paper.lore", false)
                        .asGuiItem(event -> playAnvilSound(player)), features.rename, player));
            }

            gui.setItem(6, 1, ItemBuilder.from(Material.SPECTRAL_ARROW).name(Component.text(getMessage("armorstands.page.second"))).asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, true));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));
        }

        gui.getFiller().fill(ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.text("§r")).asGuiItem());
        gui.open(player);
        invs.put(player, status);
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

    @SuppressWarnings("deprecation")
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

    private void startMovePosition(Player player, ArmorStand armorStand) {
        UUID uuid = armorStand.getUniqueId();
        if (moving.containsKey(player)) {
            player.sendMessage(getMessage("armorstands.move.player_already_moving"));
            return;
        }
        if (moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("armorstands.move.armorstand_already_moving"));
            return;
        }

        moving.put(player, new ArmorStandPositionMovement(armorStand, Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            if (player.getWorld().getUID().equals(armorStand.getWorld().getUID()))
                armorStand.teleport(getRelativeArmorStandPosition(player, armorStand));
        }, 0, 1), armorStand.getLocation()));
        run();
    }

    private void startMovePosition(Player player, ArmorStand armorStand, Axis axis) {
        UUID uuid = armorStand.getUniqueId();
        if (moving.containsKey(player)) {
            player.sendMessage(getMessage("armorstands.move.player_already_moving"));
            return;
        }
        if (moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("armorstands.move.armorstand_already_moving"));
            return;
        }

        float yaw = player.getLocation().getYaw();
        boolean normalizeYaw = yaw <= -90 || yaw >= 90;
        float playerStart = axis.getYawOrPitch(player.getLocation(), normalizeYaw);
        double armorStandStart = axis.getValue(armorStand.getLocation());
        boolean negate = axis.shouldNegate(playerStart);

        moving.put(player, new ArmorStandPositionMovement(armorStand, Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            float playerCurrent = axis.getYawOrPitch(player.getLocation(), normalizeYaw);
            double difference = (playerCurrent - playerStart) / 60D;
            if (negate)
                difference = -difference;

            Location loc = armorStand.getLocation().clone();
            axis.setValue(loc, armorStandStart + difference);
            armorStand.teleport(loc);
        }, 0, 1), armorStand.getLocation()));
        run();
    }

    private void startMoveBodyPart(Player player, ArmorStand armorStand, BodyPart bodyPart) {
        UUID uuid = armorStand.getUniqueId();
        if (moving.containsKey(player)) {
            player.sendMessage(getMessage("armorstands.move.player_already_moving"));
            return;
        }
        if (moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("armorstands.move.armorstand_already_moving"));
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

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        moving.forEach((player, movement) -> {
            if (movement instanceof ArmorStandPositionMovement)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.position.title")));
            else if (movement instanceof ArmorStandBodyPartMovement) {
                BodyPart bodyPart = ((ArmorStandBodyPartMovement) movement).bodyPart;
                String activated = getMessage("armorstands.move.title.color_activated"),
                        deactivated = getMessage("armorstands.move.title.color_deactivated");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.move.title.text")
                        .replace("%normal%", bodyPart.normalYawDir.getString(bodyPart.normalPitchDir))
                        .replace("%sneak%", bodyPart.sneakYawDir.getString(bodyPart.sneakPitchDir))
                        .replace("%color_normal%", !player.isSneaking() ? activated : deactivated)
                        .replace("%color_sneak%", player.isSneaking() ? activated : deactivated)));
            }
        });
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
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (moving.containsKey(player) && moving.get(player) instanceof ArmorStandBodyPartMovement movement) {

            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), this);
            Location loc = player.getLocation();
            movement.zeroYaw = loc.getYaw();
            movement.zeroPitch = loc.getPitch();
            movement.zeroAngle = movement.bodyPart.get(movement.armorStand);
        }
    }

    @EventHandler
    public void onMovementInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!moving.containsKey(player))
            return;

        Action action = event.getAction();
        if (action != Action.PHYSICAL) {
            event.setCancelled(true);
            onMovementInteraction(player, action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        }
    }

    @EventHandler
    public void onMovementInteraction(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!moving.containsKey(player))
            return;

        event.setCancelled(true);
        onMovementInteraction(player, false);
    }

    @EventHandler
    public void onMovementInteraction(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;
        if (!moving.containsKey(player))
            return;

        if (event.getEntityType() == EntityType.ARMOR_STAND)
            event.setCancelled(true);
        onMovementInteraction(player, true);
    }

    @SuppressWarnings("deprecation")
    public void onMovementInteraction(Player player, boolean leftClick) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        ArmorStandMovement movement = moving.remove(player);
        if (movement.task != null)
            movement.task.cancel();

        if (!leftClick) {
            if (movement instanceof ArmorStandPositionMovement)
                movement.armorStand.teleport(((ArmorStandPositionMovement) movement).cancelLocation);
            else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement)
                bodyPartMovement.bodyPart.apply(movement.armorStand, bodyPartMovement.cancelAngle);
            playArmorStandBreakSound(player);
        } else
            playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    private Location getRelativeArmorStandPosition(Player player, ArmorStand armorStand) {
        Location playerLoc = player.getLocation();
        Location loc = playerLoc.clone().add(playerLoc.getDirection().multiply(3));
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

        return loc.setDirection(playerLoc.clone().subtract(armorStand.getLocation()).toVector());
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

        gui.setItem(1, 9, checkDeactivated(applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.SHULKER_SHELL), getMessage("armorstands.private.name"),
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
        int row = index / 2 + 2, col = index % 2 == 0 ? 7 : 8;

        Material mat;
        String key;
        FeatureData feature;
        FeaturesData features = Config.get().features;
        switch (index) {
            case 0:
                mat = Material.GLASS;
                key = "invisible";
                feature = features.invisibility;
                break;
            case 1:
                mat = Material.GOLDEN_APPLE;
                key = "invulnerable";
                feature = features.invulnerability;
                break;
            case 2:
                mat = Material.STICK;
                key = "show_arms";
                feature = features.showArms;
                break;
            case 3:
                mat = Material.PHANTOM_MEMBRANE;
                key = "gravity";
                feature = features.gravity;
                break;
            case 4:
                mat = Material.STONE_SLAB;
                key = "base_plate";
                feature = features.basePlate;
                break;
            case 5:
                mat = Material.TOTEM_OF_UNDYING;
                key = "small";
                feature = features.small;
                break;
            default:
                throw new IllegalArgumentException("Parameter \"index\" out of range.");
        }

        gui.setItem(row, col, checkDeactivated(applyNameAndLore(ItemBuilder.from(mat), "armorstands.settings." + key, enabled).glow(enabled).asGuiItem(event -> {
            playSpyglassSound(player);

            boolean newEnabled = !enabled;
            switch (index) {
                case 0 -> armorStand.setInvisible(newEnabled);
                case 1 -> armorStand.setInvulnerable(newEnabled);
                case 2 -> armorStand.setArms(newEnabled);
                case 3 -> armorStand.setGravity(newEnabled);
                case 4 -> armorStand.setBasePlate(newEnabled);
                case 5 -> armorStand.setSmall(newEnabled);
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

    private ItemStack[] getEquipment(ArmorStand armorStand, Player player) {
        EntityEquipment equipment = armorStand.getEquipment();
        return new ItemStack[]{getEquipmentItem(EquipmentSlot.HEAD, equipment.getHelmet(), player),
                getEquipmentItem(EquipmentSlot.CHEST, equipment.getChestplate(), player),
                getEquipmentItem(EquipmentSlot.LEGS, equipment.getLeggings(), player),
                getEquipmentItem(EquipmentSlot.FEET, equipment.getBoots(), player),
                getEquipmentItem(EquipmentSlot.HAND, equipment.getItemInMainHand(), player),
                getEquipmentItem(EquipmentSlot.OFF_HAND, equipment.getItemInOffHand(), player)};
    }

    @SuppressWarnings("deprecation")
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
                armorStand.isInvulnerable(),
                armorStand.hasArms(),
                armorStand.hasGravity(),
                armorStand.hasBasePlate(),
                armorStand.isSmall()};
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

        void setValue(Location loc, double value) {
            switch (this) {
                case X -> loc.setX(value);
                case Y -> loc.setY(value);
                case Z -> loc.setZ(value);
            }
        }

        float getYawOrPitch(Location loc, boolean normalizeYaw) {
            return switch (this) {
                case X, Z -> {
                    float yaw = loc.getYaw();
                    if (normalizeYaw)
                        while (yaw < 0)
                            yaw += 360;
                    yield yaw;
                }
                case Y -> loc.getPitch();
            };
        }

        boolean shouldNegate(float yaw) {
            while (yaw < 0)
                yaw += 360;
            while (yaw > 360)
                yaw -= 360;

            return switch (this) {
                case X -> yaw > 270 || yaw < 90;
                case Y -> true;
                case Z -> yaw < 180;
            };
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

            Location cancelLocation;

            public ArmorStandPositionMovement(ArmorStand armorStand, BukkitTask task, Location cancelLocation) {
                super(armorStand);
                this.task = task;
                this.cancelLocation = cancelLocation;
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
