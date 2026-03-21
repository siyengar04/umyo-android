# umyo-android

Android app for uMyo — BLE bridge and OTA firmware updater.

**Download:** [v0.1.0 release](https://github.com/ultimaterobotics/umyo-android/releases/tag/v0.1.0)
**Docs:** https://make.udevices.io
**Discord:** https://discord.com/invite/dEmCPBzv9G

## What this does

- Connects to uMyo devices over BLE (GATT)
- Forwards EMG + IMU + magnetometer data over UDP to the PC workbench
- Supports up to 3 simultaneous devices
- OTA firmware updates over BLE (requires OTA-capable bootloader on device)
- Filters BLE scan to uMyo devices only

## Install

Download the APK from [Releases](https://github.com/ultimaterobotics/umyo-android/releases) and install it. Enable "Install from unknown sources" in developer options if prompted.

Requires Android 8.0+. Android 11+ requires Location permission and Location toggle ON for BLE scan.

## Usage

1. Open the app, enter your PC's IP address and port (default: `26750`)
2. Power on uMyo — press button until LED is solid blue (BLE mode)
3. Tap **Start GATT Raw** — devices connect automatically
4. On PC, run the [uf1-tools workbench](https://github.com/ultimaterobotics/uf1-tools)

For OTA firmware update, see the [OTA guide](https://make.udevices.io/guides/whats-new-march-2026#how-to-update-firmware-via-ota).

## Requirements

- Android phone with BLE support
- PC and phone on the same WiFi network
- uMyo with fw-ble or fw-ble-ota firmware

## Related

- [uf1-tools](https://github.com/ultimaterobotics/uf1-tools) — PC workbench
- [uMyo firmware](https://github.com/ultimaterobotics/uMyo)
- [umyo-bootloader](https://github.com/ultimaterobotics/umyo-bootloader)
