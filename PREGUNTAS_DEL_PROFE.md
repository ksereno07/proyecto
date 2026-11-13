# 🎓 Guía de defensa: preguntas que te puede hacer el profe (con respuestas)

> Estilo: respuestas que podés decir con tus palabras. Leélas dos veces y
> quedás listo. Ordenadas de lo más probable a lo más raro.

---

## 1. ARQUITECTURA GENERAL

### P: ¿Qué patrón de arquitectura usa tu app y por qué?
**R:** MVVM (Model–View–ViewModel). La Vista son las pantallas de Compose
(`ui/screens`), el ViewModel es `AppViewModel.kt` que guarda el estado y la
lógica, y el Modelo es la base SQLite (`DbHelper.kt`) más los data classes
(`Models.kt`). Lo usé porque separa la interfaz de la lógica: si mañana
cambio SQLite por Firebase, no tengo que rehacer ninguna pantalla, solo la
capa de datos. Además el ViewModel sobrevive a rotaciones de pantalla (no se
pierde el estado al girar el teléfono).

### P: ¿Por qué hiciste todo local con SQLite y no con una nube?
**R:** Porque es para zonas rurales donde la señal se cae seguido. La app
funciona 100% offline: la base vive en el teléfono. Dejé el acceso a datos
concentrado en `DbHelper.kt` justamente para que migrar a Firebase después
sea solo reemplazar esa capa. Y le agregué el modo sin internet con cola de
envío, que es donde luego entra Firebase.

### P: ¿Cuáles son las capas de tu proyecto?
**R:** 
- `MainActivity` → punto de entrada (tema, permisos, arranca la navegación).
- `ui/AppNav.kt` → navegación entre pantallas (las rutas).
- `ui/screens` → las pantallas de Compose (Vista).
- `AppViewModel` → estado compartido + lógica (ViewModel).
- `data/` → `DbHelper` (SQLite), `Models` (data classes), `Session`
  (SharedPreferences).
- `util/` → utilidades: `Security` (cifrado), `NetWatcher` (señal),
  `SyncNotifier` (notificaciones), `PalmModelAnalyzer` (modelo TFLite),
  `PalmAnalyzer` (escáner: modelo + reglas), `PhotoStorage` (fotos),
  `LocationHelper` (GPS), `Fmt` (formatos).
- `ui/components` → piezas reutilizables (tarjetas, campos, banner offline).

---

## 2. NAVEGACIÓN Y ROUTES (lo que más pregunta el profe)

### P: ¿Para qué sirve un "route"?
**R:** Es el nombre/string que identifica una pantalla dentro del `NavHost`.
Por ejemplo `"login"`, `"owner/home"`, `"worker/report/{parcelId}"`. Cuando
quiero ir a una pantalla le digo al `NavController` que navegue a ese route:
`nav.navigate("owner/reports")`. Es como la dirección de cada pantalla.

### P: ¿Qué es el NavHost y el NavController?
**R:** El `NavHost` es el contenedor que muestra la pantalla actual según la
ruta activa; adentro declaro todos los routes con `composable("ruta") { ...
}`. El `NavController` (que creo con `rememberNavController()`) es el que
controla la navegación: sabe en qué pantalla estoy, y con `navigate()` voy
adelante y con `popBackStack()` vuelvo atrás.

### P: ¿Cómo decides en qué pantalla arranca la app?
**R:** En `AppNav.kt` calculo el `startDestination` con un `when` sobre
`vm.currentUser?.role`: si es OWNER arranca en `"owner/home"`, si es WORKER
en `"worker/home"`, y si es null (nadie logueado) en `"login"`. El
`currentUser` sale de la sesión guardada en SharedPreferences, así el usuario
entra directo a su panel si ya inició sesión antes.

### P: ¿Cómo le pasás el id de la parcela a las pantallas del trabajador?
**R:** Con un argumento de navegación. El route se declara como
`"worker/parcel/{parcelId}"` y le paso
`arguments = listOf(navArgument("parcelId") { type = NavType.LongType })`.
Cuando el trabajador toca su parcela hago
`nav.navigate("worker/parcel/$parcelId")`, y en la pantalla destino lo
leo con `entry.arguments?.getLong("parcelId") ?: 0L`. Así la misma pantalla
sirve para cualquier parcela.

