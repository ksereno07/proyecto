/*
 * AppViewModel.kt - CEREBRO DE LA APLICACION (patron MVVM)
 * ---------------------------------------------------------
 * Esta clase guarda el estado que muestran todas las pantallas y ejecuta
 * las acciones del usuario. Es el puente entre la interfaz (Compose) y la
 * base de datos local (DbHelper).
 *
 * Estado que mantiene:
 *  - currentUser: usuario que tiene la sesion abierta (dueno o trabajador).
 *  - ownerParcel / ownerStats / ownerWorkers / ownerReports / ownerMaterials:
 *    datos del panel del dueno.
 *  - workerParcels / activeParcel / activeTasks / activeReports: datos del
 *    trabajador y de la parcela que esta consultando.
 *  - prices: catalogo de precios promedio de mercado.
 *
 * Acciones principales:
 *  - login, registerOwner, registerWorker: autenticacion y registro.
 *  - createParcel, regenerateCode, joinParcel: grupos y codigos de invitacion.
 *  - reportWork, requestMaterial, analyzeAndSaveDiagnosis: trabajo del obrero.
 *  - setReportStatus, setMaterialStatus: aprobar/pagar/entregar desde el dueno.
 *
 * Todas las consultas a SQLite se ejecutan en Dispatchers.IO para que la
 * interfaz nunca se congele. Al terminar cada accion se recargan los datos
 * para que las pantallas se actualicen solas.
 */

// el paquete raíz de la app, aquí vive todo el chorro
package com.agropalma.app

// import de la Application, el contexto global de la app
import android.app.Application
// imports de Compose pa' los estados observables (la UI se refresca sola)
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// el ViewModel y su scope de corrutinas, la base del MVVM
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// la BD local y los modelos de datos que maneja la app
import com.agropalma.app.data.AppSettings
import com.agropalma.app.data.DbHelper
import com.agropalma.app.data.Diagnosis
import com.agropalma.app.data.JoinResult
import com.agropalma.app.data.MaterialRequest
import com.agropalma.app.data.OwnerStats
import com.agropalma.app.data.Parcel
import com.agropalma.app.data.PaymentsSummary
import com.agropalma.app.data.PriceItem
import com.agropalma.app.data.Role
import com.agropalma.app.data.Session
import com.agropalma.app.data.Task
import com.agropalma.app.data.User
import com.agropalma.app.data.WorkReport
import com.agropalma.app.data.WorkerTotal
// utilidades: análisis de palmas, vigilante de red, seguridad y notificaciones
import com.agropalma.app.util.DiagnosisResult
import com.agropalma.app.util.NetWatcher
import com.agropalma.app.util.PalmAnalyzer
import com.agropalma.app.util.Security
import com.agropalma.app.util.SyncNotifier
// corrutinas: IO pa' la BD, launch pa' no trabar la UI wey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// aquí arranca el cerebro MVVM que conecta la UI con la BD
class AppViewModel(application: Application) : AndroidViewModel(application) {

    // la BD local (SQLite): aquí se guarda todo
    private val db = DbHelper(application)
    // el que recuerda la sesión: sabe quién anda logueado
    private val session = Session(application)
    // los ajustes de la app: por ahora el modo oscuro/claro
    private val settings = AppSettings(application)
    // el hilo de IO pa' las queries, que no se congele la UI wey
    private val io = Dispatchers.IO
    // el vigilante de la señal: vive pendiente de si hay internet o no
    private val net = NetWatcher(application)
    // acceso corto a la Application (sirve pa' tirar las notificaciones)
    private val app = getApplication<Application>()

    // true = hay internet, false = sin señal. Lo lee el banner de arriba.
    val online = net.online

    // el usuario logueado ahorita (null = nadie ha entrado)
    var currentUser by mutableStateOf<User?>(null)
        private set

