<div align="center">
  <a href="https://star-history.com/#the-dise/Mir-Pay-Wallet&Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="src/ic_banner_dark.svg" />
      <source media="(prefers-color-scheme: light)" srcset="src/ic_banner_light.svg" />
      <img alt="Star History Chart" src="src/ic_banner_light.svg" width="512" height="auto" alt="Boxfish logo" />
    </picture>
  </a>
</div>

# Boxfish

**This project is forked [2dust/v2rayNG](https://github.com/2dust/v2rayNG)**

Boxfish is a v2Ray client for Android, supporting both [Xray Core](https://github.com/XTLS/Xray-core) and [v2Fly Core](https://github.com/v2fly/v2ray-core).

[![API](https://img.shields.io/badge/API-29%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/lollipop)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/the-dise/boxfish)](https://github.com/the-dise/boxfish/commits/master)
[![CodeFactor](https://www.codefactor.io/repository/github/the-dise/boxfish/badge)](https://www.codefactor.io/repository/github/the-dise/boxfish)
[![GitHub Releases](https://img.shields.io/github/downloads/the-dise/boxfish/latest/total?logo=github)](https://github.com/the-dise/boxfish/releases)
[![Chat on Telegram](https://img.shields.io/badge/Telegram-Channel-brightgreen.svg)](https://t.me/thedise)

## Overview

Boxfish provides a secure VPN client based on v2Ray and Xray cores for Android devices. It supports modern Android APIs and is developed using Kotlin. The primary purpose of the application is to bypass internet restrictions and create secure data tunnels.

## Usage

### GeoIP and GeoSite

- The `geoip.dat` and `geosite.dat` files are located in: `Android/data/me.thedise.boxfish/files/assets` (the path may vary on some Android devices).
- The download feature fetches enhanced versions from this [repository](https://github.com/Loyalsoldier/v2ray-rules-dat) (a working proxy is required).
- The latest official [domain list](https://github.com/v2fly/domain-list-community) and [IP list](https://github.com/v2fly/geoip) can be imported manually.
- Third-party data files like those from [h2y](https://guide.v2fly.org/en_US/routing/sitedata.html) can also be used by placing them in the same directory.

## Wiki

For detailed guides and additional information, please refer to the [Wiki](https://github.com/the-dise/boxfish/wiki).

## Development Guide

The Android project in the Boxfish folder can be compiled directly in Android Studio or using the Gradle wrapper. However, note that the v2Ray core inside the AAR file may be outdated.

- You can compile the `*.aar` file from Go projects such as [AndroidLibV2rayLite](https://github.com/2dust/AndroidLibV2rayLite) or [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite).
- For a quick start, check out the guides for [Go Mobile](https://github.com/golang/go/wiki/Mobile) and [Makefiles for Go Developers](https://tutorialedge.net/golang/makefiles-for-go-developers/).

### Running on Emulators

Boxfish can be run on Android emulators. For WSA (Windows Subsystem for Android), VPN permissions need to be granted using the following command:

````bash
appops set [package name] ACTIVATE_VPN allow```
````

---

<p align="center"><b>Made with ❤️ by Dise</b></p>