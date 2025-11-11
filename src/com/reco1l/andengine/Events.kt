package com.reco1l.andengine

typealias OnUpdateEvent = (deltaTimeSec: Float) -> Unit

operator fun OnUpdateEvent?.plus(other: OnUpdateEvent): OnUpdateEvent {
    if (this == null) return other
    return { this(it); other(it) }
}