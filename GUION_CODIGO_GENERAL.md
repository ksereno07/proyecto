# GUION GENERAL — EXPLICAR TODO EL CÓDIGO DE AGROPALMA

**Duración completa:** 10–12 min. **Versión express:** al final, 3 min.
Habla en primera persona, sin tecnicismos de más. Cuando digas un archivo, ábrelo en Android Studio y señálalo.

---

## 1. Pitch inicial (30 seg)

> "AgroPalma es una app Android para administrar una finca de palma. Tiene dos roles: **dueño** y **trabajador**. El dueño crea la parcela, genera un código, ve reportes, materiales, salud del cultivo y pagos. El trabajador se une con el código, reporta labores con foto y GPS, pide insumos y escanea palmas enfermas.
>
> Está hecha en **Kotlin** con **Jetpack Compose**, guarda todo en **SQLite** y funciona **sin internet**. El proyecto tiene **37 archivos Kotlin** organizados en tres capas: datos, interfaz y utilidades."

---

## 2. Tecnologías y por qué (1 min)

| Pieza | Para qué | Por qué |
|---|---|---|
| **Kotlin** | Lenguaje de toda la app | Oficial de Android, corto y seguro |
| **Jetpack Compose** | Pantallas | UI en funciones que se redibuja sola al cambiar el estado |
| **Material 3** | Botones, tarjetas, diálogos | Look consistente y accesible |
| **Navigation Compose** | Rutas entre pantallas | Maneja el atrás y los argumentos sin código manual |
| **ViewModel (MVVM)** | Cerebro y estado | Separa la lógica de la UI y sobrevive a rotaciones |
| **SQLite + SQLiteOpenHelper** | Datos de la finca | Local, rápido, sin señal |
| **SharedPreferences** | Sesión y ajustes | Dos datos pequeños no merecen una tabla |
| **Corrutinas** | Trabajo en segundo plano | La base de datos no congela la pantalla |
| **Coil** | Fotos de evidencia | Carga imágenes de forma eficiente |
| **TensorFlow/Keras** | Entrenar el modelo de hojas | Transfer learning con MobileNetV3Small |
| **LiteRT (TensorFlow Lite)** | Correr el modelo en el teléfono | Inferencia local, sin internet, 1,08 MB |
| **SHA-256** | Contraseñas | Nunca se guardan en texto plano |

> "No usé Firebase ni un backend porque el escenario es el campo: **sin señal**. La app queda preparada para sincronizar después."

---

## 3. Arquitectura: cómo viaja un dato (1 min)

> "El flujo siempre es el mismo:
>
> **Pantalla → ViewModel → DbHelper → SQLite → ViewModel → pantalla.**
>
> La pantalla nunca consulta la base de datos directamente. Llama a una función del ViewModel; el ViewModel ejecuta la consulta en segundo plano con una corrutina y actualiza una variable de estado; y como esa variable es `mutableStateOf`, **Compose redibuja solo**.
>
> Ejemplo real: el dueño marca un insumo como pagado. La pantalla llama a `vm.setMaterialPaid(...)`, el ViewModel llama a `db.setMaterialPaid(...)`, SQLite cambia la columna `paid`, el ViewModel recarga el resumen, cambia `paymentsSummary` y la gráfica se actualiza sin que yo toque la UI."

---

## 4. Estructura de archivos, uno por uno (3 min)

### Raíz — `com.agropalma.app`

- **`MainActivity.kt`** → punto de entrada. Carga el tema, crea el ViewModel compartido y muestra primero la pantalla de permisos; al aceptar, abre la navegación.
- **`AppViewModel.kt`** → el archivo central. Guarda el estado (usuario, parcela, reportes, materiales, diagnósticos, precios, modo oscuro, resumen de pagos) y tiene todas las acciones: login, registro, crear parcela, reportar, pedir material, escanear, pagar, ajustes de cuenta.
- **`AppNav.kt`** → el mapa de rutas. Decide si arranca en login, panel de dueño o de trabajador según la sesión guardada, y envuelve todo con el banner de sin internet.

