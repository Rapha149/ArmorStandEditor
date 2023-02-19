package de.rapha149.armorstandeditor.version;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent.ChatSerializer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Map.Entry;

public class Wrapper1_19_R2 implements VersionWrapper {

    private final EntityArmorStand defaultArmorStand = new EntityArmorStand(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(), 0, 0, 0);

    @Override
    public void resetArmorstandPosition(ArmorStand armorStand, BodyPart bodyPart) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        switch (bodyPart) {
            case HEAD -> handle.a(defaultArmorStand.cg);
            case BODY -> handle.b(defaultArmorStand.ch);
            case LEFT_ARM -> handle.c(defaultArmorStand.ci);
            case RIGHT_ARM -> handle.d(defaultArmorStand.cj);
            case LEFT_LEG -> handle.e(defaultArmorStand.ck);
            case RIGHT_LEG -> handle.f(defaultArmorStand.cl);
        }
    }

    @Override
    public ItemStack getArmorstandItem(ArmorStand armorStand, NamespacedKey privateKey) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        NBTTagCompound entityNBT = new NBTTagCompound();
        handle.b(entityNBT);

        NBTTagList tags = new NBTTagList();
        tags.add(NBTTagString.a(INVISIBLE_TAG));
        armorStand.getScoreboardTags().forEach(tag -> tags.add(NBTTagString.a(tag)));

        NBTTagCompound nbt = new NBTTagCompound();
        if (handle.ab())
            nbt.a("CustomName", ChatSerializer.b(handle.ac()));
        nbt.a("CustomNameVisible", armorStand.isCustomNameVisible());
        if (armorStand.isInvisible())
            nbt.a("Tags", tags);
        nbt.a("NoGravity", !armorStand.hasGravity());
        nbt.a("Silent", armorStand.isSilent());
        nbt.a("Invulnerable", armorStand.isInvulnerable());
        nbt.a("Glowing", armorStand.isGlowing());
        nbt.a("ShowArms", armorStand.hasArms());
        nbt.a("Small", armorStand.isSmall());
        nbt.a("Marker", armorStand.isMarker());
        nbt.a("NoBasePlate", !armorStand.hasBasePlate());
        nbt.a("ArmorItems", entityNBT.c("ArmorItems"));
        nbt.a("HandItems", entityNBT.c("HandItems"));
        nbt.a("Pose", entityNBT.p("Pose"));
        nbt.a("DisabledSlots", entityNBT.h("DisabledSlots"));

        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        if (pdc.has(privateKey, PersistentDataType.STRING)) {
            NBTTagCompound bukkitValues = new NBTTagCompound();
            bukkitValues.a(privateKey.toString(), pdc.get(privateKey, PersistentDataType.STRING));
            nbt.a("BukkitValues", bukkitValues);
        }

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(new ItemStack(Material.ARMOR_STAND));
        NBTTagCompound itemNBT = nmsItem.v();
        itemNBT.a("EntityTag", nbt);
        itemNBT.a(ITEM_IDENTIFIER, true);
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
        NBTTagCompound nbt = CraftItemStack.asNMSCopy(item).u();
        return nbt != null && nbt.e(ITEM_IDENTIFIER) && nbt.q(ITEM_IDENTIFIER);
    }

    @Override
    public ItemStack prepareRecipeResult(ItemStack item, int originalSlot) {
        if (item.getType() != Material.ARMOR_STAND || !isArmorstandItem(item))
            return null;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbt = nmsItem.u();

        if (nbt.e("EntityTag")) {
            NBTTagCompound entityNBT = nbt.p("EntityTag");
            entityNBT.r("ArmorItems");
            entityNBT.r("HandItems");
        }

        nbt.a(ORIGINAL_SLOT_IDENTIFIER, originalSlot);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public Entry<ItemStack, Integer> getRecipeResultAndOriginalSlot(ItemStack item) {
        if (item.getType() != Material.ARMOR_STAND || !isArmorstandItem(item))
            return null;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbt = nmsItem.u();
        if (!nbt.e(ORIGINAL_SLOT_IDENTIFIER))
            return null;

        int originalSlot = nbt.h(ORIGINAL_SLOT_IDENTIFIER);
        nbt.r(ORIGINAL_SLOT_IDENTIFIER);
        return Map.entry(CraftItemStack.asBukkitCopy(nmsItem), originalSlot);
    }
}
