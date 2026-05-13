package com.nativo.prebidsdk.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import org.prebid.mobile.api.rendering.BannerView
import org.prebid.mobile.api.rendering.listeners.BannerViewListener
import java.util.concurrent.atomic.AtomicLong

object NativoUtils {
    fun debounceAction(intervalMs: Long, action: () -> Unit): () -> Unit {
        val lastCall = AtomicLong(0L)
        return {
            val now = SystemClock.elapsedRealtime()
            val previous = lastCall.get()
            if (now - previous >= intervalMs) {
                lastCall.set(now)
                action()
            }
        }
    }

    /**
     * Checks if the listener has implemented the onNativoAdLoaded method
     * by checking if it's overridden from the default interface implementation.
     */
    @JvmStatic
    fun hasImplementedNativoCallback(listener: BannerViewListener?): Boolean {
        if (listener == null) {
            return false
        }

        return try {
            val method = listener.javaClass.getMethod("onNativoAdLoaded", BannerView::class.java)
            // Check if the method is declared in the listener's own class (not just inherited from interface)
            // This means they've overridden it from the default implementation
            method.declaringClass != BannerViewListener::class.java
        } catch (e: NoSuchMethodException) {
            false
        }
    }
    
    /**
     * Captures a rasterized snapshot of the provided View and returns it as an ImageView.
     * 
     * @param view The View to capture
     * @return ImageView containing the rasterized snapshot of the view
     */
    @JvmStatic
    fun captureViewSnapshot(view: View): ImageView {
        // Create a bitmap with the same dimensions as the view
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        
        // Create a canvas to draw the view onto the bitmap
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        
        // Create an ImageView and set the captured bitmap
        val imageView = ImageView(view.context)
        imageView.setImageBitmap(bitmap)
        
        return imageView
    }
}
