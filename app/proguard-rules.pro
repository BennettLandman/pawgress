# Release builds do not minify (see build.gradle.kts), so this file is a
# placeholder. If you ever turn on minification, keep OkHttp's optional deps quiet:
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
