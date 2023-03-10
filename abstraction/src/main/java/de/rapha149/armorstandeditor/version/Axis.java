package de.rapha149.armorstandeditor.version;

import org.bukkit.Location;
import org.bukkit.util.EulerAngle;

public enum Axis {
    X, Y, Z;

    public double getValue(Location loc) {
        return switch (this) {
            case X -> loc.getX();
            case Y -> loc.getY();
            case Z -> loc.getZ();
        };
    }

    public int getBlockValue(Location loc) {
        return switch (this) {
            case X -> loc.getBlockX();
            case Y -> loc.getBlockY();
            case Z -> loc.getBlockZ();
        };
    }

    public void setValue(Location loc, double value) {
        switch (this) {
            case X -> loc.setX(value);
            case Y -> loc.setY(value);
            case Z -> loc.setZ(value);
        }
    }

    public double getValue(EulerAngle loc) {
        return switch (this) {
            case X -> loc.getX();
            case Y -> loc.getY();
            case Z -> loc.getZ();
        };
    }

    public EulerAngle setValue(EulerAngle angle, double value) {
        return switch (this) {
            case X -> angle.setX(value);
            case Y -> angle.setY(value);
            case Z -> angle.setZ(value);
        };
    }

    public double getValueDegrees(EulerAngle angle) {
        return Math.toDegrees(getValue(angle));
    }

    public EulerAngle setValueDegrees(EulerAngle angle, double value) {
        return setValue(angle, Math.toRadians(value));
    }
}