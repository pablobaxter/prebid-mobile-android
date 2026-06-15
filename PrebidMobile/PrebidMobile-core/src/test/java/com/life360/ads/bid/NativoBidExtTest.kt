package com.life360.ads.bid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prebid.mobile.rendering.bidding.data.bid.Bid

/**
 * Covers the JSON-walking branches in [NativoBidExt]: null/missing nodes, the Boolean/Number/else
 * type handling for the `oo` flag, and malformed input. These determine ad routing, so the
 * fall-through-to-safe-default behavior must hold for every shape of input.
 */
class NativoBidExtTest {

    // region isOwnedOperated

    @Test
    fun isOwnedOperated_nullBid_returnsFalse() {
        assertFalse(NativoBidExt.isOwnedOperated(null))
    }

    @Test
    fun isOwnedOperated_booleanTrue_returnsTrue() {
        assertTrue(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("oo", true))))
    }

    @Test
    fun isOwnedOperated_booleanFalse_returnsFalse() {
        assertFalse(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("oo", false))))
    }

    @Test
    fun isOwnedOperated_numberOne_returnsTrue() {
        assertTrue(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("oo", 1))))
    }

    @Test
    fun isOwnedOperated_numberZero_returnsFalse() {
        assertFalse(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("oo", 0))))
    }

    @Test
    fun isOwnedOperated_negativeNumber_returnsTrue() {
        // Any non-zero number is truthy per the `toInt() != 0` rule.
        assertTrue(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("oo", -5))))
    }

    @Test
    fun isOwnedOperated_stringValue_returnsFalse() {
        // A non-Boolean/non-Number value hits the `else` branch.
        assertFalse(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("oo", "true"))))
    }

    @Test
    fun isOwnedOperated_missingOoKey_returnsFalse() {
        assertFalse(NativoBidExt.isOwnedOperated(bidWithNativo(JSONObject().put("other", 1))))
    }

    @Test
    fun isOwnedOperated_missingNativoNode_returnsFalse() {
        val bid = bidWithExt(JSONObject().put("notnativo", JSONObject()))
        assertFalse(NativoBidExt.isOwnedOperated(bid))
    }

    @Test
    fun isOwnedOperated_missingExtNode_returnsFalse() {
        val bid = Bid.fromJSONObject(JSONObject().put("id", "b1").put("price", 1.0))
        assertFalse(NativoBidExt.isOwnedOperated(bid))
    }

    @Test
    fun isOwnedOperated_bidWithNullJson_returnsFalse() {
        // fromJSONObject(null) yields a Bid whose jsonString is null -> early-return branch.
        assertFalse(NativoBidExt.isOwnedOperated(Bid.fromJSONObject(null)))
    }

    // endregion

    // region getNativoAdType

    @Test
    fun getNativoAdType_nullBid_returnsNull() {
        assertNull(NativoBidExt.getNativoAdType(null))
    }

    @Test
    fun getNativoAdType_validInt_mapsToEnum() {
        assertEquals(
            NativoAdType.DISPLAY,
            NativoBidExt.getNativoAdType(bidWithNativo(JSONObject().put("nativoAdType", 2)))
        )
    }

    @Test
    fun getNativoAdType_unknownInt_returnsNull() {
        assertNull(NativoBidExt.getNativoAdType(bidWithNativo(JSONObject().put("nativoAdType", 99))))
    }

    @Test
    fun getNativoAdType_nonNumberValue_returnsNull() {
        assertNull(NativoBidExt.getNativoAdType(bidWithNativo(JSONObject().put("nativoAdType", "2"))))
    }

    @Test
    fun getNativoAdType_missingKey_returnsNull() {
        assertNull(NativoBidExt.getNativoAdType(bidWithNativo(JSONObject().put("oo", true))))
    }

    @Test
    fun getNativoAdType_missingNativoNode_returnsNull() {
        assertNull(NativoBidExt.getNativoAdType(bidWithExt(JSONObject())))
    }

    // endregion

    private fun bidWithNativo(nativo: JSONObject): Bid =
        bidWithExt(JSONObject().put("nativo", nativo))

    private fun bidWithExt(ext: JSONObject): Bid {
        val json = JSONObject()
            .put("id", "b1")
            .put("price", 1.0)
            .put("ext", ext)
        return Bid.fromJSONObject(json)
    }
}
