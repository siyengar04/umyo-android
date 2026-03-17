# umyo-android

Android BLE bridge app for uMyo.
Two main functions: live data streaming and OTA firmware update.
Current release: v0.1.0.

## Live data path

uMyo → BLE GATT → this app → UF1 over UDP → Python workbench on PC

User flow:
1. Enter PC IP address and port 26750 in app
2. Start GATT Raw
3. Run `uf1_workbench.py` on PC
4. App forwards UF1 frames over UDP to the workbench

## OTA update path

uMyo bootloader (BLE mode) → this app → BLE OTA upload

User flow:
1. Put device in bootloader BLE mode (power off → hold button → short press — see bootloader/CLAUDE.md)
2. Open OTA mode in app
3. Scan for bootloader (advertises as `uECG boot`)
4. Start OTA — takes ~4–5 minutes
5. Device reboots automatically after successful upload

## Android permissions (Android 12+)

Manifest must declare and app must request at runtime:
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`

Missing these causes silent failure at BLE scanner registration on Android 12+.
This is already fixed in the codebase — do not remove these permission declarations.

## Known-tested devices

- Samsung A5 2017 ✓
- Pixel 4a 5G ✓ (required Android 12+ permission fix)

## Current limitations / known rough edges

- OTA uses a **bundled `.bin` asset** — no file picker yet
- Minimal OTA UI — no fancy progress reporting, no device chooser
- **Single-device only** — multi-device BLE is the active next work area, app architecture not yet adapted for it
- No cleanup/refactor of OTA flow yet — it works but it's intentionally rough
- iPhone / iOS: no app exists, completely out of scope for now

## UF1 fanout — what Android does with BLE payloads

S1 (60-byte notify) → fans out to:
- 3× UF1 STATUS + EMG_RAW frames (one per electrode)
- 1× UF1 STATUS + QUAT frame

S2 (52-byte raw + 26-byte aux) → fans out to:
- UF1 STATUS + EMG_RAW (from 52-byte)
- UF1 0x03 IMU
- UF1 0x04 MAG
- UF1 0x05 QUAT
(last three from 26-byte aux)

If firmware payload sizes or profiles change, this fanout logic must be updated in sync.

## Branch / release status

v0.1.0 is released (APK on GitHub).
Active development on feature branches — check branch state before working.
