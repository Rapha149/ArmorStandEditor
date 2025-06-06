package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.CooldownData;
import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Config.PermissionsData;
import de.rapha149.armorstandeditor.Messages.Message;
import de.rapha149.armorstandeditor.pages.*;
import de.rapha149.armorstandeditor.pages.Page.GuiResult;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Messages.getMessage;
import static de.rapha149.armorstandeditor.Messages.getRawMessage;

public class Util {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    public static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    public static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    public static final LegacyComponentSerializer EDIT_SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();

    public static final NamespacedKey PRIVATE_KEY = NamespacedKey.fromString("private", ArmorStandEditor.getInstance());
    public static final NamespacedKey ITEM_KEY = NamespacedKey.fromString("item", ArmorStandEditor.getInstance());
    public static final NamespacedKey ORIGINAL_SLOT_KEY = NamespacedKey.fromString("originalslot", ArmorStandEditor.getInstance());
    public static final List<Integer> EQUIPMENT_SLOTS = List.of(11, 20, 29, 38, 19, 21);

    public static final Page ARMOR_PAGE = new ArmorPage();
    public static final Page SETTINGS_PAGE = new SettingsPage();
    public static final Page ADVANCED_POSITION_PAGE = new AdvancedPositionPage();
    public static final Page ADVANCED_ROTATION_PAGE = new AdvancedRotationPage();
    public static final Page ADVANCED_POSE_PAGE = new AdvancedPosePage();
    public static final Page ADVANCED_POSE_PRESETS_PAGE = new AdvancedPosePresetsPage();

    private static final Map<Material, String> ADVANCED_PAGES = new LinkedHashMap<>() {{
        put(Material.FEATHER, "position");
        put(Material.ENDER_EYE, "rotation");
        put(Material.STICK, "pose");
    }};

    public static Map<Player, ArmorStandStatus> invs = new HashMap<>();
    public static Map<Long, AnvilGUI> anvilInvs = new HashMap<>();
    public static List<UUID> armorPageCooldowns = new ArrayList<>();
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

    public static void makeArmorstandItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public static boolean isArmorstandItem(ItemStack item) {
        PersistentDataContainer pdc;
        return item.hasItemMeta() &&
               (pdc = item.getItemMeta().getPersistentDataContainer()).has(ITEM_KEY, PersistentDataType.BYTE) &&
               pdc.get(ITEM_KEY, PersistentDataType.BYTE) == 1;
    }

    public static void openGUI(Player player, ArmorStand armorStand, int page, boolean advancedControls) {
        PermissionsData permissions = Config.get().permissions;
        if (permissions.general != null && !player.hasPermission(permissions.general)) {
            player.closeInventory();
            player.spigot().sendMessage(getMessage("no_permission").spigot());
            playBassSound(player);
            return;
        }

        boolean adminBypass = false;
        String privateValue = armorStand.getPersistentDataContainer().get(PRIVATE_KEY, PersistentDataType.STRING);
        if (privateValue != null && !privateValue.isEmpty() && !privateValue.equals(player.getUniqueId().toString())) {
            if (permissions.ignorePrivate != null && !player.hasPermission(permissions.ignorePrivate)) {
                player.spigot().sendMessage(getMessage("armorstands.no_permission").spigot());
                return;
            }
            adminBypass = true;
        }

        if (isArmorStandUsed(player, armorStand)) {
            player.closeInventory();
            player.spigot().sendMessage(getMessage("armorstands.already_open").spigot());
            playBassSound(player);
            return;
        }

        if (page == 1) {
            CooldownData cooldown = Config.get().armorPageCooldown;
            if (cooldown.ticks > 0) {
                UUID uuid = armorStand.getUniqueId();
                if (armorPageCooldowns.contains(uuid)) {
                    if (cooldown.message)
                        player.spigot().sendMessage(getMessage("armorstands.armor_page_cooldown").spigot());
                    return;
                }
                armorPageCooldowns.add(uuid);
                Bukkit.getScheduler().runTaskLater(ArmorStandEditor.getInstance(), () -> armorPageCooldowns.remove(uuid), cooldown.ticks);
            }
        }

        GuiResult result = (!advancedControls ? switch (page) {
            case 1 -> ARMOR_PAGE;
            case 2 -> SETTINGS_PAGE;
            default -> throw new IllegalArgumentException("Invalid page: " + page);
        } : switch (page) {
            case 1 -> ADVANCED_POSITION_PAGE;
            case 2 -> ADVANCED_ROTATION_PAGE;
            case 3 -> ADVANCED_POSE_PAGE;
            default -> throw new IllegalArgumentException("Invalid advanced page: " + page);
        }).getGui(player, armorStand, adminBypass);

        Gui gui = result.gui();
        ArmorStandStatus status = result.status();

        addPageItems(player, armorStand, gui, page, advancedControls);

        gui.setCloseGuiAction(event -> {
            result.closeAction().run();

            if (invs.containsKey(player) && invs.get(player).time == status.time)
                invs.remove(player);
        });

        gui.open(player);
        invs.put(player, status);
    }

