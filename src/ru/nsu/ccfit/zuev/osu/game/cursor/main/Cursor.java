package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import androidx.annotation.Nullable;

import com.osudroid.game.CursorEvent;

import org.anddev.andengine.input.touch.TouchEvent;

import java.util.ArrayList;

/**
 * Represents a cursor in gameplay.
 */
public class Cursor {
    /**
     * Whether this {@link Cursor} is blocked from a {@link TouchEvent#ACTION_DOWN}.
     */
    public boolean mouseBlocked = false;

    /**
     * Whether the {@link Cursor} is currently pressed down.
     */
    public boolean mouseDown = false;

    /**
     * A list of {@link CursorEvent}s of this {@link Cursor} that has not been processed yet.
     */
    public final ArrayList<CursorEvent> events = new ArrayList<>(25);

    /**
     * Obtains the earliest {@link CursorEvent} of this {@link Cursor}.
     *
     * @param actions The actions to filter by. If none are provided, no filtering is done.
     * @return The earliest {@link CursorEvent}, or {@code null} if there are no {@link CursorEvent}s.
     */
    @Nullable
    public CursorEvent getEarliestEvent(int ...actions) {
        if (actions.length == 0) {
            return events.isEmpty() ? null : events.get(0);
        }

        int size = events.size();

        for (int i = 0; i < size; i++) {
            var event = events.get(i);

            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < actions.length; j++) {
                if (event.action == actions[j]) {
                    return event;
                }
            }
        }

        return null;
    }

    /**
     * Obtains the latest {@link CursorEvent} of this {@link Cursor}.
     *
     * @param actions The actions to filter by. If none are provided, no filtering is done.
     * @return The latest {@link CursorEvent}, or {@code null} if there are no {@link CursorEvent}s.
     */
    @Nullable
    public CursorEvent getLatestEvent(int ...actions) {
        if (actions.length == 0) {
            return events.isEmpty() ? null : events.get(events.size() - 1);
        }

        int size = events.size();

        for (int i = size - 1; i >= 0; i--) {
            var event = events.get(i);

            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < actions.length; j++) {
                if (event.action == actions[j]) {
                    return event;
                }
            }
        }

        return null;
    }
}
