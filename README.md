# AgroPalma 🌴

App Android para gestión de parcelas de palma: el **dueño** crea su grupo de
parcela, invita a su cuadrilla con un código dinámico y revisa reportes,
materiales y costos; el **trabajador** se une con el código, reporta su trabajo
con foto y GPS, pide materiales y escanea una palma enferma para recibir un
diagnóstico orientativo.

- **Lenguaje:** Kotlin + Jetpack Compose + Material 3
- **Base de datos:** SQLite local (`SQLiteOpenHelper`) — funciona sin internet
- **Paquete:** `com.agropalma.app`
- **Android mínimo:** 8.0 (API 26)
- **Compila con:** Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10

---

## 1. Abrir el proyecto

1. Abre **Android Studio** (el que ya tienes instalado).
2. `File → Open…`
3. Selecciona la carpeta:

   ```
   C:\Users\stevx\AndroidStudioProjects\AgroPalma
   ```

4. Espera a que Gradle sincronice (la primera vez descarga dependencias).
5. Conecta un teléfono con depuración USB o inicia un emulador.
6. Pulsa **Run ▶**.

El APK de prueba también queda generado en:

```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 2. Qué hace cada parte del código

Todos los archivos tienen un **comentario de cabecera** y comentarios internos
en español explicando qué hace cada bloque. Guía rápida:

| Archivo / carpeta | Responsabilidad |
|---|---|
| `MainActivity.kt` | Punto de entrada: carga el tema (claro/oscuro según ajustes), pide permisos y arranca la navegación. |
| `AppViewModel.kt` | Cerebro MVVM: estado y acciones (login, parcelas, reportes, materiales, escáner, cola sin internet, ajustes de cuenta y modo oscuro). |
| `data/DbHelper.kt` | Crea y consulta la base SQLite: usuarios, parcelas, membresías, labores, reportes, materiales (con marca de pagado), diagnósticos y precios. |
| `data/Models.kt` | Clases de datos que viajan entre la base y las pantallas (incluye `PaymentsSummary` para la gráfica). |
| `data/Session.kt` | Recuerda qué usuario tiene la sesión abierta. |
| `data/AppSettings.kt` | Recuerda el modo claro/oscuro elegido en Ajustes. |
| `util/Security.kt` | Cifra las contraseñas con SHA-256 + sal. |
| `util/NetWatcher.kt` | Vigila la señal con ConnectivityManager y avisa cuando cambia. |
| `util/SyncNotifier.kt` | Manda las notificaciones de "sin internet" y "se enviaron los pendientes". |
| `util/PalmModelAnalyzer.kt` | Carga `palm_disease.tflite` (MobileNetV3Small entrenado) y devuelve la probabilidad de hoja enferma. 100 % offline. |
| `util/PalmAnalyzer.kt` | Combina el modelo entrenado con las reglas de color: el modelo dice sana/enferma y las reglas dicen el tipo. |
| `util/PhotoStorage.kt` | Guarda las fotos y crea las URI seguras para la cámara. |
| `util/LocationHelper.kt` | Lee el GPS para adjuntarlo al reporte. |
| `util/Fmt.kt` | Formatea dinero, fechas, coordenadas y estados. |
| `ui/AppNav.kt` | Rutas de navegación entre todas las pantallas (envueltas por el banner offline). |
| `ui/screens/PermissionSplashScreen.kt` | Portón de permisos al arrancar: pide ubicación, cámara y notificaciones antes de entrar. |
| `ui/components/OfflineBanner.kt` | La cinta verde de "sin internet" que se pega arriba de toda la app. |
| `ui/components/PaymentsChart.kt` | La gráfica de barras de pagos (cuadrilla + insumos), 100% Compose sin librerías. |
| `ui/screens/SettingsScreen.kt` | Ajustes: modo oscuro, editar perfil, cambiar contraseña y eliminar cuenta. |
| `ui/theme/` | Paleta verde/mostaza/crema (con versión nocturna), tipografía y formas del diseño. |
| `ui/components/` | Barra superior/inferior, tarjetas, campos y captura de fotos reutilizables. |
| `ui/screens/` | Pantallas de login, registro, perfil, ajustes y paneles de dueño/trabajador. |

---

## 3. Flujo para probarlo

### Al abrir la app por primera vez
Antes de entrar aparece el **portón de permisos**: una pantalla que explica y
pide de una los 3 permisos que usa la app:

1. **Ubicación** — para adjuntar el GPS a los reportes de trabajo.
2. **Cámara** — para la foto de evidencia y el escáner de palma.
3. **Notificaciones** (Android 13+) — para los avisos de sin internet.

El botón **"Dar permisos y continuar"** dispara los diálogos del sistema en
cadena; al terminar entra a la app. También podés **"Continuar sin
permisos"**: la app te deja entrar igual y vuelve a pedirlos justo cuando los
necesites (p. ej. al tocar "Tomar foto"). Si ya diste todos los permisos, esa
pantalla se salta sola en las próximas aperturas.

### Dueño
1. `Soy Dueño → registrarse` con nombre, celular, correo, contraseña y finca.
2. **Crear grupo de parcela** (nombre, municipio, hectáreas).
3. En el panel aparece el código **PALMA-XXXX** con cuenta regresiva de 1 minuto.
   Se puede copiar o renovar.
4. En **Reportes** verá los avances enviados por la cuadrilla y podrá marcarlos
   como pagados; en **Costos** verá el total a pagar, la **gráfica de pagos**,
   los insumos (con botón de "pagado"), y precios promedio.

### Trabajador
1. Registrarse con cédula obligatoria.
2. `Tengo un código` → escribir el **PALMA-XXXX** vigente del dueño (vence en 1 min).
3. Dentro de la parcela puede:
   - **Reportar avance** (labor, cantidad, foto obligatoria y GPS).
   - **Pedir materiales** (insumo, cantidad, nota).
   - **Escanear palma** (foto → enfermedad probable, soluciones y muestra para laboratorio).

> El código cambia cada minuto: si el trabajador tarda, el dueño pulsa
> **Renovar** y le dicta el nuevo.

---

## 3.5 Modo sin internet (cola de envío automática)

La app **funciona completa sin señal** (todo es local) y cuando no hay internet:

- Aparece una **notificación arriba** que avisa: *"Sin internet — todo lo que
  registres queda guardado y se envía solo cuando vuelva la señal"*.
- Se muestra un **banner verde** pegado arriba de cualquier pantalla que dice
  cuántos registros van esperando señal (ej. *"Sin internet · 3 registro(s)
  esperando señal"*).
- Reportes de trabajo, solicitudes de materiales y diagnósticos del escáner se
  guardan en la **cola de envío** (columna `synced = 0` en la base). En el panel
  del dueño, esos registros llevan la pastilla **"⏳ Falta enviar"**.
- **Cuando vuelve la señal** todo se envía solito: el banner desaparece, las
  pastillas se quitan y sale la notificación *"Se enviaron N registro(s)"*.

¿Cómo funciona por dentro?

| Pieza | Qué hace |
|---|---|
| `util/NetWatcher.kt` | Usa `ConnectivityManager` + `NetworkCallback` para detectar en vivo si hay red. |
| Columna `synced` en SQLite | 1 = ya "enviado al servidor", 0 = esperando señal. Los registros viejos migraron a la cola (v2 de la base). |
| `DbHelper.pendingCount()` | Cuenta la cola (reportes + materiales + diagnósticos pendientes). |
| `DbHelper.markAllSynced()` | "Envía" la cola completa cuando vuelve la señal. |
| `util/SyncNotifier.kt` | Notificaciones del canal "Estado de la conexión" (pide permiso en Android 13+). |
| `AppViewModel` | Arranca el vigilante, escucha los cambios de señal y dispara el envío automático. |

> **Nota:** el "envío" por ahora es simulado localmente (aún no hay servidor
> real). Cuando agregues Firebase, solo hay que reemplazar lo de adentro de
> `markAllSynced()` y de `sincronizarPendientes()` en el ViewModel — la cola,
> el banner y las notificaciones ya quedan listos.

---

## 3.6 Gráfica de pagos, insumos pagados, modo oscuro y ajustes de cuenta

### Gráfica de pagos (Costos)
En la pantalla de **Costos** del dueño hay una tarjeta con una **gráfica de
barras** que combina la plata de la cuadrilla con la de los insumos en 4
columnas:

| Barra | Color | Qué representa |
|---|---|---|
| Cuadrilla pagada | Verde | Reportes ya marcados como PAID. |
| Cuadrilla por pagar | Mostaza | Reportes pendientes de pago. |
| Insumos pagados | Azul | Materiales con la marca de "pagado". |
| Insumos por pagar | Gris | Materiales que aún debe al proveedor. |

Debajo lleva una leyenda con los totales **"Ya pagado"** y **"Por pagar"**.
Está hecha 100% con Compose (`PaymentsChart.kt`), sin librerías de gráficas:
cada barra es un `Box` cuya altura es su proporción del valor máximo.

### Marcar materiales como pagados
El dueño puede decir qué insumos ya saldó con el proveedor desde **dos
lugares**:

- **Costos → "Insumos y sus pagos"**: cada pedido tiene el botón
  **"Marcar pagado"** (verde) que se vuelve **"Pagado" ✓** (con borde).
- **Reportes → pestaña Materiales**: la tarjeta del pedido lleva el botón
  **"Marcar como pagado"** / **"Pagado al proveedor"**.

Al marcar uno, la gráfica se actualiza al instante: la plata pasa de la barra
"por pagar" a la de "pagado". Se puede desmarcar si el dueño se equivocó.

### Modo oscuro
En **Perfil → Ajustes de cuenta** hay un switch de **modo oscuro** que cambia
toda la app a una paleta nocturna (fondos verde-negro, textos claros). La
preferencia se guarda en SharedPreferences (`agropalma_settings`), así que la
app recuerda el modo elegido al cerrarla y abrirla de nuevo.

### Ajustes de cuenta
La pantalla de **Ajustes** (desde el perfil, para dueños y trabajadores)
permite:

- **Editar perfil**: nombre, correo y nombre de la finca (solo dueños). El
  celular y la cédula no se editan porque son los identificadores de login.
- **Cambiar contraseña**: pide la contraseña actual (se verifica contra el
  hash guardado), la nueva y su repetición.
- **Eliminar cuenta**: borra TODO con doble confirmación — el perfil, sus
  parcelas si es dueño (labores, membresías, reportes, materiales,
  diagnósticos) o sus registros si es trabajador. Todo en una transacción
  de SQLite: o se borra todo o no se borra nada.

---

## 4. Dónde se guardan los datos

- Base SQLite: `agropalma.db` dentro del almacenamiento privado de la app
  (versión 3: materiales con columna `paid`).
- Sesión: `SharedPreferences` → `agropalma_session`.
- Ajustes (modo claro/oscuro): `SharedPreferences` → `agropalma_settings`.
- Fotos: carpeta privada `photos/` de la app.

Los datos **permanecen** aunque cierres la app. Se borran si desinstalas o si
limpias los datos de la aplicación.

### Nota sobre la nube
El proyecto está preparado para cambiar a Firebase después: todo el acceso a
datos está concentrado en `DbHelper.kt` y el `AppViewModel` solo llama a esos
métodos. Para sincronizar entre teléfonos habría que reemplazar esa capa por
Firestore/Realtime Database y autenticación, sin rehacer las pantallas.

---

## 5. Escáner de palma (modelo de IA entrenado)

El escáner funciona **sin internet ni API key** y combina dos capas:

### Capa 1 — Modelo entrenado de verdad
`PalmModelAnalyzer.kt` carga `app/src/main/assets/palm_disease.tflite`
(**MobileNetV3Small** con transfer learning desde ImageNet, 1,08 MB):

- Entrenado con **1.583 fotos reales** de hojas de palma de aceite.
- Dataset: *Image Dataset of Oil Palm Leaf Diseases* (Mendeley Data,
  DOI [10.17632/cp65skff89.1](https://doi.org/10.17632/cp65skff89.1), **CC BY 4.0**).
- Entrada 224×224, salida: probabilidad de que la hoja esté **enferma**.
- Métricas honestas (test por **cajones completos**, 354 fotos no vistas):
  **91,0 % de exactitud**, 86,5 % de recall de enfermas y AUC 0,990.
  En TFLite el resultado coincide 98,3 % con Keras. Con split aleatorio (238
  fotos) daba 97,5 %, pero ese número es más optimista; el de 91 % es el que se
  reporta. El detalle está en `ml/README.md`.

### Capa 2 — Reglas de color (tipo de enfermedad y respaldo)
`PalmAnalyzer.kt` reduce la foto a 96×96, la pasa a HSV y cuenta verdes,
amarillos, marrones y oscuros para decidir el **tipo**:

- Pudrición del Cogollo (tejido marrón/oscuro).
- Clorosis / posible Amarillamiento Letal (amarillo alto).
- Mancha foliar tipo Curvularia (marrón moderado).
- Estrés hídrico o daño mecánico.
- Follaje sano.

La app mezcla las dos opiniones: si ambas coinciden sube la certeza; si el
modelo ve enfermedad pero el color aún no, avisa de una **posible afección
temprana**; si las reglas ven daño pero el modelo dice sana, marca el caso como
**dudoso**. Si el modelo no carga (celular viejo o archivo faltante), el escáner
sigue funcionando solo con las reglas.

**Entrenamiento reproducible:** el script está en `ml/train_palm_model.py` y los
resultados (métricas, matriz de confusión y `.tflite`) en `ml/resultados/`.
Ver `ml/README.md` para las instrucciones y la atribución del dataset.

Es un apoyo orientativo: la app aclara que no reemplaza al ingeniero agrónomo
ni al laboratorio.

---

## 5.5 Cuentas de prueba (ya creadas en el emulador)

| Rol | Usuario | Contraseña |
|---|---|---|
| **Dueño** | Carlos Mendoza — celular **3001234567** | `palma123` |
| **Trabajador** | Pedro Perez — celular **3109876543** o cédula **1001234567** | `palma123` |

Ya están vinculados a la parcela **Finca La Palma** (Montería, 42 ha, código
base PALMA-5492), con 2 reportes de poda, 1 solicitud de NPK 15-15-15 y 1
diagnóstico de salud en el escáner.

---

## 6. Próximos pasos recomendados

1. **Firebase** para que dueño y trabajador compartan datos entre teléfonos.
2. **Fotos propias de la finca etiquetadas** para reentrenar el modelo con las 5 enfermedades exactas (el dataset público solo trae sana/enferma).
3. Editor de labores/tarifas desde el panel del dueño.
4. Comprobantes de pago en PDF y exportación a Excel.
5. Firma digital o QR del comprobante de trabajo.
