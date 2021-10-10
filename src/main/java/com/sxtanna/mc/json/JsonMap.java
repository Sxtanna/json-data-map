package com.sxtanna.mc.json;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import com.sxtanna.mc.json.pxth.Pxth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A simple (enough) interface designed for attachment to any object for storing any arbitrary json data
 *
 * @see JsonMap#create()
 * @see JsonMap#create(Map)
 */
@AvailableSince("0.1.0")
public interface JsonMap
{

    @NotNull
    Gson                  GSON              = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().serializeSpecialFloatingPointValues().create();
    @NotNull
    AtomicReference<Gson> FALLBACK_GSON_REF = new AtomicReference<>(GSON);
    @NotNull
    Consumer<Throwable>   IGNORED_EXCEPTION = $ -> {};
    @NotNull
    Consumer<Throwable>   PRINT_STACK_TRACE = Throwable::printStackTrace;


    /**
     * @return The raw data stored in this {@link JsonMap}, should not be manually modified
     */
    @AvailableSince("0.1.0")
    @NotNull Map<String, JsonElement> data();


    /**
     * Select the value stored at the provided path
     *
     * @return The {@link JsonElement} stored in {@link JsonMap#data()} at the provided path,
     * or {@link JsonNull#INSTANCE} if the path is empty or nothing is stored there
     */
    @AvailableSince("0.1.0")
    default @NotNull JsonElement select(@NotNull @Unmodifiable final List<String> path)
    {
        return path.isEmpty() ? JsonNull.INSTANCE : find(data(), path);
    }

    /**
     * Remove the value stored at the provided path
     *
     * @return The removed {@link JsonElement} stored in {@link JsonMap#data()} at the provided path,
     * or {@link JsonNull#INSTANCE} if the path is empty or nothing is stored there
     */
    @AvailableSince("0.1.0")
    default @NotNull JsonElement remove(@NotNull @Unmodifiable final List<String> path)
    {
        if (path.isEmpty())
        {
            return JsonNull.INSTANCE;
        }

        final JsonElement prev;

        if (path.size() == 1)
        {
            prev = data().remove(path.get(0));
        }
        else
        {
            final var json = find(data(), path.subList(0, path.size() - 1));

            if (!json.isJsonArray() && !json.isJsonObject())
            {
                return JsonNull.INSTANCE;
            }

            if (!json.isJsonArray())
            {
                prev = json.getAsJsonObject().remove(path.get(path.size() - 1));
            }
            else
            {
                try
                {
                    prev = json.getAsJsonArray().remove(Integer.parseInt(path.get(path.size() - 1)));
                }
                catch (final NumberFormatException ignored)
                {
                    return JsonNull.INSTANCE;
                }
            }
        }

        return prev != null ? prev : JsonNull.INSTANCE;
    }

    /**
     * Insert a value at the provided path
     *
     * @return The replaced {@link JsonElement} stored in {@link JsonMap#data()} at the provided path,
     * or {@link JsonNull#INSTANCE} if the path is empty or nothing was stored there
     */
    @AvailableSince("0.1.0")
    default @NotNull JsonElement insert(@NotNull @Unmodifiable final List<String> path, @NotNull final JsonElement data)
    {
        if (path.isEmpty())
        {
            return JsonNull.INSTANCE;
        }

        final JsonElement prev;

        if (path.size() == 1)
        {
            prev = data().put(path.get(0), data);
        }
        else
        {
            final var json = find(data(), path.subList(0, path.size() - 1));

            if (json.isJsonObject() || json.isJsonArray())
            {
                prev = push(path.get(path.size() - 1), json, data);
            }
            else
            {
                var root = data().computeIfAbsent(path.get(0), $ -> new JsonObject());

                for (int i = 1; i < path.size() - 1; i++)
                {
                    if (!root.isJsonArray() && !root.isJsonObject())
                    {
                        return JsonNull.INSTANCE;
                    }

                    final var next = path.get(i);

                    JsonElement node;

                    if (root.isJsonObject())
                    {
                        node = root.getAsJsonObject().get(next);
                    }
                    else
                    {
                        try
                        {
                            node = root.getAsJsonArray().get(Integer.parseInt(next));
                        }
                        catch (final NumberFormatException ignored)
                        {
                            return JsonNull.INSTANCE;
                        }
                    }

                    if (node != null)
                    {
                        root = node;
                        continue;
                    }

                    node = new JsonObject();

                    if (root.isJsonObject())
                    {
                        root.getAsJsonObject().add(next, node);
                    }
                    else
                    {
                        try
                        {
                            root.getAsJsonArray().set(Integer.parseInt(next), node);
                        }
                        catch (final NumberFormatException ignored)
                        {
                            return JsonNull.INSTANCE;
                        }
                    }

                    root = node;
                }


                prev = push(path.get(path.size() - 1), root, data);
            }
        }

        return prev != null ? prev : JsonNull.INSTANCE;
    }


