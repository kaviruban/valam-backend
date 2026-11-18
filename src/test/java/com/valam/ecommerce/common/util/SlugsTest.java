package com.valam.ecommerce.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugsTest {

    @Test
    void buildsSlugs() {
        assertEquals("cold-pressed-groundnut-oil", Slugs.slugify("Cold Pressed  Groundnut Oil!"));
        assertEquals("cafe-500g", Slugs.slugify("  Café 500g  "));
        assertEquals("a-b", Slugs.slugify("--a__b--"));
    }

    @Test
    void dropsNonLatinScripts() {
        assertEquals("", Slugs.slugify("முறுக்கு"));
        assertEquals("murukku", Slugs.slugify("முறுக்கு Murukku"));
    }
}
