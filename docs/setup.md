# CollabFlow — Local machine setup

Install tools on **Windows** or **Linux (Fedora)**.  
After this: **[running.md](running.md)** (Compose + start every service) → **[e2e-demo.md](e2e-demo.md)**.

**Pinned versions:** [tech-stack.md](tech-stack.md)

---

## Mental model

| Install on your PC | Run in Docker (do **not** install natively) |
|--------------------|-----------------------------------------------|
| Git | PostgreSQL 17 |
| JDK **21** | Redis 8 |
| Go **1.27.x** | ZooKeeper + Kafka (Confluent 7.9) |
| Docker Desktop / Engine + Compose | MailHog (SMTP catcher) |
| IDE (Cursor, IntelliJ, …) | — |

**Gradle:** use the **Wrapper** (`gradlew` / `gradlew.bat`) — no global Gradle.  
**Why Docker for data plane?** Same versions on Windows and Fedora; matches contributor infra.

---

## 1. Install checklist

### Required

- [ ] Git  
- [ ] Docker + Compose v2  
- [ ] JDK 21 (Temurin recommended)  
- [ ] Go 1.27.x  
- [ ] RAM: ideally **16 GB** (8 GB min)  
- [ ] Disk: ~10–20 GB free  

### Strongly recommended

- [ ] Cursor / IntelliJ / VS Code  
- [ ] curl  
- [ ] Windows: virtualization + WSL2 for Docker  

### Do **not** install on the host

PostgreSQL, Redis, Kafka/ZooKeeper, MailHog, global Maven/Gradle.

---

## 2. Windows

### 2.1 Git

https://git-scm.com/download/win → prefer “Git from the command line”.

```powershell
git --version
```

### 2.2 Docker Desktop

https://www.docker.com/products/docker-desktop/ (WSL2 backend). Start Desktop, wait until running.

```powershell
docker version
docker compose version
```

Give Docker ~4–6 GB RAM in Settings → Resources. If `docker` fails: `wsl --install`, reboot.

### 2.3 JDK 21

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
# or https://adoptium.net/ → Temurin 21 JDK
java -version   # 21.x
```

Set `JAVA_HOME` to the real Adoptium folder (not a placeholder):

```powershell
Get-ChildItem "C:\Program Files\Eclipse Adoptium"
[System.Environment]::SetEnvironmentVariable(
  "JAVA_HOME",
  "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot",  # your folder
  "User"
)
```

New terminal → `echo $env:JAVA_HOME` → `Test-Path $env:JAVA_HOME`.

### 2.4 Go 1.27.x

https://go.dev/dl/ (1.27.x MSI) or `winget install GoLang.Go` if version matches.

```powershell
go version   # go1.27.x windows/amd64
```

PATH must include `...\Go\bin`. `GOPATH` must **not** equal `GOROOT`.  
If Windows Application Control blocks `go.exe`, see [§5.1](#51-windows-application-control-blocks-goexe).

### 2.5 Clone

```powershell
cd D:\codes
git clone <your-remote-url> CollabFlow_V2
cd CollabFlow_V2
```

---

## 3. Linux / Fedora

### 3.1 Basics

```bash
sudo dnf update -y
sudo dnf install -y git curl wget
```

### 3.2 Docker Engine + Compose

```bash
sudo dnf -y install dnf-plugins-core
sudo dnf config-manager addrepo --from-repofile=https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
# log out / reboot, then:
docker version && docker compose version
```

Prefer Docker CE over Podman for this repo’s Compose file.

### 3.3 JDK 21

```bash
sudo dnf install -y java-21-openjdk-devel
java -version && javac -version
echo 'export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))' >> ~/.bashrc
source ~/.bashrc
```

### 3.4 Go 1.27.x

Prefer official tarball (distro packages lag):

```bash
curl -LO https://go.dev/dl/go1.27.3.linux-amd64.tar.gz   # match go.dev/dl
sudo rm -rf /usr/local/go
sudo tar -C /usr/local -xzf go1.27.3.linux-amd64.tar.gz
echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
source ~/.bashrc
go version
```

### 3.5 Clone

```bash
git clone <your-remote-url> CollabFlow_V2
cd CollabFlow_V2
```

---

## 4. Verify tools

```bash
git --version
java -version          # 21.x
go version             # 1.27.x
docker version
docker compose version
docker run --rm hello-world
```

Then continue with **[running.md](running.md)** (`.env` → Compose → bootRun / `go run`).

---

## 5. Troubleshooting (install)

| Problem | Fix |
|---------|-----|
| Docker engine not running | Start Docker Desktop; wait for steady whale icon |
| Windows: WSL2 missing | `wsl --install`, reboot |
| `java` not found | Reopen terminal; check `JAVA_HOME` + PATH |
| `go` not 1.27 | Install from go.dev/dl, not an old distro package |
| Windows: Application Control blocks `go.exe` | [§5.1](#51-windows-application-control-blocks-goexe) |
| Wrong `GOPATH` | `GOPATH` ≠ `GOROOT`; prefer `%USERPROFILE%\go` or leave unset |
| Fedora: docker permission denied | User in `docker` group + re-login |

Runtime / ports / Kafka / secrets → [running.md](running.md#8-run-troubleshooting).

### 5.1 Windows: Application Control blocks `go.exe`

```text
Program 'go.exe' failed to run: An Application Control policy has blocked this file
```

1. PATH includes `...\Go\bin`.  
2. `GOROOT` = install root; `GOPATH` ≠ `GOROOT`.  
3. Windows 11 Smart App Control → Off (personal PC; reboot).  
4. Reinstall official **MSI** into `C:\Program Files\Go`.  
5. `Unblock-File` on `go.exe` if it came from a zip.  
6. Work/school Device Guard → use **WSL2** for Go:

```bash
curl -fsSL -o /tmp/go.tgz https://go.dev/dl/go1.27.3.linux-amd64.tar.gz
rm -rf "$HOME/.local/go" && mkdir -p "$HOME/.local"
tar -C "$HOME/.local" -xzf /tmp/go.tgz
echo 'export PATH=$HOME/.local/go/bin:$PATH' >> ~/.bashrc && source ~/.bashrc
go version
```

Repo on Windows: `/mnt/d/codes/CollabFlow_V2`. Keep Docker Desktop on Windows; enable WSL integration.

---

## Contributor path

1. This file → verify §4  
2. [running.md](running.md) → `.env`, Compose, services  
3. [e2e-demo.md](e2e-demo.md) / `postman/`  
4. [AGENTS.md](../AGENTS.md) if using Cursor  

Phase 2: [phase-2.md](phase-2.md) · [future-plan.md](future-plan.md).

---

**Last updated:** 2026-10-23
