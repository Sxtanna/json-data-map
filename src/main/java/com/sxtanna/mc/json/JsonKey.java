package com.sxtanna.mc.json;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sxtanna.mc.json.pxth.Pxth;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Represents the information necessary to encode and decode a {@link JsonElement} to/from {@code T} at a certain path
 *
 * @param <T> The type this key is encoding and decoding
 */
public interface JsonKey<T>
{

    /**
     * @return The path this key points to
     */
    @NotNull Pxth pxth();

    /**
     * @return The type of the value at this key
     */
    @NotNull Type type();


    /**
     * Represents a {@link JsonKey} that directly encodes and decodes its value
     *
     * @param <T> The type this key is encoding and decoding
     */
    interface Direct<T> extends JsonKey<T>
    {

        /**
         * Encodes the provided {@link T} into a {@link JsonElement}
         *
         * @param data The data to encode
         * @return The encoded {@link JsonElement}, or {@link com.google.gson.JsonNull#INSTANCE} if null
         */
        @NotNull JsonElement into(@Nullable final T data);

        /**
         * Decodes from the provided {@link JsonElement} to {@link T}
         *
         * @param json The json to decode
         * @return The decoded {@link T}, or {@code null}
         */
        @Nullable T from(@NotNull final JsonElement json);

    }


    /**
     * @return The new {@link JsonKey} with the provided {@link Pxth} and {@link Class}
     */
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

    /**
     * @return The new {@link JsonKey} with the provided {@link Pxth} and type resolved from the {@link TypeToken}
     */
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


    /**
     * @return The new {@link JsonKey.Direct} with the provided {@link Pxth} and {@link Class} that uses the provided into and from functions
     */
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

    /**
     * @return The new {@link JsonKey.Direct} with the provided {@link Pxth} and type resolved from the {@link TypeToken} that uses the provided into and from functions
     */
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
