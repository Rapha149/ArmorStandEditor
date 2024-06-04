package de.rapha149.armorstandeditor.version;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

public interface VersionWrapper {

    String INVISIBLE_TAG = "ArmorStandEditor-Invisible";

    GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    LegacyComponentSerializer EDIT_SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();
    LegacyComponentSerializer DISPLAY_SERIALIZER = LegacyComponentSerializer.builder().hexColors().build();

    String getCustomNameForEdit(ArmorStand armorStand);

    Component getCustomNameForDisplay(ArmorStand armorStand);

    void setCustomName(ArmorStand armorStand, String customName);

    void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart);

    void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart, Axis axis);

    ItemStack getArmorstandItem(ArmorStand armorStand, NamespacedKey privateKey);

    ItemStack prepareRecipeResult(ItemStack item);
}
