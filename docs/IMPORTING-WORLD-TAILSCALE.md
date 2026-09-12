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
mini-PC, copy the archive to the laptop:

```powershell
scp "$env:USERPROFILE\Desktop\course-world.tar.gz" prill@coderlt-1:/tmp/
```

If SSH is not enabled on the laptop, use Tailscale's built-in file transfer from
the laptop instead. The received file appears in the Tailscale file inbox.

## Replace the Docker World

Run these commands from the repository root on the laptop. The first command
backs up the current Docker world. The replacement changes only `/data/world`;
the named volume, server configuration, and mod installation remain intact.

```bash
docker run --rm -v minecraft-golf-data:/data -v /tmp:/transfer alpine \
  sh -c 'tar -czf /transfer/minecraft-golf-world-backup.tar.gz -C /data/world .'
./scripts/dev-server-down.sh
docker run --rm -v minecraft-golf-data:/data -v /tmp:/transfer alpine \
  sh -c 'rm -rf /data/world && mkdir -p /data/world && tar -xzf /transfer/course-world.tar.gz -C /data/world && rm -f /data/world/session.lock'
./scripts/dev-server-up.sh
```

## Verify Before Authoring

1. Confirm the Docker container becomes healthy and the imported terrain is present.
2. Do not run `/golf dev preparecourse`; that command is for the M5 regression
   layout and can mutate its bounded preparation area.
3. Use `/golf course create ...` and the authoring commands to define the course.
4. Keep the backup until the imported world and course metadata have been tested.

## Alternative: Tailscale File Transfer

From the laptop, a local guide or archive can be sent to the mini-PC with:

```bash
tailscale file cp <file> minipc-zjo6i:
```

On Windows, the Tailscale file transfer receiver places incoming files in its
file inbox, normally the user's Downloads folder. Move the file to the desired
working location if needed.
