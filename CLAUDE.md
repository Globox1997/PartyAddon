# PartyAddon

The sections marked *(project)* are specific to this mod. All other sections are shared by every mod and kept identical across projects.

## Project *(project)*
- Mod ID: `partyaddon`
- Base package: `net.partyaddon`
- Minecraft: 1.21.1
- Java: 21
- Loader: Fabric only (single Gradle project)
- Description: Party system for LevelZ. Players invite each other into groups, share vanilla and LevelZ experience with a group bonus, and see their group members' health in a HUD. The party screen is a LibZ inventory tab.
- Entrypoints: `net.partyaddon.PartyAddonMain` (main), `net.partyaddon.PartyAddonClient` (client), `net.partyaddon.config.ModMenuIntegration` (modmenu)
- Required dependencies: Fabric API, Cloth Config, LevelZ, LibZ
- Optional / compile-only compat: Clumps (`ClumpsCompat`, only loaded when `isClumpsLoaded`)
- Compile-only, on the dev runtime: Mod Menu (`ModMenuIntegration`)

## Versions and build setup *(project)*
- All exact versions (Minecraft, loader, Fabric API, dependencies) live in `gradle.properties`. That file is the source of truth; read it instead of assuming a version.
- Mappings: official Mojang mappings via `mappings loom.officialMojangMappings()`. There are no Yarn mappings in this project.
- Plugin: Fabric Loom (`fabric-loom`, remapping). Mod dependencies use the `mod*` configurations (`modImplementation`, `modCompileOnly`, `modApi`, ...).
- Loom remaps the mixin annotations in the built class files from Mojang names to intermediary. There is no refmap in the jar; this is expected.
- Java toolchain, `release`, `fabric.mod.json` and the mixin `compatibilityLevel` (`JAVA_<n>`) all use the same Java version.

## Build and run *(project)*
```
./gradlew build          # build/libs/partyaddon-<mod_version>.jar
./gradlew runClient      # game dir run/
./gradlew runServer
./gradlew genSources     # decompiled vanilla sources with Mojang names
```
- Loom accepts `--args="--quickPlaySingleplayer \"<world>\""`.
- The run `mods` folder may only contain jars for the current Minecraft version; keep jars for other versions in a subfolder.
- The Mojang-named game jar is in `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<minecraft_version>-loom.mappings.*/`.
- CI: `.github/workflows/build.yml` builds the mod, `publish.yml` publishes the jar.

## Mixins
- Mixins live in `src/main/java/net/partyaddon/mixin` and `partyaddon.mixins.json`.
- Keep them small, prefer `@Inject` over `@Overwrite`. MixinExtras (`@WrapWithCondition`, `@WrapOperation`, `@Local`, ...) ships with Fabric Loader and may be used.
- `defaultRequire` is 1: a missing target crashes the game when the mixin applies.
- Nothing validates targets at build time. Check every new or changed `method` / `@At(target = ...)` against the bytecode of the game jar with `javap -p -c -cp <jar> <class>`, not against decompiled source.
- Mixins into other mods (`mixin/compat`) use that mod's own names with `remap = false`. Lambda targets keep the name from the mod's source (e.g. `lambda$onPlayerCollision$N` in LevelZ), even when the enclosing method is renamed to a Mojang name.
- Confirm mixins applied at runtime: Fabric logs `Mixing <mixin> from partyaddon.mixins.json into <class>` in `run/logs/debug.log`.

## Code conventions
- Use Mojang names everywhere: code, mixin targets and descriptors, `@Shadow` / `@Accessor` / `@Invoker`. Online tutorials often use Yarn names (`PlayerEntity`, `World`); never copy them without checking. If unsure about a name, check the project or the jars with `javap`; ask rather than invent.
- Prefer loader APIs where a hook exists. Use mixins only when there is no API solution.
- Always put `@Override` on overriding methods so renames in future versions cause a compile error instead of a silently dead method.
- Always use curly braces for `if` statements, even for single-line bodies.
- All player-facing text goes through lang files (`Component.translatable`), no hardcoded strings.
- Do not add new dependencies without asking first.

## Working style
- Briefly explain what you plan to change, then change it. For larger tasks, propose a plan first.
- Only modify files that are necessary for the task. No unrequested refactoring.
- Do not delete files and do not run git commands that alter history (no `reset --hard`, `push`, `rebase`) unless explicitly told to.
- Never write comments in files.

## Off limits
- Never read or write credentials, tokens or signing keys (e.g. `.env`, publish tokens for Modrinth/CurseForge).
- Only touch `gradle.properties` for version numbers, and only after asking.

## Mod specifics *(project)*
- Key classes and packages:
    - `group/PartyManager`: server-side state (parties, invitations, stars), all party actions, experience sharing, invitation expiry and the member health sync every 10 ticks. Parties only hold online players; disconnecting removes a player from their party.
    - `group/Party`: members, leader and the per-party experience pools that carry fractional experience between pickups.
    - `group/ClientParty`: client-side copy of the synced state. `version` increases on every change except member health and lets `PartyScreen` rebuild its player list only when needed; `pendingInvitationsVersion` increases on every pending invitation sync.
    - `network/PartyAddonServerPacket` / `PartyAddonClientPacket`: payload registration, receivers and send helpers. Payload records live in `network/packet`.
    - `screen/PartyScreen`: party screen (player list with search, invitation, group). Opened by the server through `PartyScreenPacket`, either from the LibZ tab (`screen/widget/PartyTab`) or the keybind in `init/KeyInit`.
    - `util/PartyRenderHelper`: group HUD with names and hearts. `util/NameHelper`: name lookup through the tab list, then the world.
    - `mixin/ExperienceOrbEntityMixin` and `mixin/compat/LevelExperienceOrbEntityMixin`: redirect vanilla and LevelZ orb pickups into `PartyManager.shareVanillaExperience` / `shareLevelZExperience`.
- The server always answers an invite with `SyncPendingInvitationsPacket`, also when it rejects the invite, so the client can re-enable the invite button.
- The config (`config/PartyAddonConfig`, AutoConfig with Jankson) holds server and client options in one file; `groupSize`, `groupXpBonus` and `fullGroupBonus` are synced to clients with `SyncSettingsPacket` on join.
