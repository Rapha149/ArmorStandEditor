package de.rapha149.armorstandeditor.version;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.Map.Entry;

public interface VersionWrapper {

    String ITEM_IDENTIFIER = "ArmorStandEditor";
    String ORIGINAL_SLOT_IDENTIFIER = "OriginalSlot";
    String INVISIBLE_TAG = "ArmorStandEditor-Invisible";

    GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    LegacyComponentSerializer EDIT_SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();
    LegacyComponentSerializer DISPLAY_SERIALIZER = LegacyComponentSerializer.builder().hexColors().build();

    String getCustomNameForEdit(ArmorStand armorStand);

    Component getCustomNameForDisplay(ArmorStand armorStand);

    void setCustomName(ArmorStand armorStand, String customName);

    void resetArmorstandPosition(ArmorStand armorStand, BodyPart bodyPart);

    ItemStack getArmorstandItem(ArmorStand armorStand, NamespacedKey privateKey);

    boolean isArmorstandItem(ItemStack item);

    ItemStack prepareRecipeResult(ItemStack item, int originalSlot);

    Entry<ItemStack, Integer> getRecipeResultAndOriginalSlot(ItemStack item);
}