    /**
     * @see JsonMap#select(List)
     */
    @AvailableSince("0.1.0")
    default @NotNull JsonElement select(@NotNull final Pxth pxth)
    {
        return select(pxth.path());
    }

    /**
     * @see JsonMap#remove(List)
     */
    @AvailableSince("0.1.0")
    default @NotNull JsonElement remove(@NotNull final Pxth pxth)
    {
        return remove(pxth.path());
    }

    /**
     * @see JsonMap#insert(List, JsonElement)
     */
    @AvailableSince("0.1.0")
    default @NotNull JsonElement insert(@NotNull final Pxth pxth, @NotNull final JsonElement data)
    {
        return insert(pxth.path(), data);
    }


    //<editor-fold desc="Select Methods">

    //<editor-fold desc="Select Nullable">

    //<editor-fold desc="Select">

    /**
     * Select and decode the value stored at the provided path
     *
     * @param gson             The {@link Gson} instance to use in the decoding process
     * @param exceptionHandler The handler for any caught exception during the decoding process
     * @return The {@link T} decoded from the {@link JsonElement} stored in {@link JsonMap#data()} at the provided path,
     * or null if the path is empty, nothing is stored there, or the decoding process fails
     * @see JsonMap#select(List)
     * @see JsonMap#select(Pxth)
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return eval(path, type, false, gson, exceptionHandler);
    }

    /**
     * Select and decode the value stored at the provided path, using a {@link TypeToken}
     *
     * @see JsonMap#select(List, Class, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return eval(path, type.getType(), false, gson, exceptionHandler);
    }

    /**
     * @see JsonMap#select(List, Class, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(pxth.path(), type, gson, exceptionHandler);
    }

    /**
     * @see JsonMap#select(List, TypeToken, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(pxth.path(), type, gson, exceptionHandler);
    }

    /**
     * Select and decode the value stored using the path and type from the provided {@link JsonKey}
     *
     * @see JsonMap#select(Pxth, Class, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return jKey instanceof JsonKey.Direct<T> direct ? select(direct, exceptionHandler) : eval(jKey.pxth().path(), jKey.type(), false, gson, exceptionHandler);
    }

    /**
     * Select and decode the value stored using the path and type from the provided {@link JsonKey.Direct}
     *
     * @see JsonMap#select(Pxth, Class, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final JsonKey.Direct<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        try
        {
            return jKey.from(select(jKey.pxth().path()));
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return null;
    }
    //</editor-fold>


    //<editor-fold desc="Select with Default Exception Handler">

    /**
     * @see JsonMap#select(List, Class, Gson, Consumer)
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return select(path, type, gson, PRINT_STACK_TRACE);
    }

    /**
     * @see JsonMap#select(List, TypeToken, Gson, Consumer)
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return select(path, type, gson, PRINT_STACK_TRACE);
    }

    /**
     * @see JsonMap#select(Pxth, Class, Gson, Consumer)
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return select(pxth.path(), type, gson);
    }

    /**
     * @see JsonMap#select(Pxth, TypeToken, Gson, Consumer)
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return select(pxth.path(), type, gson);
    }

    /**
     * @see JsonMap#select(JsonKey, Gson, Consumer)
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return select(jKey, gson, PRINT_STACK_TRACE);
    }

    /**
     * @see JsonMap#select(JsonKey.Direct, Consumer)
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final JsonKey.Direct<T> jKey)
    {
        return select(jKey, PRINT_STACK_TRACE);
    }
    //</editor-fold>


    //<editor-fold desc="Select with Default Gson">

    /**
     * @see JsonMap#select(List, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(path, type, defaultGson(), exceptionHandler);
    }

    /**
     * @see JsonMap#select(List, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(path, type, defaultGson(), exceptionHandler);
    }

    /**
     * @see JsonMap#select(Pxth, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(pxth.path(), type, exceptionHandler);
    }

    /**
     * @see JsonMap#select(Pxth, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(pxth.path(), type, exceptionHandler);
    }

    /**
     * @see JsonMap#select(JsonKey, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(jKey, defaultGson(), exceptionHandler);
    }
    //</editor-fold>


    //<editor-fold desc="Select with Default Gson and Exception Handler">

    /**
     * @see JsonMap#select(List, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return select(path, type, defaultGson(), PRINT_STACK_TRACE);
    }

    /**
     * @see JsonMap#select(List, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return select(path, type, defaultGson(), PRINT_STACK_TRACE);
    }

    /**
     * @see JsonMap#select(Pxth, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final Class<T> type)
    {
        return select(pxth.path(), type);
    }

    /**
     * @see JsonMap#select(Pxth, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type)
    {
        return select(pxth.path(), type);
    }

    /**
     * @see JsonMap#select(JsonKey, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#PRINT_STACK_TRACE
     */
    @AvailableSince("0.1.0")
    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey)
    {
        return select(jKey, defaultGson(), PRINT_STACK_TRACE);
    }
    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="Select Optional">

    //<editor-fold desc="Select">

    /**
     * Select and decode the value stored at the provided path
     *
     * @param gson             The {@link Gson} instance to use in the decoding process
     * @param exceptionHandler The handler for any caught exception during the decoding process
     * @return An {@link Optional} containing {@link T} decoded from the {@link JsonElement} stored in {@link JsonMap#data()} at the provided path,
     * or {@link Optional#empty()} if the path is empty, nothing is stored there, or the decoding process fails
     * @see JsonMap#select(List)
     * @see JsonMap#select(Pxth)
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(eval(path, type, false, gson, exceptionHandler));
    }

    /**
     * Select and decode the value stored at the provided path, using a {@link TypeToken}
     *
     * @see JsonMap#selectOpt(List, Class, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(eval(path, type.getType(), false, gson, exceptionHandler));
    }

    /**
     * @see JsonMap#selectOpt(List, Class, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(pxth.path(), type, gson, exceptionHandler);
    }

    /**
     * @see JsonMap#selectOpt(List, TypeToken, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(pxth.path(), type, gson, exceptionHandler);
    }

    /**
     * Select and decode the value stored using the path and type from the provided {@link JsonKey}
     *
     * @see JsonMap#select(JsonKey, Gson, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(select(jKey, gson, exceptionHandler));
    }

    /**
     * Select and decode the value stored using the path and type from the provided {@link JsonKey.Direct}
     *
     * @see JsonMap#select(JsonKey.Direct, Consumer)
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey.Direct<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(select(jKey, exceptionHandler));
    }
    //</editor-fold>


    //<editor-fold desc="Select with Default Exception Handler">

    /**
     * @see JsonMap#selectOpt(List, Class, Gson, Consumer)
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return selectOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    /**
     * @see JsonMap#selectOpt(List, TypeToken, Gson, Consumer)
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return selectOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    /**
     * @see JsonMap#selectOpt(Pxth, Class, Gson, Consumer)
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return selectOpt(pxth.path(), type, gson);
    }

    /**
     * @see JsonMap#selectOpt(Pxth, TypeToken, Gson, Consumer)
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return selectOpt(pxth.path(), type, gson);
    }

    /**
     * @see JsonMap#selectOpt(JsonKey, Gson, Consumer)
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return selectOpt(jKey, gson, IGNORED_EXCEPTION);
    }

    /**
     * @see JsonMap#selectOpt(JsonKey.Direct, Consumer)
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey.Direct<T> jKey)
    {
        return selectOpt(jKey, IGNORED_EXCEPTION);
    }
    //</editor-fold>


    //<editor-fold desc="Select with Default Gson">

    /**
     * @see JsonMap#selectOpt(List, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(path, type, defaultGson(), exceptionHandler);
    }

    /**
     * @see JsonMap#selectOpt(List, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(path, type, defaultGson(), exceptionHandler);
    }

    /**
     * @see JsonMap#selectOpt(Pxth, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(pxth.path(), type, exceptionHandler);
    }

    /**
     * @see JsonMap#selectOpt(Pxth, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(pxth.path(), type, exceptionHandler);
    }

    /**
     * @see JsonMap#selectOpt(JsonKey, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(jKey, defaultGson(), exceptionHandler);
    }
    //</editor-fold>


    //<editor-fold desc="Select with Default Gson and Exception Handler">

    /**
     * @see JsonMap#selectOpt(List, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return selectOpt(path, type, defaultGson(), IGNORED_EXCEPTION);
    }

    /**
     * @see JsonMap#selectOpt(List, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return selectOpt(path, type, defaultGson(), IGNORED_EXCEPTION);
    }

    /**
     * @see JsonMap#selectOpt(Pxth, Class, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type)
    {
        return selectOpt(pxth.path(), type);
    }

    /**
     * @see JsonMap#selectOpt(Pxth, TypeToken, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type)
    {
        return selectOpt(pxth.path(), type);
    }

    /**
     * @see JsonMap#selectOpt(JsonKey, Gson, Consumer)
     * @see JsonMap#FALLBACK_GSON_REF
     * @see JsonMap#IGNORED_EXCEPTION
     */
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey)
    {
        return selectOpt(jKey, defaultGson(), IGNORED_EXCEPTION);
    }
    //</editor-fold>

    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="Remove Methods">

    //<editor-fold desc="Remove Nullable">

    //<editor-fold desc="Remove">
    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return eval(path, type, true, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return eval(path, type.getType(), true, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(pxth.path(), type, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(pxth.path(), type, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return jKey instanceof JsonKey.Direct<T> direct ? remove(direct, exceptionHandler) : eval(jKey.pxth().path(), jKey.type(), true, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final JsonKey.Direct<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        try
        {
            return jKey.from(remove(jKey.pxth().path()));
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return null;
    }
    //</editor-fold>


    //<editor-fold desc="Remove with Default Exception Handler">
    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return remove(path, type, gson, PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return remove(path, type, gson, PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return remove(pxth.path(), type, gson);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return remove(pxth.path(), type, gson);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return remove(jKey, gson, PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final JsonKey.Direct<T> jKey)
    {
        return remove(jKey, PRINT_STACK_TRACE);
    }
    //</editor-fold>


    //<editor-fold desc="Remove with Default Gson">
    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(path, type, defaultGson(), exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(path, type, defaultGson(), exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(pxth.path(), type, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(pxth.path(), type, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(jKey, defaultGson(), exceptionHandler);
    }
    //</editor-fold>


    //<editor-fold desc="Remove with Default Gson and Exception Handler">
    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return remove(path, type, defaultGson(), PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return remove(path, type, defaultGson(), PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final Class<T> type)
    {
        return remove(pxth.path(), type);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type)
    {
        return remove(pxth.path(), type);
    }

    @AvailableSince("0.1.0")
    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey)
    {
        return remove(jKey, defaultGson(), PRINT_STACK_TRACE);
    }
    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="Remove Optional">

    //<editor-fold desc="Remove">
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(eval(path, type, true, gson, exceptionHandler));
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(eval(path, type.getType(), true, gson, exceptionHandler));
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(pxth.path(), type, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(pxth.path(), type, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(remove(jKey, gson, exceptionHandler));
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey.Direct<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(remove(jKey, exceptionHandler));
    }
    //</editor-fold>


    //<editor-fold desc="Remove with Default Exception Handler">
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return removeOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return removeOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return removeOpt(pxth.path(), type, gson);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return removeOpt(pxth.path(), type, gson);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return removeOpt(jKey, gson, IGNORED_EXCEPTION);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey.Direct<T> jKey)
    {
        return removeOpt(jKey, IGNORED_EXCEPTION);
    }
    //</editor-fold>


    //<editor-fold desc="Remove with Default Gson">
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(path, type, defaultGson(), exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(path, type, defaultGson(), exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(pxth.path(), type, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(pxth.path(), type, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(jKey, defaultGson(), exceptionHandler);
    }
    //</editor-fold>


    //<editor-fold desc="Remove with Default Gson and Exception Handler">
    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return removeOpt(path, type, defaultGson(), IGNORED_EXCEPTION);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return removeOpt(path, type, defaultGson(), IGNORED_EXCEPTION);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final Class<T> type)
    {
        return removeOpt(pxth.path(), type);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final Pxth pxth, @NotNull final TypeToken<T> type)
    {
        return removeOpt(pxth.path(), type);
    }

    @AvailableSince("0.1.0")
    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey)
    {
        return removeOpt(jKey, defaultGson(), IGNORED_EXCEPTION);
    }
    //</editor-fold>

    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="Insert Methods">
    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(path, data.getClass(), data, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        try
        {
            final var json = gson.toJsonTree(data, type);

            if (json != null && !json.isJsonNull())
            {
                insert(path, json);
            }
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final T data, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(pxth.path(), data, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(pxth.path(), type, data, gson, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        if (jKey instanceof JsonKey.Direct<T> direct)
        {
            insert(direct, data, exceptionHandler);
        }
        else
        {
            try
            {
                final var json = gson.toJsonTree(data, jKey.type());

                if (json != null && !json.isJsonNull())
                {
                    insert(jKey.pxth().path(), json);
                }
            }
            catch (final Throwable ex)
            {
                exceptionHandler.accept(ex);
            }
        }
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final JsonKey.Direct<T> jKey, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        try
        {
            final var json = jKey.into(data);

            if (!json.isJsonNull())
            {
                insert(jKey.pxth().path(), json);
            }
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }
    }


    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(path, data, gson, PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(path, type, data, gson, PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(pxth.path(), data, gson);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(pxth.path(), type, data, gson);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(jKey, data, gson, PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final JsonKey.Direct<T> jKey, @NotNull final T data)
    {
        insert(jKey, data, PRINT_STACK_TRACE);
    }


    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(path, data, defaultGson(), exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(path, type, data, defaultGson(), exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(pxth.path(), data, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(pxth.path(), type, data, exceptionHandler);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(jKey, data, defaultGson(), exceptionHandler);
    }


    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data)
    {
        insert(path, data, defaultGson(), PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data)
    {
        insert(path, type, data, defaultGson(), PRINT_STACK_TRACE);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final T data)
    {
        insert(pxth.path(), data);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final Pxth pxth, @NotNull final Class<? extends T> type, @NotNull final T data)
    {
        insert(pxth.path(), type, data);
    }

    @AvailableSince("0.1.0")
    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data)
    {
        insert(jKey, data, defaultGson(), PRINT_STACK_TRACE);
    }
    //</editor-fold>


    private <T> @Nullable T eval(@NotNull @Unmodifiable final List<String> path, @NotNull final Type type, final boolean remove, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        final var json = remove ? remove(path) : select(path);
        if (json.isJsonNull())
        {
            return null;
        }

        try
        {
            return gson.fromJson(json, type);
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return null;
    }


    /**
     * @return The new {@link JsonMap} using a {@link LinkedHashMap}
     */
    @AvailableSince("0.1.0")
    @Contract(" -> new")
    static @NotNull JsonMap create()
    {
        return create(new LinkedHashMap<>());
    }

    /**
     * @return The new {@link JsonMap} with the provided data {@link Map}
     */
    @AvailableSince("0.1.0")
    @Contract(value = "_ -> new", pure = true)
    static @NotNull JsonMap create(@NotNull final Map<String, JsonElement> data)
    {
        return () -> data;
    }


    //<editor-fold desc="Extract Methods">

    //<editor-fold desc="Extract Nullable">
    static <T> @Nullable T extract(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        final var json = find(data, key.pxth().path());
        if (json.isJsonNull())
        {
            return null;
        }

        try
        {
            return gson.fromJson(json, key.type());
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return null;
    }

    static <T> @Nullable T extract(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key, @NotNull final Gson gson)
    {
        return extract(data, key, gson, PRINT_STACK_TRACE);
    }

    static <T> @Nullable T extract(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        if (!(key instanceof JsonKey.Direct<T> direct))
        {
            return extract(data, key, defaultGson(), exceptionHandler);
        }

        try
        {
            return direct.from(find(data, key.pxth().path()));
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return null;
    }

    static <T> @Nullable T extract(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key)
    {
        return extract(data, key, PRINT_STACK_TRACE);
    }
    //</editor-fold>


    //<editor-fold desc="Extract Optional">
    static <T> @NotNull Optional<T> extractOpt(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        final var json = find(data, key.pxth().path());
        if (json.isJsonNull())
        {
            return Optional.empty();
        }

        try
        {
            return Optional.ofNullable(gson.fromJson(json, key.type()));
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return Optional.empty();
    }

    static <T> @NotNull Optional<T> extractOpt(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key, @NotNull final Gson gson)
    {
        return extractOpt(data, key, gson, IGNORED_EXCEPTION);
    }

    static <T> @NotNull Optional<T> extractOpt(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        if (!(key instanceof JsonKey.Direct<T> direct))
        {
            return extractOpt(data, key, defaultGson(), exceptionHandler);
        }

        try
        {
            return Optional.ofNullable(direct.from(find(data, key.pxth().path())));
        }
        catch (final Throwable ex)
        {
            exceptionHandler.accept(ex);
        }

        return Optional.empty();
    }

    static <T> @NotNull Optional<T> extractOpt(@NotNull final Map<String, JsonElement> data, @NotNull final JsonKey<T> key)
    {
        return extractOpt(data, key, IGNORED_EXCEPTION);
    }
    //</editor-fold>

    //</editor-fold>


    private static @NotNull Gson defaultGson()
    {
        final var gson = FALLBACK_GSON_REF.get();
        return gson != null ? gson : GSON;
    }

    private static @NotNull JsonElement find(@NotNull final Map<String, JsonElement> data, @NotNull @Unmodifiable final List<String> path)
    {
        if (path.isEmpty())
        {
            return JsonNull.INSTANCE;
        }

        var json = data.get(path.get(0));

        if (json == null || path.size() == 1 || (!json.isJsonArray() && !json.isJsonObject()))
        {
            return json != null ? json : JsonNull.INSTANCE;
        }

        for (int i = 1; i < path.size(); i++)
        {
            if (json == null || (!json.isJsonArray() && !json.isJsonObject()))
            {
                return json != null ? json : JsonNull.INSTANCE;
            }

            final var next = path.get(i);

            if (!json.isJsonArray())
            {
                json = json.getAsJsonObject().get(next);
            }
            else
            {
                try
                {
                    json = json.getAsJsonArray().get(Integer.parseInt(next));
                }
                catch (final NumberFormatException ignored)
                {
                    return JsonNull.INSTANCE;
                }
            }
        }

        return json != null ? json : JsonNull.INSTANCE;
    }

    private static @NotNull JsonElement push(@NotNull final String name, @NotNull final JsonElement json, @NotNull final JsonElement data)
    {
        if (!json.isJsonArray() && !json.isJsonObject())
        {
            return JsonNull.INSTANCE;
        }

        final JsonElement prev;

        if (json.isJsonObject())
        {
            final var jobj = json.getAsJsonObject();

            prev = jobj.remove(name);

            jobj.add(name, data);
        }
        else
        {
            try
            {
                prev = json.getAsJsonArray().set(Integer.parseInt(name), data);
            }
            catch (final NumberFormatException ignored)
            {
                return JsonNull.INSTANCE;
            }
        }

        return prev != null ? prev : JsonNull.INSTANCE;
    }

}