### `data/` — la capa de datos

- **`DbHelper.kt`** → la base de datos completa: crea las 8 tablas, migra versiones, inserta, consulta, actualiza y borra.
- **`Models.kt`** → `data class` que transportan datos: `User`, `Parcel`, `Task`, `WorkReport`, `MaterialRequest`, `Diagnosis`, `PriceItem`, `OwnerStats`, `WorkerTotal`, `PaymentsSummary`.
- **`Session.kt`** → guarda en SharedPreferences el **id del usuario logueado**, para no pedir login cada vez.
- **`AppSettings.kt`** → guarda el **modo oscuro** en SharedPreferences.

### `ui/theme/` — el look

- **`Color.kt`** → las dos paletas (clara y oscura) y los 14 colores `PalmX` que leen la paleta activa.
- **`Theme.kt`** → arma el tema Material 3, elige paleta según el modo e inyecta la paleta con `CompositionLocalProvider`.
- **`Type.kt`** → tamaños y pesos de letra, pensados para leerse en campo.

### `ui/components/` — piezas reutilizables

- **`Common.kt`** → barra superior, menú inferior, tarjetas, campos de texto, títulos, pastillas de estado, estado vacío.
- **`OfflineBanner.kt`** → cinta verde que aparece sin internet y muestra cuántos registros esperan.
- **`PaymentsChart.kt`** → gráfica de 4 barras, 100% Compose.
- **`PhotoEvidence.kt`** → tomar foto o elegir de galería, con vista previa y ruta del archivo.

### `ui/screens/` — las pantallas

**Entrada y cuentas**
- **`PermissionSplashScreen.kt`** → pide ubicación, cámara y notificaciones; se puede continuar sin permisos.
- **`LoginScreen.kt`** → entrar con celular o cédula + contraseña; elige rol y lleva al registro.
- **`RegisterOwnerScreen.kt`** → nombre, celular, correo, contraseña y finca.
- **`RegisterWorkerScreen.kt`** → nombre, cédula, celular, correo opcional y contraseña.
- **`ProfileScreen.kt`** → datos de la cuenta, parcela activa, cerrar sesión y entrar a Ajustes.
- **`SettingsScreen.kt`** → modo oscuro, editar nombre/correo/finca, cambiar contraseña y borrar cuenta.

**Dueño**
- **`OwnerHomeScreen.kt`** → resumen: parcela, código `PALMA-XXXX` con cuenta regresiva de 1 minuto, trabajadores, totales, últimos reportes y alertas.
- **`CreateParcelScreen.kt`** → crea la parcela y siembra las 4 labores base.
- **`OwnerReportsScreen.kt`** → tres pestañas: reportes, materiales y salud; permite marcar pagos y entregas.
- **`OwnerCostsScreen.kt`** → costos, gráfica de pagos, saldo por trabajador, insumos y catálogo de precios.

**Trabajador**
- **`WorkerHomeScreen.kt`** → lista de parcelas a las que se unió.
- **`WorkerJoinScreen.kt`** → unirse con el código del dueño.
- **`WorkerParcelScreen.kt`** → detalle: avance de labores, reportes, diagnósticos y accesos a reportar, pedir material y escanear.
- **`ReportWorkScreen.kt`** → labor, cantidad con botones grandes, foto obligatoria, GPS y pago estimado.
- **`MaterialRequestScreen.kt`** → insumo del catálogo, cantidad, nota, precio promedio y total.
- **`ScannerScreen.kt`** → foto de la hoja, análisis local y resultado con recomendación.

### `util/` — ayudantes

