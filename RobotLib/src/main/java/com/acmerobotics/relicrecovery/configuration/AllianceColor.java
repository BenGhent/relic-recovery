package com.acmerobotics.relicrecovery.configuration;

public enum AllianceColor {
    BLUE(0),
    RED(1);

    private int index;

    AllianceColor(int i) {
        index = i;
    }

    public int getIndex() {
        return index;
    }

    public static AllianceColor fromIndex(int i) {
        for (AllianceColor color : AllianceColor.values()) {
            if (color.getIndex() == i) {
                return color;
            }
        }
        return null;
    }
}