### P: ¿Por qué LongType y no StringType para el parcelId?
**R:** Porque el id de la parcela en SQLite es un entero autoincremental
(`INTEGER PRIMARY KEY AUTOINCREMENT`), y Kotlin lo representa como `Long`.
Pasar el tipo correcto evita conversiones y errores de parseo.

### P: ¿Qué hace popUpTo cuando uno inicia sesión?
**R:** Al navegar del login al panel uso
`popBackStack(login, inclusive = true)` (o `popUpTo` en navegación
declarativa) para sacar el login de la pila. Así cuando el usuario presiona
"atrás" en el panel NO regresa al login (no tiene sentido volver a firmar
si ya está adentro) — se sale de la app directamente.

### P: ¿Qué pantallas tiene tu app? (nómbralas)
**R:** 
- Públicas: Login, Registro Trabajador, Registro Dueño.
- Dueño: Inicio (panel con código y totales), Crear Parcela, Reportes
  (3 pestañas: reportes/materiales/salud), Costos, Perfil.
- Trabajador: Inicio (mis parcelas), Unirse con código, la Parcela
  (labores y avance), Reportar Trabajo, Pedir Materiales, Escáner IA, Perfil.

---

## 3. COMPOSE (la UI)

### P: ¿Qué es un @Composable?
**R:** Es una función de Kotlin anotada con `@Composable` que describe una
parte de la interfaz. Se puede llamar dentro de otras composables y Compose
las convierte en la UI de la pantalla. No "dibuja" una sola vez: se
re-ejecutan (recomposición) cuando cambia el estado que leen.

### P: ¿Qué es remember y mutableStateOf?
**R:** `mutableStateOf` crea un estado observable; si cambia, Compose vuelve
a dibujar lo que lo lee. `remember` hace que ese estado sobreviva a las
recomposiciones (que no se resetee a cada rato). Ejemplo real: en la pantalla
de login, `var password by remember { mutableStateOf("") }` guarda lo que
escribe el usuario; cuando cambia, el campo se actualiza solo.

### P: ¿Qué es la recomposición?
**R:** Es el proceso donde Compose re-ejecuta solo las composables que leen
un estado que cambió. Si el usuario teclea una letra, se redibuja ese campo
de texto, no toda la pantalla. Por eso es más eficiente que el sistema de
XML + findViewById de la vieja escuela.

### P: ¿Qué es un Scaffold?
**R:** Es la estructura básica de pantalla de Material 3: te da los espacios
estándar (topBar, content, snackbarHost, etc.). En mis pantallas lo uso para
tener la barra superior (`AgroTopBar`) y el contenido scrolleable con el
fondo de color correcto.

### P: ¿Por qué tenés componentes reutilizables en Common.kt?
**R:** Para no repetir código de UI (principio DRY). `AgroTopBar`,
`AgroCard`, `AgroField`, `StatusPill`, `StatTile`, `EmptyState` se usan en
muchas pantallas. Si cambio el diseño de la tarjeta, lo cambio en un solo
lado y todas las pantallas se actualizan.

### P: ¿Qué es un Modifier?
**R:** Es el objeto que decora un composable: le dice tamaño
(`fillMaxWidth`), padding, fondo, alineación, click, etc. Se encadenan con
punto: `Modifier.fillMaxWidth().padding(16.dp)`.

### P: ¿Qué es AnimatedVisibility y dónde lo usás?
**R:** Es una animación de Compose que muestra/oculta contenido con una
transición. Lo uso en el `OfflineBanner`: cuando no hay internet entra con
`expandVertically() + fadeIn()` y cuando vuelve la señal se esconde con
`shrinkVertically() + fadeOut()`, así el banner aparece y desaparece suave
y no de golpe.

