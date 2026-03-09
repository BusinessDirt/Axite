package github.businessdirt.axite.vanadium

interface VanadiumAdapter {
    fun onStart() {}
    fun onUpdate(deltaTime: Float) {}
    fun onRender() {}
    fun onShutdown() {}
}