- **`Security.kt`** → hash SHA-256 con sal para las contraseñas.
- **`PhotoStorage.kt`** → guarda fotos en la carpeta privada y crea `Uri` seguras con `FileProvider`.
- **`LocationHelper.kt`** → última ubicación conocida; si no hay permiso, el reporte queda "Sin GPS".
- **`NetWatcher.kt`** → vigila la red con `ConnectivityManager` y expone un `StateFlow<Boolean>`.
- **`PalmModelAnalyzer.kt`** → carga `palm_disease.tflite` (MobileNetV3Small entrenado con 1.583 fotos reales de hojas) y devuelve la probabilidad de que la hoja esté enferma. Todo offline.
- **`PalmAnalyzer.kt`** → combina las dos capas: el modelo dice sana/enferma y las reglas de color (96×96 + HSV) dicen el tipo (pudrición, clorosis, mancha foliar o estrés). Si el modelo no carga, quedan solo las reglas.
- **`SyncNotifier.kt`** → notificaciones de "sin internet" y "registros enviados".
- **`Fmt.kt`** → formato de pesos, fechas, coordenadas y estados en español.

---

## 5. La base de datos (2 min)

> "`DbHelper` hereda de `SQLiteOpenHelper` y crea `agropalma.db` en **versión 3**. Tiene 8 tablas:
>
> 1. **`users`** → cuentas, rol, cédula, celular, correo, finca y contraseña cifrada.
> 2. **`parcels`** → finca, dueño, hectáreas, código y vencimiento.
> 3. **`memberships`** → qué trabajador está en qué parcela.
> 4. **`tasks`** → labores con meta, unidad y tarifa por unidad.
> 5. **`reports`** → trabajo reportado, foto, GPS, valor y estado.
> 6. **`materials`** → insumos pedidos, precio, estado, pagado y sincronizado.
> 7. **`diagnoses`** → resultado del escáner de palma.
> 8. **`prices`** → catálogo de precios promedio, sembrado al crear la base.
>
> Para guardar uso `ContentValues` + `insert`. Para leer, `rawQuery` + `Cursor` y convierto cada fila a un modelo. Para actualizar, `update` con la columna y el id. Para operaciones delicadas, como borrar una cuenta, uso una **transacción**: o se borra todo, o no se borra nada.
>
> Y las migraciones están en `onUpgrade`: **v1→v2** agrega `synced` a reportes, materiales y diagnósticos; **v2→v3** agrega `paid` a materiales. Nada de borrar y recrear: los datos del usuario se conservan."

**Consultas clave que puedo mencionar:**
- `getOwnerStats()` → total por pagar, insumos pendientes, trabajadores, reportes de hoy y alertas.
- `getWorkerTotals()` → pendiente y pagado por cada trabajador.
- `getPaymentsSummary()` → los 4 números de la gráfica.
- `joinByCode()` → valida código, vencimiento y membresía duplicada.
- `deleteUser()` → borra en cascada dentro de una transacción.

---

## 6. Flujo completo del dueño (1.5 min)

> "1. Se registra con nombre, celular, correo y finca.
> 2. Crea la parcela: se guarda en `parcels`, se genera un código `PALMA-XXXX` que dura **1 minuto** y se crean las 4 labores base: poda, cosecha, plateo y aplicación fitosanitaria, cada una con su tarifa.
> 3. Comparte el código; el trabajador se une y aparece en `memberships`.
> 4. En Inicio ve el resumen y el código con cuenta regresiva; puede copiarlo o regenerarlo.
> 5. En Reportes ve los avances con foto y GPS, y puede marcarlos como pagados.
> 6. En Materiales ve los pedidos y su costo; puede marcarlos como entregados o pagados.
> 7. En Salud ve los diagnósticos del escáner.
> 8. En Costos ve la gráfica, el saldo por trabajador y el catálogo de precios.
> 9. En Perfil entra a Ajustes: tema, datos, contraseña o borrar cuenta."

---

## 7. Flujo completo del trabajador (1.5 min)

