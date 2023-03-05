package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.PermissionsData;
import de.rapha149.armorstandeditor.pages.ArmorPage;
import de.rapha149.armorstandeditor.pages.Page;
import de.rapha149.armorstandeditor.pages.Page.GuiResult;
import de.rapha149.armorstandeditor.pages.SettingsPage;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class Util {

    public static final NamespacedKey PRIVATE_KEY = NamespacedKey.fromString("private", ArmorStandEditor.getInstance());
    public static final List<Integer> EQUIPMENT_SLOTS = List.of(11, 20, 29, 38, 19, 21);

    public static final Page armorPage = new ArmorPage();
    public static final Page settingsPage = new SettingsPage();

    public static Map<Player, ArmorStandStatus> invs = new HashMap<>();
    public static Map<Long, AnvilGUI> anvilInvs = new HashMap<>();
    public static boolean disabling = false;

    public static void onDisable() {
        disabling = true;
        invs.values().forEach(status -> {
            saveEquipment(status);
            status.player.closeInventory();
        });
        invs.clear();
        anvilInvs.values().forEach(AnvilGUI::closeInventory);
        anvilInvs.clear();
        Events.moving.values().forEach(Events::cancelMovement);
        Events.moving.clear();
    }

    public static void openGUI(Player player, ArmorStand armorStand, int page, boolean advancedControls) {
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

        GuiResult result = (switch (page) {
            case 1 -> armorPage;
            case 2 -> settingsPage;
            default -> throw new IllegalArgumentException("Invalid page: " + page);
        }).getGui(player, armorStand, adminBypass);

/*            String key = "armorstands.advanced_controls." + switch (page) {
                case 1 -> "position";
                case 2 -> "rotation";
                case 3 -> "pose";
                default -> throw new IllegalStateException("Unexpected value: " + page);
            } + ".";

            int deactivatedStatus = getDeactivatedStatus(features.advancedControls, player);
            if (deactivatedStatus == 0) {
                deactivatedStatus = getDeactivatedStatus(switch (page) {
                    case 1 -> features.movePosition;
                    case 2 -> features.rotate;
                    case 3 -> features.moveBodyParts;
                    default -> throw new IllegalStateException("Unexpected value: " + page);
                }, player);
            }

            gui = Gui.gui().title(Component.text(getMessage(key + "title"))).rows(deactivatedStatus != 0 ? 5 : 6).disableAllInteractions().create();
            status = new ArmorStandStatus(player, armorStand, gui, page, advancedControls);

            gui.setItem(1, 1, applyNameAndLore(ItemBuilder.from(Material.ARROW), "armorstands.advanced_controls.leave").asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, 1, false));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));

            if (deactivatedStatus != 0) {
                gui.setItem(3, 5, applyNameAndLoreWithoutKeys(ItemBuilder.from(Material.BARRIER),
                        getMessage("armorstands.advanced_controls.deactivated").replace("%menu%", getMessage(key + "name")),
                        getMessage("armorstands.features." + (deactivatedStatus == 1 ? "deactivated" : "no_permission")), false).asGuiItem());
            } else {
                switch (page) {

                }
            }
        }*/

        Gui gui = result.gui();
        ArmorStandStatus status = result.status();

        int maxPages = advancedControls ? 3 : 2;
        if (page > 1) {
            gui.setItem(gui.getRows(), 1, ItemBuilder.from(Material.SPECTRAL_ARROW).name(Component.text(getMessage("armorstands.page.back")
                    .replace("%current%", String.valueOf(page)).replace("%max%", String.valueOf(maxPages)))).asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, page - 1, advancedControls));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));
        }
        if (page < maxPages) {
            gui.setItem(gui.getRows(), 9, ItemBuilder.from(Material.SPECTRAL_ARROW).name(Component.text(getMessage("armorstands.page.forward")
                    .replace("%current%", String.valueOf(page)).replace("%max%", String.valueOf(maxPages)))).asGuiItem(event -> {
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, page + 1, advancedControls));
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
            }));
        }

        gui.setCloseGuiAction(event -> {
            result.closeAction().run();

            if (invs.containsKey(player) && invs.get(player).time == status.time)
                invs.remove(player);
        });

        gui.open(player);
        invs.put(player, status);
    }

    public static void playAnvilSound(Player player) {
        playSound(player, Sound.BLOCK_ANVIL_PLACE);
    }

    public static void playBassSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1F);
    }

    public static void playArmorStandHitSound(Player player) {
        playSound(player, Sound.ENTITY_ARMOR_STAND_HIT, 1F);
    }

    public static void playArmorStandBreakSound(Player player) {
        playSound(player, Sound.ENTITY_ARMOR_STAND_BREAK);
    }

    public static void playSpyglassSound(Player player) {
        playSound(player, Sound.ITEM_SPYGLASS_USE, 1F);
    }

    public static void playExperienceSound(Player player) {
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public static void playStepSound(Player player) {
        playSound(player, Sound.BLOCK_ANVIL_STEP);
    }

    public static void playSound(Player player, Sound sound) {
        playSound(player, sound, 0.75F);
    }

    public static void playSound(Player player, Sound sound, float volume) {
        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, 1F);
    }

    public static boolean isArmorStandUsed(Player exclude, ArmorStand armorStand) {
        UUID uuid = exclude.getUniqueId();
        UUID armorStandUuid = armorStand.getUniqueId();
        return invs.entrySet().stream().anyMatch(entry -> !entry.getKey().getUniqueId().equals(uuid) &&
                                                               entry.getValue().armorStand.getUniqueId().equals(armorStandUuid)) ||
               Events.moving.entrySet().stream().anyMatch(entry -> !entry.getKey().getUniqueId().equals(uuid) &&
                                                            entry.getValue().armorStand.getUniqueId().equals(armorStandUuid)) ||
               Events.vehicleSelection.entrySet().stream().anyMatch(entry -> !entry.getKey().getUniqueId().equals(uuid) &&
                                                                      entry.getValue().getKey().getUniqueId().equals(armorStandUuid));
    }

    public static boolean saveEquipment(ArmorStandStatus status) {
        if (!status.saveEquipment)
            return false;
        status.saveEquipment = false;

        Player player = status.player;
        ArmorStand armorStand = status.armorStand;
        Gui gui = status.gui;

        Inventory inv = gui.getInventory();
        List<ItemStack> newItems = EQUIPMENT_SLOTS.stream().map(slot -> Optional.ofNullable(inv.getItem(slot)).orElseGet(() -> new ItemStack(Material.AIR))).toList();

        EntityEquipment equipment = armorStand.getEquipment();
        equipment.setHelmet(newItems.get(0));
        equipment.setItemInMainHand(newItems.get(4));
        equipment.setItemInOffHand(newItems.get(5));

        AtomicBoolean messageSent = new AtomicBoolean(false);
        Map.of(1, "CHESTPLATE",
                2, "LEGGINGS",
                3, "BOOTS").forEach((slot, type) -> {
            ItemStack item = newItems.get(slot);
            if (!item.getType().isAir() && !item.getType().toString().endsWith("_" + type)) {
                for (ItemStack drop : player.getInventory().addItem(item).values())
                    player.getWorld().dropItem(player.getLocation(), drop);

                if (!messageSent.get()) {
                    messageSent.set(true);
                    player.sendMessage(getMessage("armorstands.equipment.invalid"));
                    playBassSound(player);
                }

                item = new ItemStack(Material.AIR);
            }
            switch (slot) {
                case 1 -> equipment.setChestplate(item);
                case 2 -> equipment.setLeggings(item);
                case 3 -> equipment.setBoots(item);
            }
        });

        return true;
    }

    public enum Axis {
        X, Y, Z;

        public double getValue(Location loc) {
            return switch (this) {
                case X -> loc.getX();
                case Y -> loc.getY();
                case Z -> loc.getZ();
            };
        }

        public double getBlockValue(Location loc) {
            return switch (this) {
                case X -> loc.getBlockX();
                case Y -> loc.getBlockY();
                case Z -> loc.getBlockZ();
            };
        }

        public void setValue(Location loc, double value) {
            switch (this) {
                case X -> loc.setX(value);
                case Y -> loc.setY(value);
                case Z -> loc.setZ(value);
            }
        }
    }

    public static class ArmorStandStatus {

        public long time;
        public Player player;
        public ArmorStand armorStand;
        public Gui gui;
        public int page;
        public boolean advancedControls;
        public boolean saveEquipment = false;

        public ArmorStandStatus(Player player, ArmorStand armorStand, Gui gui, int page, boolean advancedControls) {
            this.time = System.currentTimeMillis();
            this.player = player;
            this.armorStand = armorStand;
            this.gui = gui;
            this.page = page;
            this.advancedControls = advancedControls;
        }
    }
}
