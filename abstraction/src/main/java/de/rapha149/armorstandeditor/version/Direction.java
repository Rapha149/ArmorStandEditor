package de.rapha149.armorstandeditor.version;

public enum Direction {
        X(4), Y(2), Z(2);

        public final int factor;

        Direction(int factor) {
            this.factor = factor;
        }

        public String getString(Direction other) {
            if (this == X)
                return this + "/" + other;
            if (other == X)
                return other + "/" + this;
            if (this == Y)
                return this + "/" + other;
            if (other == Y)
                return other + "/" + this;
            return this + "/" + other;
        }
    }