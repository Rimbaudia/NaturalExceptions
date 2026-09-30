
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The sky dungeon tests hold a few dozen whole citadels and sanctums in memory at once.
tasks.withType<Test>().configureEach {
    maxHeapSize = "1536m"
}
