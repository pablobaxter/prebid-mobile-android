/*
 *    Copyright 2018-2021 Prebid.org, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.prebid.mobile.rendering.interstitial;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import org.prebid.mobile.LogUtil;
import org.prebid.mobile.rendering.models.InterstitialDisplayPropertiesInternal;
import org.prebid.mobile.rendering.utils.helpers.Utils;
import org.prebid.mobile.rendering.views.interstitial.InterstitialManager;
import org.prebid.mobile.rendering.views.webview.PrebidWebViewBase;
import org.prebid.mobile.rendering.views.webview.WebViewBase;
import org.prebid.mobile.rendering.views.webview.mraid.JSInterface;
import org.prebid.mobile.rendering.views.webview.mraid.Views;

//Used for MRAID ad expansion
public class AdExpandedDialog extends AdBaseDialog {
    private static final String TAG = AdExpandedDialog.class.getSimpleName();

    public AdExpandedDialog(final Context context, final WebViewBase webViewBaseLocal, InterstitialManager interstitialManager) {
        super(context, webViewBaseLocal, interstitialManager);

        // Ensure device status bar remains visible
        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        // Hide the web view for now to prevent glitching
        if (webViewBase != null) {
            webViewBase.setAlpha(0f);
        }

        //On MRAID expand we should not remove the old adview such that when the user closes the expanded ad
        //they see the old ad.

        preInit();

        // Set dialog background color from DisplayProperties
        InterstitialDisplayPropertiesInternal displayProperties = interstitialManager.getInterstitialDisplayProperties();
        if (displayProperties != null && displayProperties.dialogBackgroundColor != null) {
            adViewContainer.setBackgroundColor(displayProperties.dialogBackgroundColor);
        }

        // Attach adViewContainer to the dialog before show() is called so the WebView is already
        // in the view hierarchy when the slide-in animation begins, preventing a visible glitch.
        Views.removeFromParent(adViewContainer);
        addContentView(adViewContainer,
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                )
        );

        if (webViewBase != null && webViewBase.isMRAID()) {
            webViewBase.getMRAIDInterface().onStateChange(JSInterface.STATE_EXPANDED);
        }
        setOnCancelListener(dialog -> {
            try {
                if (webViewBase != null) {
                    //detach from closecontainer
                    webViewBase.detachFromParent();
                    //add it back to WebView.
                    PrebidWebViewBase defaultContainer = (PrebidWebViewBase) webViewBase.getPreloadedListener();

                    //use getPreloadedListener() to get defaultContainer, as defaultContainer is not initiated for non-mraid cases(such as interstitials)
                    defaultContainer.addView(webViewBase);
                    ////IMP - get the default state
                    defaultContainer.setVisibility(View.VISIBLE);
                    //do not ever call prebidWebView.visible. It makes the default adview on click of expand to be blank.
                    if (context instanceof Activity) {
                        ((Activity) context).setRequestedOrientation(initialOrientation);
                    } else {
                        LogUtil.error(TAG, "Context is not Activity, can not set orientation");
                    }

                    webViewBase.getMRAIDInterface().onStateChange(JSInterface.STATE_DEFAULT);
                }
            }
            catch (Exception e) {
                LogUtil.error(TAG, "Expanded ad closed but post-close events failed: " + Log.getStackTraceString(e));
            }
        });

        webViewBase.setDialog(this);
        bypassTouchListener();
    }

    // Bypass the default WebViewBase listener which prevents scrolling
    @SuppressLint("ClickableViewAccessibility")
    private void bypassTouchListener() {
        webViewBase.setOnTouchListener((v, event) -> {
            return false; // Return false to allow event propagation
        });
    }

    @Override
    protected void addCloseView() {
        super.addCloseView();
        if (closeView != null) {
            // Ensure close button sits comfortably below the status bar
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) closeView.getLayoutParams();
            params.topMargin += Utils.convertDpToPx(32, closeView.getContext());
            closeView.setLayoutParams(params);
        }
    }

    @Override
    protected void handleCloseClick() {
        interstitialManager.interstitialClosed(webViewBase);
    }

    @Override
    protected void handleDialogShow() {
        webViewBase.setOnContentReadyCallback(() ->
                webViewBase.postDelayed(() ->
                                webViewBase.animate().alpha(1f).setDuration(250).start()
                        ,300)

        );
    }
}
