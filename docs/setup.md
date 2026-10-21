# CollabFlow — Local Development Setup

Guide for **Windows** and **Linux (Fedora)** so you (and contributors) can run and develop CollabFlow on any machine.

**Pinned versions:** [tech-stack.md](tech-stack.md)  
**This doc will grow** once `infrastructure/docker-compose.yml` and services exist. For now it covers **what to install before Week 1 coding**.

---

## Mental model (important)

| Runs on your PC (install) | Runs in Docker (do **not** install natively for MVP) |
|---------------------------|------------------------------------------------------|
| Git | PostgreSQL 17 |
| JDK **21** | Redis 8 |
| Go **1.27.x** | ZooKeeper + Kafka (Confluent 7.9) |
| Docker Desktop / Docker Engine + Compose | — |
| IDE / editor (Cursor, IntelliJ, VS Code, …) | — |
| curl / HTTP client (optional) | — |

**Gradle:** do **not** install a global Gradle if you can avoid it. Services will ship a **Gradle Wrapper** (`./gradlew`). You only need the JDK.

**Why Docker for Postgres/Redis/Kafka?** Same versions on Windows and Fedora; no “works on my machine” DB installs; matches how contributors run infra.

---

## 1. Install checklist (do this first)

### Required now (before any coding)

- [ ] **Git**
- [ ] **Docker** + Compose v2 (able to run containers)
- [ ] **JDK 21** (Temurin recommended)
- [ ] **Go 1.27.x** (needed from Week 9; install now so both machines match)
- [ ] **RAM:** ideally **16 GB** (8 GB minimum; Kafka + ZK + Postgres + Redis + IDEs are heavy)
- [ ] **Disk:** ~10–20 GB free for images + builds

### Strongly recommended

- [ ] Cursor / IntelliJ IDEA / VS Code
- [ ] curl (Windows 10+ has it; Fedora: `curl`)
- [ ] Make sure virtualization is enabled (BIOS) for Docker on Windows

### Not required natively

- PostgreSQL installer  
- Redis installer  
- Kafka / ZooKeeper installer  
- Global Maven or Gradle  

---

## 2. Windows setup

### 2.1 Git

1. Install from https://git-scm.com/download/win  
2. During setup, prefer **Git from the command line** and **LF** or default line endings (team can standardize later).  
3. Verify:

```powershell
git --version
```

### 2.2 Docker Desktop

1. Install **Docker Desktop for Windows**: https://www.docker.com/products/docker-desktop/  
2. Requirements: WSL2 backend (Docker installer guides you).  
3. Start Docker Desktop; wait until it says running.  
4. Verify:

```powershell
docker version
docker compose version
```

Both should succeed without errors.

**Tips**

- Give Docker enough RAM in Settings → Resources (e.g. 4–6 GB).  
- If `docker` fails: ensure WSL2 is installed (`wsl --install`) and reboot.

### 2.3 JDK 21

**Option A — winget (easy):**

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

**Option B — manual:** https://adoptium.net/ (Temurin 21 → Windows x64 → JDK)

Verify (new terminal):

```powershell
java -version
# should show 21.x
```

Set `JAVA_HOME` if tools don’t find Java:

```powershell
# Example path — adjust to your install
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot", "User")
```

Restart the terminal after setting `JAVA_HOME`.

### 2.4 Go 1.27.x

1. Download Windows installer from https://go.dev/dl/ (pick **1.27.x**).  
2. Or winget (if the package version is 1.27.x):

```powershell
winget search GoLang.Go
winget install GoLang.Go
```

Verify:

```powershell
go version
# go version go1.27.x windows/amd64
```

Optional (GOPATH modules are default):

```powershell
go env GOPATH
```

### 2.5 Clone the repo

```powershell
cd D:\Codes   # or wherever you keep projects
git clone <your-remote-url> CollabFlow_V2
cd CollabFlow_V2
```

(Until a remote exists, just work in your existing local folder.)

---

## 3. Linux / Fedora setup

### 3.1 Git + basics

```bash
sudo dnf update -y
sudo dnf install -y git curl wget
git --version
```

### 3.2 Docker Engine + Compose (Fedora)

Official approach (summary — follow current Docker docs if UI changes):

```bash
# Remove old/podman conflicts if you hit issues (optional)
# sudo dnf remove docker docker-client docker-client-latest docker-common ...

sudo dnf -y install dnf-plugins-core
sudo dnf config-manager addrepo --from-repofile=https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

**Log out and back in** (or reboot) so the `docker` group applies.

Verify:

```bash
docker version
docker compose version
```

**Note:** Fedora often ships **Podman**. This project targets **Docker Compose** for contributor consistency. Prefer Docker CE as above, or document a Podman-compatible compose later if the team wants it.

### 3.3 JDK 21 (Fedora)

```bash
sudo dnf install -y java-21-openjdk-devel
java -version
javac -version
```

Optional Temurin via Adoptium packages if you prefer the same binary as Windows.

Set `JAVA_HOME` (path may vary):

```bash
echo 'export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))' >> ~/.bashrc
source ~/.bashrc
echo $JAVA_HOME
```

### 3.4 Go 1.27.x (Fedora)

Fedora repos may lag. Prefer official tarball for the **pinned** version:

```bash
# Example — replace version/arch to match https://go.dev/dl/
curl -LO https://go.dev/dl/go1.27.3.linux-amd64.tar.gz
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

## 4. Verify everything (both OS)

Run these before saying “I’m ready to code”:

```bash
git --version
java -version          # 21.x
go version             # 1.27.x
docker version
docker compose version
```

Quick Docker smoke (pulls a tiny image):

