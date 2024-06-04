package de.rapha149.armorstandeditor.version;

import net.kyori.adventure.text.Component;
import net.minecraft.core.Vector3f;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent.ChatSerializer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class Wrapper1_19_R1 implements VersionWrapper {

    private final EntityArmorStand defaultArmorStand = new EntityArmorStand(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(), 0, 0, 0);

    @Override
    public String getCustomNameForEdit(ArmorStand armorStand) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        if (!handle.Y())
            return null;

        return EDIT_SERIALIZER.serialize(GSON_SERIALIZER.deserialize(ChatSerializer.a(handle.Z())));
    }

    @Override
    public Component getCustomNameForDisplay(ArmorStand armorStand) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        if (!handle.Y())
            return null;

        return GSON_SERIALIZER.deserialize(ChatSerializer.a(handle.Z()));
    }

    @Override
    public void setCustomName(ArmorStand armorStand, String customName) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        handle.b(customName == null ? null : ChatSerializer.a(GSON_SERIALIZER.serialize(EDIT_SERIALIZER.deserialize(customName))));
    }

    @Override
    public void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart) {
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
    public void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart, Axis axis) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        Vector3f currentAngle, defaultAngle;
        switch (bodyPart) {
            case HEAD:
                currentAngle = handle.cg;
                defaultAngle = defaultArmorStand.cg;
                break;
            case BODY:
                currentAngle = handle.ch;
                defaultAngle = defaultArmorStand.ch;
                break;
            case LEFT_ARM:
                currentAngle = handle.ci;
                defaultAngle = defaultArmorStand.ci;
                break;
            case RIGHT_ARM:
                currentAngle = handle.cj;
                defaultAngle = defaultArmorStand.cj;
                break;
            case LEFT_LEG:
                currentAngle = handle.ck;
                defaultAngle = defaultArmorStand.ck;
                break;
            case RIGHT_LEG:
                currentAngle = handle.cl;
                defaultAngle = defaultArmorStand.cl;
                break;
            default:
                return;
        }

        Vector3f newAngle = switch (axis) {
            case X -> new Vector3f(defaultAngle.b(), currentAngle.c(), currentAngle.d());
            case Y -> new Vector3f(currentAngle.b(), defaultAngle.c(), currentAngle.d());
            case Z -> new Vector3f(currentAngle.b(), currentAngle.c(), defaultAngle.d());
        };
        switch(bodyPart) {
            case HEAD -> handle.a(newAngle);
            case BODY -> handle.b(newAngle);
            case LEFT_ARM -> handle.c(newAngle);
            case RIGHT_ARM -> handle.d(newAngle);
            case LEFT_LEG -> handle.e(newAngle);
            case RIGHT_LEG -> handle.f(newAngle);
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
        if (handle.Y())
            nbt.a("CustomName", ChatSerializer.b(handle.Z()));
        nbt.a("CustomNameVisible", armorStand.isCustomNameVisible());
        if (armorStand.isInvisible())
            nbt.a("Tags", tags);
        nbt.a("NoGravity", !armorStand.hasGravity());
        nbt.a("Silent", armorStand.isSilent());
        nbt.a("Invulnerable", armorStand.isInvulnerable());
        nbt.a("ShowArms", armorStand.hasArms());
        nbt.a("Small", armorStand.isSmall());
        nbt.a("Marker", armorStand.isMarker());
        nbt.a("NoBasePlate", !armorStand.hasBasePlate());
        nbt.a("Glowing", armorStand.isGlowing());
        nbt.a("HasVisualFire", armorStand.isVisualFire());
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
        ItemStack item = CraftItemStack.asBukkitCopy(nmsItem);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(armorStand.getCustomName());
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public ItemStack prepareRecipeResult(ItemStack item) {
        if (item.getType() != Material.ARMOR_STAND)
            return null;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbt = nmsItem.u();

        if (nbt.e("EntityTag")) {
            NBTTagCompound entityNBT = nbt.p("EntityTag");
            entityNBT.r("ArmorItems");
            entityNBT.r("HandItems");
        }

        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
