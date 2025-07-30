package de.rapha149.armorstandeditor.version;

import net.minecraft.core.Vector3f;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R5.util.CraftChatMessage;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class Wrapper1_21_R5 implements VersionWrapper {

    @Override
    public Optional<String> getCustomNameJson(ArmorStand armorStand) {
        return Optional.ofNullable(((CraftArmorStand) armorStand).getHandle().al()).map(CraftChatMessage::toJSON);
    }

    @Override
    public void setCustomName(ArmorStand armorStand, String customNameJson) {
        ((CraftArmorStand) armorStand).getHandle().b(CraftChatMessage.fromJSONOrNull(customNameJson));
    }

    @Override
    public void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        switch (bodyPart) {
            case HEAD -> handle.a(EntityArmorStand.b);
            case BODY -> handle.b(EntityArmorStand.c);
            case LEFT_ARM -> handle.c(EntityArmorStand.d);
            case RIGHT_ARM -> handle.d(EntityArmorStand.e);
            case LEFT_LEG -> handle.e(EntityArmorStand.f);
            case RIGHT_LEG -> handle.f(EntityArmorStand.g);
        }
    }

    @Override
    public void resetArmorStandBodyPart(ArmorStand armorStand, BodyPart bodyPart, Axis axis) {
        EntityArmorStand handle = ((CraftArmorStand) armorStand).getHandle();
        Vector3f currentAngle, defaultAngle;
        switch (bodyPart) {
            case HEAD:
                currentAngle = handle.u();
                defaultAngle = EntityArmorStand.b;
                break;
            case BODY:
                currentAngle = handle.v();
                defaultAngle = EntityArmorStand.c;
                break;
            case LEFT_ARM:
                currentAngle = handle.x();
                defaultAngle = EntityArmorStand.d;
                break;
            case RIGHT_ARM:
                currentAngle = handle.y();
                defaultAngle = EntityArmorStand.e;
                break;
            case LEFT_LEG:
                currentAngle = handle.z();
                defaultAngle = EntityArmorStand.f;
                break;
            case RIGHT_LEG:
                currentAngle = handle.A();
                defaultAngle = EntityArmorStand.g;
                break;
            default:
                return;
        }

        Vector3f newAngle = switch (axis) {
            case X -> new Vector3f(defaultAngle.a(), currentAngle.b(), currentAngle.c());
            case Y -> new Vector3f(currentAngle.a(), defaultAngle.b(), currentAngle.c());
            case Z -> new Vector3f(currentAngle.a(), currentAngle.b(), defaultAngle.c());
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
        TagValueOutput output = TagValueOutput.a(new ProblemReporter.a());
        handle.d(output);
        NBTTagCompound nbt = output.b();
        nbt.a("id", "minecraft:armor_stand");
        nbt.r("Pos");
        nbt.r("UUID");
        nbt.r("WorldUUIDLeast");
        nbt.r("WorldUUIDMost");
        nbt.r("Passengers");

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(new ItemStack(Material.ARMOR_STAND));
        NBTTagCompound itemNBT = nmsItem.f() ? new NBTTagCompound() : (NBTTagCompound) net.minecraft.world.item.ItemStack.b.encodeStart(DynamicOpsNBT.a, nmsItem).getOrThrow();
        NBTTagCompound components = itemNBT.n("components");
        components.a("minecraft:entity_data", nbt);
        itemNBT.a("components", components);

        ItemStack item = CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.b.parse(DynamicOpsNBT.a, itemNBT).getOrThrow());
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public ItemStack prepareRecipeResult(ItemStack item) {
        if (item.getType() != Material.ARMOR_STAND)
            return null;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbt = nmsItem.f() ? new NBTTagCompound() : (NBTTagCompound) net.minecraft.world.item.ItemStack.b.encodeStart(DynamicOpsNBT.a, nmsItem).getOrThrow();
        NBTTagCompound components = nbt.n("components");

        if (components.b("minecraft:entity_data")) {
            NBTTagCompound entityNBT = components.n("minecraft:entity_data");
            entityNBT.r("equipment");
        }

        return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.b.parse(DynamicOpsNBT.a, nbt).getOrThrow());
    }
}
