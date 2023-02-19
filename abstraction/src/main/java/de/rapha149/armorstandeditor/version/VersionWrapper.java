package de.rapha149.armorstandeditor.version;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.Map.Entry;

public interface VersionWrapper {

    String ITEM_IDENTIFIER = "ArmorStandEditor";
    String ORIGINAL_SLOT_IDENTIFIER = "OriginalSlot";
    String INVISIBLE_TAG = "ArmorStandEditor-Invisible";

    void resetArmorstandPosition(ArmorStand armorStand, BodyPart bodyPart);

    ItemStack getArmorstandItem(ArmorStand armorStand, NamespacedKey privateKey);

    boolean isArmorstandItem(ItemStack item);

    ItemStack prepareRecipeResult(ItemStack item, int originalSlot);

    Entry<ItemStack, Integer> getRecipeResultAndOriginalSlot(ItemStack item);
}
