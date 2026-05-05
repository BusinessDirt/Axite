package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.core.events.Event
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory


class VanadiumSandbox : VanadiumAdapter {

    override suspend fun initialize(scope: CoroutineScope) {

    }

    override fun update(frameInfo: FrameInfo) {

    }

    override fun render(interpolation: Double) {

    }

    override fun onEvent(event: Event) {
        LoggerFactory.getLogger(VanadiumSandbox::class.java).info("Event handler called ${event.javaClass.simpleName}")
    }
}