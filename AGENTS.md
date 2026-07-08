# 🏗️ Construcción IA — Documentación del Proyecto

## 📋 Descripción general

**Construcción IA** es una aplicación Android que usa inteligencia artificial para asistir en el diseño y visualización de proyectos arquitectónicos y de construcción. Permite generar imágenes de edificaciones a partir de descripciones textuales mediante **Gemini 1.5 Pro**, reconocer texto de planos con **ML Kit OCR**, visualizar imágenes generadas, exportar presentaciones a **PowerPoint (PPTX)** con **Apache POI**, y reproducir animaciones con **Lottie**.

| Tecnología | Versión |
|---|---|
| Kotlin | 2.1.0 |
| Android Gradle Plugin | 8.7.3 |
| Compose BOM | 2024.12.01 |
| Min SDK / Target SDK | 26 / 35 |
| Hilt (DI) | 2.53.1 |
| KSP | 2.1.0-1.0.29 |

## 📁 Estructura del proyecto

| Módulo | Tipo | Propósito |
|---|---|---|
| `:app` | Aplicación | Punto de entrada, Activity principal, inyección de dependencias global |
| `:core-common` | Librería | Clases compartidas: `AppResult`, `AppException`, `ImageBytes`, utilidades |
| `:core-network` | Librería | Cliente HTTP (OkHttp), modelo Generative AI de Google, constantes de API, DI de red |
| `:core-ui` | Librería | Componentes Compose reutilizables, tema, vistas base |
| `:core-testing` | Librería | Fakes compartidos (`FakeGeminiRepository`, `FakeOcrRepository`, `FakeExportRepository`), reglas JUnit (`MainDispatcherRule`) |
| `:feature-generation` | Librería | Generación de imágenes con Gemini — UI, ViewModel, caso de uso, repositorio, servicio API |
| `:feature-viewer` | Librería | Visualizador de imágenes generadas — galería, zoom, detalles |
| `:feature-ocr` | Librería | Reconocimiento óptico de caracteres con ML Kit — escaneo de planos y documentos |
| `:feature-export` | Librería | Exportación a PowerPoint (PPTX) con Apache POI |
| `:feature-animation` | Librería | Animaciones Lottie para transiciones y feedback visual |

## 🧱 Arquitectura

**Clean Architecture + MVVM** con tres capas por feature:

