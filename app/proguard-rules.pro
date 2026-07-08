# =============================================================================
# ProGuard / R8 Rules — ConstrucciónIA App
#
# Tecnologías cubiertas:
#   Room, Firebase (Crashlytics, Analytics, Auth, Firestore, Storage),
#   Gson, Hilt/Dagger, Coil, ML Kit Text Recognition, Apache POI,
#   Kotlin Coroutines, Kotlin Serialization, Jetpack Compose,
#   Navigation Compose, Lifecycle/ViewModel, FileProvider.
#
# Modo full de R8: si android:enableR8.fullMode=true, muchas reglas
# adicionales son necesarias porque R8 es más agresivo.
# =============================================================================

# ---------------------------------------------------------------------------
# REGLAS GENERALES – NO TOCAR
# ---------------------------------------------------------------------------

-# No comprimir ni obfuscar clases de la app
-keep class com.construccionia.app.** { *; }

-# Mantener nombres de clases y miembros con acceso desde XML (layouts, manifest, etc.)
-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod, SourceFile, LineNumberTable

-# Mantener SourceFile y LineNumberTable para Crashlytics
-keepattributes SourceFile, LineNumberTable
-keep public class * extends java.lang.Exception

-# Mantener clases de datos Kotlin (data class -> componentN(), copy(), toString(), etc.)
-keepclassmembers class * {
    *** component*();
    void copy*(...);
    kotlin.jvm.internal.DefaultConstructorMarker <init>(...);
}
-dontwarn kotlin.**

-# Mantener constructores sintéticos de Kotlin (DefaultConstructorMarker)
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }

-# Mantener @JvmStatic y @JvmOverloads
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic *;
}

-# ---------------------------------------------------------------------------
# R8 FULL MODE — Reglas necesarias cuando android:enableR8.fullMode=true
# ---------------------------------------------------------------------------

-# R8 full mode elimina clases que solo se usan por reflection (Gson, Firebase, etc.)
-keep,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation class * {
    @com.google.gson.annotations.Expose <fields>;
}

-# No eliminar clases de tipo Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-# No eliminar clases con métodos native
-keepclassmembers class * {
    native <methods>;
}

-# ---------------------------------------------------------------------------
# ROOM — Base de datos local (KSP / kapt)
# ---------------------------------------------------------------------------

-# Room genera impl (MyDatabase_Impl, DAO_Impl, etc.)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-# Mantener todas las clases anotadas con @Entity, @Dao, @Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *
-keep @androidx.room.Relation class *

-# Mantener DAOs generados
-keep interface * extends androidx.room.* { *; }

-# Mantener TypeConverters
-keep @androidx.room.TypeConverter class * { *; }

-# Room utiliza reflection para TypeConverters
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

-# Mantener clases de datos de Room (data class usada como @Entity)
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

# ---------------------------------------------------------------------------
# FIREBASE — Crashlytics, Analytics, Auth, Firestore, Storage
# ---------------------------------------------------------------------------

-# Crashlytics — mantener datos de excepciones
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

-# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.analytics.ktx.** { *; }

-# Firebase Authentication
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.auth.ktx.** { *; }

-# Firebase Firestore — mantener documentos, POJOs y snapshots
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.firestore.ktx.** { *; }
-keep class com.google.firebase.firestore.model.** { *; }
-keepclassmembers class * extends com.google.firebase.firestore.DocumentSnapshot { *; }

-# Firestore usa reflection para mapear POJOs -> mantener todos los POJOs
-keepclassmembers class * {
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class *
-keep @com.google.firebase.firestore.Exclude class *

-# Firebase Storage
-keep class com.google.firebase.storage.** { *; }
-keep class com.google.firebase.storage.ktx.** { *; }

-# Firebase Options y configuraciones (reflection para inicialización)
-keep class com.google.firebase.FirebaseOptions { *; }
-keep class com.google.firebase.FirebaseApp { *; }

-# No obfuscar clases de Firebase que se cargan con reflection
-keep class com.google.firebase.components.** { *; }
-keep class com.google.firebase.heartbeatinfo.** { *; }
-keep class com.google.firebase.platforminfo.** { *; }
-keep class com.google.firebase.provider.** { *; }

-# Mantener Firebase Performance (si se usa en el futuro)
-keep class com.google.firebase.perf.** { *; }

# ---------------------------------------------------------------------------
# GSON — JSON Parsing (reflection en campos)
# ---------------------------------------------------------------------------

-# Mantener las anotaciones de Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
    @com.google.gson.annotations.JsonAdapter <fields>;
}

-# Mantener los TypeAdapter y TypeAdapterFactory personalizados
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class * extends com.google.gson.reflect.TypeToken

-# Mantener constructores sin argumentos para Gson (necesita crear instancias)
-keepclassmembers class * {
    public <init>();
    public <init>(java.lang.String);
    public <init>(int);
}

