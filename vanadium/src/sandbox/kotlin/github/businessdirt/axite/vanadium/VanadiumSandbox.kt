package github.businessdirt.axite.vanadium

class VanadiumSandbox : VanadiumAdapter {

    override fun configure(config: VanadiumConfig) {
        config.applicationName = "Sandbox"
    }

    override fun initialize() {}
    override fun update(deltaTime: Long) {}
    override fun shutdown() {}
}