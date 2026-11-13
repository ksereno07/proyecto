/*
 * DbHelper.kt - BASE DE DATOS LOCAL (SQLite)
 * ------------------------------------------
 * Crea y administra la base de datos "agropalma.db" dentro del telefono.
 * Aqui se guarda TODO de forma permanente: cuentas, parcelas, codigos de
 * invitacion, trabajadores unidos, labores, reportes con foto y GPS,
 * solicitudes de materiales, diagnosticos y precios de referencia.
 *
 * Tablas:
 *  - users:         cuentas de duenos y trabajadores (contrasena cifrada).
 *  - parcels:       parcelas/fincas y su codigo dinamico PALMA-XXXX.
 *  - memberships:   que trabajador pertenece a que parcela.
 *  - tasks:         labores creadas por el dueno con su tarifa por unidad.
 *  - reports:       avances de trabajo enviados por el trabajador.
 *  - materials:     pedidos de insumos con precio de mercado.
 *  - diagnoses:     resultados del escaner de palma enferma.
 *  - prices:        catalogo de precios promedio para calcular costos.
 *
 * Nota: esta capa se puede reemplazar mas adelante por Firebase sin tocar
 * las pantallas, porque el ViewModel solo llama a estos metodos.
 */

// el paquete donde vive toda la capa de datos, aquí nomás
package com.agropalma.app.data

// imports pa' escribir filas y abrir la base de datos SQLite
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
// el calendario pa' saber a qué hora arranca el día wey
import java.util.Calendar

// el ayudante de la base de datos: crea las tablas, migra versiones y conecta con agropalma.db
// version 2: le agregue la columna "synced" a reports, materials y diagnoses
// (1 = ya se envió al servidor, 0 = todavía espera señal). No la cagues bajando esto.
// version 3: le agregue "paid" a materials (1 = el dueño ya pagó ese insumo),
// pa' la gráfica de pagos y el control de la cartera de la finca.
class DbHelper(context: Context) : SQLiteOpenHelper(context, "agropalma.db", null, 3) {