> "1. Se registra con cédula obligatoria, porque se usa para la planilla.
> 2. Se une a la parcela con el código del dueño; la app valida que exista, que no esté vencido y que no esté ya unido.
> 3. En su panel ve sus parcelas y el avance de cada labor.
> 4. Reporta trabajo: elige la labor, pone la cantidad con botones grandes, adjunta **foto obligatoria**, toma el GPS y ve el pago estimado antes de enviar.
> 5. Pide materiales: elige del catálogo, pone cantidad y nota, y ve el total con el precio promedio.
> 6. Escanea una palma: toma la foto y `PalmAnalyzer` corre el modelo entrenado + las reglas de color sin internet; el resultado se guarda y le llega al dueño.
> 7. Si no hay señal, todo se guarda igual y queda en la cola."

---

## 8. Cómo se guarda todo (2 min) ← la pregunta clave

> "Hay **cuatro formas de guardado**, y cada una tiene su razón:
>
> **1. Datos de la finca → SQLite (`agropalma.db`).**
> Usuarios, parcelas, membresías, labores, reportes, materiales, diagnósticos y precios. Es relacional, permite sumas, filtros y joins, y funciona sin internet.
>
> **2. Sesión → SharedPreferences (`agropalma_session`).**
> Solo guarda `user_id`. Al abrir la app, el ViewModel lo lee y te lleva directo a tu panel.
>
> **3. Preferencias → SharedPreferences (`agropalma_settings`).**
> Solo guarda `dark_mode`. Por eso el modo oscuro se recuerda aunque cierres la app.
>
> **4. Fotos → archivos privados.**
> `PhotoStorage` las guarda en la carpeta privada de la app; en SQLite solo se guarda la **ruta**, no la imagen. Así la base no se infla.
>
> **¿Y la nube?** Todavía no hay servidor. Dejé la cola lista: `synced = 0` significa pendiente y `synced = 1` enviado. `pendingCount()` alimenta el banner, y `markAllSynced()` se ejecuta al abrir la app con red o cuando `NetWatcher` detecta que volvió la señal. En producción solo se reemplaza esa marca por una llamada real a una API."

**Ejemplo concreto para defender:**
> "Cuando el trabajador guarda un reporte, `insertReport()` calcula `amount = unidades × tarifa`, pone `status = PENDING`, guarda foto y GPS, y pone `synced` en 1 si había internet o en 0 si no. Esa sola fila ya alimenta el panel del dueño, los costos y la gráfica."

---

## 9. Seguridad, permisos y modo offline (1 min)

- **Contraseñas:** `Security.hash()` aplica SHA-256 con sal fija. Se guarda el hash, nunca la contraseña. Al cambiar contraseña se verifica la actual comparando hashes.
- **Permisos:** ubicación (GPS del reporte), cámara (evidencia y escáner), estado de red (banner) y notificaciones (avisos). Si el usuario no los da, la app sigue funcionando y el reporte queda "Sin GPS".
- **Offline:** `NetWatcher` avisa en vivo; `OfflineBanner` muestra la cola; `SyncNotifier` notifica cuando se cae o vuelve la señal. Nada se pierde: todo se guarda local primero.

---

## 10. Lo nuevo que agregué (1.5 min)

> "**Modelo de IA entrenado:** el escáner ya no es solo reglas de color. Entrené un **MobileNetV3Small** con **1.583 fotos reales** del dataset público *Image Dataset of Oil Palm Leaf Diseases* (Mendeley Data, DOI 10.17632/cp65skff89.1, CC BY 4.0). El modelo va dentro del APK como `palm_disease.tflite` (1,08 MB) y corre en el teléfono con LiteRT, **sin internet**. Lo evalué con un split estricto por cajones (cajas que el modelo nunca vio) y acertó el **91,0 %**, con AUC 0,990; con split aleatorio daba 97,5 %, pero reporto el 91 % porque es el número honesto. Ahora `PalmModelAnalyzer` dice sana/enferma y `PalmAnalyzer` usa las reglas de color para decir **qué tipo** es; si el modelo no carga, las reglas siguen funcionando solas.
>
> **Gráfica de pagos:** `getPaymentsSummary()` suma en SQLite lo pagado y pendiente de cuadrilla e insumos; `PaymentsChart` dibuja 4 barras sin librerías. Se actualiza en vivo.
>
> **Insumo pagado:** columna `paid` en `materials`, con un switch en Costos y en Reportes; al cambiarla se recalcula la gráfica.
>
> **Modo oscuro:** dos paletas y `CompositionLocalProvider`; los colores `PalmX` cambian solos en todas las pantallas. La preferencia vive en `AppSettings`.
>
> **Ajustes de cuenta:** `SettingsScreen` permite editar nombre, correo y finca, cambiar contraseña verificando la actual, y borrar la cuenta en una transacción con doble confirmación."