### P: ¿Qué es Material 3?
**R:** Es el sistema de diseño de Google para Android (sucesor de Material
2). Me da componentes listos (campos, chips, diálogos, barras), theming con
`MaterialTheme` y accesibilidad. En `ui/theme` definí la paleta de la app
(verde palma #1B4332, mostaza #E09F3E, crema #F7F4EA) y la tipografía.

---

## 4. APPVIEWMODEL Y CORRUTINAS

### P: ¿Para qué sirve el AppViewModel?
**R:** Es el cerebro de la app en MVVM. Guarda el estado observable que leen
las pantallas (`currentUser`, `ownerReports`, `pendientesEnvio`, etc.) y
ejecuta todas las acciones: login, registros, crear parcela, unirse con
código, reportar trabajo, pedir materiales, escáner, sincronizar pendientes.
Las pantallas no tocan la base directamente: le piden al ViewModel y él
actualiza el estado, y Compose redibuja solo.

### P: ¿Qué es viewModelScope y por qué lo usás?
**R:** Es el alcance (scope) de corrutinas atado al ciclo de vida del
ViewModel: todas mis operaciones largas (consultas SQLite) las lanzo con
`viewModelScope.launch`. Si el ViewModel muere (se cierra la pantalla para
siempre), las corrutinas se cancelan solas y no quedan basura corriendo.

### P: ¿Por qué Dispatchers.IO y withContext?
**R:** Porque las consultas a SQLite son operaciones de disco, y si las corro
en el hilo principal la UI se congela (Android hasta te tira un
"Application Not Responding"). `withContext(Dispatchers.IO)` mueve esa
consulta al hilo de entrada/salida, la UI sigue fluida, y el resultado
regresa al hilo principal para actualizar el estado. Ejemplo:
`withContext(io) { db.authenticate(...) }`.

### P: ¿Qué es una corrutina en pocas palabras?
**R:** Es una forma de escribir código asíncrono sin anidar callbacks. Puedo
"suspender" una función mientras espera el disco o la red, sin bloquear el
hilo. En mi app se lanzan con `launch` dentro del `viewModelScope`.

### P: ¿Qué hace el init del AppViewModel?
**R:** Varias cosas en orden:
1. Si hay sesión guardada en SharedPreferences, revive al usuario y carga su
   panel según el rol.
2. Prende el `NetWatcher` (el vigilante de la señal).
3. Crea el canal de notificaciones.
4. Estado inicial de red: si hay internet sincroniza pendientes, si no,
   tira la notificación de "sin internet".
5. Lanza una corrutina que se queda colectando cambios de señal: cuando
   cambia de offline a online envía la cola, y al revés avisa que no hay
   red.

### P: ¿Qué es OwnerSnapshot?
**R:** Es un data class privado que uso en `loadOwnerData()` para cargar
TODA la información del panel del dueño (parcela, estadísticas, trabajadores,
reportes, materiales, diagnósticos) en un solo bloque en el hilo IO, y luego
asignarla de una vez a los estados observables. Evita que la pantalla se
vea a medio cargar (flicker).

---

## 5. BASE DE DATOS SQLITE

### P: ¿Qué es SQLiteOpenHelper?
**R:** Es la clase de Android que administra la base SQLite. Te crea la base
la primera vez (`onCreate`), la abre/actualiza cuando cambia la versión
(`onUpgrade`) y te da `writableDatabase`/`readableDatabase` para insertar y
consultar. Mi clase `DbHelper` hereda de ella.

### P: ¿Qué tablas tenés y qué guarda cada una?
**R:** Ocho tablas:
- `users`: cuentas de dueños y trabajadores (contraseña cifrada).
- `parcels`: las parcelas/fincas con su código dinámico PALMA-XXXX.
- `memberships`: qué trabajador pertenece a qué parcela (relación N:M).
- `tasks`: labores con su tarifa por unidad (las crea el dueño).
- `reports`: avances de trabajo con foto, GPS y valor.
- `materials`: pedidos de insumos con precio estimado.
- `diagnoses`: resultados del escáner de palma.
- `prices`: catálogo de precios promedio de mercado.

### P: ¿Qué es onCreate y onUpgrade? ¿Por qué migraste y no borraste?
**R:** `onCreate` corre solo la primera vez que se crea la base (ahí creo
las 8 tablas). `onUpgrade` corre cuando subo la versión de la base. La base
va por la **versión 3**: la 2 agregó la columna `synced` (modo offline) y la
3 agregó la columna `paid` a materials (los insumos pagados). Lo hice con
`ALTER TABLE ... ADD COLUMN paid INTEGER NOT NULL DEFAULT 0` en vez de borrar
y recrear, porque el DROP TABLE le volaría las cuentas y datos al usuario.
Con la migración real los datos viejos quedan intactos (los materiales
viejos quedan como "por pagar", que es lo honesto).

### P: ¿Cómo insertás un registro?
**R:** Con un `ContentValues` que es como un mapa columna→valor
(`put("status", "PENDING")`), y `writableDatabase.insert("reports", null,
values)`. Me devuelve el id de la fila nueva. En los inserts de
reports/materials/diagnoses también guardo `synced`: 1 si había internet al
crear (ya va "enviado") o 0 si no había (queda en la cola).

### P: ¿Cómo leés los datos de un Cursor?
**R:** La consulta `rawQuery()` devuelve un Cursor que apunta a las filas
resultado. Me muevo con `moveToFirst()`/`moveToNext()` y saco cada columna
por índice con `getInt`, `getString`, `getDouble`, `getLong` y
`getColumnIndexOrThrow("nombre_columna")`. Luego uso mappers privados
(`toUser`, `toReport`, etc.) que convierten la fila en un data class de
Kotlin, para que las pantallas trabajen con objetos y no con cursores.

### P: ¿Cómo evitás usuarios duplicados?
**R:** Antes de registrar chequeo con `phoneExists()` y `cedulaExists()`
que el celular o la cédula no existan ya; si existen devuelvo un error y no
inserto. Y para el login, `authenticate()` hace el WHERE por celular o
cédula Y contraseña cifrada a la vez.

---

## 6. MODO SIN INTERNET (la joya del proyecto)

### P: ¿Cómo sabe la app que no hay internet?
**R:** Con la clase `NetWatcher` que usa el `ConnectivityManager` del
sistema. Al arrancar la app registro un `NetworkCallback`: cuando el sistema
reporta `onAvailable` (hay red) o `onLost` (se cayó), el watcher actualiza
un `StateFlow<Boolean>` llamado `online`. El ViewModel colecta ese flujo y
reacciona a los cambios en vivo, sin estar haciendo consultas cada rato
(polling). Ojo: esto necesita el permiso `ACCESS_NETWORK_STATE` en el
manifest, sin él truena.

### P: ¿Qué es un StateFlow y collectAsState?
**R:** `StateFlow` es un flujo de datos reactivo de Kotlin que siempre tiene
un valor actual. El ViewModel lo expone (`online`) y en Compose lo convierto
a estado con `vm.online.collectAsState()`, así cuando cambia el valor la UI
se redibuja sola (el banner aparece/desaparece sin que yo refresque nada).

### P: ¿Cómo funciona la cola de envío?
**R:** Los tres tipos de registros "viajables" (reportes, materiales,
diagnósticos) tienen una columna `synced` en la base: 1 = ya enviado al
servidor, 0 = esperando señal. Cuando guardás algo sin internet queda en 0.
`DbHelper.pendingCount()` cuenta cuántos hay en 0 entre las tres tablas (es
el número que muestra el banner) y `DbHelper.markAllSynced()` los pone
todos en 1 — eso es el "envío".

### P: ¿Qué pasa exactamente cuando vuelve la señal?
**R:** El callback del NetWatcher actualiza el StateFlow, el ViewModel lo
detecta en su colector (compara el estado anterior `estaba` con el actual
para detectar transiciones), y llama `sincronizarPendientes()`: marca la
cola como enviada, refresca el contador del banner (queda en 0), recarga
la pantalla abierta con los datos ya sincronizados y tira la notificación
"Se enviaron N registro(s)". Todo automático, el usuario no toca nada.

### P: ¿Cómo mostrás las notificaciones y qué permiso necesita?
**R:** Con `SyncNotifier` uso `NotificationCompat.Builder` sobre un canal
`NotificationChannel` que creé con id `"estado_conexion"` e importancia
alta (`IMPORTANCE_HIGH`) para que se vea arriba. Las notificaciones son:
"Sin internet" (id fijo 1001) y "Se enviaron N registro(s)" (id 1002) —
ids fijos para que se actualicen en vez de acumularse. Desde Android 13
(API 33) el sistema exige el permiso `POST_NOTIFICATIONS`, así que lo pido
en el portón de permisos del inicio (`PermissionSplashScreen`), junto con
ubicación y cámara, usando `RequestMultiplePermissions`. En Android 12 o
menor no hace falta pedirlo.

### P: ¿Por qué el banner muestra la cantidad de registros?
**R:** Porque el ViewModel mantiene `pendientesEnvio` actualizado: cada vez
que se guarda un registro (reporte/material/diagnóstico) llama
`refrescarPendientes()` que consulta `pendingCount()` en la base. El
`OfflineBanner` (que está envolviendo TODO el NavHost) muestra ese número:
"Sin internet · 3 registro(s) esperando señal". Así el usuario sabe que no
perdió nada.

### P: ¿Dónde está el banner y por qué?
**R:** En `AppNav.kt`: metí el `NavHost` dentro de una Column y le puse el
`OfflineBanner` arriba. Como AppNav es el padre de todas las pantallas, el
banner se ve en cualquier pantalla de la app, no tuve que copiarlo en cada
una.

### P: ¿Por qué la pastilla "⏳ Falta enviar" solo sale en algunos registros?
**R:** En `OwnerReportsScreen` cada tarjeta revisa el campo `synced` del
registro: si es false (se creó sin señal), dibujo la pastilla. Cuando la
cola se envía y se recarga la pantalla, el campo queda en true y la pastilla
desaparece sola.

### P: ¿Esto ya envía a un servidor real?
**R:** No — es simulación local porque aún no hay backend. El "envío" por
ahora es marcar la fila como synced=1. Está hecho a propósito así: cuando
agregue Firebase, solo cambio lo de adentro de `markAllSynced()` en el
DbHelper y de `sincronizarPendientes()` en el ViewModel. La cola, el
contador, el banner y las notificaciones quedan funcionando igual.

---

## 7. SEGURIDAD Y SESIÓN

### P: ¿Cómo guardás las contraseñas?
**R:** Nunca en texto plano. En `Security.kt` uso `MessageDigest` con
SHA-256: le pego una "sal" (SALT) delante de la contraseña, saco el hash y
lo convierto a hexadecimal. Ese hash guardo en la base. Para validar en el
login, cifro lo que escribió el usuario con el mismo algoritmo y comparo
los hashes. El hash no se puede revertir: si alguien ve la base no
recupera la contraseña.

### P: ¿Qué es una "sal" (salt)?
**R:** Es un texto fijo que le agrego a la contraseña antes de hashear
(`SALT + password`). Evita que alguien con una tabla de hashes precalculados
(rainbow table) adivine contraseñas comunes. En mi caso la sal es fija
("agropalma-local-v1") porque la app es local; en producción con servidor
se usaría una sal aleatoria por usuario.

### P: ¿Cómo recuerda la app que ya iniciaste sesión?
**R:** Con `Session.kt` sobre `SharedPreferences` (un archivo
llavecito-valor que Android guarda en el almacenamiento privado de la app).
Guardo el `user_id` del usuario logueado. Cuando abrís la app de nuevo, el
`init` del ViewModel lee ese id, busca el usuario en la base y te lleva
derecho a tu panel. Al cerrar sesión, `clear()` borra la preferencia.

### P: ¿Qué es el código PALMA-XXXX y por qué vence?
**R:** Es el código de invitación de la parcela: "PALMA-" + 4 dígitos
aleatorios. Vence en 1 minuto por seguridad: si un trabajador le pasa el
código a alguien de afuera, ese código ya no sirve al rato. El dueño lo
puede renovar con un botón (`regenerateCode()`) y cada código nuevo
reemplaza al anterior en la tabla parcels. El trabajador se une con
`joinByCode()`: valida que el código exista y no esté vencido, y crea la
fila en `memberships`.

---

## 8. FOTOS Y GPS

### P: ¿Cómo tomás o elegís la foto de evidencia?
**R:** Con la API de resultados de actividad: creo dos launchers con
`rememberLauncherForActivityResult` — uno con
`ActivityResultContracts.PickVisualMedia()` para la galería y otro con
`TakePicture()` para la cámara (que escribe en un archivo temporal creado
con FileProvider). Cuando el usuario elige/toma la foto, el launcher me
devuelve la URI y la proceso. La cámara necesita el permiso `CAMERA`.

### P: ¿Dónde se guardan las fotos y por qué ahí?
**R:** `PhotoStorage.saveFromUri()` copia la foto desde la URI a una carpeta
privada de la app (`photos/`). Lo hago porque las URIs de la galería son
temporales (el sistema puede revocar el permiso o borrar la imagen) y la
foto es la evidencia del trabajo: la copia en el espacio privado garantiza
que el reporte siempre pueda mostrarla, incluso sin internet.

### P: ¿Cómo obtenés la ubicación GPS?
**R:** Con `LocationHelper`: uso el `LocationManager` del sistema y busco
la última posición conocida entre los providers GPS y red
(`GPS_PROVIDER`/`NETWORK_PROVIDER`). Necesita los permisos
`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`, que pido en la pantalla de
reporte antes de usarla. La coordenada se guarda con el reporte para que el
dueño sepa dónde se hizo la labor. (Además el PalmAnalyzer puede leer el
GPS de los metadatos EXIF de la foto si la cámara lo dejó.)

### P: ¿Y si no hay GPS disponible?
**R:** La app no explota: el reporte muestra "Sin GPS" y se guarda sin
coordenadas (la columna acepta null). La foto obligatoria sí es dura, pero
la ubicación es opcional.

---

## 9. ESCÁNER "IA" DE PALMA ENFERMA

### P: ¿Cómo funciona tu escáner de IA?
**R:** Son dos capas en `PalmAnalyzer.kt` + `PalmModelAnalyzer.kt`. Todo corre
en el teléfono, **sin internet ni API**:

**Capa 1 — modelo entrenado (la IA de verdad):**
1. `PalmModelAnalyzer` abre `assets/palm_disease.tflite`, un **MobileNetV3Small**
   entrenado con **1.583 fotos reales** de hojas de palma (transfer learning
   desde ImageNet). Pesa 1,08 MB y va dentro del APK.
2. Reduce la foto a 224×224, la pasa a píxeles RGB 0–255 y ejecuta el modelo con
   LiteRT (TensorFlow Lite), también local.
3. El modelo devuelve una probabilidad de **hoja sana vs. enferma**.
   En test acertó **91,0 %** con un split estricto por cajones (cajas que el
   modelo nunca vio); con split aleatorio daba 97,5 %, pero reporto el número
   honesto (ver `ml/README.md`).

**Capa 2 — reglas de color (el tipo y el respaldo):**
1. Cargo la foto en un `Bitmap` y la reduzco a 96×96 (para que sea rápido).
2. Recorro los píxeles y convierto cada RGB a HSV (tono/saturación/valor).
3. Con el tono cuento proporciones: verdes, amarillos, marrones y oscuros.
4. Con esos porcentajes aplico reglas: marrón/oscuro alto → Pudrición del
   Cogollo; amarillo alto → Clorosis / posible Amarillamiento Letal;
   marrón moderado → mancha tipo Curvularia; todo verde con poco brillo →
   estrés hídrico; verde sano → follaje sano.
5. `blendWithModel` mezcla las dos opiniones: si ambas coinciden sube la
   certeza; si el modelo ve enfermedad y el color no, avisa "posible afección
   temprana"; si el color ve daño y el modelo no, queda como caso dudoso.
6. Devuelvo un `DiagnosisResult` con enfermedad, certeza (%), gravedad,
   hallazgo, recomendación y el % del modelo, y lo guardo en `diagnoses`.

### P: ¿Y si el modelo no carga?
**R:** `PalmModelAnalyzer` devuelve `null` (celular viejo, archivo faltante,
error de memoria) y `PalmAnalyzer` sigue solo con las reglas de color. El
escáner nunca se queda sin responder.

### P: ¿De dónde salieron las fotos para entrenar?
**R:** Del dataset público *Image Dataset of Oil Palm Leaf Diseases*
(Mendeley Data, DOI `10.17632/cp65skff89.1`, licencia **CC BY 4.0**, autores
Rahmanto, Julianto y Arrahimi, 2025): 899 hojas enfermas y 684 sanas. El
dataset **no** va en el APK, solo el modelo entrenado. La atribución aparece
en la propia pantalla del escáner y en `ml/README.md`.

### P: ¿Por qué HSV y no RGB directo?
**R:** Porque en HSV el tono (hue) separa el color puro, y así un píxel
amarillo es amarillo sin importar si la foto salió más clara u oscura (el
brillo va en otro canal). Con RGB puro la iluminación de la foto me haría
clasificar mal.

### P: ¿Es un diagnóstico confiable?
**R:** Es orientativo. La app misma lo aclara en la pantalla: no reemplaza
al ingeniero agrónomo ni al laboratorio — la recomendación incluye qué
muestra enviar al lab. El modelo público solo distingue **sana/enferma**, no
las 5 enfermedades; por eso el tipo lo ponen las reglas de color. Además las
fotos del dataset son hojas recortadas: en campo, con fondo y otra luz, puede
fallar más. Los números: **91 % de exactitud** con split estricto por cajones
(cajas que el modelo nunca vio) y 97,5 % con split aleatorio; reporto el 91 %
porque es el honesto. Lo siguiente es reentrenar con fotos etiquetadas de la
finca.

---

## 10. REGLAS DE NEGOCIO (el "para qué" de la app)

### P: ¿Cómo se calcula lo que se le paga al trabajador?
**R:** Cada labor (task) tiene una tarifa por unidad (ej. $2.600 por
palma podada). El reporte lleva la cantidad hecha, y el valor es
`cantidad × tarifa` (en el insert: `amount = units * task.ratePerUnit`).
El pago estimado se ve en vivo en la pantalla mientras el trabajador sube
la cantidad. El dueño ve esos montos en "Por pagar" hasta que marca el
reporte como pagado.

### P: ¿Qué estados manejan reportes y materiales?
**R:** Los reportes: `PENDING` (pendiente) → `PAID` (pagado) cuando el dueño
lo liquida. Los materiales: `PENDING` → `DELIVERED` (entregado). El estado
se guarda como texto en la base y la UI lo traduce a español bonito con
`Fmt.statusLabel()` y lo pinta con la pastilla de color correspondiente.

### P: ¿Cómo se calculan los costos del dueño?
**R:** En `computeStats()`: sumo los montos de reportes PENDING (por
pagar), el costo de materiales PENDING (insumos), cuento los reportes de
hoy y los trabajadores vinculados. La pantalla de Costos también muestra el
saldo por trabajador (pendiente/pagado) y el catálogo de precios promedio
de mercado que sirve para estimar el valor de cada pedido de material.

### P: ¿Cómo se vincula un trabajador a una parcela?
**R:** El dueño crea la parcela (con nombre, municipio y hectáreas), la app
le genera automáticamente 4 labores base (poda, cosecha, plateo y aplicación
fitosanitaria) con sus tarifas y el primer código PALMA-XXXX. El trabajador
se registra, entra a "Tengo un código", escribe el código vigente y se crea
la fila en `memberships`. Desde ahí ve las labores y puede reportar sobre
esa parcela.

### P: ¿Qué pasa si el trabajador se queda sin internet a mitad del reporte?
**R:** Nada malo: el reporte se guarda igual (todo es local), solo queda
con `synced=0` esperando señal. El contador del banner sube, el dueño ve la
pastilla "⏳ Falta enviar" y cuando vuelve la señal se envía todo solito.
Ese fue el requisito clave del proyecto para el campo.

---

## 11. RÁPIDAS (trivia de esas que agarran de sorpresa)

**P: ¿Qué es un data class?**
R: Una clase de Kotlin que solo transporta datos; te genera equals,
toString, copy, etc. Todos mis modelos (User, Parcel, WorkReport...) son
data classes.

**P: ¿Qué es un enum class?**
R: Un tipo con valores fijos. Tengo `Role { OWNER, WORKER }` para los dos
roles de la app — mejor que strings porque el compilador me protege de
escribir "OWENR".

**P: ¿Qué es un object en Kotlin?**
R: Un singleton: una clase con una sola instancia global. `SyncNotifier` y
`Security` son objects — no necesito construirlos, los uso directo.

**P: ¿Por qué los ids son Long?**
R: Porque SQLite guarda los ids como INTEGER de 8 bytes y AUTOINCREMENT
puede crecer mucho; Long es el tipo de Kotlin que mapea eso sin desbordarse.

**P: ¿Qué es null safety y dónde la usás?**
R: El sistema de tipos de Kotlin que separa lo que puede ser null. Uso
`currentUser?`, `activeParcel?`, `photoPath: String?` — la cámara/foto
opcional, el usuario que puede no estar logueado. Con `?.let` o `?: 0L`
manejo los casos vacíos.

**P: ¿Qué es un callback / lambda onResult?**
R: Una función que le paso a otra para que me avise cuando termine. Mis
métodos del ViewModel reciben `onResult: (String?) -> Unit`: si viene null
todo salió bien, si viene un string es el mensaje de error que muestro en
rojo.

**P: ¿Qué es Coil?**
R: La librería de carga de imágenes que uso en las tarjetas (`AsyncImage`):
carga las fotos de evidencia desde el archivo, con cache y placeholder
mientras carga.

**P: ¿Qué es Fmt.kt?**
R: Los formateadores: money (pesos con separador de miles), number, date,
coordinates (coordenadas GPS legibles) y statusLabel (PENDING→Pendiente,
etc.). Centralizarlo evita que cada pantalla formatee distinto.

**P: ¿Qué es el AndroidManifest.xml?**
R: El archivo que le describe mi app al sistema Android: nombre del
proceso, la Activity principal, y todos los permisos que necesita:
CÁMARA, ubicación fina/bracearia, `ACCESS_NETWORK_STATE` (para el vigilante
de señal) y `POST_NOTIFICATIONS` (avisos offline).

**P: ¿Qué versión mínima de Android soporta y por qué?**
R: Android 8.0 (API 26) porque uso `NotificationChannel` (que nació en API
26) y Compose va cómodo ahí. Y el pedido de permiso de notificaciones lo
condiciono con `Build.VERSION.SDK_INT >= TIRAMISU` porque solo Android 13+
lo exige.

**P: Si giras el teléfono, se pierde lo que escribiste?**
R: No: los campos usan `remember` (sobrevive recomposiciones) y el estado
importante vive en el ViewModel, que sobrevive al cambio de configuración.
Lo que se pierde con giro en Compose puro se maneja con
`rememberSaveable` en los formularios.

**P: ¿Cómo probarías todo el modo offline?**
R: Ya lo probé: en el emulador apagué wifi y datos (`svc wifi disable` /
`svc data disable`), la app avisó con notificación y banner, un trabajador
reportó trabajo sin señal (quedó "1 registro esperando"), el dueño vio la
pastilla "Falta enviar", y al prender el wifi se envió todo solo con su
notificación de éxito.

---

## 12. PERMISOS AL ARRANCAR (el "portón")

### P: ¿Por qué pides los permisos al INICIO y no cuando se usan?
**R:** Porque es una app de campo: mejor que el usuario dé los permisos de
una al arrancar, antes de estar en plena jornada sin señal y que le salten
diálogos a media labor. `PermissionSplashScreen` es la primera pantalla:
lista los 3 permisos con icono y explicación de para qué sirve cada uno, y
con un botón los pide todos. Igual no es obligatorio: hay un "Continuar sin
permisos" y la app los vuelve a pedir en el momento justo si faltan.

### P: ¿Cómo pedís varios permisos de un solo golpe?
**R:** Con el contrato `ActivityResultContracts.RequestMultiplePermissions()`
y un `rememberLauncherForActivityResult`. Le paso el array de permisos
faltantes y Android va sacando los diálogos uno detrás de otro. En el callback
sigo adelante sin importar la respuesta — no bloqueo la app.

### P: ¿Qué permisos pedís exactamente?
**R:** Ubicación precisa y aproximada (`ACCESS_FINE_LOCATION` +
`ACCESS_COARSE_LOCATION`, van juntos como manda la documentación), cámara
(`CAMERA`) y notificaciones (`POST_NOTIFICATIONS`, solo si el teléfono es
Android 13 o más nuevo, porque en versiones anteriores no se pide).

### P: ¿Por qué la pantalla de permisos no vuelve a aparecer?
**R:** Antes de dibujar nada, la pantalla chequea con
`ContextCompat.checkSelfPermission()` cuáles permisos faltan. Si la lista de
faltantes queda vacía, un `LaunchedEffect` llama directo al `onListo()` y se
brinca la pantalla — el usuario ni la ve. Además, en MainActivity uso
`rememberSaveable` para que el "ya pasé por el portón" sobreviva al giro de
pantalla.

### P: ¿Qué pasa si el usuario rechaza los permisos?
**R:** Nada grave: la app entra igual (degradación elegante). La ubicación
queda "Sin GPS" en el reporte, las notificaciones no salen (el SyncNotifier
checa el permiso antes de tirarlas), y la cámara se vuelve a pedir justo
cuando toca "Tomar foto".

### P: ¿Por qué agregaste el permiso CAMERA al manifest si la cámara ya funcionaba?
**R:** Porque `TakePicture` con `FileProvider` puede funcionar sin permiso,
pero al DECLARAR `CAMERA` en el manifest Android exige tenerlo concedido
antes de lanzar el intent — si no, truena con `SecurityException`. Como
ahora lo pedimos en el portón, declaré el permiso y además blindé el botón
de cámara en `PhotoEvidence`: chequea el permiso antes de abrir, y si no lo
tiene lo pide en el momento y abre la cámara al concederlo.

### P: ¿Qué es LaunchedEffect?
**R:** Un efecto de Compose que corre código suspendido cuando entra en
composición (y cuando cambia su clave). Lo uso para el auto-salto del
portón cuando no falta ningún permiso, y en las pantallas para cargar datos
al llegar a ellas.
