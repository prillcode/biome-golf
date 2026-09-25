# Google Cloud Deployment and Mod Distribution

## Purpose

This guide describes a production-minded way to:

1. run the Minecraft Golf Fabric dedicated server in Docker on Google Cloud;
2. expose it at a stable domain such as `play.example.com`; and
3. distribute the Minecraft Golf mod so players can install the matching client.

It complements the local integration workflow in `dev-server/README.md`. The
tracked `dev-server/docker-compose.yml` remains the reproducible development and
acceptance environment; this document does not turn it into a production
configuration.

The examples reflect the repository baseline at the time this guide was written:

| Component | Version or setting |
|---|---|
| Minecraft Java Edition | `26.2` |
| Fabric Loader | `0.19.5` or newer compatible version |
| Fabric API | tested with `0.160.0+26.2` |
| Java | `25` |
| Minecraft Golf | `0.6.0` release baseline |
| Server port | TCP `25565` |
| Initial JVM heap | `4G` |

Treat that table as a release matrix, not as a promise that future releases use
the same versions. Update the table, server configuration, release notes, and
player instructions together whenever compatibility changes.

## Platform decision: Compute Engine, not Cloud Run

Use a **Google Compute Engine VM** running Docker Engine and Docker Compose.

Do not deploy the game server as a Cloud Run service. Cloud Run can execute an
arbitrary Linux container, but public Cloud Run services accept request-oriented
HTTP/HTTP2/gRPC traffic. They do not expose a normal public, long-lived Minecraft
TCP listener. Cloud Run service requests also have a finite timeout (currently up
to 60 minutes), and its stateless scaling model is a poor fit for one authoritative
Minecraft process and its world directory.

Cloud Run worker pools can receive private VPC TCP traffic, but that does not
provide the public, stable TCP endpoint or storage model needed here. Placing a
load balancer and more services around a worker pool would be needless complexity
compared with one VM.

GKE can also run the container, but Kubernetes is not justified for a single
1–4-player family server. Use it only if future requirements genuinely include a
fleet of independently scheduled servers.

References:

