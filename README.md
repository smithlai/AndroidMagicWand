# AndroidMagicWand


Trouble shooting:

> No matching variant of com.android.tools.build:gradle:7.4.2 was found.  
File -> Settings -> Build, Excution and deployment -> Build Tools -> Gradle -> Gradle JDK -> 11+

>Your project has set `android.useAndroidX=true`, but configuration `:app:debugRuntimeClasspath` still contains legacy support libraries, which may cause runtime issues.
> This behavior will not be allowed in Android Gradle plugin 8.0.  
android.enableJetifier=true 是一個 Gradle 屬性，用於自動將非 AndroidX 的依賴庫轉換為 AndroidX。

> Cause: jetified-multik-openblas-jvm-0.2.0 extracted from path C:\Users\tw023281\.gradle\caches\transforms-3\c0c303ab2b38e6ea6db165a871b51ee7\transformed\jetified-multik-openblas-jvm-0.2.0\linuxX64\libmultik_jni-linuxX64.so is not an ABI  
This is caused by AGP Upgrade Assistant(Tools -> AGP Upgrade Assistant,7.4->8.0), the is nothing to help but recreate the project.


> Caused by: java.io.FileNotFoundException: This file can not be opened as a file descriptor; it is probably compressed

gradle.properties:
`android.bundle.enableUncompressedNativeLibs=false`

> The option setting 'android.bundle.enableUncompressedNativeLibs=false' is deprecated.

* Make *.so available in /data/app/<APP>/lib/arm64 for Hexagon
  Modify gradle.properties to build.gradle

   ```
   The option setting 'android.bundle.enableUncompressedNativeLibs=false' is deprecated.
   The current default is 'true'.

   It will be removed in version 8.0 of the Android Gradle plugin.
   You can add the following to your build.gradle instead:
   android {
      packagingOptions {
         jniLibs {
               useLegacyPackaging = true
         }
      }
   }
   ```



2D plot
https://github.com/halfhp/androidplot

3D plot
https://github.com/jfree/orson-charts-android
