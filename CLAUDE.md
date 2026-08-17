# CLAUDE.md

Guidance for working in this repository.

## What this is

VoxyMap (repo name: 3D Maps Voxy Addon) is a **client-only Fabric mod** for Minecraft
that renders a full-screen 3D world map using [Voxy](https://github.com/MCRcortex/voxy)
LOD data. Pressing `M` opens a `Screen` that takes over the game camera and lets the
player fly over the terrain Voxy has already stored.

It does not draw the map itself. It moves the vanilla camera and suppresses the things
that would get in the way (fog, weather, clouds, the HUD, the held item), so the normal
world render — fed by Voxy — becomes the map.

## Toolchain

| Thing | Version |
| --- | --- |
| Minecraft | `26.2` |
| Java | **25** (JDK 25 required to build; MC 26.2 runs on Java 25) |
| Fabric Loader | `0.19.3` |
| Fabric API | `0.157.0+26.2` |
| Loom | `1.17.19` (plugin id `net.fabricmc.fabric-loom`) |
| Gradle | `9.7.0` (Loom 1.17 needs ≥ 9.5) |
| Voxy | `0.2.18-beta` (compile-only) |
| Iris | `1.11.2+26.2-fabric` (compile-only) |
| Xaero's World Map | `1.44.2` (compile-only) |

All versions live in `gradle.properties`; `build.gradle` reads them from there. Bump
them in one place.

```shell
./gradlew build          # produces build/libs/VoxyMap-<version>.jar
```

Set `JAVA_HOME` to a JDK 25 if the default JVM is older.

## Minecraft 26.x is deobfuscated — this changes everything

From the 26.x series Mojang ships the client with real class and method names and no
longer publishes `client_mappings`. Consequences that keep biting:

- **No `mappings` line in `build.gradle`.** `loom.officialMojangMappings()` fails with
  "Failed to find official mojang mappings".
- **Use the `net.fabricmc.fabric-loom` plugin, not `net.fabricmc.fabric-loom-remap`.**
  The `-remap` variant exists for older, obfuscated versions.
- **Plain `implementation` / `clientCompileOnly`**, not `modImplementation` /
  `modCompileOnly`. There is no remap step, so mod jars are ordinary dependencies.
- **`remap = false` on every mixin annotation.** The names in this source tree are the
  names at runtime; there is no refmap.
- The built `jar` is the shippable mod jar — there is no `remapJar`.

## Layout

```
src/main/resources/         fabric.mod.json, mixin config, lang files, icon
src/client/java/com/voxymap/client/
  VoxyMapClient.java        entrypoint; polls M each client tick
  gui/MapScreen.java        the map screen: camera control, HUD, input
  map/VoxyMapCameraController.java  the detached camera state the mixins read
  map/VoxyMapGuiRenderer.java       Iris-shaderpack fallback path (see below)
  map/VoxyBridge.java               toggles Voxy's environmental fog
  map/MapRenderSettingsGuard.java   forces clouds off while the map is open
  map/VoxyMapSettings.java          config/voxymap.properties
  integration/              Xaero's World Map bridge + its return button
  mixin/                    all mixins, listed in voxymap.client.mixins.json
```

`VoxyMapCameraController` is the hinge: `MapScreen` writes the desired camera into it
each frame, and `CameraMixin` applies it to the vanilla `Camera`. Everything else keys
off `VoxyMapCameraController.isActive()`.

`VoxyMapGuiRenderer` is only used when an Iris shader pack is active — the shader
pipeline cannot render the detached map camera in the normal world pass, so Voxy's
world render is cancelled and re-run straight into the main render target just before
the GUI draws.

## Porting notes / API landmines

Things that moved recently. Check these first when a new Minecraft version breaks the build:

- `GuiGraphics` **no longer exists**; it is `net.minecraft.client.gui.GuiGraphicsExtractor`.
  `drawString` → `text`, `drawCenteredString` → `centeredText`.
- `Screen.render(...)` → `Screen.extractRenderState(GuiGraphicsExtractor, int, int, float)`.
  `Screen.renderBackground(...)` → `Screen.extractBackground(...)`.
- `net.minecraft.client.gui.Gui` is now screen/overlay management. The HUD widgets
  (crosshair, hotbar, health…) live in `net.minecraft.client.gui.Hud` as `extractX` methods.
- `Minecraft.setScreen` / `Minecraft.screen` → `Minecraft.gui.setScreen(...)` / `Minecraft.gui.screen()`.
- `Minecraft.getMainRenderTarget()` → `Minecraft.gameRenderer.mainRenderTarget()`.
- `Player.displayClientMessage(Component, boolean)` → `LocalPlayer.sendSystemMessage(Component)`.
- `ResourceLocation` is `net.minecraft.resources.Identifier`; `ResourceKey.location()` is `identifier()`.
- `GuiRenderer.render(GpuBufferSlice)` → `GuiRenderer.render()` (no arguments).
- Input arrives as records: `mouseClicked(MouseButtonEvent, boolean)`, `keyPressed(KeyEvent)`.
- **The depth buffer is reversed**: clear depth to `0.0`, not `1.0`. Vanilla clears via
  `RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(...)`.

Voxy 0.2.18-beta specifics:

- `IGetVoxyRenderSystem` was renamed to `IVoxyRenderSystemHolder`.
- `VoxyRenderSystem.renderOpaque(Viewport)` → `renderOpaque(Viewport, int depthTextureViewGlId, int colorTextureViewGlId)`;
  Voxy now owns the framebuffer and restores GL state itself.
- `RenderPipelineFactory.createIrisPipeline` is private and takes five arguments.
  Returning `null` from it makes `createPipeline` fall back to `NormalRenderPipeline`.

## Verifying mixins without launching the game

Mixin failures are runtime failures, and a wrong `method =` with `require = 0` fails
*silently* — the map simply misbehaves. Do not trust "it compiles".

Because the game is deobfuscated, the client jar can be inspected directly:

```shell
curl -s https://piston-meta.mojang.com/mc/game/version_manifest_v2.json   # find the version json
# then download its downloads.client.url as client.jar
javap -p -classpath client.jar net.minecraft.client.Camera            # method/field names
javap -p -c -classpath client.jar net.minecraft.client.renderer.GameRenderer   # @At INVOKE call sites
```

After changing any mixin, confirm for each one that (a) the target class exists,
(b) every `method = "..."` name exists on it, (c) the handler's parameter list equals
the target's parameter list, and (d) every `@At(target = "...")` call site is really
present in the method being injected into. A short `javap` + regex script over
`src/client/java/com/voxymap/client/mixin` does this in seconds and is worth rerunning
on every version bump.

Prefer the default `require = 1` for vanilla targets so a broken injector fails loudly
at load. `require = 0` is reserved for `@Pseudo` mixins into optional mods
(`XaeroGuiMapMixin`), which legitimately may not be present.

## Conventions

- Mixin handler methods are prefixed `voxymap$`.
- Optional-mod access is wrapped in a nested holder class (`IrisAccess`, `XaeroAccess`)
  so the classes are only loaded when the mod is present, guarded by
  `FabricLoader.getInstance().isModLoaded(...)`.
- User-visible strings are translation keys in `assets/voxymap/lang/`
  (`en_us`, `pl_pl`, `ru_ru` — keep all three in sync).
- The Nether is deliberately unsupported and shows an explanatory panel.