    /**
     * Se ejecuta la primera vez que se abre la base de datos.
     * Aqui se crean todas las tablas y se cargan los precios de referencia.
     */
    override fun onCreate(db: SQLiteDatabase) {
        // Cuentas: rol (OWNER/WORKER), datos personales y contrasena cifrada.
        db.execSQL(
            """
            CREATE TABLE users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                name TEXT NOT NULL,
                cedula TEXT,
                phone TEXT NOT NULL,
                email TEXT,
                password TEXT NOT NULL,
                finca TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // tabla de parcelas con su código PALMA-XXXX y la fecha en que vence
        db.execSQL(
            """
            CREATE TABLE parcels(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                location TEXT NOT NULL,
                hectares REAL NOT NULL,
                code TEXT NOT NULL,
                code_expires_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // tabla que une trabajadores con parcelas, pa' saber quién anda en cuál finca
        db.execSQL(
            """
            CREATE TABLE memberships(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parcel_id INTEGER NOT NULL,
                worker_id INTEGER NOT NULL,
                joined_at INTEGER NOT NULL,
                UNIQUE(parcel_id, worker_id)
            )
            """.trimIndent()
        )
        // tabla de labores con su meta y la tarifa que se paga por unidad
        db.execSQL(
            """
            CREATE TABLE tasks(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parcel_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                target_count INTEGER NOT NULL,
                unit TEXT NOT NULL,
                rate_per_unit REAL NOT NULL,
                assigned_worker_id INTEGER,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // tabla de reportes de trabajo: cantidad, foto, GPS y cuánto se paga
        db.execSQL(
            """
            CREATE TABLE reports(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parcel_id INTEGER NOT NULL,
                worker_id INTEGER NOT NULL,
                task_id INTEGER,
                task_title TEXT NOT NULL,
                description TEXT NOT NULL,
                units REAL NOT NULL,
                unit TEXT NOT NULL,
                photo_path TEXT,
                latitude REAL,
                longitude REAL,
                amount REAL NOT NULL,
                status TEXT NOT NULL,
                synced INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // tabla de pedidos de insumos con su costo estimado
        db.execSQL(
            """
            CREATE TABLE materials(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parcel_id INTEGER NOT NULL,
                worker_id INTEGER NOT NULL,
                item TEXT NOT NULL,
                quantity REAL NOT NULL,
                unit TEXT NOT NULL,
                note TEXT NOT NULL,
                photo_path TEXT,
                unit_price REAL NOT NULL,
                market_avg REAL NOT NULL,
                status TEXT NOT NULL,
                paid INTEGER NOT NULL DEFAULT 0,
                synced INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // tabla de diagnósticos del escáner de palmas enfermas
        db.execSQL(
            """
            CREATE TABLE diagnoses(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parcel_id INTEGER NOT NULL,
                worker_id INTEGER NOT NULL,
                photo_path TEXT NOT NULL,
                disease TEXT NOT NULL,
                confidence INTEGER NOT NULL,
                severity TEXT NOT NULL,
                finding TEXT NOT NULL,
                recommendation TEXT NOT NULL,
                synced INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // tabla de precios promedio del mercado pa' estimar costos
        db.execSQL(
            """
            CREATE TABLE prices(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item TEXT NOT NULL UNIQUE,
                unit TEXT NOT NULL,
                avg_price REAL NOT NULL
            )
            """.trimIndent()
        )
        // mete los precios de referencia pa' que la tabla no arranque vacía
        seedPrices(db)
    }

    /**
     * Catalogo inicial de precios promedio de mercado. El dueno lo usa para
     * estimar cuanto costaran los insumos que pide la cuadrilla.
     * Los valores son de referencia y se pueden ajustar a la zona.
     */
    // llena la tabla de precios con el catálogo inicial, pa' que el dueño estime costos
    private fun seedPrices(db: SQLiteDatabase) {
        // la lista de insumos con su unidad y precio de mercado aproximado
        val prices = listOf(
            arrayOf("Fertilizante NPK 15-15-15", "bulto 50 kg", 145000.0),
            arrayOf("Abono orgánico", "bulto 50 kg", 65000.0),
            arrayOf("Fungicida sistémico", "litro", 89000.0),
            arrayOf("Herbicida glifosato", "litro", 32000.0),
            arrayOf("Insecticida foliar", "litro", 47000.0),
            arrayOf("Machete", "unidad", 45000.0),
            arrayOf("Botas de caucho", "par", 85000.0),
            arrayOf("Guantes de trabajo", "par", 12000.0),
            arrayOf("Combustible", "galón", 16000.0)
        )
        // metemos cada insumo como fila nueva en la tabla prices
        prices.forEach { p ->
            // guarda nombre, unidad y precio promedio del insumo
            db.insert("prices", null, ContentValues().apply {
                put("item", p[0] as String)
                put("unit", p[1] as String)
                put("avg_price", p[2] as Double)
            })
        }
    }

    // se corre cuando la db ya existía pero con versión vieja: aquí se migra
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // OJO wey: antes de aquí se borraba TODO y se recreaba. YA NO:
        // eso le volaba las cuentas y los datos al usuario. Ahora sí
        // hacemos migración de verdad, agregando lo nuevo sin tocar lo viejo.
        if (oldVersion < 2) {
            // v2: llega la columna "synced" (1 = ya enviado, 0 = esperando señal).
            // Los registros viejos quedan en 0, o sea, entran a la cola y se
            // envían solos en cuanto haya internet. Cero dramas.
            db.execSQL("ALTER TABLE reports ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE materials ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE diagnoses ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            // v3: llega la columna "paid" en materials (1 = el dueño ya pagó
            // ese insumo). Los pedidos viejos quedan en 0, o sea "sin pagar",
            // que es lo honesto: si ya lo había pagado en la vida real, el
            // dueño lo marca con un toque en la pantalla de pagos. Fácil.
            db.execSQL("ALTER TABLE materials ADD COLUMN paid INTEGER NOT NULL DEFAULT 0")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: USUARIOS (CUENTAS Y CONTRASEÑAS)
    //  Propósito: crear cuentas de dueños y trabajadores, buscarlas por
    //  celular/cédula e iniciar sesión (comparando la contraseña cifrada).
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Comprueba si un celular ya tiene cuenta (para no registrar duplicados).
     */
    fun phoneExists(phone: String): Boolean =
        // pregunta directo a SQLite si ese celular ya existe, no la cagues registrando duplicados
        readableDatabase.rawQuery("SELECT 1 FROM users WHERE phone = ? LIMIT 1", arrayOf(phone)).use { it.moveToFirst() }

    /**
     * Comprueba si una cedula ya esta registrada por otro trabajador.
     */
    fun cedulaExists(cedula: String): Boolean =
        // igual pero con la cédula: si otro ya la tiene, ahí se queda el registro
        readableDatabase.rawQuery("SELECT 1 FROM users WHERE cedula = ? LIMIT 1", arrayOf(cedula)).use { it.moveToFirst() }

    /**
     * Guarda una cuenta nueva. La contrasena ya llega cifrada desde Security.hash.
     * Devuelve el id generado o -1 si fallo.
     */
    fun insertUser(
        // los datos de la cuenta: rol, nombre, cédula, celular y correo
        role: Role,
        name: String,
        cedula: String?,
        phone: String,
        email: String?,
        // la contraseña ya llega cifrada desde Security.hash, NO la cifres otra vez aquí
        passwordHash: String,
        finca: String?
    ): Long {
        // el paquetito de valores que le metemos a la fila nueva
        val values = ContentValues().apply {
            // metemos todos los campos de la cuenta en el ContentValues
            put("role", role.name)
            put("name", name)
            put("cedula", cedula)
            put("phone", phone)
            put("email", email)
            put("password", passwordHash)
            put("finca", finca)
            // la fecha de creación en milisegundos, pa' la posteridad
            put("created_at", System.currentTimeMillis())
        }
        // guarda la fila y regresa el id, o -1 si algo falló
        return writableDatabase.insert("users", null, values)
    }

    // revisa si el login vale: entra por celular o cédula y la contraseña cifrada debe coincidir
    fun authenticate(identifier: String, passwordHash: String): User? {
        // la query: el user puede entrar con celular O cédula, pero la contraseña va sí o sí
        val sql = "SELECT * FROM users WHERE (phone = ? OR cedula = ?) AND password = ? LIMIT 1"
        // corre la query con lo que escribió el user en la pantalla de login
        readableDatabase.rawQuery(sql, arrayOf(identifier, identifier, passwordHash)).use { c ->
            // si hay fila las credenciales van bien, si no, no existe ese user
            return if (c.moveToFirst()) c.toUser() else null
        }
    }

    // trae un user por su id, pa' cuando ya sabemos quién es
    fun getUser(id: Long): User? {
        // busca la fila por id y la convierte a objeto User
        readableDatabase.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(id.toString())).use { c ->
            // si no hay fila es que el user no existe, así de simple
            return if (c.moveToFirst()) c.toUser() else null
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: AJUSTES DE LA CUENTA
    //  Propósito: editar los datos personales (nombre, correo, finca),
    //  cambiar la contraseña y BORRAR la cuenta con todo lo suyo.
    //  El celular y la cédula NO se editan porque son los identificadores
    //  de login; cambiarlos sería un lío, mejor así de sencillo.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Actualiza los datos editables de la cuenta: nombre, correo y (si es
     * dueño) el nombre de la finca. El celular y la cédula quedan quietos.
     */
    fun updateUser(id: Long, name: String, email: String?, finca: String?) {
        // el paquetito con lo que sí se puede editar
        val values = ContentValues().apply {
            // el nombre nuevo, el correo (puede ir null) y la finca
            put("name", name)
            put("email", email)
            put("finca", finca)
        }
        // actualiza la fila del user por su id, ahí nomás
        writableDatabase.update("users", values, "id = ?", arrayOf(id.toString()))
    }

    /**
     * Cambia la contraseña: ya llega cifrada desde Security.hash, aquí
     * nomás se guarda encima de la vieja.
     */
    fun updatePassword(id: Long, passwordHash: String) {
        // mete el hash nuevo en la fila del user
        writableDatabase.update(
            "users",
            ContentValues().apply { put("password", passwordHash) },
            "id = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * Comprueba si la contraseña cifrada que mandan coincide con la guardada.
     * Se usa pa' verificar la contraseña ACTUAL antes de cambiarla.
     */
    fun checkPassword(id: Long, passwordHash: String): Boolean =
        // si hay una fila con ese id y ese hash, la contraseña va bien
        readableDatabase.rawQuery(
            "SELECT 1 FROM users WHERE id = ? AND password = ? LIMIT 1",
            arrayOf(id.toString(), passwordHash)
        ).use { it.moveToFirst() }

    /**
     * BORRA la cuenta del user CON TODO lo que le pertenece, sin dejar
     * huérfanos en la base de datos:
     *  - si es dueño: sus parcelas y todo lo de esas parcelas (labores,
     *    membresías, reportes, materiales y diagnósticos).
     *  - si es trabajador: sus membresías y sus propios reportes,
     *    materiales y diagnósticos.
     * Todo va en UNA transacción: o se borra todo, o no se borra nada.
     */
    fun deleteUser(userId: Long) {
        // abrimos la bd pa' escribir y arrancamos la transacción tod-o-o
        val db = writableDatabase
        // o se borra todo o nada, así funcionan las transacciones wey
        db.beginTransaction()
        try {
            // las parcelas que le pertenecen como dueño (pa' limpiarlas con todo)
            val misParcelas = "SELECT id FROM parcels WHERE owner_id = $userId"
            // los reportes de sus parcelas O los que él mismo mandó
            db.execSQL("DELETE FROM reports WHERE parcel_id IN ($misParcelas) OR worker_id = $userId")
            // los materiales de sus parcelas O los que él mismo pidió
            db.execSQL("DELETE FROM materials WHERE parcel_id IN ($misParcelas) OR worker_id = $userId")
            // los diagnósticos de sus parcelas O los que él mismo escaneó
            db.execSQL("DELETE FROM diagnoses WHERE parcel_id IN ($misParcelas) OR worker_id = $userId")
            // las labores de sus parcelas se van también
            db.execSQL("DELETE FROM tasks WHERE parcel_id IN ($misParcelas)")
            // y las membresías de sus parcelas O las suyas de trabajador
            db.execSQL("DELETE FROM memberships WHERE parcel_id IN ($misParcelas) OR worker_id = $userId")
            // ahora sí: las parcelas y la cuenta como tal
            db.execSQL("DELETE FROM parcels WHERE owner_id = $userId")
            db.execSQL("DELETE FROM users WHERE id = $userId")
            // todo salió chido: confirmamos la transacción
            db.setTransactionSuccessful()
        } finally {
            // cerramos la transacción si o si (commit si hubo success, rollback si no)
            db.endTransaction()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: PARCELAS Y CÓDIGOS DE INVITACIÓN
    //  Propósito: crear la parcela/fca del dueño, generar el código
    //  PALMA-XXXX que vence en 1 minuto, validarlo cuando el trabajador
    //  se une y consultar las parcelas y sus miembros.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Crea una parcela nueva con un codigo PALMA-XXXX y siembra las 4 labores
     * base. El codigo vence en 1 minuto (CODE_LIFETIME_MS).
     */
    fun createParcel(ownerId: Long, name: String, location: String, hectares: Double): Long {
        // le generamos un código PALMA-XXXX bien fresh
        val code = newCode()
        // la hora exacta en que nace la parcela
        val now = System.currentTimeMillis()
        // el paquetito con todos los datos de la parcela nueva
        val values = ContentValues().apply {
            // dueño, nombre, ubicación y hectáreas de la finca
            put("owner_id", ownerId)
            put("name", name)
            put("location", location)
            put("hectares", hectares)
            // el código y su vencimiento: dura un minuto, rapidito
            put("code", code)
            put("code_expires_at", now + CODE_LIFETIME_MS)
            put("created_at", now)
        }
        // guarda la parcela y se queda con su id
        val id = writableDatabase.insert("parcels", null, values)
        // si sí se guardó, le siembramos las 4 labores base
        if (id > 0) seedTasks(id)
        return id
    }

    // mete las 4 labores base que toda parcela nueva necesita, pa' no crearlas a mano
    private fun seedTasks(parcelId: Long) {
        // la fecha pa' el created_at de todas las labores
        val now = System.currentTimeMillis()
        // aquí van las labores con su meta, unidad y tarifa por unidad
        val tasks = listOf(
            arrayOf("Poda Sanitaria y Formación", "Corte parejo de hojas bajeras secas. Despeje la calle central para facilitar el paso de carretillas y el alce.", 50, "palmas", 2600.0),
            arrayOf("Cosecha de Racimos", "Corte de racimos maduros y alce hasta el punto de acopio. Reporte cada racimo cortado.", 40, "racimos", 1800.0),
            arrayOf("Plateo y Limpieza de Tronco", "Limpieza del plato de la palma y retiro de maleza alrededor del tronco.", 30, "platos", 3200.0),
            arrayOf("Aplicación Fitosanitaria", "Aplicación dirigida de fungicida en palmas con síntomas. Use equipo de protección.", 20, "palmas", 4500.0)
        )
        // insertamos cada labor como fila en la tabla tasks
        tasks.forEach { t ->
            // el paquetito: a qué parcela va, título, meta, unidad y tarifa
            writableDatabase.insert("tasks", null, ContentValues().apply {
                put("parcel_id", parcelId)
                // título, descripción, meta, unidad, tarifa y fecha, todo el combo
                put("title", t[0] as String)
                put("description", t[1] as String)
                put("target_count", t[2] as Int)
                put("unit", t[3] as String)
                put("rate_per_unit", t[4] as Double)
                put("created_at", now)
            })
        }
    }

    // la última parcela que creó el dueño, pa' su panel de una vez
    fun getOwnerParcel(ownerId: Long): Parcel? {
        // junta parcels con users pa' sacar también el nombre del dueño
        val sql = "SELECT p.*, u.name AS owner_name FROM parcels p JOIN users u ON u.id = p.owner_id " +
            "WHERE p.owner_id = ? ORDER BY p.created_at DESC LIMIT 1"
        // corre la query y agarra la primera, o sea la más reciente
        readableDatabase.rawQuery(sql, arrayOf(ownerId.toString())).use { c ->
            // si no hay fila es que aún no tiene parcela
            return if (c.moveToFirst()) c.toParcel() else null
        }
    }

    // trae una parcela por su id, con el nombre del dueño incluido
    fun getParcel(id: Long): Parcel? {
        // el JOIN pa' traer el owner_name junto con los datos de la parcela
        val sql = "SELECT p.*, u.name AS owner_name FROM parcels p JOIN users u ON u.id = p.owner_id WHERE p.id = ?"
        // busca y convierte la fila a objeto Parcel
        readableDatabase.rawQuery(sql, arrayOf(id.toString())).use { c ->
            // null si no existe esa parcela, no hay de otra
            return if (c.moveToFirst()) c.toParcel() else null
        }
    }

    // regenera el código de invitación: el viejo ya valió, aquí sale uno nuevo con 1 minuto más
    fun regenerateCode(parcelId: Long): Parcel? {
        // código nuevo bien fresh
        val code = newCode()
        // actualiza la fila de la parcela con el código nuevo y su nuevo vencimiento
        writableDatabase.update(
            "parcels",
            ContentValues().apply {
                put("code", code)
                put("code_expires_at", System.currentTimeMillis() + CODE_LIFETIME_MS)
            },
            "id = ?",
            arrayOf(parcelId.toString())
        )
        // regresa la parcela ya actualizada pa' mostrar el código nuevo
        return getParcel(parcelId)
    }

    // genera un código PALMA-XXXX que no se repita con los que ya existen
    private fun newCode(): String {
        // pa' sacar números al azar
        val random = java.util.Random()
        // el candidato a código y cuántos intentos llevamos
        var candidate: String
        var attempts = 0
        do {
            // arma "PALMA-" más un número de 4 dígitos, así de fácil
            candidate = "PALMA-" + (1000 + random.nextInt(9000))
            attempts++
        // mientras haya intentos y el código ya esté ocupado, sigue probando
        } while (attempts < 25 && codeInUse(candidate))
        return candidate
    }

    // checa si alguna otra parcela ya tiene ese código de invitación
    private fun codeInUse(code: String): Boolean =
        // si la query trae al menos una fila, el código ya está ocupado wey
        readableDatabase.rawQuery("SELECT 1 FROM parcels WHERE code = ? LIMIT 1", arrayOf(code)).use { it.moveToFirst() }

    /**
     * Valida el codigo que escribe el trabajador:
     *  1. Que exista en alguna parcela.
     *  2. Que no haya vencido.
     *  3. Que el trabajador no este ya unido.
     * Si todo esta bien, crea la membresia (parcela + trabajador).
     */
    fun joinByCode(code: String, workerId: Long): JoinResult {
        // agarra el texto y le quita los espacios de más, y lo pone en MAYÚSCULAS
        val normalized = code.trim().uppercase()
        // busca la parcela que tenga ese código de invitación
        val sql = "SELECT p.*, u.name AS owner_name FROM parcels p JOIN users u ON u.id = p.owner_id " +
            "WHERE p.code = ? ORDER BY p.created_at DESC LIMIT 1"
        // corre la query con el código que escribió el trabajador
        readableDatabase.rawQuery(sql, arrayOf(normalized)).use { c ->
            // si no hay fila, ese código no existe, pa' qué seguir
            if (!c.moveToFirst()) {
                return JoinResult(false, "Ese código no existe. Pídele al dueño el código vigente.")
            }
            // convierte la fila encontrada a objeto Parcel
            val parcel = c.toParcel()
            // si ya pasó el minuto de vida del código, venció, pide otro
            if (System.currentTimeMillis() > parcel.codeExpiresAt) {
                return JoinResult(false, "El código venció. Pídele al dueño que genere uno nuevo.")
            }
            // si ya se había unido antes, no dejamos duplicar la membresía
            if (isMember(parcel.id, workerId)) {
                return JoinResult(false, "Ya estás vinculado a esta parcela.", parcel)
            }
            // todo bien: crea la membresía que une al trabajador con la parcela
            writableDatabase.insert("memberships", null, ContentValues().apply {
                put("parcel_id", parcel.id)
                put("worker_id", workerId)
                put("joined_at", System.currentTimeMillis())
            })
            // éxito wey, ya está dentro de la finca
            return JoinResult(true, "Te uniste a ${parcel.name}.", parcel)
        }
    }

    // dice si un trabajador ya está metido en esa parcela
    fun isMember(parcelId: Long, workerId: Long): Boolean =
        // busca la membresía por los dos ids, si hay fila es que ya es miembro
        readableDatabase.rawQuery(
            "SELECT 1 FROM memberships WHERE parcel_id = ? AND worker_id = ? LIMIT 1",
            arrayOf(parcelId.toString(), workerId.toString())
        ).use { it.moveToFirst() }

    // todas las fincas donde trabaja el trabajador, las que se ha ido uniendo
    fun getWorkerParcels(workerId: Long): List<Parcel> {
        // junta membresías con parcelas y users pa' traerlas completitas
        val sql = "SELECT p.*, u.name AS owner_name FROM memberships m " +
            "JOIN parcels p ON p.id = m.parcel_id JOIN users u ON u.id = p.owner_id " +
            "WHERE m.worker_id = ? ORDER BY m.joined_at DESC"
        // aquí se van acumulando las parcelas
        val list = mutableListOf<Parcel>()
        // recorre todas las filas y las va convirtiendo a objetos
        readableDatabase.rawQuery(sql, arrayOf(workerId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toParcel())
        }
        return list
    }

    // la cuadrilla completa: todos los trabajadores unidos a una parcela
    fun getParcelMembers(parcelId: Long): List<User> {
        // trae los users con membresía en esa parcela, ordenaditos por nombre
        val sql = "SELECT u.* FROM memberships m JOIN users u ON u.id = m.worker_id " +
            "WHERE m.parcel_id = ? ORDER BY u.name"
        // la lista de la cuadrilla
        val list = mutableListOf<User>()
        // va metiendo cada trabajador a la lista
        readableDatabase.rawQuery(sql, arrayOf(parcelId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toUser())
        }
        return list
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: LABORES (TAREJOS DE LA FINCA)
    //  Propósito: consultar las labores de una parcela con su meta y su
    //  tarifa por unidad. Se crean 4 labores base al crear la parcela.
    // ═══════════════════════════════════════════════════════════════════

    // todas las labores de una parcela, ordenaditas por id
    fun getTasks(parcelId: Long): List<Task> {
        // aquí se van guardando las labores
        val list = mutableListOf<Task>()
        // saca las labores de esa parcela y las convierte a objetos
        readableDatabase.rawQuery(
            "SELECT * FROM tasks WHERE parcel_id = ? ORDER BY id",
            arrayOf(parcelId.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(c.toTask())
        }
        return list
    }

    // una labor por su id, por si necesitas ver sus detalles
    fun getTask(id: Long): Task? {
        // busca la labor y la convierte a objeto, null si no existe
        readableDatabase.rawQuery("SELECT * FROM tasks WHERE id = ?", arrayOf(id.toString())).use { c ->
            return if (c.moveToFirst()) c.toTask() else null
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: REPORTES DE TRABAJO
    //  Propósito: guardar el avance del trabajador (cantidad, foto y GPS),
    //  calcular el pago (cantidad x tarifa) y consultarlos para el dueño
    //  y para el propio trabajador.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Guarda un reporte de trabajo. El valor a pagar se calcula aqui mismo:
     * cantidad reportada x tarifa por unidad de la labor.
     * El estado inicial es PENDING (pendiente de pago del dueno).
     */
    fun insertReport(
        // la parcela, el trabajador que reporta y la labor que hizo
        parcelId: Long,
        workerId: Long,
        task: Task,
        // cuántas unidades hizo, su descripción y la foto de evidencia
        units: Double,
        description: String,
        photoPath: String?,
        // dónde estaba parado cuando reportó (GPS)
        latitude: Double?,
        longitude: Double?,
        // si había señal al momento de guardar
        online: Boolean
    ): Long {
        // el paquete gigante con todo el reporte
        val values = ContentValues().apply {
            // parcela, trabajador y la labor que se está reportando
            put("parcel_id", parcelId)
            put("worker_id", workerId)
            put("task_id", task.id)
            put("task_title", task.title)
            // lo que contó el trabajador y la unidad de la labor
            put("description", description)
            put("units", units)
            put("unit", task.unit)
            // la foto de evidencia y su ubicación GPS
            put("photo_path", photoPath)
            put("latitude", latitude)
            put("longitude", longitude)
            // la plata a pagar: unidades x tarifa, multiplicadito aquí mismo
            put("amount", units * task.ratePerUnit)
            // arranca pendiente de pago, el dueño decide cuándo paga
            put("status", "PENDING")
            // si había internet al momento de guardar, ya va "enviado" (1);
            // si no, queda en la cola esperando señal (0). Así de fácil.
            put("synced", if (online) 1 else 0)
            // fecha en que se mandó el reporte
            put("created_at", System.currentTimeMillis())
        }
        // guarda el reporte en la tabla y regresa su id
        return writableDatabase.insert("reports", null, values)
    }

    // todos los reportes de una parcela, del más reciente al más viejo
    fun getReports(parcelId: Long): List<WorkReport> {
        // trae los reportes con el nombre del trabajador que los mandó
        val sql = "SELECT r.*, u.name AS worker_name FROM reports r JOIN users u ON u.id = r.worker_id " +
            "WHERE r.parcel_id = ? ORDER BY r.created_at DESC"
        // aquí se juntan los reportes
        val list = mutableListOf<WorkReport>()
        // recorre las filas y las convierte a objetos WorkReport
        readableDatabase.rawQuery(sql, arrayOf(parcelId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toReport())
        }
        return list
    }

    // solo los reportes de UN trabajador en una parcela, pa' que vea los suyos
    fun getReportsByWorker(parcelId: Long, workerId: Long): List<WorkReport> {
        // igual que getReports pero filtrado por trabajador
        val sql = "SELECT r.*, u.name AS worker_name FROM reports r JOIN users u ON u.id = r.worker_id " +
            "WHERE r.parcel_id = ? AND r.worker_id = ? ORDER BY r.created_at DESC"
        // su lista de reportes
        val list = mutableListOf<WorkReport>()
        // convierte cada fila a WorkReport y la agrega
        readableDatabase.rawQuery(sql, arrayOf(parcelId.toString(), workerId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toReport())
        }
        return list
    }

    // cambia el estado de un reporte: PENDING o PAID, lo que decida el dueño
    fun setReportStatus(id: Long, status: String) {
        // actualiza la columna status en la fila de ese reporte, ahí nomás
        writableDatabase.update("reports", ContentValues().apply { put("status", status) }, "id = ?", arrayOf(id.toString()))
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: SOLICITUDES DE MATERIALES
    //  Propósito: guardar los pedidos de insumos que hace la cuadrilla
    //  (con su costo estimado) y consultarlos para que el dueño los
    //  apruebe y marque como entregados.
    // ═══════════════════════════════════════════════════════════════════

    fun insertMaterial(
        // quién pide y en qué parcela se necesita el insumo
        parcelId: Long,
        workerId: Long,
        // qué pidió, cuánto, en qué unidad y su nota
        item: String,
        quantity: Double,
        unit: String,
        note: String,
        // foto del insumo, precio por unidad y el promedio del mercado
        photoPath: String?,
        unitPrice: Double,
        marketAvg: Double,
        // si había internet al guardar el pedido
        online: Boolean
    ): Long {
        // el paquetito del pedido de material
        val values = ContentValues().apply {
            // parcela, trabajador, insumo, cantidad y unidad
            put("parcel_id", parcelId)
            put("worker_id", workerId)
            put("item", item)
            put("quantity", quantity)
            put("unit", unit)
            // la nota del trabajador y su foto si subió una
            put("note", note)
            put("photo_path", photoPath)
            // el precio por unidad y el promedio de mercado pa' comparar
            put("unit_price", unitPrice)
            put("market_avg", marketAvg)
            // arranca pendiente hasta que el dueño lo apruebe
            put("status", "PENDING")
            // igual que el reporte: con señal va enviado, sin señal a la cola
            put("synced", if (online) 1 else 0)
            // fecha del pedido
            put("created_at", System.currentTimeMillis())
        }
        // guarda el pedido y regresa su id
        return writableDatabase.insert("materials", null, values)
    }

    // los pedidos de insumos de una parcela, del más nuevo al más viejo
    fun getMaterials(parcelId: Long): List<MaterialRequest> {
        // trae los materiales con el nombre de quién los pidió
        val sql = "SELECT m.*, u.name AS worker_name FROM materials m JOIN users u ON u.id = m.worker_id " +
            "WHERE m.parcel_id = ? ORDER BY m.created_at DESC"
        // aquí se juntan los pedidos
        val list = mutableListOf<MaterialRequest>()
        // convierte cada fila a MaterialRequest
        readableDatabase.rawQuery(sql, arrayOf(parcelId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toMaterial())
        }
        return list
    }

    // cambia el estado de un pedido: PENDING o DELIVERED cuando ya llegó
    fun setMaterialStatus(id: Long, status: String) {
        // actualiza el status de esa fila, rapidito
        writableDatabase.update("materials", ContentValues().apply { put("status", status) }, "id = ?", arrayOf(id.toString()))
    }

    // marca un insumo como PAGADO (o desmarca si el dueño la cagó): 1/0
    fun setMaterialPaid(id: Long, paid: Boolean) {
        // el booleano lo volvemos 1/0 porque SQLite no conoce de true/false
        writableDatabase.update(
            "materials",
            ContentValues().apply { put("paid", if (paid) 1 else 0) },
            "id = ?",
            arrayOf(id.toString())
        )
    }

    /**
     * El resumen de pagos de la finca en 4 números: lo pagado y pendiente
     * de la cuadrilla (reportes) y lo pagado y pendiente de insumos
     * (materiales). Es la data que alimenta la gráfica de barras de pagos.
     */
    fun getPaymentsSummary(parcelId: Long): PaymentsSummary {
        // arrancamos todo en cero, por si no hay data todavía
        var workersPaid = 0.0
        var workersPending = 0.0
        var materialsPaid = 0.0
        var materialsPending = 0.0

        // la plata de la cuadrilla: sumas separadas por estado del reporte
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(CASE WHEN status = 'PAID' THEN amount ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN status != 'PAID' THEN amount ELSE 0 END), 0) " +
                "FROM reports WHERE parcel_id = ?",
            arrayOf(parcelId.toString())
        ).use { c ->
            // si trae fila (siempre trae con SUM), repartimos las sumas
            if (c.moveToFirst()) { workersPaid = c.getDouble(0); workersPending = c.getDouble(1) }
        }

        // la plata de los insumos: costo (cantidad x precio) separado por paid
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(CASE WHEN paid = 1 THEN quantity * unit_price ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN paid = 0 THEN quantity * unit_price ELSE 0 END), 0) " +
                "FROM materials WHERE parcel_id = ?",
            arrayOf(parcelId.toString())
        ).use { c ->
            // igual: repartimos las sumas de pagado y por pagar
            if (c.moveToFirst()) { materialsPaid = c.getDouble(0); materialsPending = c.getDouble(1) }
        }

        // el resumen completito pa' la gráfica
        return PaymentsSummary(workersPaid, workersPending, materialsPaid, materialsPending)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: DIAGNÓSTICOS DEL ESCÁNER
    //  Propósito: guardar el resultado del análisis de la palma enferma
    //  (enfermedad, certeza, hallazgo y recomendación) para que el dueño
    //  lo vea como alerta de salud del cultivo.
    // ═══════════════════════════════════════════════════════════════════

    fun insertDiagnosis(
        // dónde está la palma y quién la escaneó
        parcelId: Long,
        workerId: Long,
        // la foto de la palma analizada
        photoPath: String,
        // qué enfermedad es, qué tan seguro y qué tan grave
        disease: String,
        confidence: Int,
        severity: String,
        // lo que se vio y qué hay que hacerle
        finding: String,
        recommendation: String,
        // si había internet al guardar
        online: Boolean
    ): Long {
        // el paquetito del diagnóstico
        val values = ContentValues().apply {
            // parcela, trabajador y la foto analizada
            put("parcel_id", parcelId)
            put("worker_id", workerId)
            put("photo_path", photoPath)
            // enfermedad, certeza y gravedad detectadas
            put("disease", disease)
            put("confidence", confidence)
            put("severity", severity)
            // el hallazgo visual y la recomendación de tratamiento
            put("finding", finding)
            put("recommendation", recommendation)
            // y el diagnóstico también entra a la cola si no había señal
            put("synced", if (online) 1 else 0)
            // fecha del diagnóstico
            put("created_at", System.currentTimeMillis())
        }
        // guarda el diagnóstico y regresa su id
        return writableDatabase.insert("diagnoses", null, values)
    }

    // los diagnósticos de una parcela, pa' las alertas de salud del cultivo
    fun getDiagnoses(parcelId: Long): List<Diagnosis> {
        // trae los diagnósticos con el nombre de quién escaneó
        val sql = "SELECT d.*, u.name AS worker_name FROM diagnoses d JOIN users u ON u.id = d.worker_id " +
            "WHERE d.parcel_id = ? ORDER BY d.created_at DESC"
        // la lista de alertas
        val list = mutableListOf<Diagnosis>()
        // convierte cada fila a Diagnosis y la agrega
        readableDatabase.rawQuery(sql, arrayOf(parcelId.toString())).use { c ->
            while (c.moveToNext()) list.add(c.toDiagnosis())
        }
        return list
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: COLA DE ENVÍO (MODO SIN INTERNET)
    //  Propósito: contar los registros que aún no se han "enviado al
    //  servidor" (synced = 0) y marcarlos como enviados cuando vuelve la
    //  señal. Aquí es donde más adelante entra Firebase: por ahora el
    //  envío es local, pero la lógica de la cola ya queda lista.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cuántos registros esperando señal hay en total: reportes +
     * materiales + diagnósticos con synced = 0. Es el número del banner.
     */
    fun pendingCount(): Int {
        // aquí se van sumando los pendientes de todas las tablas
        var total = 0
        // recorremos las 3 tablas que se envían y sumamos los pendientes
        listOf("reports", "materials", "diagnoses").forEach { tabla ->
            // cuenta cuántas filas tienen synced = 0 en esa tabla
            readableDatabase.rawQuery("SELECT COUNT(*) FROM $tabla WHERE synced = 0", null).use { c ->
                // si trae algo, se suma al total
                if (c.moveToFirst()) total += c.getInt(0)
            }
        }
        // el número del banner: cuánta cosa espera señal
        return total
    }

    /**
     * "Envía" toda la cola: marca los registros pendientes como synced = 1.
     * Devuelve cuántos se enviaron (0 si la cola estaba vacía).
     */
    fun markAllSynced(): Int {
        // cuántos registros se "enviaron" en total
        var total = 0
        // en cada tabla ponemos synced=1 a todo lo que estaba en 0 y sumamos
        listOf("reports", "materials", "diagnoses").forEach { tabla ->
            // cambia synced a 1 en todos los pendientes y regresa cuántos tocó
            total += writableDatabase.update(
                tabla,
                ContentValues().apply { put("synced", 1) },
                "synced = 0",
                null
            )
        }
        // cuántos salieron de la cola, eso regresa
        return total
    }

    // ---------------- Prices & summaries ----------------

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: PRECIOS DE MERCADO Y ESTADÍSTICAS
    //  Propósito: consultar el catálogo de precios promedio y calcular los
    //  totales del panel del dueño (por pagar, insumos, reportes del día,
    //  alertas de salud y saldo por cada trabajador).
    // ═══════════════════════════════════════════════════════════════════
    // todo el catálogo de precios de mercado, ordenadito por nombre
    fun getPrices(): List<PriceItem> {
        // la lista de precios
        val list = mutableListOf<PriceItem>()
        // saca todos los precios y arma un PriceItem por cada fila
        readableDatabase.rawQuery("SELECT * FROM prices ORDER BY item", null).use { c ->
            while (c.moveToNext()) {
                // mete nombre, unidad y precio promedio en el objeto
                list.add(PriceItem(c.getString(c.getColumnIndexOrThrow("item")), c.getString(c.getColumnIndexOrThrow("unit")), c.getDouble(c.getColumnIndexOrThrow("avg_price"))))
            }
        }
        return list
    }

    // busca el precio de un insumo por nombre, sin importar mayúsculas
    fun priceFor(item: String): PriceItem? =
        // agarra todos los precios y se queda con el que coincida, si existe
        getPrices().firstOrNull { it.item.equals(item, ignoreCase = true) }

    // las estadísticas del panel del dueño, contando desde las 12 de la noche
    fun getOwnerStats(parcelId: Long): OwnerStats = computeStats(parcelId, startOfToday())

    /**
     * Calcula los totales que ve el dueno en su panel:
     * por pagar (reportes no pagados), costo de insumos pendientes,
     * numero de trabajadores, reportes de hoy y alertas de salud.
     */
    private fun computeStats(parcelId: Long, today: Long): OwnerStats {
        // todos los contadores arrancan en cero, ahí nomás
        var pendingPay = 0.0
        var pendingCount = 0
        var materialsTotal = 0.0
        var materialsCount = 0
        var workerCount = 0
        var reportCount = 0
        var alertCount = 0

        // suma la plata de los reportes NO pagados y cuántos son
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(amount), 0), COUNT(*) FROM reports WHERE parcel_id = ? AND status != 'PAID'",
            arrayOf(parcelId.toString())
        ).use { c -> if (c.moveToFirst()) { pendingPay = c.getDouble(0); pendingCount = c.getInt(1) } }

        // suma el costo de los materiales PENDING y cuántos pedidos hay
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(quantity * unit_price), 0), COUNT(*) FROM materials WHERE parcel_id = ? AND status = 'PENDING'",
            arrayOf(parcelId.toString())
        ).use { c -> if (c.moveToFirst()) { materialsTotal = c.getDouble(0); materialsCount = c.getInt(1) } }

        // cuenta cuántos trabajadores tiene la parcela
        readableDatabase.rawQuery("SELECT COUNT(*) FROM memberships WHERE parcel_id = ?", arrayOf(parcelId.toString()))
            .use { c -> if (c.moveToFirst()) workerCount = c.getInt(0) }

        // reportes que han llegado hoy desde la madrugada
        readableDatabase.rawQuery("SELECT COUNT(*) FROM reports WHERE parcel_id = ? AND created_at >= ?", arrayOf(parcelId.toString(), today.toString()))
            .use { c -> if (c.moveToFirst()) reportCount = c.getInt(0) }

        // cuenta las alertas de salud, o sea, los diagnósticos de la parcela
        readableDatabase.rawQuery("SELECT COUNT(*) FROM diagnoses WHERE parcel_id = ?", arrayOf(parcelId.toString()))
            .use { c -> if (c.moveToFirst()) alertCount = c.getInt(0) }

        // arma el objeto con todos los totales pa' las tarjetas del panel
        return OwnerStats(pendingPay, pendingCount, materialsTotal, materialsCount, workerCount, reportCount, alertCount)
    }

    // cuánto se le debe y cuánto pagado tiene cada trabajador de la parcela
    fun getWorkerTotals(parcelId: Long): List<WorkerTotal> {
        // la query gorda: suma lo pendiente, lo pagado y cuenta reportes por worker
        val sql = """
            SELECT u.id, u.name,
                   COALESCE(SUM(CASE WHEN r.status != 'PAID' THEN r.amount ELSE 0 END), 0) AS pending,
                   COALESCE(SUM(CASE WHEN r.status = 'PAID' THEN r.amount ELSE 0 END), 0) AS paid,
                   COUNT(r.id) AS reports
            FROM memberships m
            JOIN users u ON u.id = m.worker_id
            LEFT JOIN reports r ON r.worker_id = u.id AND r.parcel_id = m.parcel_id
            WHERE m.parcel_id = ?
            GROUP BY u.id, u.name
            ORDER BY u.name
        """.trimIndent()
        // aquí se juntan los totales por trabajador
        val list = mutableListOf<WorkerTotal>()
        // corre la query y arma un WorkerTotal por cada fila
        readableDatabase.rawQuery(sql, arrayOf(parcelId.toString())).use { c ->
            while (c.moveToNext()) {
                // id, nombre, pendiente, pagado y cuántos reportes mandó
                list.add(WorkerTotal(c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3), c.getInt(4)))
            }
        }
        return list
    }

    // calcula las 12 de la noche de hoy, pa' saber qué cuenta como "hoy"
    private fun startOfToday(): Long {
        // calendario de hoy y le ponemos la hora en cero
        val cal = Calendar.getInstance()
        // hora, minuto, segundo y milisegundos a cero: puro inicio del día
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // regresa ese momento en milisegundos
        return cal.timeInMillis
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECCIÓN: LECTORES DE CURSOR (SQLite → MODELOS)
    //  Propósito: convertir cada fila que devuelve SQLite en un objeto
    //  Kotlin (User, Parcel, Task...). Así las pantallas no trabajan con
    //  SQL sino con datos listos para mostrar.
    // ═══════════════════════════════════════════════════════════════════

    // convierte una fila SQLite en un objeto User, pa' no andar con cursores
    private fun Cursor.toUser() = User(
        // va sacando cada columna de la fila y la mete al objeto
        id = getLong(getColumnIndexOrThrow("id")),
        // si el rol dice OWNER es dueño, si no, trabajador nomás
        role = if (getString(getColumnIndexOrThrow("role")) == "OWNER") Role.OWNER else Role.WORKER,
        name = getString(getColumnIndexOrThrow("name")),
        cedula = getStringOrNull("cedula"),
        phone = getString(getColumnIndexOrThrow("phone")),
        email = getStringOrNull("email"),
        finca = getStringOrNull("finca"),
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    // convierte una fila de parcels (con owner_name incluido) a objeto Parcel
    private fun Cursor.toParcel() = Parcel(
        // va leyendo cada columna y armando la parcela
        id = getLong(getColumnIndexOrThrow("id")),
        ownerId = getLong(getColumnIndexOrThrow("owner_id")),
        ownerName = getString(getColumnIndexOrThrow("owner_name")),
        name = getString(getColumnIndexOrThrow("name")),
        location = getString(getColumnIndexOrThrow("location")),
        hectares = getDouble(getColumnIndexOrThrow("hectares")),
        // el código de invitación y cuándo vence
        code = getString(getColumnIndexOrThrow("code")),
        codeExpiresAt = getLong(getColumnIndexOrThrow("code_expires_at")),
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    // convierte una fila de tasks a objeto Task, la labor completa
    private fun Cursor.toTask() = Task(
        // lee todas las columnas de la labor
        id = getLong(getColumnIndexOrThrow("id")),
        parcelId = getLong(getColumnIndexOrThrow("parcel_id")),
        title = getString(getColumnIndexOrThrow("title")),
        description = getString(getColumnIndexOrThrow("description")),
        targetCount = getInt(getColumnIndexOrThrow("target_count")),
        unit = getString(getColumnIndexOrThrow("unit")),
        ratePerUnit = getDouble(getColumnIndexOrThrow("rate_per_unit")),
        // si no tiene trabajador asignado va null, si no, su id
        assignedWorkerId = if (isNull(getColumnIndexOrThrow("assigned_worker_id"))) null else getLong(getColumnIndexOrThrow("assigned_worker_id")),
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    // convierte una fila de reports a objeto WorkReport, todo el reporte
    private fun Cursor.toReport() = WorkReport(
        // va leyendo cada columna del reporte
        id = getLong(getColumnIndexOrThrow("id")),
        parcelId = getLong(getColumnIndexOrThrow("parcel_id")),
        workerId = getLong(getColumnIndexOrThrow("worker_id")),
        workerName = getString(getColumnIndexOrThrow("worker_name")),
        // la labor puede ir sin id, por eso el chequeo de null
        taskId = if (isNull(getColumnIndexOrThrow("task_id"))) null else getLong(getColumnIndexOrThrow("task_id")),
        taskTitle = getString(getColumnIndexOrThrow("task_title")),
        description = getString(getColumnIndexOrThrow("description")),
        units = getDouble(getColumnIndexOrThrow("units")),
        unit = getString(getColumnIndexOrThrow("unit")),
        photoPath = getStringOrNull("photo_path"),
        // la ubicación GPS puede no existir, va null si no la mandó
        latitude = if (isNull(getColumnIndexOrThrow("latitude"))) null else getDouble(getColumnIndexOrThrow("latitude")),
        longitude = if (isNull(getColumnIndexOrThrow("longitude"))) null else getDouble(getColumnIndexOrThrow("longitude")),
        amount = getDouble(getColumnIndexOrThrow("amount")),
        status = getString(getColumnIndexOrThrow("status")),
        // el 1/0 de SQLite lo volvemos boolean aquí, más claro
        synced = getInt(getColumnIndexOrThrow("synced")) == 1,
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    // convierte una fila de materials a objeto MaterialRequest
    private fun Cursor.toMaterial() = MaterialRequest(
        // lee todas las columnas del pedido
        id = getLong(getColumnIndexOrThrow("id")),
        parcelId = getLong(getColumnIndexOrThrow("parcel_id")),
        workerId = getLong(getColumnIndexOrThrow("worker_id")),
        workerName = getString(getColumnIndexOrThrow("worker_name")),
        item = getString(getColumnIndexOrThrow("item")),
        quantity = getDouble(getColumnIndexOrThrow("quantity")),
        unit = getString(getColumnIndexOrThrow("unit")),
        note = getString(getColumnIndexOrThrow("note")),
        photoPath = getStringOrNull("photo_path"),
        // precios y estado del pedido
        unitPrice = getDouble(getColumnIndexOrThrow("unit_price")),
        marketAvg = getDouble(getColumnIndexOrThrow("market_avg")),
        status = getString(getColumnIndexOrThrow("status")),
        // el 1/0 de "pagado" lo volvemos boolean, igual que el synced
        paid = getInt(getColumnIndexOrThrow("paid")) == 1,
        // el synced de la db (0/1) lo volvemos boolean aquí
        synced = getInt(getColumnIndexOrThrow("synced")) == 1,
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    // convierte una fila de diagnoses a objeto Diagnosis
    private fun Cursor.toDiagnosis() = Diagnosis(
        // lee cada columna del diagnóstico
        id = getLong(getColumnIndexOrThrow("id")),
        parcelId = getLong(getColumnIndexOrThrow("parcel_id")),
        workerId = getLong(getColumnIndexOrThrow("worker_id")),
        workerName = getString(getColumnIndexOrThrow("worker_name")),
        photoPath = getString(getColumnIndexOrThrow("photo_path")),
        // enfermedad, certeza, gravedad, hallazgo y recomendación
        disease = getString(getColumnIndexOrThrow("disease")),
        confidence = getInt(getColumnIndexOrThrow("confidence")),
        severity = getString(getColumnIndexOrThrow("severity")),
        finding = getString(getColumnIndexOrThrow("finding")),
        recommendation = getString(getColumnIndexOrThrow("recommendation")),
        // el synced de 0/1 a boolean, pa' saber si ya viajó al servidor
        synced = getInt(getColumnIndexOrThrow("synced")) == 1,
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    // lee una columna de texto y si está NULL regresa null, sin dramas
    private fun Cursor.getStringOrNull(column: String): String? {
        // busca la posición de la columna en la fila
        val index = getColumnIndexOrThrow(column)
        // si la columna está vacía manda null, si no, el texto
        return if (isNull(index)) null else getString(index)
    }

    // las constantes de toda la clase DbHelper
    companion object {
        // cuánto dura el código de invitación: 60 segundos, o sea 1 minuto
        const val CODE_LIFETIME_MS = 60_000L
    }
}
