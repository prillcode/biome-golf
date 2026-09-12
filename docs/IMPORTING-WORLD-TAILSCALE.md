# Importing a World from Another Tailscale Host

Use this guide to bring a course built in a normal Java Edition instance on the
mini-PC into the laptop's Minecraft Golf Docker server.

## Before Starting

- Stop the source Java Edition instance completely so all world data is flushed.
- Confirm the source and destination Minecraft versions match.
- Do not run `dev-server-reset.sh`; it deletes the destination Docker world and
  its volume.
- Keep the source world and archive until the import has been accepted.

## Archive the Source World

On the Windows mini-PC, replace `<world-folder>` with the folder name under
`%APPDATA%\.minecraft\saves`:

```powershell
$world = "$env:APPDATA\.minecraft\saves\<world-folder>"
tar -czf "$env:USERPROFILE\Desktop\course-world.tar.gz" -C $world .
```

## Transfer the Archive

The laptop's Tailscale node is currently `coderlt-1`. From PowerShell on the
Windows mini-PC, use Tailscale file transfer to send the archive to the laptop:

```text
tailscale file cp "$env:USERPROFILE\Desktop\course-world.tar.gz" coderlt-1:
```

On Linux, the received file appears in the Tailscale file inbox and is available
from the user's Downloads folder. In this workflow the archive is:

```text
/home/prill/Downloads/course-world.tar.gz
```

SSH is an alternative if Tailscale file transfer is unavailable:

```powershell
scp "$env:USERPROFILE\Desktop\course-world.tar.gz" prill@coderlt-1:/tmp/
```

## Replace the Docker World

Run these commands from the repository root on the laptop. Stop the server before
making the backup so the world is fully flushed. The replacement changes only
`/data/world`; the named volume, server configuration, and mod installation remain
intact.

```bash
GOLF_VOLUME=dev-server_minecraft-golf-data
./scripts/dev-server-down.sh
docker run --rm -v "$GOLF_VOLUME:/data" -v "$HOME/Downloads:/transfer" alpine \
  sh -c 'tar -czf /transfer/minecraft-golf-world-backup.tar.gz -C /data/world .'
docker run --rm -v "$GOLF_VOLUME:/data" -v "$HOME/Downloads:/transfer" alpine \
  sh -c 'rm -rf /data/world && mkdir -p /data/world && tar -xzf /transfer/course-world.tar.gz -C /data/world && rm -f /data/world/session.lock'
./scripts/dev-server-up.sh
```

The `dev-server_` prefix comes from Docker Compose's project name. Confirm the
volume name with `docker volume ls` if the server was started using a different
Compose project name.

## Verify Before Authoring

1. Confirm the Docker container becomes healthy and the imported terrain is present.
2. Do not run `/golf dev preparecourse`; that command is for the M5 regression
   layout and can mutate its bounded preparation area.
3. Use `/golf course create ...` and the authoring commands to define the course.
4. Keep the backup until the imported world and course metadata have been tested.
