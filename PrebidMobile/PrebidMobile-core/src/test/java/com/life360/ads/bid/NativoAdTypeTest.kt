package com.life360.ads.bid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the wire-format contract of [NativoAdType.fromInt]: the integer values are sent by the
 * server, so an accidental reordering or renumbering of the enum would silently misroute ad types.
 */
class NativoAdTypeTest {

    @Test
    fun fromInt_mapsEachKnownValue() {
        assertEquals(NativoAdType.ARTICLE, NativoAdType.fromInt(0))
        assertEquals(NativoAdType.DISPLAY, NativoAdType.fromInt(2))
        assertEquals(NativoAdType.CTP_VIDEO, NativoAdType.fromInt(3))
        assertEquals(NativoAdType.CAROUSEL, NativoAdType.fromInt(4))
        assertEquals(NativoAdType.STP_VIDEO, NativoAdType.fromInt(5))
        assertEquals(NativoAdType.STANDARD_DISPLAY, NativoAdType.fromInt(6))
        assertEquals(NativoAdType.STORY, NativoAdType.fromInt(7))
    }

    @Test
    fun fromInt_gapValueOne_returnsNull() {
        // 1 is intentionally absent from the enum.
        assertNull(NativoAdType.fromInt(1))
    }

    @Test
    fun fromInt_unknownValue_returnsNull() {
        assertNull(NativoAdType.fromInt(99))
        assertNull(NativoAdType.fromInt(-1))
    }
}
