# GUION — EXPLICAR EL CÓDIGO DE AGROPALMA

**Duración:** 6–7 minutos. Todo en palabras sencillas, como si le explicaras a un compañero.

---

## 1. Apertura (20 seg)

> "AgroPalma es una app Android para manejar una finca de palma. Está hecha en **Kotlin** con **Jetpack Compose**, y es **offline-first**: todo se guarda en el teléfono, no necesita internet para funcionar. El código está separado en tres capas: **datos**, **lógica** y **pantallas**. Eso se llama **MVVM** y es lo que hace que el proyecto no sea un solo archivo gigante."

---

## 2. Cómo está organizado (30 seg)

Señala las tres carpetas en Android Studio:

> "Dentro de `com.agropalma.app` hay tres carpetas:
>
> - **`data/`** → la base de datos, los modelos y las preferencias. Aquí se guarda todo.
> - **`ui/`** → las pantallas, los componentes reutilizables y el tema (colores).
> - **`util/`** → ayudas: cifrar contraseñas, guardar fotos, GPS, detectar internet.
>
> La regla es simple: **las pantallas no tocan la base de datos**. Las pantallas hablan con el ViewModel, y el ViewModel habla con los datos. Así, si cambio algo de la base de datos, no tengo que tocar 20 pantallas."

---

## 3. Recorrido del código, en orden de uso (2 min)

**a) `MainActivity.kt` — la puerta de entrada**
> "Android abre esto cuando tocas el ícono. Carga el tema, crea el ViewModel compartido (el cerebro) y muestra primero la pantalla de permisos (ubicación, cámara, notificaciones). Cuando el usuario acepta, arranca la navegación."

**b) `ui/AppNav.kt` — el mapa de pantallas**
> "Aquí están todas las rutas: login, registro de dueño, registro de trabajador, panel del dueño, panel del trabajador, escáner, etc. Al abrir, mira si hay sesión guardada: si es dueño lo manda a `owner/home`, si es trabajador a `worker/home`, y si no hay nadie, al login. También envuelve toda la app con el **banner de sin internet**."

**c) `AppViewModel.kt` — el cerebro**
> "Guarda el estado de la app: usuario actual, parcela, reportes, materiales, modo oscuro, etc. Todas esas variables son `mutableStateOf`, y eso es clave: cuando cambian, **Compose redibuja la pantalla solo**. El ViewModel también tiene las acciones: login, registrar, crear parcela, reportar trabajo, pedir material, cambiar contraseña… Y usa **corrutinas** (`viewModelScope` + `Dispatchers.IO`) para que la base de datos trabaje en segundo plano y la pantalla no se congele."

**d) `data/DbHelper.kt` — la base de datos**
> "Es la clase más grande. Hereda de `SQLiteOpenHelper` y crea la base `agropalma.db` con sus tablas: usuarios, parcelas, tareas, reportes, materiales, diagnósticos y precios. Aquí están todas las consultas: insertar, leer, actualizar y borrar."

**e) `data/Models.kt` — los modelos**
> "Son `data class` simples: `User`, `Parcel`, `WorkReport`, `MaterialRequest`, `PaymentsSummary`… Sirven para pasar los datos ordenados entre la base de datos y las pantallas, en vez de andar pasando columnas sueltas."

**f) `ui/screens/` y `ui/components/` — las pantallas**
> "Cada pantalla es una función `@Composable`. Las piezas que se repiten (tarjetas, botones, barra superior, gráfica) están en `components/`, para no copiar y pegar."

**g) `util/` — los ayudantes**
> "`Security` cifra la contraseña con SHA-256, `PhotoStorage` guarda las fotos de evidencia, `NetWatcher` avisa si hay internet, `PalmModelAnalyzer` corre el modelo de IA entrenado, `PalmAnalyzer` combina ese modelo con las reglas de color y `SyncNotifier` manda las notificaciones."

---

## 4. Cómo se guarda todo (2 min) ← la parte importante

> "Aquí hay que diferenciar **dos tipos de guardado**, porque no es lo mismo un dato de la finca que una preferencia de la app."

**1. Los datos reales van en SQLite (`agropalma.db`)**
> "Cuando el trabajador reporta una labor, `DbHelper.insertReport()` arma un `ContentValues` con todos los campos y hace `insert("reports", ...)`. Lo mismo pasa con usuarios, parcelas, materiales y diagnósticos.
>
> Para leer, se hace una consulta con `rawQuery`, se recorre el `Cursor` y se convierte cada fila en un modelo con funciones como `toReport()`.
>
> Para actualizar, se usa `update(...)`: por ejemplo, cuando el dueño marca un insumo como pagado, solo se cambia la columna `paid` de ese material.
>
> Y para que no se pierda nada cuando actualizo la app, está `onUpgrade`: la base va en **versión 3**, y las migraciones de v1→v2 y v2→v3 agregan las columnas nuevas **sin borrar los datos viejos**."