    // la parcela del dueño (null si todavía no crea ninguna)
    var ownerParcel by mutableStateOf<Parcel?>(null)
        private set
    // las estadísticas del panel del dueño
    var ownerStats by mutableStateOf(OwnerStats())
        private set
    // la cuadrilla: trabajadores que se unieron a su parcela
    var ownerWorkers by mutableStateOf<List<User>>(emptyList())
        private set
    // reportes de trabajo que mandan los compas del campo
    var ownerReports by mutableStateOf<List<WorkReport>>(emptyList())
        private set
    // solicitudes de materiales faltando por aprobar
    var ownerMaterials by mutableStateOf<List<MaterialRequest>>(emptyList())
        private set
    // diagnósticos de palmas enfermas (los del escáner)
    var ownerDiagnoses by mutableStateOf<List<Diagnosis>>(emptyList())
        private set
    // totales de lo que hay que pagarle a cada trabajador
    var ownerTotals by mutableStateOf<List<WorkerTotal>>(emptyList())
        private set

    // las parcelas donde trabaja el obrero
    var workerParcels by mutableStateOf<List<Parcel>>(emptyList())
        private set
    // la parcela que está viendo ahorita el trabajador
    var activeParcel by mutableStateOf<Parcel?>(null)
        private set
    // las labores de esa parcela
    var activeTasks by mutableStateOf<List<Task>>(emptyList())
        private set
    // los reportes hechos en esa parcela
    var activeReports by mutableStateOf<List<WorkReport>>(emptyList())
        private set
    // los diagnósticos de palmas de esa parcela
    var activeDiagnoses by mutableStateOf<List<Diagnosis>>(emptyList())
        private set

    // catálogo de precios promedio del mercado
    var prices by mutableStateOf<List<PriceItem>>(emptyList())
        private set

    // true = hay algo cargando (pa' el loader de la UI)
    var busy by mutableStateOf(false)
        private set

    // cuántos registros van en la cola esperando señal (modo sin internet)
    var pendientesEnvio by mutableStateOf(0)
        private set

    // el modo visual de la app: false = claro (de siempre), true = noche.
    // Arranca con lo que el user dejó elegido en Ajustes la última vez
    var darkMode by mutableStateOf(settings.darkMode)
        private set

    // el resumen de pagos de la finca: lo que alimenta la gráfica de barras
    // (pagado/pendiente de cuadrilla e insumos)
    var paymentsSummary by mutableStateOf(PaymentsSummary())
        private set