```bash
docker run --rm hello-world
```

---

## 5. Run local infrastructure (ready)

```bash
# from repo root
copy .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
cd infrastructure
docker compose --env-file ../.env up -d
docker compose --env-file ../.env ps
```

Details and smoke tests: [infrastructure/README.md](../infrastructure/README.md).

```bash
# Java (Week 2+ — from a service folder)
./gradlew bootRun          # Linux/macOS
gradlew.bat bootRun        # Windows

# Go (Week 9+)
go run ./cmd/server
```

Default local ports (planned):

| Port | Service |
|------|---------|
| 5432 | PostgreSQL |
| 6379 | Redis |
| 2181 | ZooKeeper |
| 9092 | Kafka |
| 8080 | api-gateway |
| 8081 | auth-service |
| 8082 | platform-service |
| 8083 | task-service |
| 9081 | collab-service |
| 9082 | realtime-service |

---

## 6. Contributor quick start (when code exists)

1. Read [setup.md](setup.md) (this file) + [tech-stack.md](tech-stack.md)  
2. Install required tools (section 1)  
3. Clone repo  
4. `docker compose up -d` in `infrastructure/`  
5. Follow [implementation-guide.md](implementation-guide.md) / tick [progress-tracker.md](progress-tracker.md)  
6. Open [AGENTS.md](../AGENTS.md) if using Cursor  

---

## 7. Do this **right now** (Kaung — initial start)

Before we write Week 1 Compose / first Java service:

1. **Windows machine you’re on:** install/verify Docker Desktop, JDK 21, Go 1.27, Git.  
2. Run the verify commands in section 4; fix anything that fails.  
3. On **Fedora** (if you use it): same four tools via section 3.  
4. You do **not** need to install Postgres/Redis/Kafka on the host.  
5. Tell the agent when Docker + Java 21 + Go 1.27 work — then we start **Week 1** (`infrastructure/docker-compose.yml`).

---

## 8. Troubleshooting (common)

| Problem | Fix |
|---------|-----|
| Docker: “engine not running” / `dockerDesktopLinuxEngine` pipe missing | **Start Docker Desktop** from the Start menu; wait until the whale icon is steady; then retry `docker version` |
| Windows: WSL2 missing | `wsl --install`, reboot, reopen Docker |
| `java` not found | Reopen terminal; check `JAVA_HOME` and PATH |
| `go` not 1.27 | Install from go.dev/dl, not an old distro package |
| Windows: `Application Control policy has blocked` `go.exe` | See [§8.1](#81-windows-application-control-blocks-goexe) below |
| Wrong `GOPATH` | `GOPATH` must **not** be the same folder as `GOROOT`. Use e.g. `%USERPROFILE%\go` for GOPATH (modules workspace/cache), `GOROOT` = install root only. Prefer PATH entry `...\Go\bin` |
| Port 5432/6379 already in use | Stop local Postgres/Redis or change compose ports later |
| Fedora permission denied on docker | User in `docker` group + re-login |

### 8.1 Windows: Application Control blocks `go.exe`

Error looks like:

```text
Program 'go.exe' failed to run: An Application Control policy has blocked this file
```

Your Go files are present; Windows **policy** is refusing to execute them (Smart App Control, WDAC, or AppLocker — common on Windows 11 / managed PCs).

**Try in order:**

1. **PATH** — must include the **`bin`** folder, e.g. `C:\Users\TAT\Go\bin` (not only `C:\Users\TAT\Go`).
2. **Fix env (recommended):**
   - `GOROOT` = `C:\Users\<you>\Go` (install root — has `bin`, `src`, `VERSION`)
   - `GOPATH` = `C:\Users\<you>\go` **or leave unset** (do **not** set GOPATH = GOROOT)
3. **Smart App Control** (Windows 11): Settings → Privacy & security → Windows Security → App & browser control → Smart App Control → set to **Off** if it is On/Evaluation (requires reboot). Only do this if you understand the security tradeoff on a personal PC. **Confirmed:** turning SAC Off unblocks official Go MSI installs on personal Windows 11.
4. **Reinstall Go** with the official **`.msi`** from https://go.dev/dl/ into `C:\Program Files\Go` (often trusted more than a zip unzipped under the user profile).
5. **Unblock the file** (sometimes helps for zip downloads):

```powershell
Unblock-File -Path "C:\Users\TAT\Go\bin\go.exe"
Get-ChildItem "C:\Users\TAT\Go\bin\*" | Unblock-File
```

6. If this is a **work/school PC**, Application Control / **Device Guard** may block **all** Windows `go.exe` (including `C:\Program Files\Go`). You cannot fix that without IT. Use **WSL2** (below), a VM, or another PC.

**WSL2 workaround (recommended when Device Guard blocks Go on Windows):**

You already have Ubuntu-24.04. Open **Ubuntu** from the Start menu (or `wsl`), then:

```bash
# Install Go 1.27.x into your home (no sudo)
curl -fsSL -o /tmp/go.tgz https://go.dev/dl/go1.27.3.linux-amd64.tar.gz
rm -rf "$HOME/.local/go"
mkdir -p "$HOME/.local"
tar -C "$HOME/.local" -xzf /tmp/go.tgz
echo 'export PATH=$HOME/.local/go/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
go version   # expect: go1.27.3 linux/amd64
```

Repo on Windows is available in WSL as `/mnt/d/Codes/CollabFlow_V2`.  
Keep Docker Desktop running on Windows; enable WSL integration for Ubuntu if needed.  
Java can stay on Windows for now; use WSL for **Go** services later.

---

**Last updated:** 2026-10-21 · Update this file when Compose and services land.
