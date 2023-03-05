package cn.powernukkitx.cloud.util;

public enum EnumOS {
    LINUX,
    SOLARIS,
    WINDOWS,
    OSX,
    UNKNOWN;

    static EnumOS OS = null;

    public static EnumOS getOs() {
        if (OS != null) {
            return OS;
        }
        String s = System.getProperty("os.name").toLowerCase();
        return OS = (s.contains("win") ? WINDOWS : (s.contains("mac") ? OSX : (s.contains("solaris") ? SOLARIS : (s.contains("sunos") ? SOLARIS : (s.contains("linux") ? LINUX : (s.contains("unix") ? LINUX : UNKNOWN))))));
    }

    public static boolean isWindows() {
        return getOs() == WINDOWS;
    }

    public static boolean isMac() {
        return getOs() == OSX;
    }

    public static boolean isUnix() {
        return getOs() == LINUX || getOs() == SOLARIS;
    }
}
