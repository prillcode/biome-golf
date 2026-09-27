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
   git tag -a vX.Y.Z -m "Release X.Y.Z"
   git push origin vX.Y.Z
   ```

6. The release workflow builds and publishes the release. Check that release with
   `gh release view vX.Y.Z` (using the tag you just pushed).
7. After the release is available on Modrinth, update the live server as described
   below. Tagging and publishing alone do not update the production instance.

## After release: update the live server

The on-demand production server (`birdie-biome`, hosted at `bbmc.apcode.dev`) is
controlled by the `apcode-api` Worker (`api.apcode.dev`) in the `apcode-dev`
repository. Its `BIRDIE_BIOME_MOD_URL` Worker secret is currently pinned to the
Modrinth CDN JAR for `biome-golf-0.7.1.jar`. A new release does not change this
secret, so the live server remains on its existing version until the pin is
updated.

After confirming the new version is published and downloadable on Modrinth:

1. In the `apcode-dev` checkout, follow `docs/secrets.md` to update the
   `BIRDIE_BIOME_MOD_URL` Worker secret to the exact versioned JAR URL from that
   Modrinth release. Do not put secret values in this repository or its logs.
   The server must use the pinned version URL, not the moving `latest` alias.
2. Follow `apcode-dev/docs/deployment.md` and
   `apcode-dev/docs/minecraft-operations.md` to apply the Worker configuration
   and start the on-demand Droplet (`birdie-biome`). The Droplet downloads the
   configured JAR on its next start; an already-running server does not hot-swap
   the mod.
3. Verify the server is healthy and that its startup logs identify the expected
   mod version, using the apcode-dev operational procedures. If startup or
   download fails, stop and resolve it there before calling the release live.

This manual pin is the current production contract. Dynamic Modrinth version
resolution could remove the per-release secret update, but is not enabled; only
adopt it after the Modrinth project is approved and the apcode-dev deployment is
explicitly changed and verified.

For course or other world-content changes, use the separate
[world sync workflow](WORLD-SYNC-WORKFLOW.md); publishing a mod release does not
promote local world edits.

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

The dedicated server downloads the public Modrinth JAR at boot. Its production
configuration lives in the deployment repository (`apcode-dev`), not here; this
repository's development Compose files are not the production deployment.

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
