package de.rapha149.armorstandeditor.version;

import org.bukkit.entity.ArmorStand;
import org.bukkit.util.EulerAngle;

public enum BodyPart {
    HEAD(Direction.Z, Direction.X, Direction.Y, Direction.X),
    BODY(Direction.Y, Direction.X, Direction.Z, Direction.X),
    LEFT_ARM(Direction.Z, Direction.X, Direction.Y, Direction.X),
    RIGHT_ARM(Direction.Z, Direction.X, Direction.Y, Direction.X),
    LEFT_LEG(Direction.Z, Direction.X, Direction.Y, Direction.X),
    RIGHT_LEG(Direction.Z, Direction.X, Direction.Y, Direction.X);

    public final Direction normalYawDir, normalPitchDir;
    public final Direction sneakYawDir, sneakPitchDir;

    BodyPart(Direction normalYawDir, Direction normalPitchDir, Direction sneakYawDir, Direction sneakPitchDir) {
        this.normalYawDir = normalYawDir;
        this.normalPitchDir = normalPitchDir;
        this.sneakYawDir = sneakYawDir;
        this.sneakPitchDir = sneakPitchDir;
    }

    public EulerAngle get(ArmorStand armorStand) {
        switch (this) {
            case HEAD:
                return armorStand.getHeadPose();
            case BODY:
                return armorStand.getBodyPose();
            case LEFT_ARM:
                return armorStand.getLeftArmPose();
            case RIGHT_ARM:
                return armorStand.getRightArmPose();
            case LEFT_LEG:
                return armorStand.getLeftLegPose();
            case RIGHT_LEG:
                return armorStand.getRightLegPose();
        }
        return null;
    }

    public void set(ArmorStand armorStand, EulerAngle angle) {
        switch (this) {
            case HEAD:
                armorStand.setHeadPose(angle);
                break;
            case BODY:
                armorStand.setBodyPose(angle);
                break;
            case LEFT_ARM:
                armorStand.setLeftArmPose(angle);
                break;
            case RIGHT_ARM:
                armorStand.setRightArmPose(angle);
                break;
            case LEFT_LEG:
                armorStand.setLeftLegPose(angle);
                break;
            case RIGHT_LEG:
                armorStand.setRightLegPose(angle);
                break;
        }
    }
}