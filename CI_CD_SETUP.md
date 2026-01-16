# CI/CD Pipeline Setup Guide

This document describes how to set up the CI/CD pipeline for automated builds and Google Play Store deployment.

---

## Overview

The pipeline includes:
- **Automated builds** on every push/PR
- **Unit tests** and **lint checks**
- **Signed release builds** on version tags
- **Automatic deployment** to Google Play Store
- **GitHub Releases** with APK/AAB artifacts

---

## Pipeline Flow

```
Push to branch → Build → Test → Lint
                          ↓
Tag v*.*.* → Build Release → Sign AAB → Deploy to Play Store
                                 ↓
                          Create GitHub Release
```

---

## Required GitHub Secrets

Go to **Repository → Settings → Secrets and variables → Actions** and add:

### 1. API Keys (for app functionality)
| Secret | Description |
|--------|-------------|
| `API_KEY` | Anthropic API key |
| `API_URL` | API base URL (optional) |
| `HF_API_KEY` | HuggingFace API key |

### 2. Signing Keys (for release builds)
| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key password |

### 3. Google Play (for deployment)
| Secret | Description |
|--------|-------------|
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Service account JSON content |

---

## Step-by-Step Setup

### Step 1: Create Release Keystore

```bash
# Generate new keystore
keytool -genkey -v -keystore release-keystore.jks \
  -alias chatagent \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Convert to base64 for GitHub secret
base64 -i release-keystore.jks -o keystore-base64.txt

# Copy content of keystore-base64.txt to KEYSTORE_BASE64 secret
```

### Step 2: Set Up Google Play Service Account

1. Go to [Google Play Console](https://play.google.com/console)
2. Navigate to **Setup → API access**
3. Click **Create new service account**
4. Follow the link to Google Cloud Console
5. Create service account with role **Service Account User**
6. Create JSON key and download it
7. Back in Play Console, grant **Release manager** permission
8. Copy the entire JSON content to `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret

### Step 3: Create App in Google Play Console

Before first deployment:
1. Create new app in Google Play Console
2. Complete store listing (title, description, screenshots)
3. Set up content rating
4. Set up pricing and distribution
5. Create first release manually (required by Google)

### Step 4: Configure GitHub Environment

1. Go to **Repository → Settings → Environments**
2. Create environment named `production`
3. Add protection rules (optional):
   - Required reviewers
   - Wait timer

---

## Usage

### Trigger Build on Push
Any push to `master`, `main`, or `develop` triggers build and tests.

### Create Release
```bash
# Tag the release
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push tag to trigger deployment
git push origin v1.0.0
```

### Deployment Tracks

| Tag | Track |
|-----|-------|
| `v*.*.*` | Internal Testing |

To promote to other tracks, use Fastlane locally:
```bash
# Promote to beta
bundle exec fastlane promote_to_beta

# Promote to production (10% rollout)
bundle exec fastlane promote_to_production
```

---

## Local Fastlane Usage

### Install Fastlane
```bash
# Install Ruby gems
bundle install

# Or install directly
gem install fastlane
```

### Available Commands
```bash
# Run tests
bundle exec fastlane test

# Build debug
bundle exec fastlane build_debug

# Build release (requires env vars)
export KEYSTORE_FILE=path/to/keystore.jks
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=your_alias
export KEY_PASSWORD=your_key_password
bundle exec fastlane build_release

# Deploy to internal track
bundle exec fastlane deploy_internal

# Deploy to beta
bundle exec fastlane deploy_beta

# Deploy to production
bundle exec fastlane deploy_production
```

---

## Troubleshooting

### Build fails with signing error
- Check that `KEYSTORE_BASE64` is correctly encoded
- Verify `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` are correct
- Ensure keystore file is valid: `keytool -list -keystore release-keystore.jks`

### Google Play upload fails
- Verify service account has correct permissions
- Check that app exists in Play Console
- Ensure package name matches: `com.example.chatagent`
- First release must be uploaded manually

### Version code already exists
- Increment `versionCode` in `build.gradle.kts`
- Or use Fastlane: `bundle exec fastlane increment_version`

---

## Files Structure

```
.github/
└── workflows/
    ├── android-ci.yml      # Main CI/CD pipeline
    └── pr-review.yml       # PR review workflow

fastlane/
├── Fastfile               # Fastlane configuration
└── Pluginfile             # Fastlane plugins

Gemfile                    # Ruby dependencies
CI_CD_SETUP.md            # This file
```

---

## Security Notes

- Never commit keystore files to repository
- Never commit service account JSON
- Use GitHub Secrets for all sensitive data
- Enable branch protection on `master`/`main`
- Consider requiring PR reviews for releases
