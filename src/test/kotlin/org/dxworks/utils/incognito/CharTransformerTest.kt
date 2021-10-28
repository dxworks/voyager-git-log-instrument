package org.dxworks.utils.incognito

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CharTransformerTest {
    @AfterEach
    internal fun tearDown() {
        CharTransformer.DEFAULT_FILE.delete()
    }

    @Test
    internal fun checkDefaultFileGetsGeneratedIfNotExists() {
        CharTransformer.DEFAULT_FILE.delete()
        val charTransformer = CharTransformer()
        assertTrue{CharTransformer.DEFAULT_FILE.exists()}
        println(CharTransformer.DEFAULT_FILE.readLines())
        println(charTransformer.charMap)
    }

    @Test
    internal fun testAlgorithmGeneratesRandomMapping() {
        CharTransformer.DEFAULT_FILE.delete()
        val charTransformer1 = CharTransformer()
        println(charTransformer1.charMap)
        CharTransformer.DEFAULT_FILE.delete()
        val charTransformer2 = CharTransformer()
        println(charTransformer2.charMap)
        assertNotEquals(charTransformer1.charMap, charTransformer2.charMap)
    }
}
