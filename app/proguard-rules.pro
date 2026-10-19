# Keep kotlinx.serialization generated serializers if you turn on minification.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.biasnil.stylusnotes.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
