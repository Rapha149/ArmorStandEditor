package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.Config.FeaturesData;
import de.rapha149.armorstandeditor.Config.FeaturesData.FeatureData;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandBodyPartMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandPositionMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandPositionMovement.ArmorStandPositionSnapInMovement;
import de.rapha149.armorstandeditor.Events.ArmorStandMovement.ArmorStandRotationMovement;
import de.rapha149.armorstandeditor.version.BodyPart;
import de.rapha149.armorstandeditor.version.Direction;
import de.rapha149.armorstandeditor.version.Axis;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.Particle.DustOptions;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class Events implements Listener, Runnable {

    private VersionWrapper wrapper = ArmorStandEditor.getInstance().wrapper;

    private final String INVISIBLE_TAG = "ArmorStandEditor-Invisible";
    private final List<Double> SNAP_IN_DISTANCES = List.of(0.5D, 1.0D, 1.5D, 2D, 3D, 4D, 5D);

    public static final Map<Player, ArmorStandMovement> moving = new HashMap<>();
    public static final Map<Player, Entry<ArmorStand, Boolean>> vehicleSelection = new HashMap<>();

    public Events() {
        Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), this, 0, 20);
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand))
            return;

        if (armorStand.removeScoreboardTag(INVISIBLE_TAG))
            Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> armorStand.setInvisible(true));
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        onInteraction(event);
        if (event.isCancelled())
            return;

        if (!(event.getRightClicked() instanceof ArmorStand armorStand))
            return;

        Player player = event.getPlayer();
        if (player.isSneaking()) {
            String key = Config.get().advancement;
            if (key != null) {
                Advancement advancement = Bukkit.getAdvancement(NamespacedKey.fromString(key));
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    if (!progress.isDone())
                        progress.getRemainingCriteria().forEach(progress::awardCriteria);
                }
            }

            event.setCancelled(true);
            Util.openGUI(player, armorStand, 1, false);
        } else if (player.getInventory().getItem(event.getHand()).getType() == Material.NAME_TAG)
            armorStand.setCustomNameVisible(true);
    }

    @EventHandler
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        if (Util.invs.values().stream().anyMatch(status -> armorStand.getUniqueId().equals(status.armorStand.getUniqueId())))
            event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ArmorStand armorStand))
            return;

        UUID uuid = entity.getUniqueId();
        AtomicBoolean reloadDrops = new AtomicBoolean(false);
        new HashMap<>(Util.invs).values().stream().filter(status -> status.armorStand.getUniqueId().equals(uuid)).forEach(status -> {
            if (Util.saveEquipment(status))
                reloadDrops.set(true);
            status.gui.close(status.player);
        });

        if (reloadDrops.get()) {
            List<ItemStack> drops = event.getDrops();
            drops.clear();
            drops.add(new ItemStack(Material.ARMOR_STAND));

            EntityEquipment equipment = armorStand.getEquipment();
            drops.add(equipment.getHelmet());
            drops.add(equipment.getChestplate());
            drops.add(equipment.getLeggings());
            drops.add(equipment.getBoots());
            drops.add(equipment.getItemInMainHand());
            drops.add(equipment.getItemInOffHand());
        }
    }

    private boolean isDeactivated(FeatureData feature, Player player) {
        return !feature.enabled || (feature.permission != null && !player.hasPermission(feature.permission));
    }

    public static boolean isPlayerDoingSomethingOutsideOfInv(Player player) {
        return moving.containsKey(player) || vehicleSelection.containsKey(player);
    }

    public static void startMovePosition(Player player, ArmorStand armorStand, boolean snapIn) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        if (!snapIn) {
            moving.put(player, new ArmorStandPositionMovement(armorStand, Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                if (player.getWorld().getUID().equals(armorStand.getWorld().getUID())) {
                    Location playerLoc = player.getLocation();
                    Location loc = playerLoc.clone().add(playerLoc.getDirection().multiply(3));
                    loc.setYaw(armorStand.getLocation().getYaw());
                    loc.setPitch(armorStand.getLocation().getPitch());
                    loc.setY(playerLoc.getY());

                    Location aboveBlock = loc.clone();
                    for (int i = 0; i < 3; i++) {
                        Block block = aboveBlock.getBlock();
                        if (block.isPassable()) {
                            loc = aboveBlock;
                            break;
                        }

                        aboveBlock.setY(block.getRelative(BlockFace.UP).getLocation().getY());
                    }

                    armorStand.teleport(loc);
                }
            }, 0, 1)));
        } else {
            long time = System.currentTimeMillis();

            ArmorStandPositionSnapInMovement movement = new ArmorStandPositionSnapInMovement(armorStand, null);
            movement.task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
                if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()))
                    return;

                Location center = movement.getCurrentLocation();
                List<Location> locations = movement.locations;
                if (locations.isEmpty()) {
                    locations.add(center);
                    BiConsumer<Double, Double> addLocation = (x, z) -> {
                        Location loc = new Location(center.getWorld(), x, center.getY(), z, center.getYaw(), center.getPitch());
                        if (loc.distanceSquared(player.getLocation()) <= 2500)
                            locations.add(loc);
                    };

                    double distance = movement.distance;
                    int count = 0;
                    double coveredDistance = 0;
                    while (count < 3 || coveredDistance < 3) {
                        count++;
                        coveredDistance += distance;

                        double minX = center.getX() - coveredDistance, maxX = center.getX() + coveredDistance,
                                minZ = center.getZ() - coveredDistance, maxZ = center.getZ() + coveredDistance;

                        double x = minX, z = minZ;
                        for (; x <= maxX; x += distance)
                            addLocation.accept(x, z);
                        x -= distance;
                        for (; z <= maxZ; z += distance)
                            addLocation.accept(x, z);
                        z -= distance;
                        for (; x >= minX; x -= distance)
                            addLocation.accept(x, z);
                        x += distance;
                        for (; z > minZ; z -= distance)
                            addLocation.accept(x, z);
                    }
                }

                DustOptions options = new DustOptions(Color.fromRGB(255, 0, 0), 0.5F);
                for (Location loc : locations)
                    player.spawnParticle(Particle.REDSTONE, loc, 1, options);

                if (System.currentTimeMillis() > time + 1000) {
                    Location eyeLoc = player.getEyeLocation();
                    Vector eyeVec = eyeLoc.toVector();
                    double currentDot = 0;
                    Location closest = null;
                    for (Location loc : locations) {
                        double dot = loc.toVector().subtract(eyeVec).normalize().dot(eyeLoc.getDirection());
                        if (dot > currentDot) {
                            currentDot = dot;
                            closest = loc;
                        }
                    }

                    if (closest != null && !closest.equals(center)) {
                        armorStand.teleport(closest);
                        movement.updateOffset(closest);
                        locations.clear();
                    } else
                        armorStand.teleport(center);
                } else
                    armorStand.teleport(center);
            }, 0, 1);

            moving.put(player, movement);
        }

        runTask();
    }

    public static void startSnapInMovePosition(Player player, ArmorStand armorStand, Axis axis) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        long time = System.currentTimeMillis();

        ArmorStandPositionSnapInMovement movement = new ArmorStandPositionSnapInMovement(armorStand, axis);
        movement.task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()))
                return;

            Location center = movement.getCurrentLocation();
            List<Location> locations = movement.locations;
            if (locations.isEmpty()) {
                locations.add(center);
                Consumer<Location> addLocation = loc -> {
                    if (loc.distanceSquared(player.getLocation()) <= 2500)
                        locations.add(loc.clone());
                };

                double distance = movement.distance;
                int count = 0;
                double coveredDistance = 0;
                Location plus = center.clone(), minus = center.clone();
                while (count < 3 || coveredDistance < 5) {
                    count++;
                    coveredDistance += distance;

                    axis.setValue(plus, axis.getValue(plus) + distance);
                    axis.setValue(minus, axis.getValue(minus) - distance);
                    addLocation.accept(plus);
                    addLocation.accept(minus);
                }
            }

            DustOptions options = new DustOptions(Color.fromRGB(255, 0, 0), 0.5F);
            for (Location loc : locations)
                player.spawnParticle(Particle.REDSTONE, loc, 1, options);

            if (System.currentTimeMillis() > time + 1000) {
                Location eyeLoc = player.getEyeLocation();
                Vector eyeVec = eyeLoc.toVector();
                double currentDot = 0;
                Location closest = null;
                for (Location loc : locations) {
                    double dot = loc.toVector().subtract(eyeVec).normalize().dot(eyeLoc.getDirection());
                    if (dot > currentDot) {
                        currentDot = dot;
                        closest = loc;
                    }
                }

                if (closest != null && !closest.equals(center)) {
                    armorStand.teleport(closest);
                    movement.updateOffset(closest);
                    locations.clear();
                } else
                    armorStand.teleport(center);
            } else
                armorStand.teleport(center);
        }, 0, 1);

        moving.put(player, movement);
        runTask();
    }

    public static void startMoveBodyPart(Player player, ArmorStand armorStand, BodyPart bodyPart) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        ArmorStandBodyPartMovement movement = new ArmorStandBodyPartMovement(armorStand, bodyPart,
                player.getLocation().getYaw(), player.getLocation().getPitch());
        movement.task = Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(), () -> {
            EulerAngle angle = bodyPart.get(armorStand);
            Location loc = player.getLocation();

            boolean sneaking = player.isSneaking();
            Direction yawDir = sneaking ? bodyPart.sneakYawDir : bodyPart.normalYawDir,
                    pitchDir = sneaking ? bodyPart.sneakPitchDir : bodyPart.normalPitchDir;

            float yaw = loc.getYaw(), pitch = loc.getPitch();
            while (yaw < 0)
                yaw += 360;
            while (pitch < 0)
                pitch += 360;

            double yawChange = Math.toRadians((yaw - movement.zeroYaw) * yawDir.factor),
                    pitchChange = Math.toRadians((pitch - movement.zeroPitch) * pitchDir.factor);
            angle = switch (yawDir) {
                case X -> angle.setX(movement.zeroAngle.getX() + yawChange);
                case Y -> angle.setY(movement.zeroAngle.getY() + yawChange);
                case Z -> angle.setZ(movement.zeroAngle.getZ() + yawChange);
            };
            angle = switch (pitchDir) {
                case X -> angle.setX(movement.zeroAngle.getX() + pitchChange);
                case Y -> angle.setY(movement.zeroAngle.getY() + pitchChange);
                case Z -> angle.setZ(movement.zeroAngle.getZ() + pitchChange);
            };
            bodyPart.set(armorStand, angle);
        }, 1, 1);

        moving.put(player, movement);
        runTask();
    }

    public static void startRotationMovement(Player player, ArmorStand armorStand) {
        UUID uuid = armorStand.getUniqueId();
        if (isPlayerDoingSomethingOutsideOfInv(player) || moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            player.sendMessage(getMessage("not_possible_now"));
            return;
        }

        moving.put(player, new ArmorStandRotationMovement(armorStand, Bukkit.getScheduler().runTaskTimer(ArmorStandEditor.getInstance(),
                () -> armorStand.setRotation(player.getLocation().getYaw(), armorStand.getLocation().getPitch()), 0, 1)));
        runTask();
    }

    @Override
    public void run() {
        // titles
        moving.forEach((player, movement) -> {
            if (movement instanceof ArmorStandPositionMovement) {
                String message;
                if (movement instanceof ArmorStandPositionSnapInMovement snapInMovement) {
                    message = getMessage("armorstands.move_position.title.snapin")
                                      .replace("%distance%", String.valueOf(snapInMovement.distance))
                                      .replace("%aligned_color%", getMessage("armorstands.move_position.title.snapin_color_aligned_" +
                                                                             (player.isSneaking() ? "active" : "inactive")));
                } else
                    message = getMessage("armorstands.move_position.title.normal");

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
                BodyPart bodyPart = bodyPartMovement.bodyPart;
                String activated = getMessage("armorstands.move_body_parts.title.color_activated"),
                        deactivated = getMessage("armorstands.move_body_parts.title.color_deactivated");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.move_body_parts.title.text")
                        .replace("%normal%", bodyPart.normalYawDir.getString(bodyPart.normalPitchDir))
                        .replace("%sneak%", bodyPart.sneakYawDir.getString(bodyPart.sneakPitchDir))
                        .replace("%color_normal%", !player.isSneaking() ? activated : deactivated)
                        .replace("%color_sneak%", player.isSneaking() ? activated : deactivated)));
            } else if (movement instanceof ArmorStandRotationMovement) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.rotate.title")));
            }
        });

        vehicleSelection.forEach((player, entry) -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(getMessage("armorstands." + (entry.getValue() ? "passenger" : "vehicle") + ".choose.title"))));

        // remove when player is far away
        moving.entrySet().removeIf(entry -> {
            Player player = entry.getKey();
            ArmorStandMovement movement = entry.getValue();
            ArmorStand armorStand = movement.armorStand;
            if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()) || player.getLocation().distanceSquared(armorStand.getLocation()) > 2500) {
                movement.task.cancel();
                cancelMovement(movement);
                return true;
            }
            return false;
        });
        vehicleSelection.entrySet().removeIf(entry -> entry.getKey().getLocation().distanceSquared(entry.getValue().getKey().getLocation()) > 2500);
    }

    public static void runTask() {
        // titles
        moving.forEach((player, movement) -> {
            if (movement instanceof ArmorStandPositionMovement) {
                String message;
                if (movement instanceof ArmorStandPositionSnapInMovement snapInMovement) {
                    message = getMessage("armorstands.move_position.title.snapin")
                                      .replace("%distance%", String.valueOf(snapInMovement.distance))
                                      .replace("%aligned_color%", getMessage("armorstands.move_position.title.snapin_color_aligned_" +
                                                                             (player.isSneaking() ? "active" : "inactive")));
                } else
                    message = getMessage("armorstands.move_position.title.normal");

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
                BodyPart bodyPart = bodyPartMovement.bodyPart;
                String activated = getMessage("armorstands.move_body_parts.title.color_activated"),
                        deactivated = getMessage("armorstands.move_body_parts.title.color_deactivated");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.move_body_parts.title.text")
                        .replace("%normal%", bodyPart.normalYawDir.getString(bodyPart.normalPitchDir))
                        .replace("%sneak%", bodyPart.sneakYawDir.getString(bodyPart.sneakPitchDir))
                        .replace("%color_normal%", !player.isSneaking() ? activated : deactivated)
                        .replace("%color_sneak%", player.isSneaking() ? activated : deactivated)));
            } else if (movement instanceof ArmorStandRotationMovement) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMessage("armorstands.rotate.title")));
            }
        });

        vehicleSelection.forEach((player, entry) -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(getMessage("armorstands." + (entry.getValue() ? "passenger" : "vehicle") + ".choose.title"))));

        // remove when player is far away
        moving.entrySet().removeIf(entry -> {
            Player player = entry.getKey();
            ArmorStandMovement movement = entry.getValue();
            ArmorStand armorStand = movement.armorStand;
            if (!player.getWorld().getUID().equals(armorStand.getWorld().getUID()) || player.getLocation().distanceSquared(armorStand.getLocation()) > 2500) {
                movement.task.cancel();
                cancelMovement(movement);
                return true;
            }
            return false;
        });
        vehicleSelection.entrySet().removeIf(entry -> entry.getKey().getLocation().distanceSquared(entry.getValue().getKey().getLocation()) > 2500);
    }

    @EventHandler
    public void onInteract(EntityInteractEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (event.getEntityType() == EntityType.ARMOR_STAND &&
            moving.values().stream().anyMatch(movement -> movement.armorStand.getUniqueId().equals(uuid))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarSlot(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!moving.containsKey(player) || !(moving.get(player) instanceof ArmorStandPositionSnapInMovement movement))
            return;

        int index = SNAP_IN_DISTANCES.indexOf(movement.distance);
        int previous = event.getPreviousSlot(), current = event.getNewSlot();
        if (previous == 8 && current == 0)
            index++;
        else if (previous == 0 && current == 8)
            index--;
        else if (current > previous)
            index++;
        else if (current < previous)
            index--;

        if (index < 0 || index > SNAP_IN_DISTANCES.size() - 1) {
            Util.playBassSound(player);
            return;
        }

        Util.playSound(player, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON);
        movement.distance = SNAP_IN_DISTANCES.get(index);
        movement.locations.clear();
        run();
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!moving.containsKey(player))
            return;

        Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), this);
        ArmorStandMovement movement = moving.get(player);
        if (movement instanceof ArmorStandPositionSnapInMovement snapInMovement) {
            snapInMovement.locations.clear();

            Location loc = snapInMovement.getCurrentLocation().getBlock().getLocation();
            if (event.isSneaking())
                loc.add(0.5, 0, 0.5);
            else if (snapInMovement.axis == null) {
                double[] alignmentXZ = snapInMovement.previousAlignmentXZ;
                loc.add(alignmentXZ[0], 0, alignmentXZ[1]);
            } else
                snapInMovement.axis.setValue(loc, snapInMovement.axis.getValue(loc) + snapInMovement.previousAlignmentSingle);
            snapInMovement.updateOffset(loc);
        } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
            Location loc = player.getLocation();
            bodyPartMovement.zeroYaw = loc.getYaw();
            bodyPartMovement.zeroPitch = loc.getPitch();
            bodyPartMovement.zeroAngle = bodyPartMovement.bodyPart.get(bodyPartMovement.armorStand);
        }
    }

    @EventHandler
    public void onInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action == Action.PHYSICAL)
            return;

        if (moving.containsKey(player)) {
            event.setCancelled(true);
            onMovementInteraction(player, action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        }

        if (vehicleSelection.containsKey(player)) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
                return;

            event.setCancelled(true);
            onVehicleSelectionInteraction(player, null, false);
        }
    }

    @EventHandler
    public void onInteraction(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (moving.containsKey(player)) {
            event.setCancelled(true);
            onMovementInteraction(player, false);
        }

        if (vehicleSelection.containsKey(player)) {
            event.setCancelled(true);
            onVehicleSelectionInteraction(player, null, false);
        }
    }

    @EventHandler
    public void onInteraction(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;

        if (moving.containsKey(player)) {
            event.setCancelled(true);
            onMovementInteraction(player, true);
        }

        if (vehicleSelection.containsKey(player)) {
            event.setCancelled(true);
            onVehicleSelectionInteraction(player, event.getEntity(), true);
        }
    }

    private void onMovementInteraction(Player player, boolean leftClick) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        ArmorStandMovement movement = moving.remove(player);
        if (movement.task != null)
            movement.task.cancel();

        if (!leftClick) {
            cancelMovement(movement);
            Util.playArmorStandBreakSound(player);
        } else
            Util.playExperienceSound(player);
    }

    public static void cancelMovement(ArmorStandMovement movement) {
        if (movement instanceof ArmorStandPositionMovement positionMovement) {
            movement.armorStand.teleport(positionMovement.originalLocation);
        } else if (movement instanceof ArmorStandBodyPartMovement bodyPartMovement) {
            bodyPartMovement.bodyPart.set(movement.armorStand, bodyPartMovement.cancelAngle);
        } else if (movement instanceof ArmorStandRotationMovement rotationMovement) {
            movement.armorStand.setRotation(rotationMovement.originalYaw, movement.armorStand.getLocation().getPitch());
        }
    }

    private void onVehicleSelectionInteraction(Player player, Entity entity, boolean leftClick) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        Entry<ArmorStand, Boolean> entry = vehicleSelection.remove(player);
        ArmorStand armorStand = entry.getKey();
        boolean asPassenger = entry.getValue();

        if (leftClick) {
            String key = "armorstands." + (asPassenger ? "passenger" : "vehicle") + ".choose.";
            FeaturesData features = Config.get().features;
            if (!(asPassenger ? features.passenger : features.vehicle).players && entity instanceof Player) {
                player.sendMessage(getMessage(key + ".no_players"));
                Util.playBassSound(player);
                return;
            }
            if (entity.getUniqueId().equals(armorStand.getUniqueId())) {
                player.sendMessage(getMessage(key + ".not_itself"));
                Util.playBassSound(player);
                return;
            }

            if (asPassenger)
                entity.addPassenger(armorStand);
            else
                armorStand.addPassenger(entity);
            Util.playExperienceSound(player);
        } else
            Util.playArmorStandBreakSound(player);
    }


    /*private void setEquipmentItem(Player player, ArmorStandStatus status, Gui gui, EquipmentSlot slot) {
        int index, row, col;
        switch (slot) {
            case HEAD:
                index = 0;
                row = 2;
                col = 3;
                break;
            case CHEST:
                index = 1;
                row = 3;
                col = 3;
                break;
            case LEGS:
                index = 2;
                row = 4;
                col = 3;
                break;
            case FEET:
                index = 3;
                row = 5;
                col = 3;
                break;
            case HAND:
                index = 4;
                row = 3;
                col = 2;
                break;
            case OFF_HAND:
                index = 5;
                row = 3;
                col = 4;
                break;
            default:
                return;
        }

        AtomicBoolean eventFired = new AtomicBoolean(false);
        ItemStack current = status.equipment[index];
        if (current == null || current.getType().isAir()) {
            String key = switch (slot) {
                case HEAD -> "helmet";
                case CHEST -> "chestplate";
                case LEGS -> "leggings";
                case FEET -> "boots";
                case HAND -> "mainhand";
                case OFF_HAND -> "offhand";
            };

            gui.updateItem(row, col, applyNameAndLore(ItemBuilder.from(Material.WHITE_STAINED_GLASS_PANE),
                    "armorstands.equipment." + key, "armorstands.equipment.lore", false).asGuiItem(event -> {
                ArmorStandEditor.getInstance().getLogger().info("Clicked empty slot");
                if (eventFired.get()) {
                    event.setCancelled(true);
                    return;
                }
                eventFired.set(true);

                ItemStack item;
                if (event.getClick() == ClickType.NUMBER_KEY) {
                    item = player.getInventory().getItem(event.getHotbarButton());
                    item = item == null ? new ItemStack(Material.AIR) : item.clone();
                } else {
                    item = event.getCursor().clone();
                    if (event.isRightClick())
                        item.setAmount(1);
                }

                event.setCancelled(false);
                event.setCurrentItem(new ItemStack(Material.AIR));
                status.notCancelled.add(event.hashCode());
                status.equipment[index] = ItemNbt.removeTag(item, "mf-gui");
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> setEquipmentItem(player, status, gui, slot));
            }));
        } else {
            gui.updateItem(row, col, ItemBuilder.from(current).asGuiItem(event -> {
                ArmorStandEditor.getInstance().getLogger().info("Clicked full slot");
                if (eventFired.get()) {
                    event.setCancelled(true);
                    return;
                }
                eventFired.set(true);

                ItemStack item;
                ItemStack cursor = event.getCursor();
                if (event.getClick() == ClickType.NUMBER_KEY) {
                    item = player.getInventory().getItem(event.getHotbarButton());
                    item = item == null ? new ItemStack(Material.AIR) : item.clone();
                } else if (cursor.isSimilar(current)) {
                    item = current.clone();
                    item.setAmount(item.getAmount() + (event.isLeftClick() ? cursor.getAmount() : 1));
                } else if (event.isRightClick() && cursor.getType() == Material.AIR) {
                    if (cursor.getType() == Material.AIR) {
                        item = current.clone();
                        item.setAmount((int) (item.getAmount() / 2D));
                    } else
                        item = cursor.clone();
                } else
                    item = cursor.clone();

                event.setCancelled(false);
                event.setCurrentItem(status.equipment[index].clone());
                status.notCancelled.add(event.hashCode());
                status.equipment[index] = ItemNbt.removeTag(item, "mf-gui");
                Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> setEquipmentItem(player, status, gui, slot));
            }));
        }
    }*/

    /*private ItemStack[] getEquipment(ArmorStand armorStand, Player player, boolean glassPanes) {
        EntityEquipment equipment = armorStand.getEquipment();
        return new ItemStack[]{getEquipmentItem(EquipmentSlot.HEAD, equipment.getHelmet(), player, glassPanes),
                getEquipmentItem(EquipmentSlot.CHEST, equipment.getChestplate(), player, glassPanes),
                getEquipmentItem(EquipmentSlot.LEGS, equipment.getLeggings(), player, glassPanes),
                getEquipmentItem(EquipmentSlot.FEET, equipment.getBoots(), player, glassPanes),
                getEquipmentItem(EquipmentSlot.HAND, equipment.getItemInMainHand(), player, glassPanes),
                getEquipmentItem(EquipmentSlot.OFF_HAND, equipment.getItemInOffHand(), player, glassPanes)};
    }

    private ItemStack getEquipmentItem(EquipmentSlot slot, ItemStack current, Player player, boolean glassPanes) {
        ReplaceEquipmentFeatureData feature = Config.get().features.replaceEquipment;
        boolean deactivated = isDeactivated(feature, player);

        if (current != null && !current.getType().isAir() && (!deactivated || !feature.useDeactivatedItem))
            return current;
        if (!glassPanes)
            return new ItemStack(Material.AIR);

        ItemStack item = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        String key = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            case HAND -> "mainhand";
            case OFF_HAND -> "offhand";
        };

        meta.setDisplayName(getMessage("armorstands.equipment." + key));
        meta.setLore(Arrays.asList(getMessage("armorstands.equipment.lore").split("\n")));
        item.setItemMeta(meta);

        return ItemNbt.setString(checkDeactivated(item, feature, player), "empty_slot", "");
    }*/

    static class ArmorStandMovement {

        ArmorStand armorStand;
        BukkitTask task;

        ArmorStandMovement(ArmorStand armorStand) {
            this.armorStand = armorStand;
        }

        static class ArmorStandPositionMovement extends ArmorStandMovement {

            Location originalLocation;

            public ArmorStandPositionMovement(ArmorStand armorStand, BukkitTask task) {
                super(armorStand);
                this.task = task;
                this.originalLocation = armorStand.getLocation();
            }

            static class ArmorStandPositionSnapInMovement extends ArmorStandPositionMovement {

                Axis axis;
                double[] previousAlignmentXZ;
                double previousAlignmentSingle;
                double[] offsetXZ;
                double offsetSingle;
                double distance = 1;
                List<Location> locations = new ArrayList<>();

                public ArmorStandPositionSnapInMovement(ArmorStand armorStand, Axis axis) {
                    super(armorStand, null);
                    this.axis = axis;

                    Location loc = armorStand.getLocation();

                    if (axis == null) {
                        previousAlignmentXZ = new double[]{loc.getX() - loc.getBlockX(), loc.getZ() - loc.getBlockZ()};
                        offsetXZ = new double[]{0, 0};
                    } else {
                        previousAlignmentSingle = axis.getValue(loc) - axis.getBlockValue(loc);
                        offsetSingle = 0;
                    }
                }

                public Location getCurrentLocation() {
                    Location loc = originalLocation.clone();
                    if (axis == null)
                        loc.add(offsetXZ[0], 0, offsetXZ[1]);
                    else
                        axis.setValue(loc, axis.getValue(loc) + offsetSingle);
                    return loc;
                }

                public void updateOffset(Location current) {
                    if (axis == null) {
                        offsetXZ[0] = current.getX() - originalLocation.getX();
                        offsetXZ[1] = current.getZ() - originalLocation.getZ();
                    } else
                        offsetSingle = axis.getValue(current) - axis.getValue(originalLocation);
                }
            }
        }

        static class ArmorStandBodyPartMovement extends ArmorStandMovement {

            BodyPart bodyPart;
            float zeroYaw, zeroPitch;
            EulerAngle zeroAngle, cancelAngle;

            public ArmorStandBodyPartMovement(ArmorStand armorStand, BodyPart bodyPart, float zeroYaw, float zeroPitch) {
                super(armorStand);
                this.bodyPart = bodyPart;

                this.zeroYaw = zeroYaw;
                while (this.zeroYaw < 0)
                    this.zeroYaw += 360;

                this.zeroPitch = zeroPitch;
                while (this.zeroPitch < 0)
                    this.zeroPitch += 360;

                zeroAngle = cancelAngle = bodyPart.get(armorStand);
            }
        }

        static class ArmorStandRotationMovement extends ArmorStandMovement {

            float originalYaw;

            public ArmorStandRotationMovement(ArmorStand armorStand, BukkitTask task) {
                super(armorStand);
                this.task = task;
                this.originalYaw = armorStand.getLocation().getYaw();
            }
        }
    }

    // recipe
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null)
            return;
        CraftingInventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof Player player))
            return;
        if (isDeactivated(Config.get().features.copy, player))
            return;

        ItemStack original = null;
        int originalSlot = -1;
        int count = 0;
        ItemStack[] matrix = inventory.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null)
                continue;
            if (item.getType() != Material.ARMOR_STAND)
                return;

            if (wrapper.isArmorstandItem(item)) {
                if (original != null)
                    return;
                original = item;
                originalSlot = i;
            } else
                count += item.getAmount();
        }

        if (count > Material.ARMOR_STAND.getMaxStackSize())
            return;

        if (original != null && count > 0) {
            ItemStack result = wrapper.prepareRecipeResult(original, originalSlot);
            result.setAmount(count);
            inventory.setResult(result);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() != SlotType.RESULT)
            return;
        if (!(event.getInventory() instanceof CraftingInventory inventory))
            return;

        Entry<ItemStack, Integer> entry = wrapper.getRecipeResultAndOriginalSlot(event.getCurrentItem());
        if (entry == null)
            return;

        inventory.setResult(entry.getKey());

        ItemStack[] matrix = inventory.getMatrix().clone();
        for (int i = 0; i < matrix.length; i++) {
            if (i != entry.getValue())
                matrix[i] = null;
            else if (matrix[i] != null)
                matrix[i] = matrix[i].clone();
        }
        Bukkit.getScheduler().runTask(ArmorStandEditor.getInstance(), () -> inventory.setMatrix(matrix));
    }
}
