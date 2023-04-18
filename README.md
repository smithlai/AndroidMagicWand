# AndroidMagicWand

gradle.properties
> android.enableJetifier=true
>
> 
> 

Trouble shooting:

> No matching variant of com.android.tools.build:gradle:7.4.2 was found.  
File -> Settings -> Build, Excution and deployment -> Build Tools -> Gradle -> Gradle JDK -> 11+

>Your project has set `android.useAndroidX=true`, but configuration `:app:debugRuntimeClasspath` still contains legacy support libraries, which may cause runtime issues.
> This behavior will not be allowed in Android Gradle plugin 8.0.  
android.enableJetifier=true 是一個 Gradle 屬性，用於自動將非 AndroidX 的依賴庫轉換為 AndroidX。
