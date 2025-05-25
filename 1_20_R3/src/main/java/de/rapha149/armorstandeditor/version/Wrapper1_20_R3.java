package de.rapha149.armorstandeditor.version;

import net.minecraft.core.Vector3f;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent.ChatSerializer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class Wrapper1_20_R3 implements VersionWrapper {

    private final EntityArmorStand defaultArmorStand = new EntityArmorStand(((CraftWorld) Bukkit.getWorlds().get(0)).getHandle(), 0, 0, 0);

    @Override
    public Optional<String> getCustomNameJson(ArmorStand armorStand) {
        return Optional.ofNullable(((CraftArmorStand) armorStand).getHandle().af()).map(ChatSerializer::a);
    }

    @Override
    public void setCustomName(ArmorStand armorStand, String customNameJson) {
        ((CraftArmorStand) armorStand).getHandle().b(customNameJson == null ? null : ChatSerializer.a(customNameJson));
    }

    @Override
    public void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        switch (bodyPart) {
            case HEAD -> handle.a(defaultArmorStand.cc);
            case BODY -> handle.b(defaultArmorStand.cd);
            case LEFT_ARM -> handle.c(defaultArmorStand.ce);
            case RIGHT_ARM -> handle.d(defaultArmorStand.cf);
            case LEFT_LEG -> handle.e(defaultArmorStand.cg);
            case RIGHT_LEG -> handle.f(defaultArmorStand.ch);
        }
    }

    @Override
    public void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart, Axis axis) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        Vector3f currentAngle, defaultAngle;
        switch (bodyPart) {
            case HEAD:
                currentAngle = handle.cc;
                defaultAngle = defaultArmorStand.cc;
                break;
            case BODY:
                currentAngle = handle.cd;
                defaultAngle = defaultArmorStand.cd;
                break;
            case LEFT_ARM:
                currentAngle = handle.ce;
                defaultAngle = defaultArmorStand.ce;
                break;
            case RIGHT_ARM:
                currentAngle = handle.cf;
                defaultAngle = defaultArmorStand.cf;
                break;
            case LEFT_LEG:
                currentAngle = handle.cg;
                defaultAngle = defaultArmorStand.cg;
                break;
            case RIGHT_LEG:
                currentAngle = handle.ch;
                defaultAngle = defaultArmorStand.ch;
                break;
            default:
                return;
        }

        Vector3f newAngle = switch (axis) {
            case X -> new Vector3f(defaultAngle.b(), currentAngle.c(), currentAngle.d());
            case Y -> new Vector3f(currentAngle.b(), defaultAngle.c(), currentAngle.d());
            case Z -> new Vector3f(currentAngle.b(), currentAngle.c(), defaultAngle.d());
        };
        switch (bodyPart) {
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
        if (armorStand.isInvisible())
            tags.add(NBTTagString.a(INVISIBLE_TAG));
        armorStand.getScoreboardTags().forEach(tag -> tags.add(NBTTagString.a(tag)));

        NBTTagCompound nbt = new NBTTagCompound();
        getCustomNameJson(armorStand).ifPresent(json -> nbt.a("CustomName", json));
        nbt.a("CustomNameVisible", armorStand.isCustomNameVisible());
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
        NBTTagCompound itemNBT = nmsItem.w();
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
        NBTTagCompound nbt = nmsItem.v();

        if (nbt.e("EntityTag")) {
            NBTTagCompound entityNBT = nbt.p("EntityTag");
            entityNBT.r("ArmorItems");
            entityNBT.r("HandItems");
        }

        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
