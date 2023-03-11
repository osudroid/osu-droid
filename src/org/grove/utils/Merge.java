package org.grove.utils;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Merge {
    public static String ObjectsToString(String... args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg);
        }
        return builder.toString();
    }
    public static String ObjectsToString(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(arg.toString());
        }
        return builder.toString();
    }

    private Merge() { }
}
