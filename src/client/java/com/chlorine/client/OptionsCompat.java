package com.chlorine.client;

import net.minecraft.client.OptionInstance;

/**
 * Small helper for reading/setting enum-valued Options fields (like
 * particles) without needing to import their exact enum class.
 *
 * Minecraft 26.1/26.2 moved a large number of classes around as part of
 * removing obfuscation, and some (like the particle status enum) ended up
 * somewhere this mod's author couldn't pin down from outside a working
 * build environment. Since Java generics erase at runtime, an
 * OptionInstance can be handled as a raw type and its enum constants
 * looked up by name via reflection — this keeps working correctly no
 * matter which package the enum actually lives in, and survives future
 * renames instead of breaking on every one.
 */
public final class OptionsCompat {
    private OptionsCompat() {
    }

    /**
     * Sets the given OptionInstance to whichever of its enum's constants
     * has the given name (e.g. "MINIMAL"). Does nothing if the option's
     * value type isn't an enum or has no matching constant.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean setEnumByName(OptionInstance option, String constantName) {
        Object current = option.get();
        if (current == null || !current.getClass().isEnum()) {
            return false;
        }
        for (Object constant : current.getClass().getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(constantName)) {
                option.set(constant);
                return true;
            }
        }
        return false;
    }
}
