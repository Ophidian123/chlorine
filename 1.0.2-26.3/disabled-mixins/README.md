# Disabled mixins

Files here are excluded from the build entirely (this folder is outside
`src/`, so Gradle never compiles them) rather than deleted, so the work
isn't lost and can be picked back up once verified against a real 26.2
build environment.

## LightmapThrottleMixin.java

Targets `net.minecraft.client.renderer.LightTexture`, which fails to
compile — "cannot find symbol: class LightTexture". Minecraft 26.1 did a
complete rewrite of the lighting system ("Mojang completely rebuilt the
lighting system with the First Drop 2026"), which is a strong signal this
class was moved, renamed, or restructured as part of that rewrite — not
just an obfuscation-mapping change like most of this project's other
risk notes. That makes it a meaningfully worse guess than anything else
in Chlorine, so rather than keep gambling on it and blocking the whole
build, it's parked here until someone with a working 26.2 IDE setup can
open `com.mojang.blaze3d.platform` and `net.minecraft.client.renderer`
and find whatever actually holds the lightmap texture now (the
`Lightning` class in `com.mojang.blaze3d.platform` looks like a
plausible related class worth checking first).

To re-enable once fixed: move the file back to
`src/client/java/com/chlorine/mixin/client/`, add
`"client.LightmapThrottleMixin"` back to the `client` array in
`src/main/resources/chlorine.mixins.json`, and fix the import/target.
