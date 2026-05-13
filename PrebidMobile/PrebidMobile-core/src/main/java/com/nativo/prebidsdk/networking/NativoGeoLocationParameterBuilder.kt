package com.nativo.prebidsdk.networking

import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.rendering.networking.parameters.GeoLocationParameterBuilder

class NativoGeoLocationParameterBuilder : GeoLocationParameterBuilder() {
    override fun isEnabled(): Boolean =
        PrebidMobile.isShareGeoLocationWithNativo() || PrebidMobile.isShareGeoLocation()
}
