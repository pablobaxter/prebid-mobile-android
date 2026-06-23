package com.life360.ads.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prebid.mobile.api.exceptions.AdException
import org.prebid.mobile.api.rendering.BannerView
import org.prebid.mobile.api.rendering.listeners.BannerViewListener

/**
 * Covers [NativoUtils.hasImplementedNativoCallback], which uses reflection to tell whether a
 * publisher actually overrode the optional `onNativoAdLoaded` default method. Getting this wrong
 * means either suppressing the Nativo render path or firing a callback the publisher never wired up.
 */
class NativoUtilsTest {

    /** Implements only the required methods, leaving `onNativoAdLoaded` as the interface default. */
    private open class BaseListener : BannerViewListener {
        override fun onAdLoaded(bannerView: BannerView?) {}
        override fun onAdDisplayed(bannerView: BannerView?) {}
        override fun onAdFailed(bannerView: BannerView?, exception: AdException?) {}
        override fun onAdClicked(bannerView: BannerView?) {}
        override fun onAdClosed(bannerView: BannerView?) {}
    }

    /** Explicitly overrides the optional Nativo callback. */
    private class NativoAwareListener : BaseListener() {
        override fun onNativoAdLoaded(bannerView: BannerView?) {}
    }

    @Test
    fun hasImplementedNativoCallback_null_returnsFalse() {
        assertFalse(NativoUtils.hasImplementedNativoCallback(null))
    }

    @Test
    fun hasImplementedNativoCallback_defaultNotOverridden_returnsFalse() {
        assertFalse(NativoUtils.hasImplementedNativoCallback(BaseListener()))
    }

    @Test
    fun hasImplementedNativoCallback_overridden_returnsTrue() {
        assertTrue(NativoUtils.hasImplementedNativoCallback(NativoAwareListener()))
    }
}
