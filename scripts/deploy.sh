#!/bin/bash
# Läuft auf bums, nachdem scripts/deploy.ps1 das Paket nach /tmp/ea-overlay-player-deploy gelegt hat.
set -euo pipefail
src=/tmp/ea-overlay-player-deploy
target=/srv/overlay-player
tailscale_url=https://bums.tail1c1957.ts.net:5241

if ! id overlay-player >/dev/null 2>&1; then
    useradd --system --home "$target" --shell /usr/sbin/nologin overlay-player
fi

# Beim ersten Mal: Ports unmittelbar vorher nachsehen (Leitfaden, Abschnitt 5).
if [ ! -f /etc/systemd/system/overlay-player-updates.service ]; then
    if ss -tln | grep -qE ':(5240|5241)\b'; then
        echo "Port 5240 oder 5241 ist belegt, Abbruch" >&2
        exit 1
    fi
fi

install -d -o root -g overlay-player -m 750 "$target" "$target/server"
install -d -o root -g overlay-player -m 755 "$target/releases"
install -d -o root -g overlay-player -m 750 "$target/dateien"
python3 "$src/scripts/publish-release.py" "$src/artifacts" "$target/releases"
chgrp overlay-player "$target/releases/"*.apk "$target/releases/latest.json"
install -o root -g overlay-player -m 640 "$src/server/update_server.py" "$target/server/update_server.py"
install -o root -g overlay-player -m 640 "$src/docs/DEPLOYMENT.md" "$target/DOKU.md"

cat > /etc/systemd/system/overlay-player-updates.service <<'EOF'
[Unit]
Description=Overlay-Player: App-Updates (nur lesen, Loopback, HTTPS über Tailscale)
Documentation=file:/srv/overlay-player/DOKU.md
After=network-online.target
Wants=network-online.target

[Service]
User=overlay-player
Group=overlay-player
WorkingDirectory=/srv/overlay-player/server
Environment=PORT=5240
Environment=RELEASE_DIR=/srv/overlay-player/releases
Environment=PYTHONDONTWRITEBYTECODE=1
ExecStart=/usr/bin/python3 /srv/overlay-player/server/update_server.py
Restart=on-failure
RestartSec=3
NoNewPrivileges=true
CapabilityBoundingSet=
AmbientCapabilities=
RestrictSUIDSGID=true
UMask=0077
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
PrivateDevices=true
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
RestrictAddressFamilies=AF_INET AF_INET6
LockPersonality=true

[Install]
WantedBy=multi-user.target
EOF

python3 -m py_compile "$target/server/update_server.py"
rm -rf "$target/server/__pycache__"
systemctl daemon-reload
systemctl enable overlay-player-updates.service
systemctl restart overlay-player-updates.service

# Eigener HTTPS-Port im Tailnet, kein Funnel; andere Serve-Zuordnungen bleiben unberührt.
tailscale serve --bg --https=5241 http://127.0.0.1:5240
printf '%s\n' "{\"name\":\"Overlay-Player\",\"service\":\"overlay-player-updates\",\"url\":\"$tailscale_url/\",\"desc\":\"Android-App: Updatebereich\"}" \
    > /etc/bums-panel/apps.d/overlay-player.json

printf '%s\n' 'releases/' '__pycache__/' > "$target/.gitignore"
git -C "$target" init -q
git -C "$target" add .gitignore server/update_server.py DOKU.md
git -C "$target" -c user.name=OverlayPlayer -c user.email=overlay-player@localhost commit -qm 'Deploy Overlay-Player Update-Dienst' \
    || test -z "$(git -C "$target" status --porcelain)"

systemctl is-active overlay-player-updates.service
curl --fail --silent --show-error --retry 10 --retry-connrefused --retry-delay 1 --max-time 30 http://127.0.0.1:5240/health