    public static void openGUI(Player player, ArmorStand armorStand, Page page) {
        PermissionsData permissions = Config.get().permissions;
        if (permissions.general != null && !player.hasPermission(permissions.general)) {
            player.closeInventory();
            player.spigot().sendMessage(getMessage("no_permission").spigot());
            playBassSound(player);
            return;
        }

        boolean adminBypass = false;
        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        if (pdc.has(PRIVATE_KEY, PersistentDataType.STRING) && !pdc.get(PRIVATE_KEY, PersistentDataType.STRING).equals(player.getUniqueId().toString())) {
            if (permissions.ignorePrivate != null && !player.hasPermission(permissions.ignorePrivate)) {
                player.spigot().sendMessage(getMessage("armorstands.no_permission").spigot());
                return;
            }
            adminBypass = true;
        }

        if (isArmorStandUsed(player, armorStand)) {
            player.closeInventory();
            player.spigot().sendMessage(getMessage("armorstands.already_open").spigot());
            playBassSound(player);
            return;
        }

        GuiResult result = page.getGui(player, armorStand, adminBypass);
        Gui gui = result.gui();
        ArmorStandStatus status = result.status();

        gui.setCloseGuiAction(event -> {
            result.closeAction().run();

            if (invs.containsKey(player) && invs.get(player).time == status.time)
                invs.remove(player);
        });

        gui.open(player);
        invs.put(player, status);
    }

