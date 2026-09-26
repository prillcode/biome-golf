# Releasing

Releases are driven by git tags. Pushing a `v*` tag triggers
`.github/workflows/release.yml`, which verifies the tag matches `gradle.properties`,
runs the full build (including tests), and publishes a GitHub Release with the mod JAR
attached.

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

## Server deployment bundle

Each release also attaches a consistently named server deployment bundle,
`birdie-biome-server.tar.gz`, built by `scripts/build-server-bundle.sh`. It
contains the parameterised `dev-server/docker-compose.yml`, the
`docker-compose.geyser.yml` visitor-mode overlay, and the mod JAR. The apcode.dev
deployment consumes it from a stable URL:

```text
https://github.com/prillcode/biome-golf/releases/latest/download/birdie-biome-server.tar.gz
```

Because the asset name never changes, cutting a mod release requires **no** change
on the deployment side. The Compose file is parameterised with local-development
defaults (named volume, `minecraft-golf-dev` container, dev MOTD); the Droplet
overrides those via environment variables written by cloud-init.

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
