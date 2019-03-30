package me.chrisumb.entitymanager.util;

import org.bukkit.Bukkit;

public final class ReflectionUtil {

    public static String getSpigotVersionNMS() {
        String v = Bukkit.getServer().getClass().getPackage().getName();
        return v.substring(v.lastIndexOf('.') + 1);
    }

    public static Class<?> getVanillaClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + getSpigotVersionNMS() + "." + name);
    }

    public static Class<?> getCraftClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + getSpigotVersionNMS() + "." + name);
    }
}