-# Evitar que R8 quite el constructor sin args de data classes usadas por Gson
-keepclassmembers,allowobfuscation class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---------------------------------------------------------------------------
# HILT / DAGGER — Inyección de dependencias
# ---------------------------------------------------------------------------

-# Hilt genera componentes Dagger en tiempo de compilación
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-# Mantener clases anotadas con @Inject (constructores que Dagger/Hilt va a llamar)
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper *;
}

-# Hilt Android
-keep class dagger.hilt.android.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.processor.internal.** { *; }

-# Dagger core
-keep class dagger.** { *; }
-keep class dagger.internal.** { *; }
-keep class dagger.hilt.migration.** { *; }

-# Mantener módulos Dagger
-keep @dagger.Module class *
-keep @dagger.Provides class *
-keep @dagger.Binds class *
-keep @dagger.multibindings.IntoMap class *
-keep @dagger.multibindings.IntoSet class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.components.SingletonComponent class *

-# EntryPoints de Hilt
-keep @dagger.hilt.EntryPoint class *
-keep @dagger.hilt.android.EntryPointAccessors class *

# ---------------------------------------------------------------------------
# COIL — Carga de imágenes
# ---------------------------------------------------------------------------

-keep class coil.** { *; }
-keep class coil.transform.** { *; }
-keep class coil.decode.** { *; }
-keep class coil.request.** { *; }
-keep class coil.size.** { *; }
-dontwarn coil.**

# ---------------------------------------------------------------------------
# ML KIT — Text Recognition (y otros APIs de ML Kit)
# ---------------------------------------------------------------------------

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.odml.image.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.vision.**

# ---------------------------------------------------------------------------
# APACHE POI — Manipulación de archivos Office
# ---------------------------------------------------------------------------

-keep class org.apache.poi.** { *; }
-keep class org.apache.poi.xwpf.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.hwpf.** { *; }
-keep class org.apache.poi.hssf.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.**

# Apache POI usa reflection interna y classloader
-keep class * implements org.apache.poi.util.POILogger { *; }

# ---------------------------------------------------------------------------
# KOTLIN COROUTINES
# ---------------------------------------------------------------------------

-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Continuation y Debug
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlinx.coroutines.debug.** { *; }

# ---------------------------------------------------------------------------
# KOTLIN SERIALIZATION
# ---------------------------------------------------------------------------

-# Kotlin Serialization (@Serializable) usa serializers generados
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.construccionia.app.**$$serializer { *; }
-keepclassmembers class com.construccionia.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.construccionia.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-# Mantener serializers generados en las compilaciones de KSP/kapt
-keep class * implements kotlinx.serialization.KSerializer { *; }

# ---------------------------------------------------------------------------
# JETPACK COMPOSE
# ---------------------------------------------------------------------------

-# Mantener funciones @Composable (son referenciadas por el compilador de Compose)
-keep,allowobfuscation @androidx.compose.runtime.Composable fun <**>

-# Mantener clases de renderizado de Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-# Animaciones de Compose
-keep class androidx.compose.animation.** { *; }

-# Mantener Parcelize (implementación de Parcelable generada)
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    *** CREATOR;
}

# ---------------------------------------------------------------------------
# NAVIGATION COMPOSE
# ---------------------------------------------------------------------------

-keep class androidx.navigation.** { *; }
-keep class androidx.navigation.compose.** { *; }
-keep class androidx.navigation.fragment.** { *; }
-dontwarn androidx.navigation.**

# ---------------------------------------------------------------------------
# LIFECYCLE / VIEWMODEL
# ---------------------------------------------------------------------------

-keep class androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.viewmodel.** { *; }
-keep class androidx.lifecycle.viewmodel.ktx.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ---------------------------------------------------------------------------
# FILEPROVIDER
# ---------------------------------------------------------------------------

-keep class androidx.core.content.FileProvider { *; }
-keep class * extends androidx.core.content.FileProvider { *; }

# ---------------------------------------------------------------------------
# MISCELÁNEO — Utilidades comunes
# ---------------------------------------------------------------------------

-# Mantener clases de recursos de la app (R, BuildConfig, etc.)
-keep class com.construccionia.app.R { *; }
-keep class com.construccionia.app.BuildConfig { *; }

-# Mantener enumeraciones
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-# Mantener clases con métodos de callback para reflection
-keepclassmembers class * {
    void on*(...);
    void success(...);
    void failure(...);
    void onError(...);
    void onCompleted(...);
}

-# Mantener clases que implementan Serializable
-keep class * implements java.io.Serializable { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-# No advertir sobre referencias a clases que no existen en todas las variantes
-dontwarn com.google.errorprone.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.**
-dontwarn com.sun.**
-dontwarn java.lang.invoke.**

-# ---------------------------------------------------------------------------
# FIN DEL ARCHIVO
# ---------------------------------------------------------------------------
