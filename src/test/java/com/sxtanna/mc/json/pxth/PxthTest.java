package com.sxtanna.mc.json.pxth;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PxthTest
{

    @Test
    void testOfStringRaw()
    {
        final var pxthNone = assertDoesNotThrow(() -> Pxth.of(" "));
        final var pxthSome = assertDoesNotThrow(() -> Pxth.of("this.is.a.path"));

        assertSame(Pxth.NONE, pxthNone);
        assertEquals(0, pxthNone.path().size());

        assertNotSame(Pxth.NONE, pxthSome);
        assertEquals(4, pxthSome.path().size());

        assertIterableEquals(List.of("this", "is", "a", "path"), pxthSome.path());
    }

    @Test
    void testOfStringArr()
    {
        final var pxthNone = assertDoesNotThrow(() -> Pxth.of());
        final var pxthSome = assertDoesNotThrow(() -> Pxth.of("this", "is", "a", "path"));

        assertSame(Pxth.NONE, pxthNone);
        assertEquals(0, pxthNone.path().size());

        assertNotSame(Pxth.NONE, pxthSome);
        assertEquals(4, pxthSome.path().size());

        assertIterableEquals(List.of("this", "is", "a", "path"), pxthSome.path());
    }

    @Test
    void testOfStringCol()
    {
        final var pxthNone = assertDoesNotThrow(() -> Pxth.of(Collections.emptyList()));
        final var pxthSome = assertDoesNotThrow(() -> Pxth.of(List.of("this", "is", "a", "path")));

        assertSame(Pxth.NONE, pxthNone);
        assertEquals(0, pxthNone.path().size());

        assertNotSame(Pxth.NONE, pxthSome);
        assertEquals(4, pxthSome.path().size());

        assertIterableEquals(List.of("this", "is", "a", "path"), pxthSome.path());
    }

}