package de.rapha149.armorstandeditor.version;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface VersionWrapper {

    String INVISIBLE_TAG = "ArmorStandEditor-Invisible";
    String FIRE_TAG = "ArmorStandEditor-Fire";

    Optional<String> getCustomNameJson(ArmorStand armorStand);

    void setCustomName(ArmorStand armorStand, String customNameJson);

    void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart);

    void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart, Axis axis);

    ItemStack getArmorstandItem(ArmorStand armorStand, NamespacedKey privateKey);

    ItemStack prepareRecipeResult(ItemStack item);
}