---

## 11. Limitaciones y mejoras (30 seg)

> "Es un prototipo local: no tiene backend real, la sincronización es una cola marcada y el hash es básico para un prototipo. El modelo de IA es real y está entrenado, pero el dataset solo trae **sana/enferma**, no las 5 enfermedades; por eso el tipo lo ponen las reglas de color. Además las fotos del dataset son hojas recortadas y en campo puede fallar más. Lo siguiente sería Firebase o una API, un hash más fuerte como bcrypt, y **fotos etiquetadas de la finca** para reentrenar el modelo con las enfermedades exactas."

---

## 12. Cierre (20 seg)

> "En resumen: **Kotlin + Compose** para la interfaz, **MVVM** para ordenar, **SQLite** para los datos, **SharedPreferences** para sesión y ajustes, **archivos privados** para las fotos y una **cola `synced`** para lo offline. Todo pensado para que el trabajador lo use en el campo, sin señal y sin perder nada."

---

## Versión express (3 min)

Si te dan poco tiempo, di solo esto:

1. **Qué es:** app Android en Kotlin + Compose, offline-first, dos roles.
2. **Cómo está ordenada:** `data` (SQLite, modelos, preferencias), `ui` (pantallas, componentes, tema), `util` (hash, fotos, GPS, red, análisis).
3. **Cómo viaja un dato:** pantalla → ViewModel → DbHelper → SQLite → estado → Compose redibuja.
4. **Cómo se guarda:** SQLite para datos, SharedPreferences para sesión y modo oscuro, archivos para fotos, cola `synced` para la nube.
5. **Lo nuevo:** modelo de IA entrenado (91 % honesto por cajones), gráfica de pagos, insumo pagado, modo oscuro y ajustes de cuenta.
6. **Cierre:** funciona sin internet y los datos no se pierden.

---

## Preguntas probables (respuesta corta)

- **¿Por qué SQLite y no Firebase?** → No hay señal en la finca; SQLite siempre funciona y la cola queda lista para sincronizar.
- **¿Dónde se guarda la contraseña?** → Hash SHA-256 con sal en `users`; nunca texto plano.
- **¿Qué pasa al actualizar la app?** → `onUpgrade` migra v1→v2→v3 agregando columnas, sin borrar datos.
- **¿Cómo se refresca sola la pantalla?** → Estado `mutableStateOf`; al cambiar, Compose recompone.
- **¿Por qué el ViewModel?** → La lógica no vive en la UI y el estado sobrevive a rotaciones.
- **¿Cómo se une un trabajador?** → `joinByCode()` valida existencia, vencimiento de 1 minuto y duplicados.
- **¿De dónde salen los números de la gráfica?** → De `getPaymentsSummary()`, que suma en SQLite.
- **¿Qué pasa si no hay internet?** → Todo se guarda con `synced = 0`, aparece en el banner y se marca al volver la señal.
- **¿Cómo se borra una cuenta?** → `deleteUser()` en una transacción borra usuario, parcelas y todo lo relacionado.

---

## Demo sugerida (1 min)

1. Login dueño: **3001234567 / palma123**.
2. **Inicio** → código, trabajadores y totales.
3. **Reportes** → pestañas y marcar un reporte como pagado.
4. **Costos** → gráfica y switch de insumo pagado (la gráfica cambia).
5. **Perfil → Ajustes** → modo oscuro y edición de datos.
6. Login trabajador: **3109876543 / palma123** → reportar trabajo, pedir material y escanear.
