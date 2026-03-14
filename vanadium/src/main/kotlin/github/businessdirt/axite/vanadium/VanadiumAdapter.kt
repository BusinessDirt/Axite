package github.businessdirt.axite.vanadium

interface VanadiumAdapter {
    fun initialize() {}
    fun update(deltaTime: Float) {}
    fun input(deltaTime: Float) {}
    fun shutdown() {}
}