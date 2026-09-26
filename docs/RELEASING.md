# Releasing

Releases are driven by git tags. Pushing a `v*` tag triggers
`.github/workflows/release.yml`, which verifies the tag matches `gradle.properties`,
runs the full build (including tests), publishes the mod to
[Modrinth](https://modrinth.com/mod/biome-golf), and attaches the JAR to an internal
GitHub Release. Player-facing distribution is Modrinth.

## Cut a release

1. Set `version` in `gradle.properties` to the new version, without the `v` prefix.
2. Add a matching `## [x.y.z] - YYYY-MM-DD` section to `CHANGELOG.md` and update the
   link references at the bottom of the file.
3. Commit and push `main`.
4. Run the verification ladder in `AGENTS.md` against the exact commit being released.
5. Create and push an annotated tag that matches the version exactly:

   ```bash
   git tag -a v0.6.0 -m "Release 0.6.0"
   git push origin v0.6.0
   ```

6. The release workflow builds and publishes the release. Check it with
   `gh release view v0.6.0`.

## Modrinth

Each release publishes the remapped mod JAR to
[Modrinth](https://modrinth.com/mod/biome-golf) with the
[Minotaur](https://github.com/modrinth/minotaur) Gradle plugin (`./gradlew modrinth`,
configured at the bottom of `build.gradle`). The version, loader (`fabric`), game
version (`26.2`), required `fabric-api` dependency, and changelog (the matching
`## [x.y.z]` section of `CHANGELOG.md`) are derived automatically.

CI needs a repository secret `MODRINTH_TOKEN`: a Modrinth personal access token with
`VERSION_CREATE` (and `PROJECT_WRITE`) scope. A new project or version is reviewed by
Modrinth moderators before it is publicly listed.

The dedicated server downloads the same public Modrinth JAR at boot; there is no
separate server bundle. Compose files live in the deployment repository
(`apcode-dev`), not here.

## Reproducible builds

The build pins JAR entry timestamps and entry order (`build.gradle`), so for the same
commit a local `./gradlew build` and the CI release build produce byte-identical JARs.
This is what makes the SHA-256 checks in the verification ladder and the go-live
checklist meaningful: compare a deployed artifact's checksum against the **release asset**
it was built from, or against a clean local build of the same commit.

## Rules

- The tag **must** equal `v` plus `gradle.properties` `version`; the workflow fails
  otherwise and publishes nothing.
- One version, one tag, one CHANGELOG entry. Do not reuse or move a published tag.
- The tagged commit is the exact source of the released JAR, so run the verification
  ladder before tagging.
- Tag a commit that contains this workflow. GitHub Actions reads the workflow file from
  the tagged commit, so a tag on an older commit will not trigger a release.
