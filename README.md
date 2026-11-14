# QR Wallet for Android

A simple offline Android app that displays saved QR-code images, lets you swipe between them, and shares the currently displayed image.

## Features

- First launch opens the Android file picker so you can choose your first image.
- Add more QR images any time using **Add QR Code**.
- Swipe left/right on the displayed image to move between saved images.
- **Share QR Code** shares the currently displayed image using Android's share menu.
- Images are copied into the app's private storage, so they remain available offline and do not depend on the original file location.
- No account, server, or internet connection is needed for normal app use.

## Build the APK using GitHub

1. Create a new **public** GitHub repository named `QRWallet` (or any name you prefer).
2. Upload the contents of this project folder to the repository's root. Make sure `.github/workflows/build-apk.yml` is included.
3. If GitHub asks, commit the files to the `main` branch.
4. Open the repository's **Actions** tab. The workflow named **Build QR Wallet APK** should start automatically.
5. Wait for the workflow to finish with a green check mark.
6. Open the repository's **Releases** section.
7. Open **QR Wallet - Latest Build** and download `app-debug.apk`.
8. Transfer/open the APK on your Android phone and install it. Android may ask you to allow installation from that source.

If the workflow fails, open the failed workflow run and check the first red step. The error text will help identify what needs fixing.

## Notes

- The APK is currently a debug-signed APK, suitable for personal installation/testing. It is not a Play Store release.
- Android may show a warning because the APK is installed outside the Play Store.
- App data is private to the app. Uninstalling the app or clearing its storage deletes the QR images stored inside it.
- Use PNG, JPG, or other common image formats. The app does not generate QR codes from text; it displays QR-code images you select.
