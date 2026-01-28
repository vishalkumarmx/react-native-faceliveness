# react-native-faceliveness

Android-only React Native wrapper for the Silent-Face-Anti-Spoofing engine.

## Requirements
- Android 5.0+ (API 21)
- ARM device (arm64-v8a or armeabi-v7a)
- Camera permission handled by the host app

## Gradle defaults (override in your app)
- `compileSdkVersion`: 35
- `targetSdkVersion`: 35
- `ndkVersion`: 26.1.10909125
- `cameraXVersion`: 1.3.3

If native linking fails with the bundled `libncnn.a`, pin `ndkVersion` to `20.0.5594570` in your app's `android/build.gradle` `ext {}`.

## Props
- `cameraFacing`: `'front' | 'back'` (default: front)
- `threshold`: number (default: 0.915)
- `analysisIntervalMs`: number (default: 100, set 0 for every frame)
- `active`: boolean (default: true)

## JS Commands
Use refs to start/stop the camera pipeline manually.

```tsx
import React, { useRef } from 'react';
import FaceLivenessView, { start, stop } from 'react-native-faceliveness';

export function LivenessScreen() {
  const viewRef = useRef(null);

  return (
    <FaceLivenessView
      ref={viewRef}
      style={{ flex: 1 }}
      cameraFacing="front"
      threshold={0.915}
      analysisIntervalMs={120}
      onLiveness={(e) => {
        const { confidence, hasFace } = e.nativeEvent;
      }}
      onError={(e) => console.warn(e.nativeEvent)}
    />
  );
}

// Later
start(viewRef.current);
stop(viewRef.current);
```
