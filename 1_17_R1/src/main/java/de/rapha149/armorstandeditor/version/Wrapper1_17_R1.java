package de.rapha149.armorstandeditor.version;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent.ChatSerializer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Map.Entry;

public class Wrapper1_17_R1 implements VersionWrapper {

    private final EntityArmorStand defaultArmorStand = new EntityArmorStand(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(), 0, 0, 0);

    @Override
    public String getCustomNameForEdit(ArmorStand armorStand) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        if (!handle.hasCustomName())
            return null;

        return EDIT_SERIALIZER.serialize(GSON_SERIALIZER.deserialize(ChatSerializer.a(handle.getCustomName())));
    }

    @Override
    public Component getCustomNameForDisplay(ArmorStand armorStand) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        if (!handle.hasCustomName())
            return null;

        return GSON_SERIALIZER.deserialize(ChatSerializer.a(handle.getCustomName()));
    }

    @Override
    public void setCustomName(ArmorStand armorStand, String customName) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        handle.setCustomName(customName == null ? null : ChatSerializer.a(GSON_SERIALIZER.serialize(EDIT_SERIALIZER.deserialize(customName))));
    }

    @Override
    public void resetArmorstandPosition(ArmorStand armorStand, BodyPart bodyPart) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        switch (bodyPart) {
            case HEAD -> handle.setHeadPose(defaultArmorStand.cg);
            case BODY -> handle.setBodyPose(defaultArmorStand.ch);
            case LEFT_ARM -> handle.setLeftArmPose(defaultArmorStand.ci);
            case RIGHT_ARM -> handle.setRightArmPose(defaultArmorStand.cj);
            case LEFT_LEG -> handle.setLeftLegPose(defaultArmorStand.ck);
            case RIGHT_LEG -> handle.setRightLegPose(defaultArmorStand.cl);
        }
    }

    @Override
    public ItemStack getArmorstandItem(ArmorStand armorStand, NamespacedKey privateKey) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        NBTTagCompound entityNBT = new NBTTagCompound();
        handle.saveData(entityNBT);

        NBTTagList tags = new NBTTagList();
        tags.add(NBTTagString.a(INVISIBLE_TAG));
        armorStand.getScoreboardTags().forEach(tag -> tags.add(NBTTagString.a(tag)));

        NBTTagCompound nbt = new NBTTagCompound();
        if (handle.hasCustomName())
            nbt.setString("CustomName", ChatSerializer.a(handle.getCustomName()));
        nbt.setBoolean("CustomNameVisible", armorStand.isCustomNameVisible());
        if (armorStand.isInvisible())
            nbt.set("Tags", tags);
        nbt.setBoolean("NoGravity", !armorStand.hasGravity());
        nbt.setBoolean("Silent", armorStand.isSilent());
        nbt.setBoolean("Invulnerable", armorStand.isInvulnerable());
        nbt.setBoolean("ShowArms", armorStand.hasArms());
        nbt.setBoolean("Small", armorStand.isSmall());
        nbt.setBoolean("Marker", armorStand.isMarker());
        nbt.setBoolean("NoBasePlate", !armorStand.hasBasePlate());
        nbt.setBoolean("Glowing", armorStand.isGlowing());
        nbt.setBoolean("HasVisualFire", armorStand.isVisualFire());
        nbt.set("ArmorItems", entityNBT.get("ArmorItems"));
        nbt.set("HandItems", entityNBT.get("HandItems"));
        nbt.set("Pose", entityNBT.getCompound("Pose"));
        nbt.setInt("DisabledSlots", entityNBT.getInt("DisabledSlots"));

        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        if (pdc.has(privateKey, PersistentDataType.STRING)) {
            NBTTagCompound bukkitValues = new NBTTagCompound();
            bukkitValues.setString(privateKey.toString(), pdc.get(privateKey, PersistentDataType.STRING));
            nbt.set("BukkitValues", bukkitValues);
        }

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(new ItemStack(Material.ARMOR_STAND));
        NBTTagCompound itemNBT = nmsItem.getOrCreateTag();
        itemNBT.set("EntityTag", nbt);
        itemNBT.setBoolean(ITEM_IDENTIFIER, true);
        ItemStack item = CraftItemStack.asBukkitCopy(nmsItem);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(armorStand.getCustomName());
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public boolean isArmorstandItem(ItemStack item) {
        if (item == null)
            return false;
        NBTTagCompound nbt = CraftItemStack.asNMSCopy(item).getTag();
        return nbt != null && nbt.hasKey(ITEM_IDENTIFIER) && nbt.getBoolean(ITEM_IDENTIFIER);
    }

    @Override
    public ItemStack prepareRecipeResult(ItemStack item, int originalSlot) {
        if (item.getType() != Material.ARMOR_STAND || !isArmorstandItem(item))
            return null;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbt = nmsItem.getTag();

        if (nbt.hasKey("EntityTag")) {
            NBTTagCompound entityNBT = nbt.getCompound("EntityTag");
            entityNBT.remove("ArmorItems");
            entityNBT.remove("HandItems");
        }

        nbt.setInt(ORIGINAL_SLOT_IDENTIFIER, originalSlot);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public Entry<ItemStack, Integer> getRecipeResultAndOriginalSlot(ItemStack item) {
        if (item.getType() != Material.ARMOR_STAND || !isArmorstandItem(item))
            return null;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbt = nmsItem.getTag();
        if (!nbt.hasKey(ORIGINAL_SLOT_IDENTIFIER))
            return null;

        int originalSlot = nbt.getInt(ORIGINAL_SLOT_IDENTIFIER);
        nbt.remove(ORIGINAL_SLOT_IDENTIFIER);
        return Map.entry(CraftItemStack.asBukkitCopy(nmsItem), originalSlot);
    }
}