    public static void addPageItems(Player player, ArmorStand armorStand, Gui gui, int page, boolean advancedControls) {
        if (!advancedControls) {
            int maxPages = advancedControls ? 3 : 2;
            if (page > 1) {
                gui.setItem(gui.getRows(), 1, ItemBuilder.from(Material.SPECTRAL_ARROW).name(getMessage("armorstands.page.back", Map.of(
                        "%current%", String.valueOf(page),
                        "%max%", String.valueOf(maxPages)
                )).adventure()).asGuiItem(event -> {
                    Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, page - 1, advancedControls));
                    playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
                }));
            }
            if (page < maxPages) {
                gui.setItem(gui.getRows(), 9, ItemBuilder.from(Material.SPECTRAL_ARROW).name(getMessage("armorstands.page.forward", Map.of(
                        "%current%", String.valueOf(page),
                        "%max%", String.valueOf(maxPages)
                )).adventure()).asGuiItem(event -> {
                    Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, page + 1, advancedControls));
                    playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
                }));
            }
        } else {
            FeaturesData features = Config.get().features;
            AtomicInteger index = new AtomicInteger(1);
            ADVANCED_PAGES.forEach((mat, key) -> {
                int i = index.getAndIncrement();
                boolean isCurrent = i == page;
                gui.setItem(gui.getRows(), i + 3, checkDeactivated(applyNameAndLore(ItemBuilder.from(mat), "armorstands.advanced_controls.page_item",
                        Map.of("%menu%", getRawMessage("armorstands.advanced_controls." + key + ".name"))).glow(isCurrent).asGuiItem(event -> {
                    if (isCurrent) {
                        playBassSound(player);
                        return;
                    }

                    Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> openGUI(player, armorStand, i, advancedControls));
                    playSound(player, Sound.ITEM_BOOK_PAGE_TURN);
                }), switch (i) {
                    case 1 -> features.movePosition;
                    case 2 -> features.rotate;
                    case 3 -> features.moveBodyParts;
                    default -> throw new IllegalStateException("Unexpected value: " + i);
                }, player, true));
            });
        }
    }

    public static ItemBuilder applyNameAndLore(ItemBuilder builder, String key) {
        return applyNameAndLore(builder, key + ".name", key + ".lore");
    }

    public static ItemBuilder applyNameAndLore(ItemBuilder builder, String key, Map<String, String> replacements) {
        return applyNameAndLore(builder, key + ".name", key + ".lore", replacements);
    }

    public static ItemBuilder applyNameAndLore(ItemBuilder builder, String key, boolean status) {
        return applyNameAndLore(builder, key + ".name", key + ".lore", status);
    }

    public static ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore) {
        return applyNameAndLore(builder, name, lore, Collections.emptyMap());
    }

    public static ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore, boolean status) {
        return applyNameAndLore(builder, name, lore, Map.of("%status%", getRawMessage("armorstands.status." + (status ? "on" : "off"))));
    }

    public static ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore, Map<String, String> replacements) {
        builder.name(getMessage(name, replacements).adventure());
        if (!lore.isEmpty()) {
            builder.lore(Arrays.stream(getRawMessage(lore).split("\n|\\\\n")).map(line ->
                    new Message(line, replacements).adventure().decoration(TextDecoration.ITALIC, false)).toList());
        }
        return builder;
    }

    public static ItemBuilder applyNameAndLoreWithoutKeys(ItemBuilder builder, String name, String lore) {
        return applyNameAndLoreWithoutKeys(builder, name, lore, Collections.emptyMap());
    }

    public static ItemBuilder applyNameAndLoreWithoutKeys(ItemBuilder builder, String name, String lore, Map<String, String> replacements) {
        builder.name(new Message(name, replacements).adventure());
        if (!lore.isEmpty()) {
            builder.lore(Arrays.stream(lore.split("\n|\\\\n")).map(line ->
                    new Message(line, replacements).adventure().decoration(TextDecoration.ITALIC, false)).toList());
        }
        return builder;
    }

    public static int getDeactivatedStatus(FeatureData feature, Player player) {
        if (!feature.enabled)
            return 1;
        if (feature.permission != null && !player.hasPermission(feature.permission))
            return 2;
        return 0;
    }

    public static boolean isDeactivated(FeatureData feature, Player player) {
        return getDeactivatedStatus(feature, player) != 0;
    }

    public static Material getDeactivatedMaterial(Material original) {
        String deactivatedItem = Config.get().deactivatedItem;
        if (deactivatedItem == null)
            return original;
        else {
            Material mat = Material.matchMaterial(deactivatedItem);
            if (mat == null)
                mat = Material.GRAY_DYE;
            return mat;
        }
    }

    public static GuiItem checkDeactivated(GuiItem item, FeatureData feature, Player player) {
        return checkDeactivated(item, feature, player, false);
    }

    public static GuiItem checkDeactivated(GuiItem item, FeatureData feature, Player player, boolean forceOriginalMaterial) {
        int status = getDeactivatedStatus(feature, player);
        if (status == 0)
            return item;

        return replaceWithDeactivatedItem(item, player, status, forceOriginalMaterial);
    }

    public static GuiItem replaceWithDeactivatedItem(GuiItem item, Player player, int status, boolean forceOriginalMaterial) {
        ItemStack itemStack = item.getItemStack();
        Optional<Component> name = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ?
                Optional.of(Component.text(itemStack.getItemMeta().getDisplayName())) : Optional.empty();
        return replaceWithDeactivatedItem(itemStack.getType(), name, player, status, forceOriginalMaterial);
    }

    public static GuiItem replaceWithDeactivatedItem(Material original, Optional<Component> name, Player player, int status, boolean forceOriginalMaterial) {
        Material mat = forceOriginalMaterial ? original : getDeactivatedMaterial(original);

        ItemBuilder builder = ItemBuilder.from(mat);
        if (mat == Material.AIR)
            return builder.asGuiItem();

        name.ifPresent(builder::name);
        builder.lore(Arrays.stream(getRawMessage("armorstands.features." + (status == 2 ? "no_permission" : "deactivated")).split("\n|\\\\n"))
                .map(line -> new Message(line).adventure()).collect(Collectors.toList()));

        return builder.setNbt("deactivated", true).asGuiItem(event -> Util.playBassSound(player));
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

    public static boolean teleportArmorStand(Player player, ArmorStand armorStand, Location loc) {
        int max = Config.get().features.movePosition.maxDistanceSquared;
        if (max == 0 || (player.getWorld().getUID().equals(loc.getWorld().getUID()) && player.getLocation().distanceSquared(loc) <= max)) {
            armorStand.teleport(loc);
            return true;
        }
        return false;
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
                    player.spigot().sendMessage(getMessage("armorstands.equipment.invalid").spigot());
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

    public static void runOneTimeItemClickAction(ArmorStandStatus status, Runnable action) {
        Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> {
            if (status.oneTimeItemClicked)
                return;
            status.oneTimeItemClicked = true;
            action.run();
        });
    }

    public static class ArmorStandStatus {

        public long time;
        public Player player;
        public ArmorStand armorStand;
        public Gui gui;
        public boolean saveEquipment = false;
        public boolean oneTimeItemClicked = false;

        public ArmorStandStatus(Player player, ArmorStand armorStand, Gui gui) {
            this.time = System.currentTimeMillis();
            this.player = player;
            this.armorStand = armorStand;
            this.gui = gui;
        }
    }
}
