package com.nativo.prebidsdk.renderer

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import com.nativo.prebidsdk.bid.NativoAdType
import com.nativo.prebidsdk.bid.NativoBidExt
import com.nativo.prebidsdk.utils.NativoUtils
import org.json.JSONObject
import org.prebid.mobile.LogUtil
import org.prebid.mobile.api.data.AdFormat
import org.prebid.mobile.api.exceptions.AdException
import org.prebid.mobile.api.rendering.PrebidDisplayView
import org.prebid.mobile.api.rendering.PrebidMobileInterstitialControllerInterface
import org.prebid.mobile.api.rendering.pluginrenderer.PluginEventListener
import org.prebid.mobile.api.rendering.pluginrenderer.PrebidMobilePluginRenderer
import org.prebid.mobile.configuration.AdUnitConfiguration
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse
import org.prebid.mobile.rendering.bidding.display.InterstitialController
import org.prebid.mobile.rendering.bidding.interfaces.InterstitialControllerListener
import org.prebid.mobile.rendering.bidding.listeners.DisplayVideoListener
import org.prebid.mobile.rendering.bidding.listeners.DisplayViewListener

class NativoPrebidRenderer : PrebidMobilePluginRenderer {

    companion object {
        private const val TAG = "NativoPrebidRenderer"
        const val NAME = "NativoRenderer"
        const val VERSION = "1.0.0"
        private const val BANNER_VIEW_CLASS = "org.prebid.mobile.api.rendering.BannerView"
    }

    override fun getName(): String = NAME

    override fun getVersion(): String = VERSION

    override fun getData(): JSONObject? = null

