# Radio Satelital Android

[English version](README.en.md)

[![Android](https://img.shields.io/badge/Android-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-0095D5?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.4-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%26%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)

Aplicacion Android nativa (Kotlin + Jetpack Compose) para escuchar radios, explorar por pais y gestionar radios enviadas por la comunidad.

## Tipo de aplicacion

Radio Satelital es una aplicacion de radio por internet.
Permite reproducir emisoras en vivo, explorar radios por ubicacion y administrar radios enviadas por usuarios.

## Version de la aplicacion

- Version actual: 1.0
- Version code: 1

## Stack tecnico

- Kotlin
- Jetpack Compose + Navigation
- Media3 ExoPlayer
- Firebase Authentication
- Firebase Firestore

## Requisitos

- Android Studio (version reciente)
- JDK 17
- Android SDK 34
- Gradle Wrapper incluido en el repositorio

## Configuracion de Firebase

1. Crea (o abre) un proyecto en Firebase Console.
2. Registra una app Android con este package name: `com.app.radiosatelital`.
3. Descarga el archivo `google-services.json`.
4. Copia el archivo en `app/google-services.json`.
5. En Firebase Console habilita:
	 - Authentication (Anonymous y/o Email/Password)
	 - Firestore Database

Importante:
- `google-services.json` no debe subirse al repositorio publico.
- El correo de administrador debe configurarse como propiedad Gradle local, no en codigo.

### Configuracion local de correo administrador

Para mantener el acceso de administrador desde Ajustes sin exponer tu correo en Git, define `ADMIN_EMAIL` en un archivo local no versionado.

Opcion recomendada (global en tu maquina):

Archivo: `~/.gradle/gradle.properties`

```properties
ADMIN_EMAIL=tu-correo-admin@dominio.com
```

Opcion por proyecto (tambien no versionada):

Archivo: `local.properties`

```properties
ADMIN_EMAIL=tu-correo-admin@dominio.com
```

## Compilar y ejecutar

Desde la raiz del proyecto:

```bash
./gradlew :app:assembleDebug
```

Para instalar en un dispositivo/emulador conectado:

```bash
./gradlew :app:installDebug
```

Tambien puedes abrir el proyecto en Android Studio y ejecutar la configuracion `app`.

## Estructura principal

- `app/src/main/java/com/app/radiosatelital/ui`: pantallas y estado de UI
- `app/src/main/java/com/app/radiosatelital/data`: acceso a datos (Firebase, repositorios, artwork)
- `app/src/main/java/com/app/radiosatelital`: actividad principal, servicio de reproduccion y modelos base

## Capturas de pantalla

Agrega aqui imagenes reales de la app para mostrar el estado actual de la interfaz.

Plantilla sugerida:

- Home / Explorador
- Busqueda de radios
- Reproductor
- Pantalla de moderacion

Ejemplo de bloque para cuando tengas las imagenes:

```md
![Home](URL_O_RUTA_DE_LA_IMAGEN)
![Busqueda](URL_O_RUTA_DE_LA_IMAGEN)
![Reproductor](URL_O_RUTA_DE_LA_IMAGEN)
![Moderacion](URL_O_RUTA_DE_LA_IMAGEN)
```

## Arquitectura y flujo de datos

Resumen rapido:

- UI Compose en la capa de presentacion.
- ViewModels para exponer estado y acciones de pantalla.
- Repositorios y data sources para acceso a Firebase y otras fuentes.
- Service de reproduccion para audio en segundo plano.

Flujo general:

1. La UI dispara acciones de usuario (buscar, enviar radio, reproducir).
2. El ViewModel procesa la accion y consulta al repositorio.
3. El repositorio delega en Firebase u otra fuente de datos.
4. El resultado vuelve al ViewModel y se refleja en el estado de UI.
5. Para audio, el servicio mantiene la reproduccion independiente de la pantalla.

## Solucion de problemas

- Error de Google Services:
	- Verifica que exista `app/google-services.json` y que el package name coincida con `com.app.radiosatelital`.
- Error de autenticacion/Firestore:
	- Revisa que Authentication y Firestore esten habilitados en Firebase Console.
- Error de compilacion por Java:
	- Confirma que el proyecto se ejecute con JDK 17.

## Como colaborar

1. Abre un issue para reportar bugs o proponer mejoras.
2. Haz un fork del repositorio y crea una rama descriptiva.
3. Implementa el cambio con commits claros y pequenos.
4. Verifica que la app compile con `./gradlew :app:assembleDebug`.
5. Abre un Pull Request explicando objetivo, cambios y evidencia.

## Derechos reservados

Este proyecto usa una licencia propietaria con todos los derechos reservados.
Consulta los terminos en `LICENSE`.

## Apoyo al creador

Si deseas apoyar al creador del proyecto, puedes:

- Compartir la aplicacion y el repositorio.
- Reportar errores y sugerir mejoras utiles.
- Contribuir con codigo, pruebas o documentacion.
- Abrir un issue para coordinar formas directas de apoyo o patrocinio.
