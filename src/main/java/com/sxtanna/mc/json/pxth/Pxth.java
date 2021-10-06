package com.sxtanna.mc.json.pxth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record Pxth(@NotNull @Unmodifiable List<String> path)
{

    @NotNull
    public static final Pxth NONE = new Pxth(Collections.emptyList());


    public static @NotNull Pxth of(@NotNull final String path)
    {
        return path.isBlank() ? NONE : of(path.split("\\."));
    }

    public static @NotNull Pxth of(@NotNull final String @NotNull ... path)
    {
        return path.length == 0 ? NONE : new Pxth(List.of(path));
    }

    public static @NotNull Pxth of(@NotNull final Collection<String> path)
    {
        return path.isEmpty() ? NONE : new Pxth(List.copyOf(path));
    }

}
