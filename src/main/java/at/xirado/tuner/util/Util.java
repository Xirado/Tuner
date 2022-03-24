package at.xirado.tuner.util;

import java.util.Locale;

public class Util {

    /**
     * Auto closes AutoClosables
     *
     * @param closeables Closeables
     */
    public static void closeQuietly(AutoCloseable... closeables)
    {
        for (AutoCloseable c : closeables)
        {
            if (c != null)
            {
                try
                {
                    c.close();
                }
                catch (Exception ignored) {}
            }
        }
    }

    public static boolean startsWithIgnoreCase(String string, String prefix) {
        string = string.toLowerCase(Locale.ROOT);
        prefix = prefix.toLowerCase(Locale.ROOT);
        return string.startsWith(prefix);
    }
}
