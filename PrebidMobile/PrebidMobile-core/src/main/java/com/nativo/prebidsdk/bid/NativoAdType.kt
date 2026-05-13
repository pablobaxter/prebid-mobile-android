package com.nativo.prebidsdk.bid

enum class NativoAdType(val type: Int) {
    ARTICLE(0),
    DISPLAY(2),
    CTP_VIDEO(3),
    CAROUSEL(4),
    STP_VIDEO(5),
    STANDARD_DISPLAY(6),
    STORY(7);

    companion object {
        fun fromInt(value: Int): NativoAdType? {
            return entries.firstOrNull { it.type == value }
        }
    }
}