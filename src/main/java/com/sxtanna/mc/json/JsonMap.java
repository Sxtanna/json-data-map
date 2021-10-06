package com.sxtanna.mc.json;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface JsonMap
{

    @NotNull
    Gson NORMAL_PRINT_GSON = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().serializeSpecialFloatingPointValues().create();

    @NotNull
    Consumer<Throwable> IGNORED_EXCEPTION = $ -> {};
    @NotNull
    Consumer<Throwable> PRINT_STACK_TRACE = Throwable::printStackTrace;


    @NotNull Map<String, JsonElement> data();


    default @NotNull JsonElement select(@NotNull @Unmodifiable final List<String> path)
    {
        return path.isEmpty() ? JsonNull.INSTANCE : find(path);
    }

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
            final var json = find(path.subList(0, path.size() - 1));

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
            final var json = find(path.subList(0, path.size() - 1));

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


    //<editor-fold desc="Select Methods">
    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return evalQuery(path, type, false, gson, exceptionHandler);
    }

    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return evalQuery(path, type.getType(), false, gson, exceptionHandler);
    }

    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return jKey instanceof JsonKey.Direct<T> direct ? select(direct, exceptionHandler) : evalQuery(jKey.pxth().path(), jKey.type(), false, gson, exceptionHandler);
    }

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


    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return select(path, type, gson, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return select(path, type, gson, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return select(jKey, gson, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T select(@NotNull final JsonKey.Direct<T> jKey)
    {
        return select(jKey, PRINT_STACK_TRACE);
    }


    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return select(jKey, NORMAL_PRINT_GSON, exceptionHandler);
    }


    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return select(path, type, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T select(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return select(path, type, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T select(@NotNull final JsonKey<T> jKey)
    {
        return select(jKey, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }


    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(evalQuery(path, type, false, gson, exceptionHandler));
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(evalQuery(path, type.getType(), false, gson, exceptionHandler));
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(select(jKey, gson, exceptionHandler));
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey.Direct<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(select(jKey, exceptionHandler));
    }


    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return selectOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return selectOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return selectOpt(jKey, gson, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey.Direct<T> jKey)
    {
        return selectOpt(jKey, IGNORED_EXCEPTION);
    }


    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return selectOpt(jKey, NORMAL_PRINT_GSON, exceptionHandler);
    }


    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return selectOpt(path, type, NORMAL_PRINT_GSON, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return selectOpt(path, type, NORMAL_PRINT_GSON, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> selectOpt(@NotNull final JsonKey<T> jKey)
    {
        return selectOpt(jKey, NORMAL_PRINT_GSON, IGNORED_EXCEPTION);
    }
    //</editor-fold>


    //<editor-fold desc="Remove Methods">
    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return evalQuery(path, type, true, gson, exceptionHandler);
    }

    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return evalQuery(path, type.getType(), true, gson, exceptionHandler);
    }

    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return jKey instanceof JsonKey.Direct<T> direct ? remove(direct, exceptionHandler) : evalQuery(jKey.pxth().path(), jKey.type(), true, gson, exceptionHandler);
    }

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


    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return remove(path, type, gson, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return remove(path, type, gson, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return remove(jKey, gson, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T remove(@NotNull final JsonKey.Direct<T> jKey)
    {
        return remove(jKey, PRINT_STACK_TRACE);
    }


    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return remove(jKey, NORMAL_PRINT_GSON, exceptionHandler);
    }


    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return remove(path, type, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T remove(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return remove(path, type, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }

    default <T> @Nullable T remove(@NotNull final JsonKey<T> jKey)
    {
        return remove(jKey, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }


    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(evalQuery(path, type, true, gson, exceptionHandler));
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(evalQuery(path, type.getType(), true, gson, exceptionHandler));
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(remove(jKey, gson, exceptionHandler));
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey.Direct<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return Optional.ofNullable(remove(jKey, exceptionHandler));
    }


    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Gson gson)
    {
        return removeOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Gson gson)
    {
        return removeOpt(path, type, gson, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey, @NotNull final Gson gson)
    {
        return removeOpt(jKey, gson, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey.Direct<T> jKey)
    {
        return removeOpt(jKey, IGNORED_EXCEPTION);
    }


    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(path, type, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        return removeOpt(jKey, NORMAL_PRINT_GSON, exceptionHandler);
    }


    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<T> type)
    {
        return removeOpt(path, type, NORMAL_PRINT_GSON, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull @Unmodifiable final List<String> path, @NotNull final TypeToken<T> type)
    {
        return removeOpt(path, type, NORMAL_PRINT_GSON, IGNORED_EXCEPTION);
    }

    default <T> @NotNull Optional<T> removeOpt(@NotNull final JsonKey<T> jKey)
    {
        return removeOpt(jKey, NORMAL_PRINT_GSON, IGNORED_EXCEPTION);
    }
    //</editor-fold>


    //<editor-fold desc="Insert Methods">
    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(path, data.getClass(), data, gson, exceptionHandler);
    }

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


    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(path, data, gson, PRINT_STACK_TRACE);
    }

    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(path, type, data, gson, PRINT_STACK_TRACE);
    }

    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data, @NotNull final Gson gson)
    {
        insert(jKey, data, gson, PRINT_STACK_TRACE);
    }

    default <T> void insert(@NotNull final JsonKey.Direct<T> jKey, @NotNull final T data)
    {
        insert(jKey, data, PRINT_STACK_TRACE);
    }


    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(path, data, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(path, type, data, NORMAL_PRINT_GSON, exceptionHandler);
    }

    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data, @NotNull final Consumer<Throwable> exceptionHandler)
    {
        insert(jKey, data, NORMAL_PRINT_GSON, exceptionHandler);
    }


    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final T data)
    {
        insert(path, data, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }

    default <T> void insert(@NotNull @Unmodifiable final List<String> path, @NotNull final Class<? extends T> type, @NotNull final T data)
    {
        insert(path, type, data, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }

    default <T> void insert(@NotNull final JsonKey<T> jKey, @NotNull final T data)
    {
        insert(jKey, data, NORMAL_PRINT_GSON, PRINT_STACK_TRACE);
    }
    //</editor-fold>


    private @NotNull JsonElement find(@NotNull @Unmodifiable final List<String> path)
    {
        if (path.isEmpty())
        {
            return JsonNull.INSTANCE;
        }

        var json = data().get(path.get(0));

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

    private @NotNull JsonElement push(@NotNull final String name, @NotNull final JsonElement json, @NotNull final JsonElement data)
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

    private <T> @Nullable T evalQuery(@NotNull @Unmodifiable final List<String> path, @NotNull final Type type, final boolean remove, @NotNull final Gson gson, @NotNull final Consumer<Throwable> exceptionHandler)
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

}