```
┌─────────────────────────────────────────────────┐
│                 Capa de Presentación             │
│  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  Composable  │◄─┤   ViewModel (StateFlow)  │  │
│  │  Screens     │  │   GenerationUiState      │  │
│  └──────────────┘  └──────────┬───────────────┘  │
├───────────────────────────────┼───────────────────┤
│              Capa de Dominio  │                   │
│  ┌────────────────────────────┴──────────────┐   │
│  │         UseCase (GenerateImageUseCase)     │   │
│  │         Repository (interfaz)              │   │
│  │         Modelos (GenerationPrompt, etc.)   │   │
│  └────────────────────────────┬──────────────┘   │
├───────────────────────────────┼───────────────────┤
│               Capa de Datos   │                   │
│  ┌────────────────────────────┴──────────────┐   │
│  │  RepositoryImpl  │  RemoteDataSource      │   │
│  │                   │  (GeminiApiService)   │   │
│  │  DI Module        │  Mapper               │   │
│  └───────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

**Flujo de datos:**

1. El usuario escribe un prompt en la UI Compose
2. El `ViewModel` actualiza `GenerationUiState` (Idle → Editing → Generating)
3. El `UseCase` valida el prompt (`ValidatePromptUseCase`)
4. El `RepositoryImpl` delega en `GeminiApiService`
5. `GeminiApiService` envía el prompt al SDK `GenerativeModel` (Gemini 1.5 Pro)
6. La respuesta (imagen en inlineData o fileUri) se decodifica a Bitmap
7. El resultado se propaga como `AppResult<GeneratedImage>` de vuelta al ViewModel
8. El estado cambia a `Success(generatedImage)` o `Error(exception)`

**Manejo de errores:** Toda operación retorna `AppResult<T>` (Success/Error) con una jerarquía sellada de excepciones (`NetworkException`, `AuthenticationException`, `ServerException`, `InvalidInputException`, `ProcessingException`, `StorageException`, `UnknownException`).

## 📦 Dependencias principales

| Librería | Propósito | Módulo |
|---|---|---|
| `com.google.ai.client.generativeai:generativeai` | SDK oficial de Google Generative AI (Gemini 1.5 Pro) | `:core-network`, `:feature-generation` |
| `com.google.mlkit:text-recognition` | Reconocimiento de texto en imágenes (OCR) | `:feature-ocr` |
| `org.apache.poi:poi` + `poi-ooxml` | Creación y manipulación de archivos PowerPoint PPTX | `:feature-export` |
| `com.airbnb.android:lottie-compose` | Animaciones vectoriales Lottie | `:feature-animation` |
| `com.google.dagger:hilt-android` | Inyección de dependencias (Hilt) | Todos los módulos |
| `io.coil-kt:coil-compose` | Carga y caché de imágenes en Compose | `:app`, `:core-ui`, features |
| `com.squareup.retrofit2:retrofit` | Cliente HTTP para APIs REST | `:core-network` |
| `com.squareup.okhttp3:okhttp` | Cliente HTTP + logging interceptor | `:core-network` |
| `androidx.room:room-runtime` | Base de datos local (SQLite) | `:core-network` |
| `androidx.datastore:datastore-preferences` | Almacenamiento de preferencias | `:core-network` |
| `androidx.navigation:navigation-compose` | Navegación entre pantallas | `:app`, features |
| `androidx.lifecycle:lifecycle-runtime-compose` | Lifecycle aware corrutinas + Compose | `:app`, features |
| `io.mockk:mockk` | Mocking para pruebas unitarias | Testing |
| `app.cash.turbine:turbine` | Testing de Flow/StateFlow | Testing |

## 🔐 Configuración

**API Key de Gemini:**

La API key se obtiene desde variable de entorno `GEMINI_API_KEY` en tiempo de compilación, definida en `core-network/build.gradle.kts:15`:

```kotlin
buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
```

**Formas de configurarla:**

1. **Variable de entorno del sistema:** `$env:GEMINI_API_KEY = "tu-api-key"` (PowerShell)
2. **Build Config:** Se inyecta automáticamente desde la variable de entorno al compilar
3. **Fallback en NetworkModule:** También busca `System.getProperty("GEMINI_API_KEY")` como respaldo

El `GenerativeModel` se configura en `core-network/.../di/NetworkModule.kt` con:
- Modelo: `gemini-1.5-pro`
- Safety settings: Bloqueo MEDIUM_AND_ABOVE para todas las categorías (acoso, discurso de odio, sexually explicit, contenido peligroso)

**Constantes de API** (`core-network/.../ApiConstants.kt`):
- `CONNECT_TIMEOUT`: 30s
- `READ_TIMEOUT`: 120s
- `WRITE_TIMEOUT`: 120s
- `MAX_PROMPT_LENGTH`: 5000 caracteres
- `MAX_IMAGE_SIZE_MB`: 20 MB

## 🧪 Testing

**Ejecutar tests:**

```bash
# Todos los tests unitarios
./gradlew test

# Tests de un módulo específico
./gradlew :feature-generation:test

# Tests con cobertura
./gradlew testCoverage
```

**Estructura de tests:**

```
core-common/src/test/java/.../common/
├── AppResultTest.kt          # Tests de AppResult (map, flatMap, onSuccess, onError, etc.)
└── ImageBytesTest.kt         # Tests de ImageBytes

feature-generation/src/test/java/.../generation/
├── domain/usecase/
│   └── GenerateImageUseCaseTest.kt   # Tests del caso de uso con FakeRepository y MockK
└── presentation/
    └── GenerationViewModelTest.kt    # Tests del ViewModel con Turbine y MockK
```

**Herramientas de testing:**

- **JUnit 4** — Framework de testing
- **MockK** — Mocking de dependencias
- **Turbine** — Testing de `StateFlow` y `Flow`
- **kotlinx-coroutines-test** — `runTest` y `TestDispatcher`

**Fakes disponibles** (`core-testing/src/main/java/.../testing/`):

| Fake | Implementa | Comportamiento |
|---|---|---|
| `FakeGeminiRepository` | `GeminiRepository` | Simula generación de imágenes; configurable para fallar con `setShouldFail()` |
| `FakeOcrRepository` | `OcrRepository` | Simula OCR de texto |
| `FakeExportRepository` | `ExportRepository` | Simula exportación a PPTX |
| `FakeConnectivityObserver` | — | Simula estado de conectividad |
| `MainDispatcherRule` | `TestWatcher` | Reemplaza `Dispatchers.Main` para tests de corrutinas |

**Framework de pruebas:** Los tests usan `runTest` para corrutinas y `UnconfinedTestDispatcher` como TestDispatcher predeterminado a través de `MainDispatcherRule`.

## 🚀 Cómo empezar

```bash
# 1. Clonar el repositorio
git clone <repo-url>
cd ConstruccionIA