**2. Las preferencias van en SharedPreferences**
> "Son cosas pequeñas que no son datos de la finca:
>
> - `Session` guarda el **id del usuario logueado** en `agropalma_session`. Por eso al cerrar y abrir la app te lleva directo a tu panel.
> - `AppSettings` guarda el **modo oscuro** en `agropalma_settings`, con la clave `dark_mode`. Por eso la app recuerda si la dejaste de noche.
>
> La diferencia es simple: SQLite es para datos grandes y relacionales; SharedPreferences es para ajustes pequeños tipo llave–valor."

**3. ¿Y la nube?**
> "No hay servidor todavía. Lo que hice fue dejar la app **preparada para sincronizar**: cada reporte, material o diagnóstico tiene una columna `synced`. Si se guardó sin internet, queda en `0` y aparece en el banner como pendiente. Cuando vuelve la señal, `markAllSynced()` los marca en `1` y avisa con una notificación. Es una **cola offline**: en producción solo se cambiaría esa marca por una llamada real a una API."

---

## 5. Por qué usé cada cosa (1 min)

| Tecnología | Para qué y por qué |
|---|---|
| **Kotlin** | Lenguaje oficial de Android, más corto y seguro que Java. |
| **Jetpack Compose** | Las pantallas se escriben en código con funciones; se actualizan solas cuando cambia el estado. |
| **Material 3** | Botones, tarjetas y diálogos con un look consistente y accesible. |
| **Navigation Compose** | Maneja las rutas y el botón atrás sin hacerlo a mano. |
| **ViewModel (MVVM)** | Separa la lógica de las pantallas y sobrevive a los giros de pantalla. |
| **SQLite + SQLiteOpenHelper** | Base de datos local, rápida y sin internet; ideal para el campo. |
| **SharedPreferences** | Guardar sesión y ajustes sin montar una tabla para dos datos. |
| **Corrutinas** | Consultas en segundo plano para que la UI no se congele. |
| **Coil** | Cargar las fotos de evidencia de forma eficiente. |
| **SHA-256** | Nunca guardar contraseñas en texto plano. |

---

## 6. Lo nuevo, en una frase cada uno (1 min)

> "**Gráfica de pagos:** es 100% Compose, sin librerías. `getPaymentsSummary()` suma en SQLite lo pagado y lo pendiente, y `PaymentsChart` dibuja las barras. Por eso cambia en vivo cuando marcas un insumo como pagado."
>
> "**Insumo pagado:** es una columna `paid` en la tabla `materials`. El switch llama a `setMaterialPaid()` y la gráfica se recalcula."
>
> "**Modo oscuro:** el tema tiene dos paletas. Con `CompositionLocalProvider` se inyecta la paleta activa, y los colores `PalmX` cambian solos en todas las pantallas. La elección se guarda en `AppSettings`."
>
> "**Ajustes de cuenta:** `SettingsScreen` permite editar nombre, correo y finca, cambiar contraseña (verificando la actual) y borrar la cuenta. El borrado va en una **transacción** para eliminar todo lo del usuario sin dejar datos huérfanos."

---

## 7. Cierre (20 seg)

> "En resumen: **Kotlin + Compose** para la interfaz, **MVVM** para ordenar, **SQLite** para los datos de la finca, **SharedPreferences** para sesión y ajustes, y una **cola `synced`** para el tema offline. Todo pensado para que el trabajador pueda usarlo en el campo, sin señal, sin perder nada."

---

## Posibles preguntas del profe (respuesta corta)

- **¿Por qué SQLite y no Firebase?** → Porque en la finca no hay señal; SQLite funciona siempre. La app queda lista para sincronizar después.
- **¿Dónde se guarda la contraseña?** → Nunca en texto plano: se guarda el hash SHA-256 con sal en la tabla `users`.
- **¿Qué pasa si actualizo la app?** → `onUpgrade` migra la base de v1 a v2 y a v3 agregando columnas, sin borrar datos.
- **¿Por qué el ViewModel?** → Para que la lógica no viva en las pantallas y el estado sobreviva a rotaciones.
- **¿Cómo se refresca la pantalla sola?** → El estado es `mutableStateOf`; al cambiar, Compose recompone solo.
- **¿De dónde salen los números de la gráfica?** → De `getPaymentsSummary()`, que suma en SQLite los insumos pagados y los pendientes de la parcela.

---

## Demo sugerida (30 seg, opcional)

1. Login como dueño: **3001234567 / palma123**.
2. Ir a **Costos** → mostrar la gráfica y los totales.
3. Marcar un insumo como pagado → la gráfica sube en vivo.
4. Ir a **Perfil → Ajustes** → prender modo oscuro → cerrar y abrir la app para mostrar que se recuerda.
