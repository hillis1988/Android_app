# StarFleet Idle ProGuard Rules

# Keep Compose
-dontwarn androidx.compose.**

# Keep Google Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }

# Keep Google Play Games
-keep class com.google.android.gms.games.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }

# Keep UMP (consent)
-keep class com.google.android.ump.** { *; }

# Keep our data classes (used in JSON serialization)
-keep class com.starfleet.idle.data.** { *; }

# Keep enum values (used in when() blocks)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
