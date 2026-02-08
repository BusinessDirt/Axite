ksp {
    arg("processor.prefix", "Events")
    arg("processor.methodAnnotations", "github.businessdirt.axite.events.HandleEvent")
    arg("processor.HandleEvent.interface", "github.businessdirt.axite.events.EventRegistryProvider")
}
