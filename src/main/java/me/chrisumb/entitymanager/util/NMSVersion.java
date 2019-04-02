package me.chrisumb.entitymanager.util;

public enum NMSVersion {
    v1_8_R1,
    v1_8_R2,
    v1_8_R3,
    v1_9_R1,
    v1_9_R2,
    v1_10_R1,
    v1_11_R1,
    v1_12_R1,
    v1_13_R1,
    v1_13_R2;

    public boolean isPreLootTables() {
        return ordinal() < 2;
    }

    public boolean isBefore(NMSVersion version) {
        return ordinal() < version.ordinal();
    }

    public boolean isAfter(NMSVersion version) {
        return ordinal() > version.ordinal();
    }
}
