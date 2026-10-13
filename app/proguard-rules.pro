# Mintal ships unminified (see app/build.gradle.kts): R8 is disabled for release so
# that F-Droid's reproducible build has nothing version-specific to disagree about.
# Rules kept here for the day minification is turned on.
-keepattributes *Annotation*
-keep class dev.nheox.mintal.data.db.** { *; }
