Biome Golf -- relicense to MIT; 0.9.4 release prepared, awaiting Modrinth moderation

THE PROBLEM
The Modrinth project (pRtCRbeQ) has been stuck in "Under Review" for weeks, so the
listing and every uploaded version return 404 anonymously. The custom license was the
likely blocker, since Modrinth only allows approved licenses. Relicensed the project to
MIT and prepared a 0.9.4 patch release so the published artifact matches the listing.

WHAT WE TRIED / SHIPPED
- Hit the Modrinth API directly: project pRtCRbeQ and version 6KOGW4NP both 404
  anonymously and the site shows "Project not found" -- normal pre-moderation state.
- Reverted the custom license to MIT in LICENSE, fabric.mod.json, and README; softened
  the README branding wording from MUST to "is strongly encouraged to".
- Bumped gradle.properties to 0.9.4, added the dated changelog entry and link refs.
- Ran the local ladder: ./gradlew test and ./gradlew build both green. Built
  biome-golf-0.9.4.jar (sha256 fd42c6ad...) embeds MIT and "license":"MIT".
- Committed on main as cca09fb (relicense) + e173643 (release 0.9.4). NOT pushed yet.

WHAT NEEDS TO HAPPEN NEXT
1. With user go-ahead, push main and cut the tag (tag is immutable once pushed):
   git push origin main; git tag -a v0.9.4 -m "Release 0.9.4"; git push origin v0.9.4.
   The tag triggers release.yml: build+test, GitHub release, then ./gradlew modrinth.
2. Keep waiting on Modrinth moderation; verify https://modrinth.com/mod/biomegolf goes
   live and capture the exact versioned 0.9.4 JAR URL.
3. Optional (needs user approval, no urgency -- 0.9.4 has zero gameplay changes):
   advance apcode-dev's BIRDIE_BIOME_MOD_URL pin, per docs/RELEASING.md. Use the
   versioned URL, never the moving `latest` alias.

CURRENT STATE
- main = e173643 (2 commits ahead of origin/main 1fafe42), not pushed. Latest released
  tag = v0.9.3 (2026-10-04, release workflow green). gradle.properties version = 0.9.4.
- Modrinth project pRtCRbeQ is under review; docs/RELEASING.md says the apcode-dev pin
  is currently biome-golf-0.8.1.jar -- verify in apcode-dev before relying on it.
- Untracked HANDOFF.md is tracked in this update; no MEMORY.md exists in the repo.
