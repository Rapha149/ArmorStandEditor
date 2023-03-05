package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Config;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Util;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public abstract class Page {

    final VersionWrapper wrapper = ArmorStandEditor.getInstance().wrapper;
    final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();

    public abstract GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass);

    ItemBuilder applyNameAndLore(ItemBuilder builder, String key) {
        return applyNameAndLore(builder, key + ".name", key + ".lore");
    }

    ItemBuilder applyNameAndLore(ItemBuilder builder, String key, Map<String, String> relacements) {
        return applyNameAndLore(builder, key + ".name", key + ".lore", relacements);
    }

    ItemBuilder applyNameAndLore(ItemBuilder builder, String key, boolean status) {
        return applyNameAndLore(builder, key + ".name", key + ".lore", status);
    }

    ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore) {
        return applyNameAndLoreWithoutKeys(builder, getMessage(name), getMessage(lore));
    }

    ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore, Map<String, String> replacements) {
        return applyNameAndLoreWithoutKeys(builder, getMessage(name), getMessage(lore), replacements);
    }

    ItemBuilder applyNameAndLore(ItemBuilder builder, String name, String lore, boolean status) {
        return applyNameAndLoreWithoutKeys(builder, getMessage(name), getMessage(lore), status);
    }

    ItemBuilder applyNameAndLoreWithoutKeys(ItemBuilder builder, String name, String lore, boolean status) {
        return applyNameAndLoreWithoutKeys(builder, name, lore, Map.of("%status%", getMessage("armorstands.status." + (status ? "on" : "off"))));
    }

    ItemBuilder applyNameAndLoreWithoutKeys(ItemBuilder builder, String name, String lore) {
        return applyNameAndLoreWithoutKeys(builder, name, lore, Collections.emptyMap());
    }

    ItemBuilder applyNameAndLoreWithoutKeys(ItemBuilder builder, String name, String lore, Map<String, String> replacements) {
        AtomicReference<String> nameRef = new AtomicReference<>(name);
        AtomicReference<String> loreRef = new AtomicReference<>(lore);
        replacements.forEach((key, value) -> {
            nameRef.set(nameRef.get().replace(key, value));
            loreRef.set(loreRef.get().replace(key, value));
        });

        builder.name(SERIALIZER.deserialize(nameRef.get()).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            builder.lore(Arrays.stream(loreRef.get().split("\n")).map(line -> SERIALIZER.deserialize(line)
                    .decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()));
        }
        return builder;
    }

    boolean isDeactivated(FeatureData feature, Player player) {
        return !feature.enabled || (feature.permission != null && !player.hasPermission(feature.permission));
    }

    int getDeactivatedStatus(FeatureData feature, Player player) {
        if (!feature.enabled)
            return 1;
        if (feature.permission != null && !player.hasPermission(feature.permission))
            return 2;
        return 0;
    }

    Material getDeactivatedMaterial(Material original) {
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

    @SuppressWarnings("deprecation")
    GuiItem checkDeactivated(GuiItem item, FeatureData feature, Player player) {
        boolean noPermission = false;
        if (feature.enabled) {
            noPermission = true;
            if (feature.permission == null || player.hasPermission(feature.permission))
                return item;
        }

        ItemStack itemStack = item.getItemStack();
        Material mat = getDeactivatedMaterial(itemStack.getType());

        ItemBuilder builder = ItemBuilder.from(mat);
        if (mat == Material.AIR)
            return builder.asGuiItem();

        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
            builder.setName(itemStack.getItemMeta().getDisplayName());
        builder.lore(Arrays.stream(getMessage("armorstands.features." + (noPermission ? "no_permission" : "deactivated")).split("\n"))
                .map(line -> (Component) Component.text(line)).collect(Collectors.toList()));

        return builder.setNbt("deactivated", true).asGuiItem(event -> Util.playBassSound(player));
    }

    public record GuiResult(Gui gui, ArmorStandStatus status, Runnable closeAction) {

        public GuiResult(Gui gui, ArmorStandStatus status) {
            this(gui, status, () -> {});
        }
    }
}