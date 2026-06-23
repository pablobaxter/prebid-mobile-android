package com.life360.ads.server

import com.life360.ads.bid.NativoBidResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.prebid.mobile.rendering.bidding.data.bid.Bid
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse
import org.prebid.mobile.test.utils.WhiteBox

/**
 * Covers the head-to-head auction logic in [NativoServerProxy.decideWinner] and its price helpers.
 * This decides which demand source renders, so every null-handling branch and the tie-break
 * (Nativo wins on equal price) is pinned down here.
 */
class NativoServerProxyTest {

    private lateinit var proxy: NativoServerProxy

    @Before
    fun setUp() {
        proxy = NativoServerProxy()
    }

    private fun setNativoResponse(response: NativoBidResponse?) {
        WhiteBox.setInternalState(proxy, "nativoBidResponse", response)
    }

    private fun responseWithPrice(price: Double): BidResponse {
        val bid = Mockito.mock(Bid::class.java)
        Mockito.`when`(bid.price).thenReturn(price)
        val response = Mockito.mock(BidResponse::class.java)
        Mockito.`when`(response.winningBid).thenReturn(bid)
        return response
    }

    private fun nativoResponseWithPrice(price: Double): NativoBidResponse {
        val bid = Mockito.mock(Bid::class.java)
        Mockito.`when`(bid.price).thenReturn(price)
        val response = Mockito.mock(NativoBidResponse::class.java)
        Mockito.`when`(response.winningBid).thenReturn(bid)
        return response
    }

    // region decideWinner

    @Test
    fun decideWinner_bothNull_returnsNull() {
        assertNull(proxy.decideWinner(null))
    }

    @Test
    fun decideWinner_nullPrebid_returnsNativo() {
        val nativo = Mockito.mock(NativoBidResponse::class.java)
        setNativoResponse(nativo)

        assertSame(nativo, proxy.decideWinner(null))
    }

    @Test
    fun decideWinner_nullNativo_returnsPrebid() {
        val prebid = Mockito.mock(BidResponse::class.java)

        assertSame(prebid, proxy.decideWinner(prebid))
    }

    @Test
    fun decideWinner_nativoHigherPrice_returnsNativo() {
        val nativo = nativoResponseWithPrice(5.0)
        setNativoResponse(nativo)

        assertSame(nativo, proxy.decideWinner(responseWithPrice(2.0)))
    }

    @Test
    fun decideWinner_prebidHigherPrice_returnsPrebid() {
        setNativoResponse(nativoResponseWithPrice(1.0))
        val prebid = responseWithPrice(3.0)

        assertSame(prebid, proxy.decideWinner(prebid))
    }

    @Test
    fun decideWinner_equalPrice_favorsNativo() {
        val nativo = nativoResponseWithPrice(2.0)
        setNativoResponse(nativo)

        // `nativoPrice >= prebidPrice` -> a tie goes to Nativo.
        assertSame(nativo, proxy.decideWinner(responseWithPrice(2.0)))
    }

    // endregion

    // region getBidFromResponse / getBidPrice

    @Test
    fun getBidFromResponse_nullResponse_returnsNull() {
        assertNull(proxy.getBidFromResponse(null))
    }

    @Test
    fun getBidFromResponse_returnsWinningBid() {
        val bid = Mockito.mock(Bid::class.java)
        val response = Mockito.mock(BidResponse::class.java)
        Mockito.`when`(response.winningBid).thenReturn(bid)

        assertSame(bid, proxy.getBidFromResponse(response))
    }

    @Test
    fun getBidPrice_nullResponse_returnsZero() {
        assertEquals(0.0, proxy.getBidPrice(null), 0.0)
    }

    @Test
    fun getBidPrice_noWinningBid_returnsZero() {
        val response = Mockito.mock(BidResponse::class.java)
        Mockito.`when`(response.winningBid).thenReturn(null)

        assertEquals(0.0, proxy.getBidPrice(response), 0.0)
    }

    @Test
    fun getBidPrice_returnsWinningBidPrice() {
        assertEquals(4.25, proxy.getBidPrice(responseWithPrice(4.25)), 0.0)
    }

    // endregion
}
