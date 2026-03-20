---
description: Build and run the Android app
argument-hint: [debug|release]
---

# Quick Build Command

Builds the Mooney Android app and reports any errors.

```bash
# Clean previous build artifacts
./gradlew clean

# Build the app (debug by default, or use provided argument)
./gradlew :composeApp:assemble${1:-Debug}

# Show build result
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "APK location: composeApp/build/outputs/apk/"
else
    echo "❌ Build failed - check errors above"
fi
```