- [Cloud Run container runtime contract](https://docs.cloud.google.com/run/docs/container-contract)
- [Cloud Run request timeouts](https://docs.cloud.google.com/run/docs/configuring/request-timeout)
- [Running containers on Compute Engine](https://docs.cloud.google.com/compute/docs/containers/configuring-options-to-run-containers)

## Target architecture

```text
Authenticated Minecraft Java clients
  Minecraft 26.2 + Fabric Loader + Fabric API + Minecraft Golf
                              |
                              | play.example.com, TCP 25565
                              v
                    DNS provider (DNS only)
                              |
                              v
               reserved Google Cloud external IPv4
                              |
                    VPC firewall: TCP 25565
                              |
                              v
                 Compute Engine Ubuntu VM
                              |
                       Docker Compose
                              |
                              v
                  itzg/minecraft-server
                   Fabric + server mods
                              |
                              v
             separately attached Persistent Disk
                mounted at /srv/minecraft-golf
```

One server process owns the world and all gameplay state. Do not autoscale the
Minecraft service horizontally: two containers writing the same world would not
form a valid cluster.

## Scope: Java Edition only

This guide deploys the **Java** experience, which is the supported product. Bedrock
(Geyser/Floodgate) is **not** part of a production deployment: the Bedrock play track is
shelved, and the only client-light support is the promotional **visitor mode**, in which a
Bedrock or unmodified-Java connection can watch a round (`/golf spectator`) or join the
world (`/golf spectator leave`) but cannot play golf. See
`docs/BEDROCK-VISITOR-MODE-PLAN.md`.

To let visitors reach a *production* server you would additionally need to:

- install and pin Geyser/Floodgate in the Compose service (the optional
  `dev-server/docker-compose.geyser.yml` overlay is a reference and pins the tested builds);
- open **UDP `19132`** in the VPC firewall, scoped by the same `minecraft-golf-server`
  network tag — this guide otherwise opens only TCP `25565`; and
- configure the invite with `/golf visitor address <host>:25565` (plus optional
  `/golf visitor link` and `/golf visitor spawn set`).

None of that is required for the Java deployment below, and no Bedrock play or support is
claimed.

## Capacity and region

For the MVP's intended 1–4 players, start with:

- an Ubuntu LTS x86-64 VM;
- 2 vCPUs and 8 GiB RAM, such as `e2-standard-2`;
- a 30–50 GiB balanced Persistent Disk dedicated to Minecraft data;
- a 15–20 GiB boot disk;
- a standard, non-Spot provisioning model; and
- a region geographically close to the players.

The container currently gives the JVM a 4 GiB heap. An 8 GiB VM leaves headroom
for off-heap JVM memory, Linux, Docker, and filesystem caching. The Compute Engine
free-tier `e2-micro` is not suitable: its memory is less than the configured Java
heap, and its shared CPU is too constrained for stable ticks. Do not use a Spot VM
for the primary server because it can be terminated unexpectedly.

Minecraft performance is sensitive to main-thread speed. If profiling shows slow
ticks, compare a machine family with stronger per-core performance before adding
many more cores. Measure before resizing.

Use the [Google Cloud Pricing Calculator](https://cloud.google.com/products/calculator)
for the chosen region. Include the VM, boot disk, data disk, snapshots, external
IPv4, and outbound network transfer. Create a
[Cloud Billing budget](https://docs.cloud.google.com/billing/docs/how-to/budgets),
but remember that ordinary budget alerts notify rather than cap spending.

## 1. Prepare the Google Cloud project

Install and initialize the
[Google Cloud CLI](https://docs.cloud.google.com/sdk/docs/install), then select or
create a project with billing enabled. The following shell variables make the
examples explicit; replace their values for the real deployment:

```bash
export GOLF_PROJECT_ID="your-project-id"
export GOLF_REGION="us-east1"
export GOLF_ZONE="us-east1-b"
export GOLF_NETWORK="minecraft-golf-net"
export GOLF_SUBNET="minecraft-golf-subnet"
export GOLF_VM="minecraft-golf-server"
export GOLF_DATA_DISK="minecraft-golf-data"
export GOLF_ADDRESS="minecraft-golf-ip"

gcloud auth login
gcloud config set project "$GOLF_PROJECT_ID"
gcloud services enable compute.googleapis.com
```

Use a dedicated project when practical. It makes IAM, billing, firewall rules,
and eventual teardown easier to reason about.

## 2. Create a small dedicated network

A dedicated VPC avoids relying on permissive rules in the default network:

```bash
gcloud compute networks create "$GOLF_NETWORK" \
  --subnet-mode=custom

gcloud compute networks subnets create "$GOLF_SUBNET" \
  --network="$GOLF_NETWORK" \
  --region="$GOLF_REGION" \
  --range="10.20.0.0/24"
```

Allow Minecraft only on instances with the matching network tag:

```bash
gcloud compute firewall-rules create minecraft-golf-allow-game \
  --network="$GOLF_NETWORK" \
  --direction=INGRESS \
  --action=ALLOW \
  --rules=tcp:25565 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=minecraft-golf-server
```

For a closed family deployment, replace `0.0.0.0/0` with the players' stable
source CIDRs if that is practical. Otherwise, keep Minecraft authentication and
the server whitelist enabled.

Do not create public rules for RCON, the Docker daemon, SSH from the whole Internet,
or any database. Google Cloud firewall rules can target network tags so the game
rule does not apply to unrelated VMs. See
[VPC firewall targets and source filters](https://docs.cloud.google.com/vpc/docs/add-remove-network-tags).

For administration, prefer OS Login with two-factor authentication and SSH through
Identity-Aware Proxy (IAP), or restrict TCP `22` to a known administrator CIDR.
Follow Google's
[SSH access best practices](https://docs.cloud.google.com/compute/docs/connect/ssh-best-practices/login-access)
rather than opening SSH globally.

## 3. Reserve the address and data disk

Reserve a regional external IPv4 so the domain does not change after a stop or VM
replacement:

```bash
gcloud compute addresses create "$GOLF_ADDRESS" \
  --region="$GOLF_REGION"

gcloud compute addresses describe "$GOLF_ADDRESS" \
  --region="$GOLF_REGION" \
  --format='get(address)'
```

Copy the printed address into a separate value used when creating the VM. The
`--address` flag requires the IPv4 value, not the address resource name:

```bash
export GOLF_IPV4="203.0.113.10"
```

Create a separate data disk. Keeping the world off the boot disk makes snapshots,
restores, and VM replacement safer and more obvious:

```bash
gcloud compute disks create "$GOLF_DATA_DISK" \
  --zone="$GOLF_ZONE" \
  --type=pd-balanced \
  --size=50GB
```

External addresses and disks can incur charges even when the game is stopped.
Review current pricing before leaving unused resources allocated. Google documents
the static-address model in
[Configure static external IP addresses](https://docs.cloud.google.com/compute/docs/ip-addresses/configure-static-external-ip-address).

## 4. Create the VM

The following is a representative CLI creation command. It targets Ubuntu 26.04 LTS
(`ubuntu-2604-lts-amd64`, current at the time of writing); Ubuntu 24.04 LTS
(`ubuntu-2404-lts-amd64`) remains supported if you prefer it. Confirm the current image
family and flags with
`gcloud compute images list --project ubuntu-os-cloud --filter='family~ubuntu'` before
executing it:

```bash
gcloud compute instances create "$GOLF_VM" \
  --zone="$GOLF_ZONE" \
  --machine-type=e2-standard-2 \
  --image-family=ubuntu-2604-lts-amd64 \
  --image-project=ubuntu-os-cloud \
  --boot-disk-type=pd-balanced \
  --boot-disk-size=20GB \
  --disk="name=$GOLF_DATA_DISK,device-name=$GOLF_DATA_DISK,mode=rw,boot=no" \
  --address="$GOLF_IPV4" \
  --network="$GOLF_NETWORK" \
  --subnet="$GOLF_SUBNET" \
  --tags=minecraft-golf-server \
  --metadata=enable-oslogin=TRUE,enable-oslogin-2fa=TRUE \
  --shielded-secure-boot \
  --shielded-vtpm \
  --shielded-integrity-monitoring \
  --maintenance-policy=MIGRATE \
  --provisioning-model=STANDARD
```

Do not attach a broadly privileged service account by default. If backup automation
later needs a service account, create a dedicated identity and grant only the
specific storage or snapshot permissions it requires.

## 5. Format and mount the data disk

Connect to the VM and identify the disk using stable `/dev/disk/by-id/google-*`
names:

```bash
ls -l /dev/disk/by-id/google-*
lsblk -f
```

The following format command destroys existing contents. Run it only once, and
only after confirming that the target is the newly created blank disk:

```bash
sudo mkfs.ext4 -m 0 -F \
  /dev/disk/by-id/google-minecraft-golf-data
```

Mount it and create the server directories:

```bash
sudo mkdir -p /srv/minecraft-golf
sudo mount -o discard,defaults \
  /dev/disk/by-id/google-minecraft-golf-data \
  /srv/minecraft-golf

sudo mkdir -p /srv/minecraft-golf/data
sudo mkdir -p /srv/minecraft-golf/mods
```

Add the disk to `/etc/fstab` by UUID so it mounts after reboot. Validate the entry
with `sudo mount -a` before continuing. Use Google's current
[format and mount procedure](https://docs.cloud.google.com/compute/docs/disks/format-mount-disk-linux)
as the authority for device discovery, ownership, and `fstab` options.

Make the directory writable by the numeric user used by the selected
`itzg/minecraft-server` image. Confirm that image's documented/current UID before
changing ownership; do not solve a permission failure with world-writable mode.

## 6. Install Docker safely

Install Docker Engine and the Compose plugin from Docker's official apt repository:

- [Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
- [Install the Docker Compose plugin](https://docs.docker.com/compose/install/linux/)

Do not use Docker's convenience installation script for production. Verify the
installation:

```bash
sudo systemctl enable --now docker
sudo docker version
sudo docker compose version
```

Membership in the `docker` group is effectively root access. Either keep deployment
commands behind `sudo` or grant group membership only to trusted administrators.
Use the Google VPC firewall as the authoritative public-ingress boundary; Docker
published ports can interact unexpectedly with host-level `ufw` rules.

## 7. Stage a production deployment

Use a small deployment directory for Compose metadata and the separately mounted
Persistent Disk for state:

```text
/opt/minecraft-golf/
  compose.yaml
  mods/
    minecraft-golf-<version>.jar

/srv/minecraft-golf/
  data/                    # world, configs, logs, Fabric server files
```

Build the release locally or in CI, not on the production VM:

```bash
./gradlew clean test build
```

The player/server artifact is the non-sources JAR under `build/libs/`, currently
named like `minecraft-golf-0.6.0.jar`. Do not deploy the `-sources.jar`. Transfer
the release JAR to `/opt/minecraft-golf/mods/` using `gcloud compute scp`, `scp`,
or a controlled artifact pipeline.

### Production Compose template

Create `/opt/minecraft-golf/compose.yaml` from a reviewed template like this:

```yaml
services:
  minecraft:
    # Replace this placeholder with a tested immutable digest before launch.
    image: itzg/minecraft-server@sha256:REPLACE_WITH_TESTED_DIGEST
    container_name: minecraft-golf
    restart: unless-stopped
    stop_grace_period: 2m
    ports:
      - "25565:25565/tcp"
    environment:
      EULA: "TRUE"
      TYPE: "FABRIC"
      VERSION: "26.2"
      FABRIC_LOADER_VERSION: "0.19.5"
      MEMORY: "4G"
      MODE: "survival"
      DIFFICULTY: "peaceful"
      ONLINE_MODE: "TRUE"
      # Give the public server its own identity; credit minecraft_golf as the golf mod.
      MOTD: "Example Server | Golf powered by minecraft_golf"
      MODRINTH_PROJECTS: "fabric-api:0.160.0+26.2"
      # Whitelist: replace with the family's Minecraft usernames before sharing the hostname.
      ENABLE_WHITELIST: "TRUE"
      ENFORCE_WHITELIST: "TRUE"
      WHITELIST: |
        PlayerOne
        PlayerTwo
    volumes:
      - /srv/minecraft-golf/data:/data
      - /opt/minecraft-golf/mods:/mods:ro
    healthcheck:
      test: ["CMD", "mc-health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s
```

Important production choices:

- Accept the Minecraft EULA only after reviewing it; `EULA: "TRUE"` records that
  acceptance.
- Resolve and test an immutable image digest instead of deploying the moving
  `latest` tag.
- Pin Minecraft, Fabric Loader, and Fabric API. Record the exact files selected at
  startup in the deployment/release log.
- Keep `/data` on the Persistent Disk and the staged mod directory read-only.
- Keep `online-mode` enabled.
- Configure and enforce a Minecraft whitelist (`ENABLE_WHITELIST`/`ENFORCE_WHITELIST` plus
  the `WHITELIST` list) before sharing the hostname.
- Set `MODE`/`DIFFICULTY` to match the intended world. The tracked gameplay profile is
  `survival`/`peaceful`, which is also the basis for the peaceful-server model and visitor
  mode; change these only deliberately.
- Never publish RCON. If enabled for local automation, bind it only to loopback or
  a private administrative path and use a strong secret outside version control.
- Preserve the two-minute graceful-stop window so Minecraft can save before the
  container is killed.

The upstream image documents Fabric installation and Fabric API retrieval at
[Fabric — Minecraft Server on Docker](https://docker-minecraft-server.readthedocs.io/en/latest/types-and-platforms/server-types/fabric/).
Its [Modrinth downloader](https://docker-minecraft-server.readthedocs.io/en/latest/mods-and-plugins/modrinth/)
accepts `project:version`; omitting the version would select a moving newest
compatible release at each startup.

Validate the final configuration and launch it:

```bash
cd /opt/minecraft-golf
sudo docker compose config
sudo docker compose pull
sudo docker compose up -d
sudo docker compose ps
sudo docker compose logs --tail=200 minecraft
```

Do not accept a merely running container as success. Confirm:

- the container becomes healthy;
- the log reaches Minecraft's `Done` message;
- Minecraft Golf and Fabric API are both loaded without errors;
- the deployed Golf JAR SHA-256 matches the release artifact;
- a properly configured external client can authenticate and join;
- a course is available and playable: author one with the supported operator commands
  (`/golf course create …` → build → `/golf course finalize …` → `/golf course select …`),
  or — if intentionally provisioning the repository's bundled sample campus — confirm
  `/golf dev preparecourse` succeeds on the approved seed; and
- a real shot, scoring update, disconnect/reconnect, and restart preserve state.

## 8. Point a domain at the server

At the DNS provider, create an `A` record:

```text
Type: A
Name: play
Value: <reserved Compute Engine IPv4>
TTL: Auto or 300 during initial setup
```

If Cloudflare manages the zone, set **Proxy status: DNS only** (gray cloud). The
ordinary Cloudflare HTTP proxy does not proxy the Minecraft protocol. With the
default TCP port `25565`, players enter only:

```text
play.example.com
```

If a non-default port is required, players can enter `play.example.com:<port>`, or
the DNS zone can publish a non-proxied Minecraft SRV record:

```text
Service: _minecraft
Protocol: _tcp
Name: play
Priority: 0
Weight: 0
Port: <server-port>
Target: play.example.com
```

Verify both DNS and transport from outside the GCP project. A successful DNS lookup
does not prove that the VPC firewall, Docker mapping, or Minecraft process is ready.

## 9. Operations, security, and recovery

### Routine deployment

For every server update:

1. Verify the new release through the repository's full applicable verification
   ladder before production deployment.
2. Back up the world.
3. Stage the new JAR with its version in the filename rather than overwriting the
   only known-good copy.
4. Stop the service gracefully.
5. Update the Compose reference or staged artifact.
6. Recreate the service and inspect the complete startup log.
7. Compare the deployed and released SHA-256 values.
8. Join with the exact client release and perform a smoke test.
9. Keep the previous compatible JAR until rollback is no longer needed.

Do not update Minecraft, Fabric Loader, Fabric API, the base container, and
Minecraft Golf simultaneously unless that combination has already been tested
together.

### Backups

Persistent Disk is durable storage, but it is not a backup. Configure a snapshot
schedule with retention, and periodically test a restore to a separate disk/VM.
Google recommends keeping critical application data on a secondary disk and using
scheduled snapshots:

- [Snapshot schedule overview](https://docs.cloud.google.com/compute/docs/disks/about-snapshot-schedules)
- [Persistent Disk snapshot best practices](https://docs.cloud.google.com/compute/docs/disks/snapshot-best-practices)

A live disk snapshot is normally crash-consistent. For the cleanest Minecraft
backup, quiesce writes first: announce maintenance, run `save-all flush`, stop the
container cleanly, take the snapshot, and restart. If downtime is unacceptable,
use a tested Minecraft-aware backup process and prove restoration rather than
assuming that a snapshot is sufficient.

Use a retention pattern appropriate for a family server, for example daily
snapshots retained for 7–14 days plus a less frequent longer-lived backup. Keep at
least one recovery copy outside the VM's writable filesystem.

### Monitoring

At minimum, monitor:

- VM CPU and memory pressure;
- disk capacity and filesystem errors;
- container health/restart count;
- Minecraft startup, shutdown, and watchdog messages;
- tick lag during multiplayer play;
- backup completion and restore tests; and
- monthly cost and forecast alerts.

Set maintenance expectations. `restart: unless-stopped` recovers from a process or
host reboot, but it is not a substitute for alerting or tested restoration.

### Safe teardown

Before deleting or rebuilding anything:

1. stop Minecraft cleanly;
2. create and verify a final data-disk snapshot;
3. record the DNS value and deployment versions;
4. detach or preserve the data disk; and
5. only then delete the VM.

Deleting the VM, disk, address, snapshot, and DNS record are separate operations.
Resolve each exact resource before removal.

## Mod distribution

Minecraft Golf has common/server entrypoints and client presentation code, and its
`fabric.mod.json` declares `"environment": "*"`. Therefore the same Minecraft
Golf release JAR must be installed on **both the server and every player client**.
The client sends intent while the server remains authoritative for balls, shots,
round state, and scores.

### Release compatibility contract

Every published release should clearly state:

```text
Minecraft Java Edition: 26.2
Mod loader: Fabric
Fabric Loader: 0.19.5 or newer compatible version
Fabric API: required; tested with 0.160.0+26.2
Java: 25 (normally supplied by the official launcher)
Install side: client and server
Server and client Minecraft Golf version: exact match required
```

Do not label the mod as server-only. Its HUD, input, camera, models, textures, and
client networking require the client JAR.

The exact-version rule is the supported-*play* rule, not a connection gate. Since the
optional-registry connection fix, a client **without** the mod (vanilla Java, or Bedrock via
Geyser) can still connect: it is treated as a client-light **visitor** that may watch or
explore but cannot golf. A **stale** modded client is the real hazard — it may negotiate the
same payload channels at a different build and misbehave. Immutable releases, published
checksums, and an exact-match requirement for anyone who wants to play close that gap.

### Build and verify a release artifact

Before publishing:

1. Choose a unique semantic version in `gradle.properties`; never replace the bytes
   behind an already published version.
2. Update release notes and the compatibility matrix.
3. Run the full verification ladder applicable to the milestone:

   ```bash
   ./gradlew test
   ./gradlew clean build
   ./gradlew runClient
   ./gradlew runServer
   ./scripts/dev-server-sync.sh
   ./scripts/dev-server-up.sh
   ```

4. Complete the required manual multiplayer checks.
5. Select `build/libs/minecraft-golf-<version>.jar`, not the sources JAR.
6. Calculate and publish its checksum:

   ```bash
   sha256sum build/libs/minecraft-golf-<version>.jar
   ```

7. Install that exact artifact on a clean client and a clean dedicated server and
   prove they can join and play.
8. Tag the source commit corresponding to the artifact.

The JAR already embeds this project's MIT license. Do not redistribute Mojang's
Minecraft server/client JARs. Link players to the official Minecraft launcher,
Fabric installer, and Fabric API distribution instead.

### Recommended distribution stages

#### Stage 1: private family testing

Publish a versioned GitHub Release from a verified commit and attach:

- `minecraft-golf-<version>.jar`;
- the SHA-256 checksum;
- the exact compatibility contract;
- concise installation and update steps;
- known issues; and
- the server hostname shared only with the intended testers.

Do not use an expiring CI artifact as the canonical download and do not send
unversioned files named only `minecraft-golf.jar`. A GitHub Release gives every
tester one stable artifact and makes rollback possible.

For a very small group, an exported launcher profile or modpack can simplify setup,
but the canonical Golf JAR and version matrix should still exist independently.
Respect Fabric API's redistribution terms or declare it as a platform dependency
rather than copying arbitrary third-party files into an informal bundle.

#### Stage 2: public distribution

Publish Minecraft Golf on Modrinth as the primary end-user channel. For each
version:

- upload the verified non-sources JAR;
- select project type **mod**;
- select loader **Fabric**;
- select game version **26.2**;
- mark the environment as required on both client and server;
- declare **Fabric API** as a required dependency;
- provide the source, issue tracker, license, and release notes; and
- keep the displayed version identical to `fabric.mod.json` and the Git tag.

Modrinth's launcher can then resolve compatible project versions and required
dependencies, reducing manual installation mistakes. Its publishing/API model
supports required project dependencies such as Fabric API; see the
[Modrinth API documentation](https://docs.modrinth.com/api/).

CurseForge can be added as a second channel if its audience is valuable. Mark the
file as Fabric/26.2 and Fabric API as a required related project so its client can
install the dependency. See CurseForge's
[project submission guide](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project).

Avoid publishing different bytes under the same version across channels. Compare
checksums after every upload.

### Player installation: official Minecraft Launcher

Give players these steps:

1. Install and launch Minecraft Java Edition `26.2` once using the official
   launcher, then close the game and launcher.
2. Download the official Fabric Installer from
   [fabricmc.net](https://fabricmc.net/use/installer/).
3. In the installer, select Minecraft `26.2`, keep **Create Profile** enabled, and
   install the Fabric client profile.
4. Download the release's compatible Fabric API JAR from its official Modrinth or
   CurseForge project.
5. Download the Minecraft Golf release JAR from the canonical release page.
6. Put both JARs in the profile's `mods` directory:

   | OS | Default directory |
   |---|---|
   | Windows | `%appdata%\.minecraft\mods` |
   | macOS | `~/Library/Application Support/minecraft/mods` |
   | Linux | `~/.minecraft/mods` |

7. Remove older Minecraft Golf JARs from that same profile; Fabric must not see two
   versions of the same mod ID.
8. Select the new Fabric `26.2` profile in the launcher and start the game.
9. Confirm the title screen/profile reports Fabric and check `latest.log` if launch
   fails.
10. Add the multiplayer server as `play.example.com` and connect with an
    authenticated Minecraft account.

Fabric's maintained player documentation covers
[installing Fabric on Windows](https://docs.fabricmc.net/players/installing-fabric/windows)
and [installing mods on Fabric](https://docs.fabricmc.net/players/installing-mods),
including the platform-specific `mods` paths.

Third-party launchers such as Modrinth App or Prism Launcher should use a dedicated
instance pinned to Minecraft `26.2` and Fabric. A dedicated instance prevents mods
for other Minecraft versions from leaking into the Golf profile.

### Player update procedure

When the server is upgraded:

1. announce the required Minecraft Golf and dependency versions;
2. provide one canonical download link and checksum;
3. stop the server and back it up;
4. update and verify the server JAR;
5. have every player remove the old Golf JAR and install the new one; and
6. reopen the server only after a clean matching client has passed a join and shot
   smoke test.

The server and clients should use the exact same Minecraft Golf version. Fabric
Loader validates declared dependencies, but it cannot guarantee that two builds
published with the same version contain identical bytes; immutable releases and
checksums close that gap.

### Troubleshooting checklist for players

| Symptom | Likely check |
|---|---|
| Client joins but cannot use Golf (visitor welcome/action bar) | The Minecraft Golf client JAR is missing or mismatched; install the matching release to play |
| Fabric reports a missing dependency | Install the compatible Fabric API release |
| Incompatible game/version message | Confirm Minecraft is exactly `26.2` for this release |
| Duplicate mod ID or launch failure | Remove older/duplicate Minecraft Golf JARs |
| Server rejects the connection | Compare Minecraft, Fabric Loader, Fabric API, and Golf versions with the release matrix |
| A Bedrock or unmodified-Java client sees the visitor welcome | Expected: that client is client-light and can only watch; golf requires a matching modded Java client |
| `Invalid session` | Restart the authenticated official launcher/client; do not disable server online mode |
| DNS name does not resolve | Check the DNS `A` record and propagation |
| Connection times out | Check GCP firewall, VM state, Docker port mapping, container health, and server logs |

When requesting support, ask the player for the exact release versions and the
relevant `latest.log`; do not ask them to paste account tokens or other secrets.

## Go-live checklist

- [ ] Compute Engine chosen; Cloud Run is not used for the game listener.
- [ ] Region is near the intended players.
- [ ] VM has adequate headroom; free-tier `e2-micro` is not used.
- [ ] Static external IP is reserved and attached.
- [ ] Only TCP `25565` is publicly allowed for the game.
- [ ] Administrative SSH follows OS Login/IAP or a restricted source rule.
- [ ] World data is on a separately mounted Persistent Disk.
- [ ] Docker Engine and Compose came from supported repositories.
- [ ] Container image, Minecraft, Fabric Loader, Fabric API, and Golf versions are recorded.
- [ ] `online-mode=true` and the whitelist is enabled.
- [ ] RCON and Docker are not exposed publicly.
- [ ] Container health, startup logs, and JAR checksum are verified.
- [ ] DNS is configured as DNS-only and resolves to the reserved IP.
- [ ] A real external client can join and play.
- [ ] Snapshot/backup schedule exists and a restore has been tested.
- [ ] Billing budget and monitoring alerts are configured.
- [ ] Canonical client release, checksum, compatibility matrix, and installation instructions are published.
- [ ] Server and clean client use the identical Minecraft Golf release.

## Source-of-truth links

Platform behavior and commands change. Before a new deployment, compare this guide
with the current primary documentation:

- [Compute Engine documentation](https://docs.cloud.google.com/compute/docs)
- [Static external IP configuration](https://docs.cloud.google.com/compute/docs/ip-addresses/configure-static-external-ip-address)
- [Persistent Disk formatting and mounting](https://docs.cloud.google.com/compute/docs/disks/format-mount-disk-linux)
- [Persistent Disk snapshot best practices](https://docs.cloud.google.com/compute/docs/disks/snapshot-best-practices)
- [Cloud Run runtime contract](https://docs.cloud.google.com/run/docs/container-contract)
- [Docker Engine installation](https://docs.docker.com/engine/install/ubuntu/)
- [itzg Fabric server configuration](https://docker-minecraft-server.readthedocs.io/en/latest/types-and-platforms/server-types/fabric/)
- [Fabric player documentation](https://docs.fabricmc.net/players/)
- [Modrinth documentation](https://docs.modrinth.com/)
