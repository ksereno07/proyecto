/*
 * Models.kt - MODELOS DE DATOS
 * ----------------------------
 * Clases simples (data class) que representan la informacion de la app:
 * usuarios, parcelas, labores, reportes de trabajo, solicitudes de
 * materiales, diagnosticos del escaner, precios y totales.
 *
 * No contienen logica: solo transportan datos desde SQLite hacia las
 * pantallas de Compose.
 */

// el paquete de datos, aquí viven todos los modelos wey
package com.agropalma.app.data

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: ROLES
//  Propósito: diferencia entre las dos clases de usuario de la app.
//  OWNER = dueño/administrador de la parcela. WORKER = trabajador de
//  la cuadrilla que se une con código.
// ═══════════════════════════════════════════════════════════════════
// los dos roles que existen: dueño o trabajador, ahí nomás, no hay más
enum class Role { OWNER, WORKER }

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: USUARIO (cuenta de la app)
//  Representa a dueños y trabajadores. El dueño usa 'phone' para entrar;
//  el trabajador puede entrar con celular o cédula.
// ═══════════════════════════════════════════════════════════════════
data class User(
    // su id único en la base de datos
    val id: Long,
    // pa' saber si es dueño o trabajador de campo
    val role: Role,
    // el nombre completo de la persona
    val name: String,
    // la cédula, puede ir vacía si es dueño
    val cedula: String?,
    // el celular, con este entra el dueño
    val phone: String,
    // el correo, va opcional wey
    val email: String?,
    // el nombre de la finca del dueño, también opcional
    val finca: String?,
    // fecha en milisegundos de cuándo se creó la cuenta
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: PARCELA (la finca o lote de palma)
//  Guarda quién es el dueño, el nombre, la ubicación, la extensión y el
//  código de invitación PALMA-XXXX con su fecha de vencimiento.
// ═══════════════════════════════════════════════════════════════════
data class Parcel(
    // id de la parcela en la db
    val id: Long,
    // el id del dueño que la creó
    val ownerId: Long,
    // nombre del dueño, pa' mostrárselo al trabajador
    val ownerName: String,
    // cómo se llama la parcela
    val name: String,
    // dónde queda la finca
    val location: String,
    // la extensión en hectáreas
    val hectares: Double,
    // el código de invitación PALMA-XXXX
    val code: String,
    // cuándo vence el código (dura un minuto, rapidito)
    val codeExpiresAt: Long,
    // fecha de creación de la parcela
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: LABOR (tarea de la finca)
//  Cada parcela se crea con 4 labores base (poda, cosecha, plateo y
//  aplicación). 'targetCount' es la meta y 'ratePerUnit' lo que se paga
//  por cada unidad terminada.
// ═══════════════════════════════════════════════════════════════════
data class Task(
    // id de la labor
    val id: Long,
    // a qué parcela pertenece
    val parcelId: Long,
    // el nombre de la labor, ej. "Poda Sanitaria"
    val title: String,
    // las instrucciones de cómo hacerla
    val description: String,
    // la meta: cuántas unidades hay que hacer
    val targetCount: Int,
    // en qué se mide: palmas, racimos, platos...
    val unit: String,
    // lo que se paga por cada unidad terminada
    val ratePerUnit: Double,
    // si va asignada a un trabajador fijo, null si es pa' cualquiera
    val assignedWorkerId: Long?,
    // cuándo se creó la labor
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: REPORTE DE TRABAJO (avance enviado por el trabajador)
//  Guarda la labor, la cantidad hecha, la foto de evidencia, la posición
//  GPS, el valor a pagar (cantidad x tarifa) y su estado (PENDING o PAID).
//  'synced' dice si ya se envió al servidor o si sigue esperando señal.
// ═══════════════════════════════════════════════════════════════════
data class WorkReport(
    // id del reporte
    val id: Long,
    // en qué parcela se trabajó
    val parcelId: Long,
    // id del trabajador que reportó
    val workerId: Long,
    // nombre del worker, pa' que el dueño sepa quién fue
    val workerName: String,
    // la labor que se hizo, si aplica
    val taskId: Long?,
    // el nombre de la labor, pa' mostrarlo directo
    val taskTitle: String,
    // lo que escribió contando su avance
    val description: String,
    // cuántas unidades hizo
    val units: Double,
    // la unidad en que se mide
    val unit: String,
    // la ruta de la foto de evidencia, si es que subió una
    val photoPath: String?,
    // latitud del GPS donde estaba parado
    val latitude: Double?,
    // longitud del GPS donde estaba parado
    val longitude: Double?,
    // la plata a pagar: unidades x tarifa, ya calculadito
    val amount: Double,
    // PENDING si falta pagar, PAID si ya le pagaron
    val status: String,
    // true si ya viajó al servidor, false si espera señal
    val synced: Boolean,
    // cuándo se mandó el reporte
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: SOLICITUD DE MATERIAL (pedido de insumos)
//  Guarda el insumo, la cantidad, la nota del trabajador y el precio
//  estimado (cantidad x precio promedio de mercado). Estado: PENDING o
//  DELIVERED (entregado). 'paid' dice si el dueño YA PAGÓ ese insumo
//  (para la gráfica de pagos y el control de la cartera). 'synced' =
//  si ya se envió o espera señal.
// ═══════════════════════════════════════════════════════════════════
data class MaterialRequest(
    // id del pedido
    val id: Long,
    // para qué parcela es el material
    val parcelId: Long,
    // quién lo pidió
    val workerId: Long,
    // el nombre del worker que pidió
    val workerName: String,
    // el insumo que pidió, ej. fertilizante
    val item: String,
    // cuánto pidió
    val quantity: Double,
    // en qué se mide: litros, bultos, etc.
    val unit: String,
    // la nota o aclaración que dejó
    val note: String,
    // foto del material, va opcional
    val photoPath: String?,
    // el precio por unidad del insumo
    val unitPrice: Double,
    // el precio promedio del mercado pa' comparar
    val marketAvg: Double,
    // PENDING si falta aprobar, DELIVERED si ya llegó
    val status: String,
    // true si el dueño ya pagó este insumo, false si debe plata
    val paid: Boolean,
    // true si ya se envió al servidor, false si está en la cola
    val synced: Boolean,
    // cuándo se hizo el pedido
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: DIAGNÓSTICO (resultado del escáner de palma)
//  Guarda la foto analizada, la enfermedad probable, la certeza, la
//  gravedad, el hallazgo visual y la recomendación de tratamiento.
//  'synced' = si ya viajó al servidor o sigue en la cola sin señal.
// ═══════════════════════════════════════════════════════════════════
data class Diagnosis(
    // id del diagnóstico
    val id: Long,
    // la parcela donde está la palma analizada
    val parcelId: Long,
    // quién escaneó la palma
    val workerId: Long,
    // el nombre del worker que escaneó
    val workerName: String,
    // la foto de la palma que se analizó
    val photoPath: String,
    // la enfermedad que el escáner cree que es
    val disease: String,
    // qué tan seguro está el escáner (en porcentaje)
    val confidence: Int,
    // qué tan grave está la palma
    val severity: String,
    // lo que se ve en la foto, el hallazgo
    val finding: String,
    // qué hay que hacerle pa' tratarla
    val recommendation: String,
    // true si ya viajó al servidor, false si sigue en la cola
    val synced: Boolean,
    // cuándo se hizo el diagnóstico
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: PRECIO DE MERCADO (insumo, unidad y precio promedio)
//  Sirve para estimar el costo de los pedidos de la cuadrilla.
// ═══════════════════════════════════════════════════════════════════
data class PriceItem(
    // el nombre del insumo
    val item: String,
    // cómo se vende: bulto, litro, par...
    val unit: String,
    // el precio promedio de referencia
    val avgPrice: Double
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: TOTALES DEL PANEL DEL DUEÑO
//  'OwnerStats' alimenta las tarjetas del inicio (por pagar, insumos,
//  trabajadores, reportes del día y alertas de salud).
// ═══════════════════════════════════════════════════════════════════
data class OwnerStats(
    // la plata que se debe pagar en reportes pendientes
    val pendingPay: Double = 0.0,
    // cuántos reportes faltan por pagar
    val pendingPayCount: Int = 0,
    // lo que costarían los materiales pedidos
    val materialsTotal: Double = 0.0,
    // cuántos pedidos de material hay
    val materialsCount: Int = 0,
    // cuánta cuadrilla trabaja en la parcela
    val workerCount: Int = 0,
    // reportes que llegaron hoy nomás
    val reportCount: Int = 0,
    // alertas de salud, o sea, palmas enfermas detectadas
    val alertCount: Int = 0
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: SALDO POR TRABAJADOR
//  Cuánto tiene pendiente y cuánto pagado cada trabajador, y cuántos
//  reportes ha enviado. Lo usa la pantalla de Costos del dueño.
// ═══════════════════════════════════════════════════════════════════
data class WorkerTotal(
    // id del trabajador
    val workerId: Long,
    // su nombre
    val workerName: String,
    // la plata que aún se le debe
    val pending: Double,
    // la plata que ya se le pagó
    val paid: Double,
    // cuántos reportes ha mandado en total
    val reports: Int
)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: RESPUESTA DE UNIRSE CON CÓDIGO
//  Devuelve si la unión fue exitosa y, si no, el motivo (código
//  inválido, vencido o trabajador ya vinculado).
// ═══════════════════════════════════════════════════════════════════
// lo que regresa unirse con código: si funcionó, el mensaje y la parcela (si la hay)
data class JoinResult(val success: Boolean, val message: String, val parcel: Parcel? = null)

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN: RESUMEN DE PAGOS (pa' la gráfica del dueño)
//  Los 4 números de la cartera de la finca en una sola pieza: lo pagado
//  y lo pendiente de la cuadrilla, y lo pagado y lo pendiente de los
//  insumos. Con esto se alimenta la gráfica de barras de pagos.
// ═══════════════════════════════════════════════════════════════════
data class PaymentsSummary(
    // la plata ya liquidada a los trabajadores (reportes PAID)
    val workersPaid: Double = 0.0,
    // la plata que aún se les debe (reportes PENDING)
    val workersPending: Double = 0.0,
    // lo ya pagado en insumos/materiales (paid = 1)
    val materialsPaid: Double = 0.0,
    // lo que costarán los insumos que aún no se pagan (paid = 0)
    val materialsPending: Double = 0.0
)
