package com.nativo.prebidsdk.bid

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.prebid.mobile.configuration.AdUnitConfiguration

/**
 * Regression tests for [NativoBidResponse] `applyTargeting`: price strings (`hb_pb` / `hb_pb_nativo`)
 * and creative size (`hb_size` / `hb_size_nativo`) derived from the winning bid.
 */
class NativoBidResponseTest {

    @Test
    fun hb_pb_formatsPriceWithTwoDecimalPlaces() {
        val price = 29.80
        val response = nativoBidResponse(singleBidJson(price))

        assertFalse(response.hasParseError())
        val bid = response.winningBid
        assertNotNull(bid)
        val targeting = bid!!.prebid.targeting

        assertEquals("29.80", targeting["hb_pb"])
        assertEquals("29.80", targeting["hb_pb_nativo"])
    }

    @Test
    fun hb_pb_forWholeNumberPrice_usesTwoFractionDigits() {
        val response = nativoBidResponse(singleBidJson(5.0))

        val bid = response.winningBid!!
        val hbPb = bid.prebid.targeting["hb_pb"]

        assertEquals("5.00", hbPb)
    }

    @Test
    fun hb_pb_forOnePointFive_usesTwoFractionDigits() {
        val response = nativoBidResponse(singleBidJson(1.5))

        val bid = response.winningBid!!
        val hbPb = bid.prebid.targeting["hb_pb"]

        assertEquals("1.50", hbPb)
    }

    @Test
    fun hb_pb_usesHighestPricedBid() {
        val low = bidJson("low", price = 0.5)
        val high = bidJson("high", price = 2.75)
        val json = responseJson(JSONArray().put(low).put(high))
        val response = nativoBidResponse(json)

        val winning = response.winningBid!!
        assertEquals("high", winning.id)
        assertEquals("2.75", winning.prebid.targeting["hb_pb"])
    }

    @Test
    fun hb_pb_forTypicalCpm_matchesTwoDecimalFormatting() {
        val response = nativoBidResponse(singleBidJson(0.15))

        val bid = response.winningBid!!
        assertEquals("0.15", bid.prebid.targeting["hb_pb"])
        assertEquals("0.15", bid.prebid.targeting["hb_pb_nativo"])
    }

    @Test
    fun hb_size_and_hb_size_nativo_areWidthTimesHeightFromWinningBid() {
        val response = nativoBidResponse(singleBidJson(price = 1.0, width = 320, height = 50))

        val bid = response.winningBid!!
        val targeting = bid.prebid.targeting
        val expected = "320x50"

        assertEquals(expected, targeting["hb_size"])
        assertEquals(expected, targeting["hb_size_nativo"])
    }

    @Test
    fun hb_size_usesWinningBidDimensionsWhenBidsHaveDifferentSizes() {
        val smallBanner = bidJson("small", price = 1.0, width = 320, height = 50)
        val leaderboard = bidJson("leader", price = 5.0, width = 728, height = 90)
        val response = nativoBidResponse(responseJson(JSONArray().put(smallBanner).put(leaderboard)))

        val winning = response.winningBid!!
        assertEquals("leader", winning.id)
        assertEquals("728x90", winning.prebid.targeting["hb_size"])
        assertEquals("728x90", winning.prebid.targeting["hb_size_nativo"])
    }

    @Test
    fun hb_size_whenWidthHeightMissing_defaultsToZeroByOpenRtbParser() {
        val bid = JSONObject()
            .put("id", "no-wh")
            .put("impid", "imp1")
            .put("price", 1.0)
            .put("adm", "adm")
            .put("ext", JSONObject().put("prebid", JSONObject()))
        val json = responseJson(JSONArray().put(bid))
        val response = nativoBidResponse(json)

        val winning = response.winningBid!!
        assertEquals(0, winning.width)
        assertEquals(0, winning.height)
        assertEquals("0x0", winning.prebid.targeting["hb_size"])
        assertEquals("0x0", winning.prebid.targeting["hb_size_nativo"])
    }

    private fun nativoBidResponse(json: String): NativoBidResponse {
        return NativoBidResponse(json, AdUnitConfiguration())
    }

    private fun singleBidJson(price: Double, width: Int = 320, height: Int = 50): String {
        return responseJson(JSONArray().put(bidJson("bid1", price, width, height)))
    }

    private fun responseJson(bids: JSONArray): String {
        val seatbid = JSONObject()
            .put("bid", bids)
            .put("seat", "nativo")
        return JSONObject()
            .put("id", "resp1")
            .put("seatbid", JSONArray().put(seatbid))
            .toString()
    }

    private fun bidJson(
        id: String,
        price: Double,
        width: Int = 320,
        height: Int = 50,
    ): JSONObject {
        val prebid = JSONObject()
        val ext = JSONObject().put("prebid", prebid)
        return JSONObject()
            .put("id", id)
            .put("impid", "imp1")
            .put("price", price)
            .put("adm", "adm")
            .put("w", width)
            .put("h", height)
            .put("ext", ext)
    }
}
