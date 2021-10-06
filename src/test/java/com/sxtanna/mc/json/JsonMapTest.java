package com.sxtanna.mc.json;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JsonMapTest
{

    private record TestJsonMap(@NotNull Map<String, JsonElement> data) implements JsonMap
    {

        TestJsonMap()
        {
            this(new LinkedHashMap<>());
        }


        @Contract("_, _ -> this")
        public TestJsonMap with(@NotNull final String name, @NotNull final JsonElement json)
        {
            this.data().put(name, json);
            return this;
        }

    }

    private record TestJsonObj(@NotNull JsonObject json)
    {

        TestJsonObj()
        {
            this(new JsonObject());
        }


        @Contract("_, _ -> this")
        public TestJsonObj with(@NotNull final String name, @NotNull final JsonElement json)
        {
            this.json().add(name, json);
            return this;
        }

    }

    private record TestJsonArr(@NotNull JsonArray json)
    {

        TestJsonArr()
        {
            this(new JsonArray());
        }


        @Contract("_, -> this")
        public TestJsonArr with(@NotNull final JsonElement json)
        {
            this.json().add(json);
            return this;
        }

    }


    @Test
    void testSelectShallow()
    {
        final var map = new TestJsonMap().with("hello", new JsonPrimitive("world"))
                                         .with("world", new JsonPrimitive("hello"));

        assertAll("shallow json map",
                  () ->
                  {
                      final var hello = assertDoesNotThrow(() -> map.select(List.of("world")));

                      assertInstanceOf(JsonPrimitive.class, hello);
                      assertEquals("hello", hello.getAsString());
                  },
                  () ->
                  {
                      final var world = assertDoesNotThrow(() -> map.select(List.of("hello")));

                      assertInstanceOf(JsonPrimitive.class, world);
                      assertEquals("world", world.getAsString());
                  });
    }


    @Test
    void testSelectNesting()
    {
        final var map = new TestJsonMap().with("hello", new TestJsonObj().with("world", new JsonPrimitive("hello")).json())
                                         .with("world", new TestJsonObj().with("hello", new JsonPrimitive("world")).json());

        assertAll("nesting json map",
                  () ->
                  {
                      final var helloJsonObj = assertDoesNotThrow(() -> map.select(List.of("world")));

                      assertInstanceOf(JsonObject.class, helloJsonObj);
                      assertEquals(1, helloJsonObj.getAsJsonObject().size());


                      final var world = assertDoesNotThrow(() -> map.select(List.of("world", "hello")));

                      assertInstanceOf(JsonPrimitive.class, world);
                      assertEquals("world", world.getAsString());
                  },
                  () ->
                  {
                      final var worldJsonObj = assertDoesNotThrow(() -> map.select(List.of("hello")));

                      assertInstanceOf(JsonObject.class, worldJsonObj);
                      assertEquals(1, worldJsonObj.getAsJsonObject().size());


                      final var hello = assertDoesNotThrow(() -> map.select(List.of("hello", "world")));

                      assertInstanceOf(JsonPrimitive.class, hello);
                      assertEquals("hello", hello.getAsString());
                  });
    }


    @Test
    void testSelectIndexed()
    {
        final var map = new TestJsonMap().with("array", new TestJsonArr().with(new JsonPrimitive("hello"))
                                                                         .with(new JsonPrimitive("world")).json());

        assertAll("indexed values",
                  () ->
                  {
                      final var hello = assertDoesNotThrow(() -> map.select(List.of("array", "0")));

                      assertInstanceOf(JsonPrimitive.class, hello);
                      assertEquals("hello", hello.getAsString());
                  },
                  () ->
                  {
                      final var world = assertDoesNotThrow(() -> map.select(List.of("array", "1")));

                      assertInstanceOf(JsonPrimitive.class, world);
                      assertEquals("world", world.getAsString());
                  });
    }


    @Test
    void testRemove()
    {
    }

    @Test
    void testInsert()
    {
    }


}