# 2. Configurar API key de Gemini
$env:GEMINI_API_KEY = "tu-api-key"    # Windows PowerShell
export GEMINI_API_KEY="tu-api-key"     # macOS/Linux

# 3. Compilar el proyecto
./gradlew assembleDebug

# 4. Ejecutar tests
./gradlew test

# 5. Instalar en dispositivo/emulador
./gradlew installDebug
```

**Requisitos:**
- Android Studio Hedgehog (2024.1.1) o superior
- JDK 17
- SDK Android 35
- Gradle 8.7+
- Una API key de Google Generative AI (Gemini)

## 📱 Funcionalidades

### 🎨 Generación con Gemini
- Ingreso de prompt descriptivo para generar imágenes arquitectónicas
- 4 estilos visuales: **Foto Realista**, **Esquema Técnico**, **Render 3D**, **Boceto**
- 5 proporciones de aspecto: 1:1, 4:3, 16:9, 3:4, 9:16
- Validación de prompt (mínimo 10 caracteres, no vacío)
- Escalado automático de imágenes a 2048px máximo
- Timeout de 120s para generación
- Verificación de disponibilidad de la API (`isApiAvailable()`)

### 👁️ Visualizador
- Galería de imágenes generadas
- Zoom y navegación
- Detalles de la imagen (prompt usado, fecha, dimensiones)

### 📄 OCR con ML Kit
- Escaneo de texto desde planos y documentos
- Captura con cámara o selección desde galería
- Reconocimiento de texto en tiempo real

### 📊 Exportación a PowerPoint
- Exporta imágenes generadas a presentaciones PPTX
- Usa Apache POI para crear diapositivas con formato profesional
- Empaquetado de imágenes y metadatos

### ✨ Animaciones Lottie
- Animaciones de transición entre pantallas
- Feedback visual durante generación (loading)
- Animaciones de error y éxito

## 👥 Equipo

| Rol | Responsabilidades |
|---|---|
| **Orquestador** | Coordina todo el equipo, recibe peticiones del usuario, delega tareas y supervisa el flujo completo |
| **Arquitecto** | Define la arquitectura Clean Architecture + MVVM, escoge tecnologías (Gemini, ML Kit, Apache POI), supervisa la estructura de módulos y la inyección de dependencias (Hilt) |
| **Desarrollador Android** | Implementa features (generación, visualizador, OCR, exportación, animaciones), escribe ViewModels, UseCases, repositorios, y la integración con APIs externas |
| **QA / Testing** | Escribe tests unitarios con MockK + Turbine, crea fakes en `core-testing`, verifica flujos de error, validación de prompts y transiciones de estado |
| **UI/UX** | Diseña las pantallas en Jetpack Compose, define los componentes reutilizables en `core-ui`, configura el tema, paleta de colores y animaciones Lottie |
| **Supervisor** 🔬 | Revisor exhaustivo y crítico constructivo. Analiza código, arquitectura y tests con ojo crítico, da sugerencias notables y optimiza el trabajo antes de la entrega final. Puede vetar y regresar al Desarrollador si encuentra mejoras necesarias. Actúa como gate de calidad final antes de la entrega al usuario. |

### Flujo de trabajo (fases)

```
Fase 1 ──── Definición: Requisitos, arquitectura, selección de librerías
Fase 2 ──── Core: Módulos core-common, core-network, core-ui, core-testing
Fase 3 ──── Features: feature-generation, feature-viewer
Fase 4 ──── Features: feature-ocr, feature-export, feature-animation
Fase 5 ──── Integración: App principal, navegación, tema global
Fase 6 ──── Testing: Tests unitarios, fakes, cobertura
Fase 7 ──── 🔬 Supervisión: Revisión y optimización pre-entrega
Fase 8 ──── Release: ProGuard, empaquetado, publicación
```
