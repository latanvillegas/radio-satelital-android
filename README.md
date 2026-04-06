# Radio Satelital Android

[English version](README.en.md)

[![Android](https://img.shields.io/badge/Android-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-0095D5?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.4-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%26%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)

Aplicación Android nativa para descubrir y escuchar radios en vivo, organizada por ubicación geográfica y con moderación comunitaria.

## Propuesta de valor

Radio Satelital ayuda a los usuarios a encontrar emisoras reales por país, región y ciudad de forma rápida, con reproducción fluida y una experiencia clara desde el primer uso.

## Estado del proyecto

- Estado actual: Activo (v1.0)
- Fase: Producción inicial con mejoras continuas
- Plataforma: Android

## Características principales

- Reproducción de radios en vivo con servicio en segundo plano.
- Exploración por país, región, ciudad y continente.
- Búsqueda rápida de emisoras.
- Vista de reproductor con controles claros.
- Envío de nuevas radios por parte de la comunidad.
- Moderación de radios enviadas desde modo administrador.
- Soporte de tema y ajustes de visualización dentro de la app.

## Funcionalidades

| Función | Disponible | Notas |
|---|---|---|
| Reproducción en vivo | Sí | Basado en Media3 ExoPlayer |
| Búsqueda de radios | Sí | Filtro rápido por nombre |
| Exploración por ubicación | Sí | País, región, ciudad y continente |
| Envío de radios por usuarios | Sí | Flujo con estado pendiente |
| Moderación admin | Sí | Aprobar o rechazar radios enviadas |
| Temas y ajustes visuales | Sí | Desde pantalla de Ajustes |
| Favoritos sincronizados | No (roadmap) | Planeado para la siguiente iteración |

## Casos de uso

- Escuchar radios locales de una ciudad específica.
- Descubrir emisoras nuevas por país o continente.
- Compartir una radio no listada para que el equipo la modere.
- Administrar el catálogo comunitario desde modo administrador.

## Versión de la aplicación

- Versión actual: 1.0
- Version code: 1

## Capturas de pantalla

Agrega aquí imágenes reales de la app para mostrar el estado actual de la interfaz.

Plantilla sugerida:

- Home / Explorador
- Búsqueda de radios
- Reproductor
- Pantalla de moderación

Ejemplo de bloque para cuando tengas las imágenes:

```md
![Home](URL_O_RUTA_DE_LA_IMAGEN)
![Busqueda](URL_O_RUTA_DE_LA_IMAGEN)
![Reproductor](URL_O_RUTA_DE_LA_IMAGEN)
![Moderacion](URL_O_RUTA_DE_LA_IMAGEN)
```

Recomendación: reemplaza estos placeholders por 3 a 5 capturas reales antes de publicar.

## Arquitectura y flujo de datos

Resumen rápido:

- UI Compose en la capa de presentación.
- ViewModels para exponer estado y acciones de pantalla.
- Repositorios y fuentes de datos para acceso a Firebase y otras fuentes.
- Servicio de reproducción para audio en segundo plano.

Flujo general:

1. La UI dispara acciones de usuario (buscar, enviar radio, reproducir).
2. El ViewModel procesa la acción y consulta al repositorio.
3. El repositorio delega en Firebase u otra fuente de datos.
4. El resultado vuelve al ViewModel y se refleja en el estado de UI.
5. Para audio, el servicio mantiene la reproducción independiente de la pantalla.

## Roadmap

- Favoritos y colecciones personalizadas.
- Historial de reproducción reciente.
- Mejoras de metadatos de streaming.
- Telemetría básica de reproducción (opt-in).
- Mejora de UX para moderación masiva.

## Para desarrolladores

### Stack técnico

- Kotlin
- Jetpack Compose + Navigation
- Media3 ExoPlayer
- Firebase Authentication
- Firebase Firestore

### Requisitos

- Android Studio (version reciente)
- JDK 17
- Android SDK 34
- Gradle Wrapper incluido en el repositorio

### Configuración mínima

1. Configura Firebase para la app Android `com.app.radiosatelital`.
2. Coloca `google-services.json` en `app/google-services.json`.
3. Define `ADMIN_EMAIL` en una propiedad local de Gradle (`~/.gradle/gradle.properties` o `local.properties`).

### Build rápido

```bash
./gradlew :app:assembleDebug
```

### Estructura principal

- `app/src/main/java/com/app/radiosatelital/ui`: pantallas y estado de UI.
- `app/src/main/java/com/app/radiosatelital/data`: acceso a datos (Firebase, repositorios, artwork).
- `app/src/main/java/com/app/radiosatelital`: actividad principal, servicio de reproducción y modelos base.

## Solución de problemas

- Error de Google Services: verifica que exista `app/google-services.json` y que el package name coincida con `com.app.radiosatelital`.
- Error de autenticación/Firestore: revisa que Authentication y Firestore estén habilitados en Firebase Console.
- Error de compilación por Java: confirma que el proyecto se ejecute con JDK 17.

## FAQ

1. No puedo iniciar sesión como administrador, ¿qué reviso primero?
Confirma que `ADMIN_EMAIL` esté definido localmente y que el usuario exista en Firebase Authentication con Email/Password habilitado.

2. ¿Se puede usar la app sin cuenta?
Sí, la app soporta autenticación anónima para flujos públicos.

3. ¿Cómo recupero acceso admin si olvidé la clave?
Usa la opción de restablecer clave desde la app o desde Firebase Authentication.

4. ¿Qué hago si una radio no reproduce?
Verifica conectividad, disponibilidad del stream y formato soportado por el proveedor.

## Seguridad

- No subir `google-services.json` al repositorio.
- No hardcodear correos, contraseñas ni claves API en código fuente.
- Usar propiedades locales para configuraciones sensibles (`ADMIN_EMAIL`).
- Rotar credenciales en Firebase si hubo exposición previa.

## Cómo colaborar

1. Abre un issue para reportar bugs o proponer mejoras.
2. Haz un fork del repositorio y crea una rama descriptiva.
3. Implementa el cambio con commits claros y pequeños.
4. Verifica que la app compile con `./gradlew :app:assembleDebug`.
5. Abre un Pull Request explicando objetivo, cambios y evidencia.

Checklist recomendado para PR:

- [ ] Build debug exitoso.
- [ ] Descripción clara de alcance y riesgo.
- [ ] Capturas si hubo cambios de UI.
- [ ] Sin secretos ni credenciales en cambios.

## Derechos reservados

Este proyecto usa una licencia propietaria con todos los derechos reservados.
Consulta los términos en `LICENSE`.

## Soporte y contacto

- Canal principal: Issues del repositorio.
- Para soporte de colaboración o patrocinio: abre un issue con etiqueta de contacto.

## Apoyo al creador

Si deseas apoyar al creador del proyecto, puedes:

- Compartir la aplicación y el repositorio.
- Reportar errores y sugerir mejoras útiles.
- Contribuir con código, pruebas o documentación.
- Abrir un issue para coordinar formas directas de apoyo o patrocinio.