    // lo que corre al crear el ViewModel (arranque de la app)
    init {
        // si ya había sesión guardada, lo revivimos sin pedir login otra vez
        session.userId?.let { id ->
            // buscamos al usuario por su id en la BD
            val user = db.getUser(id)
            // si sí existe, lo dejamos logueado de nuevo
            if (user != null) {
                currentUser = user
                // dueño → panel del dueño, trabajador → su panel
                if (user.role == Role.OWNER) loadOwnerData() else loadWorkerData()
            }
        }
        // prendemos el vigilante de la señal UNA vez: vive mientras la app viva
        net.start()
        // creamos el canal de notificaciones pa' que luego no truene al avisar
        SyncNotifier.crearCanal(app)
        // estado inicial: con internet mandamos la cola pendiente; sin
        // internet avisamos con notificación que todo se guardará local
        if (net.hayRed()) sincronizarPendientes(avisar = true) else SyncNotifier.sinInternet(app)
        // actualizamos el contador del banner con lo que haya en la cola
        refrescarPendientes()
        // y nos quedamos escuchando la señal: cuando CAMBIE, reaccionamos
        viewModelScope.launch {
            // memoria de cómo venía la señal, pa' detectar cambios reales
            var estaba = net.online.value
            // cada vez que CAMBIA la red nos avisa aquí
            net.online.collect { ahora ->
                // volvió la señal → enviamos todo lo encolado y avisamos
                if (ahora && !estaba) sincronizarPendientes(avisar = true)
                // se cayó la señal → notificamos que no hay internet
                if (!ahora && estaba) SyncNotifier.sinInternet(app)
                // actualizamos la memoria pa' la próxima ronda
                estaba = ahora
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN 1: AUTENTICACIÓN (INICIAR SESIÓN Y REGISTRARSE)
    //  Propósito: entrar con celular/cédula + contraseña, crear cuentas
    //  de dueño o trabajador y dejar la sesión abierta para que la app
    //  arranque directo en el panel del rol correspondiente.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Inicia sesion con celular o cedula + contrasena.
     * La contrasena se cifra antes de compararla con la guardada.
     * onResult devuelve null si todo salio bien o un mensaje de error.
     */
    fun login(identifier: String, password: String, onResult: (String?) -> Unit) {
        // si dejó campos vacíos, le avisamos que no la cague
        if (identifier.isBlank() || password.isBlank()) {
            onResult("Escribe tu celular/cédula y tu contraseña.")
            return
        }
        // lanzamos la corrutina pa' no trabar la UI wey
        viewModelScope.launch {
            // prendemos el "cargando" mientras consultamos la BD
            busy = true
            // buscamos al usuario con la contraseña cifrada (nunca en texto plano)
            val user = withContext(io) { db.authenticate(identifier.trim(), Security.hash(password)) }
            // ya acabamos, apagamos el loader
            busy = false
            // si no apareció nadie, la cagó en la contraseña o el usuario
            if (user == null) {
                onResult("Celular/cédula o contraseña incorrectos.")
            } else {
                // guardamos la sesión pa' no volver a pedir login
                session.userId = user.id
                // y dejamos asignado el usuario logueado
                currentUser = user
                // cargamos su panel según su rol
                if (user.role == Role.OWNER) loadOwnerData() else loadWorkerData()
                // todo bien, sin error
                onResult(null)
            }
        }
    }

    // registra al dueño (con el nombre de su finca) y delega en register()
    fun registerOwner(
        // los datos del dueño: nombre, celular, correo, contraseña y finca
        name: String,
        phone: String,
        email: String,
        password: String,
        finca: String,
        onResult: (String?) -> Unit
    ) = register(Role.OWNER, name, null, phone, email, password, finca, onResult)

    // registra al trabajador (aquí la cédula sí es obligatoria)
    fun registerWorker(
        // datos del trabajador: nombre, cédula, celular, correo y contraseña
        name: String,
        cedula: String,
        phone: String,
        email: String,
        password: String,
        onResult: (String?) -> Unit
    ) = register(Role.WORKER, name, cedula, phone, email, password, null, onResult)

    /**
     * Valida los datos, evita celulares/cedulas duplicados, guarda la cuenta
     * y deja la sesion iniciada para entrar directo al panel del rol.
     */
    private fun register(
        // todo lo que se necesita pa' crear la cuenta, según el rol
        role: Role,
        name: String,
        cedula: String?,
        phone: String,
        email: String,
        password: String,
        finca: String?,
        onResult: (String?) -> Unit
    ) {
        // validación básica: nombre, celular y contraseña de 4+ caracteres
        if (name.isBlank() || phone.isBlank() || password.length < 4) {
            onResult("Completa nombre, celular y una contraseña de al menos 4 caracteres.")
            return
        }
        // el trabajador sin cédula no pasa, no la cagues
        if (role == Role.WORKER && (cedula.isNullOrBlank())) {
            onResult("La cédula es obligatoria para el trabajador.")
            return
        }
        // corrutina al aire, la BD se consulta en IO como debe ser
        viewModelScope.launch {
            // prendemos el loader
            busy = true
            // revisamos si el celular o la cédula ya están registrados
            val error = withContext(io) {
                when {
                    // celular repetido, mala suerte
                    db.phoneExists(phone.trim()) -> "Ese celular ya está registrado."
                    // cédula repetida, ese pendejo ya tiene cuenta
                    !cedula.isNullOrBlank() && db.cedulaExists(cedula.trim()) -> "Esa cédula ya está registrada."
                    // todo limpio, sin error
                    else -> null
                }
            }
            // si hubo duplicado, avisamos y salimos
            if (error != null) {
                busy = false
                onResult(error)
                return@launch
            }
            // todo limpio: insertamos al nuevo usuario en la BD
            val id = withContext(io) {
                // los datos del compa, con la contraseña ya cifrada (hash)
                db.insertUser(
                    role = role,
                    name = name.trim(),
                    cedula = cedula?.trim(),
                    phone = phone.trim(),
                    email = email.trim().ifBlank { null },
                    passwordHash = Security.hash(password),
                    finca = finca?.trim()
                )
            }
            // apagamos el loader
            busy = false
            // si el id salió mal, la BD nos falló
            if (id <= 0) {
                onResult("No se pudo crear la cuenta. Intenta de nuevo.")
                return@launch
            }
            // releemos el usuario ya guardado con su id de verdad
            val user = withContext(io) { db.getUser(id) }
            // sesión iniciada al toque
            session.userId = id
            currentUser = user
            // cargamos el panel de su rol
            if (role == Role.OWNER) loadOwnerData() else loadWorkerData()
            // cuenta creada, sin errores
            onResult(null)
        }
    }

    // cerrar sesión: borramos la sesión y dejamos el estado en blanco
    fun logout() {
        // le decimos al guardador que se olvide de quién era
        session.clear()
        // reseteamos todo el panel del dueño pa' que no quede data vieja
        currentUser = null
        ownerParcel = null
        ownerStats = OwnerStats()
        ownerWorkers = emptyList()
        ownerReports = emptyList()
        ownerMaterials = emptyList()
        ownerDiagnoses = emptyList()
        ownerTotals = emptyList()
        paymentsSummary = PaymentsSummary()
        // y también lo del trabajador, ahí nomás
        workerParcels = emptyList()
        activeParcel = null
        activeTasks = emptyList()
        activeReports = emptyList()
        activeDiagnoses = emptyList()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN 1.5: AJUSTES DE LA APP Y DE LA CUENTA
    //  Propósito: el modo oscuro/claro (se guarda pa' la próxima apertura)
    //  y las cosas de la cuenta: editar nombre/correo/finca, cambiar la
    //  contraseña y BORRAR la cuenta con todo lo suyo. Cuidaito con eso.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cambia el modo visual de la app y lo guarda en SharedPreferences,
     * pa' que la próxima vez que abra siga en el mismo modo.
     * (Se llama applyDarkMode y no setDarkMode pa' no chocar con el
     * setter privado que Kotlin le genera a la propiedad darkMode.)
     */
    fun applyDarkMode(on: Boolean) {
        // guardamos la preferencia (sobrevive a cerrar la app)
        settings.darkMode = on
        // y prendemos el estado que lee el tema de toda la UI
        darkMode = on
    }

    /**
     * Edita los datos de la cuenta: nombre, correo y finca (si es dueño).
     * El celular y la cédula NO se editan porque son los identificadores
     * del login. onResult devuelve null si quedó bien o el error.
     */
    fun updateProfile(name: String, email: String, finca: String?, onResult: (String?) -> Unit) {
        // sin user logueado no hay nada que editar
        val user = currentUser ?: return
        // el nombre no puede quedar vacío, obvio
        if (name.isBlank()) {
            onResult("El nombre no puede quedar vacío.")
            return
        }
        viewModelScope.launch {
            // loader ON mientras escribe en la BD
            busy = true
            // actualizamos la fila del user con los datos nuevos
            withContext(io) {
                db.updateUser(
                    id = user.id,
                    name = name.trim(),
                    // el correo vacío se guarda como null, pa' que quede limpio
                    email = email.trim().ifBlank { null },
                    // la finca solo aplica pa' dueños, si no, se queda null
                    finca = if (user.role == Role.OWNER) finca?.trim()?.ifBlank { null } else null
                )
            }
            // releemos el user ya guardado con los datos frescos
            val fresh = withContext(io) { db.getUser(user.id) }
            // loader OFF
            busy = false
            if (fresh != null) {
                // refrescamos el user actual y su panel (por la finca nueva)
                currentUser = fresh
                if (fresh.role == Role.OWNER) loadOwnerData() else loadWorkerData()
                // todo bien
                onResult(null)
            } else {
                // no debería pasar, pero por si acaso
                onResult("No se pudo actualizar el perfil.")
            }
        }
    }

    /**
     * Cambia la contraseña: primero verifica la ACTUAL (cifrada) y si
     * coincide, guarda la nueva. Nada de cambiarle la clave a otro, wey.
     */
    fun changePassword(current: String, nuevo: String, onResult: (String?) -> Unit) {
        // sin user logueado no hay contraseña que cambiar
        val user = currentUser ?: return
        // la nueva debe tener 4+ caracteres, igual que al registrarse
        if (nuevo.length < 4) {
            onResult("La nueva contraseña debe tener al menos 4 caracteres.")
            return
        }
        viewModelScope.launch {
            // loader ON
            busy = true
            // ciframos la actual y comparamos contra la guardada
            val ok = withContext(io) { db.checkPassword(user.id, Security.hash(current)) }
            // si la actual no coincide, ni pa' qué seguir
            if (!ok) {
                busy = false
                onResult("La contraseña actual no es correcta.")
                return@launch
            }
            // sí coincide: guardamos la nueva, ya cifrada por supuesto
            withContext(io) { db.updatePassword(user.id, Security.hash(nuevo)) }
            // loader OFF
            busy = false
            // quedó cambiada
            onResult(null)
        }
    }

    /**
     * BORRA la cuenta con TODO su contenido (parcelas si es dueño,
     * membresías y registros si es trabajador) y saca al user al login.
     * No hay vuelta atrás, por eso la pantalla pide confirmación doble.
     */
    fun deleteAccount(onResult: () -> Unit) {
        // sin user no hay nada que borrar
        val user = currentUser ?: return
        viewModelScope.launch {
            // loader ON mientras vuela todo
            busy = true
            // la BD borra al user y todo lo suyo en UNA transacción
            withContext(io) { db.deleteUser(user.id) }
            // loader OFF
            busy = false
            // borramos la sesión y reseteamos el estado igual que el logout
            logout()
            // y avisamos que ya quedó borrado (la pantalla manda al login)
            onResult()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN 2: PANEL DEL DUEÑO
    //  Propósito: crear la parcela, renovar el código de invitación,
    //  recargar todos los datos del panel (trabajadores, reportes,
    //  materiales, diagnósticos) y aprobar pagos / entregas.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Recarga TODO el panel del dueno en una sola pasada: parcela, totales,
     * trabajadores, reportes, materiales, diagnosticos y precios.
     */
    fun loadOwnerData() {
        // sin usuario logueado no hacemos nada
        val user = currentUser ?: return
        // y si no es dueño, aquí nomás nos salimos
        if (user.role != Role.OWNER) return
        // corrutina pa' que la UI no se congele
        viewModelScope.launch {
            // traemos la parcela del dueño desde la BD
            val parcel = withContext(io) { db.getOwnerParcel(user.id) }
            ownerParcel = parcel
            if (parcel != null) {
                // con parcela: sacamos TODO en una sola foto (snapshot)
                val snapshot = withContext(io) {
                    // las queries: stats, cuadrilla, reportes, materiales, diagnósticos y totales
                    OwnerSnapshot(
                        stats = db.getOwnerStats(parcel.id),
                        workers = db.getParcelMembers(parcel.id),
                        reports = db.getReports(parcel.id),
                        materials = db.getMaterials(parcel.id),
                        diagnoses = db.getDiagnoses(parcel.id),
                        totals = db.getWorkerTotals(parcel.id),
                        payments = db.getPaymentsSummary(parcel.id)
                    )
                }
                // vaciamos el snapshot a los estados pa' que la UI se refresque
                ownerStats = snapshot.stats
                ownerWorkers = snapshot.workers
                ownerReports = snapshot.reports
                ownerMaterials = snapshot.materials
                ownerDiagnoses = snapshot.diagnoses
                ownerTotals = snapshot.totals
                paymentsSummary = snapshot.payments
            } else {
                // sin parcela: todo en ceros y listas vacías, tan fácil
                ownerStats = OwnerStats()
                ownerWorkers = emptyList()
                ownerReports = emptyList()
                ownerMaterials = emptyList()
                ownerDiagnoses = emptyList()
                ownerTotals = emptyList()
                paymentsSummary = PaymentsSummary()
            }
            // y de yapa los precios del mercado
            prices = withContext(io) { db.getPrices() }
        }
    }

    // crea la parcela/grupo del dueño (nombre, ubicación y hectáreas)
    fun createParcel(name: String, location: String, hectares: String, onResult: (String?) -> Unit) {
        // sin usuario no hay nada que hacer
        val user = currentUser ?: return
        // convertimos las hectáreas a número (con coma o punto decimal)
        val ha = hectares.replace(",", ".").toDoubleOrNull() ?: 0.0
        // validamos que todo venga bien llenado
        if (name.isBlank() || location.isBlank() || ha <= 0.0) {
            onResult("Completa el nombre, la ubicación y una extensión válida.")
            return
        }
        viewModelScope.launch {
            // prendemos el loader
            busy = true
            // insertamos la parcela en la BD
            withContext(io) { db.createParcel(user.id, name.trim(), location.trim(), ha) }
            // apagamos el loader
            busy = false
            // recargamos el panel pa' que aparezca la parcela nueva
            loadOwnerData()
            // todo salió bien
            onResult(null)
        }
    }

    /**
     * Genera un codigo PALMA-XXXX nuevo para la parcela del dueno.
     * El anterior deja de servir de inmediato.
     */
    fun regenerateCode() {
        // sin parcela no hay código que renovar
        val parcel = ownerParcel ?: return
        viewModelScope.launch {
            // la BD genera el código nuevo y nos devuelve la parcela actualizada
            ownerParcel = withContext(io) { db.regenerateCode(parcel.id) }
        }
    }

    // el dueño aprueba o paga un reporte de trabajo
    fun setReportStatus(reportId: Long, status: String) {
        viewModelScope.launch {
            // actualizamos el estatus del reporte en la BD
            withContext(io) { db.setReportStatus(reportId, status) }
            // refrescamos el panel del dueño
            loadOwnerData()
            // y por si el trabajador está viendo esa parcela, también
            activeParcel?.let { loadParcelData(it.id) }
        }
    }

    // el dueño aprueba o entrega un material pedido
    fun setMaterialStatus(materialId: Long, status: String) {
        viewModelScope.launch {
            // actualizamos el estatus del material en la BD
            withContext(io) { db.setMaterialStatus(materialId, status) }
            // y recargamos el panel del dueño
            loadOwnerData()
        }
    }

    // el dueño marca un insumo como PAGADO (o lo desmarca si se equivocó)
    fun setMaterialPaid(materialId: Long, paid: Boolean) {
        viewModelScope.launch {
            // guardamos el nuevo estado de pago en la BD
            withContext(io) { db.setMaterialPaid(materialId, paid) }
            // y recargamos todo el panel (la gráfica de pagos también)
            loadOwnerData()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN 3: PANEL DEL TRABAJADOR
    //  Propósito: unirse a una parcela con el código del dueño, reportar
    //  el avance del trabajo con foto y GPS, pedir materiales y analizar
    //  palmas enfermas con el escáner.
    // ═══════════════════════════════════════════════════════════════════

    // carga las parcelas donde trabaja el obrero
    fun loadWorkerData() {
        // sin usuario logueado ni nos movemos
        val user = currentUser ?: return
        // y si no es trabajador, aquí nomás nos salimos
        if (user.role != Role.WORKER) return
        viewModelScope.launch {
            // traemos sus parcelas de la BD
            workerParcels = withContext(io) { db.getWorkerParcels(user.id) }
            // y también los precios del mercado
            prices = withContext(io) { db.getPrices() }
        }
    }

    // el obrero se une a una parcela con el código PALMA-XXXX del dueño
    fun joinParcel(code: String, onResult: (JoinResult) -> Unit) {
        // sin usuario no hay quien se una
        val user = currentUser ?: return
        viewModelScope.launch {
            // prendemos el loader
            busy = true
            // la BD intenta unirlo con el código que escribió
            val result = withContext(io) { db.joinByCode(code, user.id) }
            // apagamos el loader
            busy = false
            // si quedó dentro, refrescamos sus parcelas
            if (result.success) loadWorkerData()
            // y le contamos cómo le fue
            onResult(result)
        }
    }

    // carga todo lo de una parcela específica (la que abrió el trabajador)
    fun loadParcelData(parcelId: Long) {
        viewModelScope.launch {
            // si la parcela ya no existe, nos salimos de una
            val parcel = withContext(io) { db.getParcel(parcelId) } ?: return@launch
            // traemos labores, reportes y diagnósticos de esa parcela
            val tasks = withContext(io) { db.getTasks(parcelId) }
            val reports = withContext(io) { db.getReports(parcelId) }
            val diagnoses = withContext(io) { db.getDiagnoses(parcelId) }
            // y los asignamos a los estados activos
            activeParcel = parcel
            activeTasks = tasks
            activeReports = reports
            activeDiagnoses = diagnoses
            // si nunca se cargaron los precios, los traemos ahorita
            if (prices.isEmpty()) prices = withContext(io) { db.getPrices() }
        }
    }

    /**
     * Guarda el avance de trabajo del obrero. Antes de insertar valida:
     *  - que la cantidad sea mayor que cero,
     *  - que exista la foto de evidencia (obligatoria),
     * y luego calcula el pago con la tarifa de la labor.
     */
    fun reportWork(
        // la parcela, la labor, la cantidad y la evidencia con ubicación
        parcelId: Long,
        taskId: Long,
        units: Double,
        description: String,
        photoPath: String?,
        latitude: Double?,
        longitude: Double?,
        onResult: (String?) -> Unit
    ) {
        // sin usuario logueado no hay reporte
        val user = currentUser ?: return
        // la cantidad tiene que ser mayor a cero, obvio
        if (units <= 0.0) {
            onResult("Registra una cantidad mayor a cero.")
            return
        }
        // sin foto no hay reporte, la evidencia es obligatoria wey
        if (photoPath == null) {
            onResult("La foto de evidencia es obligatoria.")
            return
        }
        viewModelScope.launch {
            // loader ON
            busy = true
            val result = withContext(io) {
                // buscamos la labor pa' calcular el pago después
                val task = db.getTask(taskId)
                // si existe la labor, insertamos el reporte completo
                if (task == null) null else db.insertReport(
                    parcelId = parcelId,
                    workerId = user.id,
                    task = task,
                    units = units,
                    description = description.trim(),
                    photoPath = photoPath,
                    latitude = latitude,
                    longitude = longitude,
                    // online: con señal va sincronizado, sin señal va pa' la cola
                    online = net.online.value
                )
            }
            // loader OFF
            busy = false
            // si no se pudo guardar, avisamos
            if (result == null || result <= 0) {
                onResult("No se pudo guardar el reporte.")
            } else {
                // si estábamos sin señal, el contador de la cola sube en el banner
                refrescarPendientes()
                loadParcelData(parcelId)
                onResult(null)
            }
        }
    }

    // el obrero pide materiales pa' trabajar (fertilizante y demás)
    fun requestMaterial(
        // la parcela, el material, la cantidad, la nota y la foto
        parcelId: Long,
        item: String,
        quantity: Double,
        unit: String,
        note: String,
        photoPath: String?,
        onResult: (String?) -> Unit
    ) {
        // sin usuario logueado no hay solicitud
        val user = currentUser ?: return
        // validamos que el material y la cantidad vengan bien
        if (item.isBlank() || quantity <= 0.0) {
            onResult("Selecciona un material y una cantidad válida.")
            return
        }
        viewModelScope.launch {
            // loader ON
            busy = true
            val result = withContext(io) {
                // sacamos el precio promedio del mercado pa' ese material
                val price = db.priceFor(item)?.avgPrice ?: 0.0
                // insertamos la solicitud con todo y precio de referencia
                db.insertMaterial(
                    parcelId = parcelId,
                    workerId = user.id,
                    item = item,
                    quantity = quantity,
                    unit = unit,
                    note = note.trim(),
                    photoPath = photoPath,
                    unitPrice = price,
                    marketAvg = price,
                    // online: con señal va sincronizado, sin señal va pa' la cola
                    online = net.online.value
                )
            }
            // loader OFF
            busy = false
            // si no se insertó, avisamos
            if (result <= 0) {
                onResult("No se pudo enviar la solicitud.")
            } else {
                // refresca la cola del banner si quedó encolado sin señal
                refrescarPendientes()
                onResult(null)
            }
        }
    }

    /**
     * Analiza la foto en el telefono (sin internet) y guarda el resultado.
     * El delay de 900 ms es solo para que el usuario vea el indicador
     * "Analizando imagen..." y la experiencia se sienta real.
     */
    fun analyzeAndSaveDiagnosis(
        // la parcela y la foto de la palma sospechosa de estar enferma
        parcelId: Long,
        photoPath: String,
        onResult: (DiagnosisResult?) -> Unit
    ) {
        // sin usuario no hay quién escanee
        val user = currentUser ?: return
        viewModelScope.launch {
            // loader ON mientras "analiza"
            busy = true
            // la pausa dramática pa' que se sienta que está pensando
            kotlinx.coroutines.delay(900)
            // el analizador local corre el modelo entrenado + reglas de color (sin internet)
            val result = withContext(io) { PalmAnalyzer.analyze(app, photoPath) }
            // si no pudo analizar la foto, avisamos y salimos
            if (result == null) {
                busy = false
                onResult(null)
                return@launch
            }
            // sí hay resultado: lo guardamos en la BD
            withContext(io) {
                // el diagnóstico completo: enfermedad, confianza, severidad y recomendación
                db.insertDiagnosis(
                    parcelId = parcelId,
                    workerId = user.id,
                    photoPath = photoPath,
                    disease = result.disease,
                    confidence = result.confidence,
                    severity = result.severity,
                    finding = result.finding,
                    recommendation = result.recommendation,
                    // online: con señal va sincronizado, sin señal va pa' la cola
                    online = net.online.value
                )
            }
            // loader OFF
            busy = false
            // actualiza la cola del banner (por si quedó esperando señal)
            refrescarPendientes()
            // refrescamos la parcela pa' que aparezca el diagnóstico
            loadParcelData(parcelId)
            // y le entregamos el resultado a la pantalla
            onResult(result)
        }
    }

    // limpia la parcela activa (cuando el obrero vuelve a su lista)
    fun clearActiveParcel() {
        // todo lo de la parcela que estaba viendo se borra
        activeParcel = null
        activeTasks = emptyList()
        activeReports = emptyList()
        activeDiagnoses = emptyList()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: MODO SIN INTERNET (COLA DE ENVÍO)
    //  Propósito: sin señal, todo lo que registra el usuario se guarda
    //  local con synced=0 (queda en cola). Cuando la red vuelve, se
    //  "envía" la cola entera y avisamos con notificación. Aquí es donde
    //  luego va Firebase: solo se cambia lo de adentro de estas funes.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Envía todos los registros en cola: los marca como enviados en la
     * base de datos y (si avisar=true) tira la notificación de éxito.
     */
    private fun sincronizarPendientes(avisar: Boolean) {
        // corrutina pa' hacer el envío en el fondo
        viewModelScope.launch {
            // marcamos lo pendiente como enviado y contamos cuántos eran
            val enviados = withContext(io) { db.markAllSynced() }
            // el contador del banner debe quedar en 0
            refrescarPendientes()
            // refrescamos la pantalla actual con los datos ya sincronizados
            recargarPantallas()
            // si había algo en la cola, avisamos con notificación
            if (avisar && enviados > 0) SyncNotifier.enviados(app, enviados)
        }
    }

    /**
     * Recuenta cuántos registros están esperando señal.
     */
    fun refrescarPendientes() {
        // en el fondo pa' no trabar la UI
        viewModelScope.launch {
            // el contador que lee el banner de "sin internet"
            pendientesEnvio = withContext(io) { db.pendingCount() }
        }
    }

    /**
     * Recarga los datos de la pantalla que esté abierta según el rol,
     * pa' que la UI se refresque sola después de sincronizar.
     */
    private fun recargarPantallas() {
        // sin usuario no hay pantalla que recargar
        val user = currentUser ?: return
        if (user.role == Role.OWNER) {
            // dueño: recargamos su panel completo
            loadOwnerData()
        } else {
            // trabajador: sus parcelas y la que esté viendo
            loadWorkerData()
            activeParcel?.let { loadParcelData(it.id) }
        }
    }

    /**
     * Cuando la app se destruye, apagamos el vigilante de la señal
     * pa' no dejar basura corriendo en el sistema.
     */
    override fun onCleared() {
        // primero lo que haría la clase papá, no la cagues
        super.onCleared()
        // y apagamos el vigilante de la señal, chao
        net.stop()
    }

    // la foto de todos los datos del panel del dueño en una sola pieza
    private data class OwnerSnapshot(
        // stats, cuadrilla, reportes, materiales, diagnósticos y totales
        val stats: OwnerStats,
        val workers: List<User>,
        val reports: List<WorkReport>,
        val materials: List<MaterialRequest>,
        val diagnoses: List<Diagnosis>,
        val totals: List<WorkerTotal>,
        // y el resumen de pagos pa' la gráfica de barras
        val payments: PaymentsSummary
    )
}
