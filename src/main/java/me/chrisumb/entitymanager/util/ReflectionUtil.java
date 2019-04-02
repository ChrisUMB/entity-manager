package me.chrisumb.entitymanager.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionUtil {

	public static final NMSVersion NMS_VERSION = NMSVersion.valueOf(getSpigotVersionNMS());

	private static String getSpigotVersionNMS() {
		String v = Bukkit.getServer().getClass().getPackage().getName();
		return v.substring(v.lastIndexOf('.') + 1);
	}

	public static Class<?> getVanillaClass(String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + NMS_VERSION + "." + name);
	}

	public static Class<?> getCraftClass(String name) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + "." + name);
	}

    static Method getMethodIfValid(
            boolean isCraft,
            String className,
            String accessibleName,
            Class<?>... parameters
    ) {
        try {
            Class<?> clazz = isCraft ? getCraftClass(className) : getVanillaClass(className);
            try {
                Method method = clazz.getDeclaredMethod(accessibleName, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ClassNotFoundException ignored) {

        }

        return null;
    }

    static Field getFieldIfValid(
            boolean isCraft,
            String className,
            String accessibleName
    ) {
        try {
            Class<?> clazz = isCraft ? getCraftClass(className) : getVanillaClass(className);
            try {
                Field field = clazz.getDeclaredField(accessibleName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        } catch (ClassNotFoundException ignored) {

        }

        return null;
    }
}
