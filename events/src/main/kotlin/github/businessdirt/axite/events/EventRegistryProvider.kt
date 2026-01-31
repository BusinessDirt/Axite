package github.businessdirt.axite.events

import kotlin.reflect.KFunction

interface EventRegistryProvider {
    val methods: List<KFunction<*>>
}
