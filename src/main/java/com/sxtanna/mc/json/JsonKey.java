package com.sxtanna.mc.json;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sxtanna.mc.json.pxth.Pxth;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.function.Function;

public interface JsonKey<T>
{

    @NotNull Pxth pxth();

    @NotNull Type type();


    interface Direct<T> extends JsonKey<T>
    {

        @NotNull JsonElement into(@Nullable final T data);

        @Nullable T from(@NotNull final JsonElement json);

    }


    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull <T> JsonKey<T> of(@NotNull final Pxth pxth,
                                      @NotNull final Class<T> type)
    {
        return new JsonKey<>()
        {
            @Override
            public @NotNull Pxth pxth()
            {
                return pxth;
            }

            @Override
            public @NotNull Type type()
            {
                return type;
            }
        };
    }

    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull <T> JsonKey<T> of(@NotNull final Pxth pxth,
                                      @NotNull final TypeToken<T> type)
    {
        return new JsonKey<>()
        {
            @Override
            public @NotNull Pxth pxth()
            {
                return pxth;
            }

            @Override
            public @NotNull Type type()
            {
                return type.getType();
            }
        };
    }


    @Contract(value = "_, _, _, _ -> new", pure = true)
    static @NotNull <T> JsonKey<T> of(@NotNull final Pxth pxth,
                                      @NotNull final Class<T> type,

                                      @NotNull final Function<@Nullable T, @NotNull JsonElement> into,
                                      @NotNull final Function<@NotNull JsonElement, @Nullable T> from)
    {
        return new JsonKey.Direct<>()
        {
            @Override
            public @NotNull Pxth pxth()
            {
                return pxth;
            }

            @Override
            public @NotNull Type type()
            {
                return type;
            }


            @Override
            public @NotNull JsonElement into(@Nullable final T data)
            {
                return into.apply(data);
            }

            @Override
            public @Nullable T from(final @NotNull JsonElement json)
            {
                return from.apply(json);
            }
        };
    }

    @Contract(value = "_, _, _, _ -> new", pure = true)
    static @NotNull <T> JsonKey<T> of(@NotNull final Pxth pxth,
                                      @NotNull final TypeToken<T> type,

                                      @NotNull final Function<@Nullable T, @NotNull JsonElement> into,
                                      @NotNull final Function<@NotNull JsonElement, @Nullable T> from)
    {
        return new JsonKey.Direct<>()
        {
            @Override
            public @NotNull Pxth pxth()
            {
                return pxth;
            }

            @Override
            public @NotNull Type type()
            {
                return type.getType();
            }


            @Override
            public @NotNull JsonElement into(@Nullable final T data)
            {
                return into.apply(data);
            }

            @Override
            public @Nullable T from(final @NotNull JsonElement json)
            {
                return from.apply(json);
            }
        };
    }

}