    override fun createBannerAdView(
        context: Context,
        displayViewListener: DisplayViewListener,
        displayVideoListener: DisplayVideoListener?,
        adUnitConfiguration: AdUnitConfiguration,
        bidResponse: BidResponse
    ): View {
        var displayViewRef: PrebidDisplayView? = null

        val forwardingListener = object : DisplayViewListener {
            override fun onAdLoaded() = displayViewListener.onAdLoaded()
            override fun onAdClicked() {
                displayViewRef?.let { displayView ->
                    val snapshot = NativoUtils.captureViewSnapshot(displayView)
                    snapshot.tag = TAG
                    snapshot.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        MATCH_PARENT
                    )
                    displayView.addView(snapshot)
                }
                displayViewListener.onAdClicked()
            }
            override fun onAdClosed() {
                displayViewListener.onAdClosed()
                displayViewRef?.let { displayView ->
                    displayView.postDelayed({
                        val snapshot = displayView.findViewWithTag<ImageView>(TAG)
                        snapshot?.let {
                            displayView.removeView(it)
                        }
                    }, 500)
                }
            }
            override fun onAdDisplayed() {
                displayViewRef?.let { displayView ->

                    // Check if bid has a Nativo ad before rendering
                    if (isNativoAd(bidResponse)) {
                        // Ensure view is attached to window before rendering
                        if (displayView.isAttachedToWindow) {
                            renderNativoAd(displayView, bidResponse)
                        } else {
                            displayView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                                override fun onViewAttachedToWindow(view: View) {
                                    renderNativoAd(displayView, bidResponse)
                                }
                                override fun onViewDetachedFromWindow(v: View) {}
                            })
                        }
                    }
                    displayViewListener.onAdDisplayed()
                }
            }
            override fun onAdFailed(exception: AdException?) = displayViewListener.onAdFailed(exception)
        }

        displayViewRef = PrebidDisplayView(
            context,
            forwardingListener,
            displayVideoListener,
            adUnitConfiguration,
            bidResponse
        )

        val adType = bidResponse.winningBid?.let { NativoBidExt.getNativoAdType(it) }
        if (adType == NativoAdType.STORY || adType == NativoAdType.STP_VIDEO || adType == NativoAdType.CTP_VIDEO) {
            displayViewRef.getInterstitialDisplayProperties().dialogBackgroundColor = Color.BLACK
        }

        // Set default height to WRAP_CONTENT for non-Nativo ads
        displayViewRef.layoutParams = FrameLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT
        )

        return displayViewRef
    }

    override fun createInterstitialController(
        context: Context,
        interstitialControllerListener: InterstitialControllerListener,
        adUnitConfiguration: AdUnitConfiguration,
        bidResponse: BidResponse
    ): PrebidMobileInterstitialControllerInterface? {
        return try {
            InterstitialController(context, interstitialControllerListener)
        } catch (exception: AdException) {
            LogUtil.error(TAG, "message: ${exception.message}")
            null
        }
    }

    override fun isSupportRenderingFor(adUnitConfiguration: AdUnitConfiguration): Boolean {
        return adUnitConfiguration.isAdType(AdFormat.BANNER)
    }

    override fun registerEventListener(
        pluginEventListener: PluginEventListener,
        listenerKey: String
    ) {
    }

    override fun unregisterEventListener(listenerKey: String) {
    }

    override fun didInjectView(view: View, inBannerView: View, bidResponse: BidResponse) {
        // Set wrap content as the default layout. Will be overridden later if is a Nativo ad.
        view.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
    }

    // Private methods

    private fun isNativoAd(bidResponse: BidResponse): Boolean {
        bidResponse.winningBid?.let {
            val nativoAdType = NativoBidExt.getNativoAdType(it)
            if (nativoAdType != null) {
                return nativoAdType != NativoAdType.STANDARD_DISPLAY
            } else {
                // Fallback
                val adm = bidResponse.winningBid?.adm ?: ""
                return adm.contains("load.js", ignoreCase = true)
            }
        }
        return false
    }

    /**
     * Expand the article full width, and attempt to expand height to parent container,
     * while also ensuring at least a minimum height of the requested bid.
     */
    private fun renderNativoAd(displayView: PrebidDisplayView, bidResponse: BidResponse) {
        val parentView = findBannerViewParent(displayView)
        parentView?.post {
            val minHeightPx = (bidResponse.winningBid?.height ?: 0).dpToPx()
            expandFullWidth(displayView)
            applyHeightStrategy(displayView, parentView, minHeightPx)
        }
    }

    /**
     * Chooses between two height strategies:
     * - MATCH_PARENT or WRAP + minimum
     */
    private fun applyHeightStrategy(displayView: PrebidDisplayView, parentView: View, minHeightPx: Int) {
        val parentHeight = if (parentView.layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT)  0 else parentView.height
        if (parentHeight >= minHeightPx) {
            // If parent is taller than minHeight, expand to match parent
            expandFullHeight(displayView)
            expandChildViews(displayView, minHeightPx, useMatchParent = true)
        } else {
            // If parent is less than minHeight, enforce the bid's minimum
            setMinHeightChainUpward(displayView, minHeightPx)
            expandChildViews(displayView, minHeightPx, useMatchParent = false)
        }
    }

    private fun findBannerViewParent(view: View): View? {
        var current: View? = view.parent as? View
        while (current != null) {
            if (current::class.java.name == BANNER_VIEW_CLASS) return current.parent as? View
            current = current.parent as? View
        }
        return null
    }

    private fun expandFullWidth(view: View) {
        var currentView: View? = view
        while (currentView != null) {
            val params = currentView.layoutParams
            if (params != null) {
                params.width = MATCH_PARENT
                currentView.layoutParams = params
            }
            if (currentView::class.java.name == BANNER_VIEW_CLASS) break
            currentView = (currentView.parent as? View)
        }
    }

    private fun expandFullHeight(view: View) {
        var currentView: View? = view
        while (currentView != null) {
            val params = currentView.layoutParams
            if (params != null) {
                params.height = MATCH_PARENT
                currentView.layoutParams = params
            }
            if (currentView::class.java.name == BANNER_VIEW_CLASS) break
            currentView = (currentView.parent as? View)
        }
    }

    private fun setMinHeightChainUpward(view: View, minHeightPx: Int) {
        var currentView: View? = view
        while (currentView != null) {
            currentView.minimumHeight = minHeightPx
            val params = currentView.layoutParams
            if (params != null) {
                params.height = WRAP_CONTENT
                currentView.layoutParams = params
            }
            if (currentView::class.java.name == BANNER_VIEW_CLASS) break
            currentView = (currentView.parent as? View)
        }
    }

    private fun expandChildViews(displayView: ViewGroup, minHeightPx: Int, useMatchParent: Boolean) {
        val firstChild = displayView.getChildAt(0)
        if (firstChild == null) {
            LogUtil.error(TAG, "Nativo renderer expected a child view on PrebidDisplayView, but none was found.")
            return
        }

        val heightParam = if (useMatchParent) MATCH_PARENT else minHeightPx

        var current = firstChild as? ViewGroup
        while (current != null) {
            val params = current.layoutParams
            if (params != null) {
                params.width = MATCH_PARENT
                params.height = heightParam
                current.layoutParams = params
            } else {
                current.layoutParams = FrameLayout.LayoutParams(
                    MATCH_PARENT,
                    heightParam
                )
            }

            current = current.getChildAt(0) as? ViewGroup
        }
    }
}

private fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}
