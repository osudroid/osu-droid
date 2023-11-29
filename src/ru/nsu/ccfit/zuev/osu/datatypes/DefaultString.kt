package ru.nsu.ccfit.zuev.osu.datatypes

// I don't even get why is needed too many abstraction but whatever.
class DefaultString(val value: String) : DefaultData<String>(value) {
    override fun instanceDefaultValue() = value
}