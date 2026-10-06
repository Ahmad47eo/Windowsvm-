# Linux Desktop / VNC

This repository now includes a phone-friendly Linux desktop package based on **Ubuntu + XFCE + TigerVNC**.

## Important

GitHub Actions runners are temporary CI machines. The Linux workflow builds a reproducible setup package; it does **not** expose the GitHub-hosted runner as a permanent VNC server.

For actual phone access, install the generated package on a Linux VM/server that you control.

## Build the package

1. Open **Actions**.
2. Select **Linux Desktop Build**.
3. Choose **Run workflow**.
4. After it finishes, download the **linux-xfce-vnc** artifact.

## On your Linux VM

Extract the artifact and run:

    sudo bash setup-xfce-vnc.sh

Set the desktop user's VNC password:

    sudo -u desktop vncpasswd

Start the phone-friendly 1280x720 desktop:

    sudo -u desktop vncserver :1 -localhost no -geometry 1280x720 -depth 24

The VNC display is **:1**, normally port **5901**.

## Phone

Install a VNC client on your phone and connect to the address of your Linux VM on port 5901.

For security, prefer a private network/VPN or an SSH tunnel rather than exposing VNC directly to the public internet. Never store VNC passwords, SSH private keys, or other secrets in this repository.

## Existing Windows workflow

The original Windows Actions workflow remains available as **Windows Test** for repository CI/testing.
