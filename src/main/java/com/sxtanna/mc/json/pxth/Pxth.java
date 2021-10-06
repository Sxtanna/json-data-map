package com.sxtanna.mc.json.pxth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a traversable path through a {@link com.sxtanna.mc.json.JsonMap}
 * <ul>
 *   <li>Each individual {@link String} within the {@link Pxth#path} should be a valid JSON key. (or a whole number {@link Integer})</li>
 * </ul>
 */
public record Pxth(@NotNull @Unmodifiable List<String> path)
{

    /**
     * Empty {@link Pxth} constant
     */
    @NotNull
    public static final Pxth NONE = new Pxth(Collections.emptyList());


    /**
     * Attempts to create a {@link Pxth} from a single {@code path} String, splitting on '{@code .}'
     *
     * @param path The optionally period delimited path
     * @return The new {@link Pxth}, or {@link Pxth#NONE} if the path is blank
     */
    public static @NotNull Pxth of(@NotNull final String path)
    {
        return path.isBlank() ? NONE : of(path.split("\\."));
    }

    /**
     * Attempts to create a {@link Pxth} from a variable amount of Strings
     *
     * @param path The individual json keys that make up the path
     * @return The new {@link Pxth}, or {@link Pxth#NONE} if no keys are provided
     */
    public static @NotNull Pxth of(@NotNull final String @NotNull ... path)
    {
        return path.length == 0 ? NONE : new Pxth(List.of(path));
    }

    /**
     * Attempts to create a {@link Pxth} from a collection of Strings
     *
     * @param path The individual json keys stored in a Collection that make up the path
     * @return Thew new {@link Pxth}, or {@link Pxth#NONE} if the Collection is empty
     */
    public static @NotNull Pxth of(@NotNull final Collection<String> path)
    {
        return path.isEmpty() ? NONE : new Pxth(List.copyOf(path));
    }

}
