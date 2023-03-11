package main.osu.storyboard;

/**
 * Created by dgsrz on 16/9/16.
 */
public enum Command {

    F, M, MX, MY, S, V, R, C, P, L, T, NONE;

    public static Command getType(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (Exception e) {
            return NONE;
        }
    }
}
