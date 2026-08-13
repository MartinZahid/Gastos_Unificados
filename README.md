# Consolidado de Gastos

App Android 100% local que consolida tus gastos de tarjeta de crédito leyendo las **notificaciones de los bancos** en tu teléfono: detecta el comercio, el monto y el banco automáticamente y los agrega a un solo registro, sin enviar ningún dato a internet.

## Qué hace

- **Captura automática**: un `NotificationListenerService` escucha las notificaciones de los bancos y, cuando detecta una compra, la guarda en la base de datos local.
- **Parsing inteligente**: extrae comercio, monto y banco de textos muy variados (compra en, retiro/compra, pago autorizado, comercio X, etc.).
- **Consolidación**: todas tus tarjetas en una sola lista, con total, promedio, mayor gasto, gasto del mes y desglose por banco.
- **Filtros**: búsqueda por comercio y filtro por banco desde un menú lateral.
- **Edición manual**: toca cualquier movimiento para editarlo o eliminarlo.
- **Alerta de no reconocidas**: si un banco cambia el formato de sus notificaciones y el parser deja de leerlas, la app avisa de forma proactiva (badge en el menú lateral + banner en Modo dev + notificación con umbral de 3 y enfriamiento de 12 h) para no perder gastos en silencio.
- **Modo dev**: entorno para "alimentar" el parser (ver sección más abajo).

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Base de datos | Room (SQLite) |
| Gradle | 8.9 (wrapper) |
| AGP | 8.5.2 |
| Kotlin | 2.0.21 + KSP 2.0.21-1.0.28 |
| Compose BOM | 2024.10.01 |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 |

## Cómo funciona el parser

`NotificationParser` procesa cada notificación:

1. **Monto** — busca patrones de dinero: `$110.00`, `MXN $110`, `monto $110`, `110.00 MXN/pesos`, etc.
2. **Comercio** — busca frases clave en este orden: `comercio/establecimiento/adquiriente`, `compra en/por`, `aprobada/autorizado en`, `pago/pagaste/compraste`, verbos con monto intermedio (`pagaste $109.00 en Carls Jr`), `cargo/realizado/retiro-compra`, `compra <X>`, y un fallback antes de `por/monto/importe`.
3. **Límites** — recorta el comercio en palabras como `monto`, `importe`, `total`, `por`, `con`, `de`, números de tarjeta (`BANAMEX512`), fechas y montos.
4. **Banco** — detecta el banco por el texto (`BANAMEX` → Citibanamex, `BANORTE` → Banorte, `BBVA`, `SANTANDER`/`SUPERMOVIL` → Santander, `MERCADO` → Mercado Pago, `NUBANK`/`NU` → Nubank, `RAPPI` → Rappi) y además usa el paquete de origen como fuente confiable.

## Bancos soportados

La escucha está configurada para estos paquetes (banca "Activa" con notificaciones):

| App | Paquete |
|---|---|
| Nu (Nubank) | `com.nu.production` |
| Mercado Pago | `com.mercadopago.wallet` |
| BBVA | `com.bancomer.mbanking`, `com.bbva.bancomer.app` |
| Santander | `mx.bancosantander.supermovil`, `com.santander.latam.mx` |
| Citibanamex | `com.banamex.banamex` |
| App propia (pruebas) | `com.example.gastos` |

Para agregar otro banco, añade su paquete en `TargetPackages` y su nombre en `BankNames` dentro de `BankNotificationListener.kt`.

## Modo dev

Accesible desde el menú ☰ → **Modo dev**. Pensado para entrenar el parser:

- **Probar** — pega cualquier texto de notificación y muestra si se parsea (OK) con comercio/monto/banco, o el motivo del fallo (NO). Trae un corpus de ejemplos.
- **Simular** — publica una notificación real desde la propia app (pide permiso la primera vez) para ver el flujo completo de captura.
- **Log de notificaciones** — registra TODAS las notificaciones escuchadas (no solo las de bancos) con su badge OK/NO. Acciones por entrada:
  - **Compra** — si ya se parseó, la inserta al instante; si no, abre un formulario manual y **aprende la frase** que antecede al comercio para reconocerla la próxima vez.
  - **Ignorar** — aprende una palabra clave para ignorar ese tipo de notificaciones (promociones, saldo, etc.).
  - **Tipo** — etiqueta la notificación con un tipo fijo (Transferencia, Saldo, Promoción, Retiro, Otro) o uno personalizado.
- **Frases aprendidas** — lista de frases COMPRA/IGNORAR con opción de borrar o agregar manualmente.

Las frases aprendidas se guardan en la base de datos local (tabla `learned_patterns`) y se aplican en el siguiente parseo.

## Build e instalación

Requisitos: JDK 17 y Android SDK (ruta en `local.properties` → `sdk.dir`).

```bash
# Compilar
./gradlew.bat :app:assembleDebug

# Instalar (teléfono conectado por USB con depuración activa)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Primeros pasos en el teléfono

1. Abre la app y toca **Permisos** (o el chip de estado en la cabecera).
2. Activa "Permisos de notificaciones" → permite el acceso a **Consolidado de Gastos**.
3. Asegúrate de que la app de tu banco tenga **"Escucha activa"** / notificaciones habilitadas.
4. Cada compra se irá sumando automáticamente. Si algo no se reconoce, entra al **Modo dev** y enséñale la frase.

## Estructura del proyecto

```
app/src/main/java/com/example/gastos/
├── data/               # Room: entidades y DAOs (Transaction, NotificationLog, LearnedPattern)
├── engine/             # NotificationParser (extracción de monto, comercio y banco)
├── listener/           # BankNotificationListener (servicio de escucha de notificaciones)
└── ui/                 # Compose: HomeScreen, DevScreen, TransactionViewModel, theme
```

## Privacidad

La app es **100% local**: toda la información (movimientos, notificaciones, frases aprendidas) vive únicamente en la base de datos SQLite del dispositivo. No hay cuentas, ni servidores, ni telemetría.

## Estado actual

Funcionalidad base implementada y probada en dispositivo (Samsung S24). El parser resuelve los formatos más comunes de Citibanamex (incluye el formato `Retiro/Compra COSTCO … BANAMEX512 monto $110.00`), Nubank, BBVA, Santander, Mercado Pago y Rappi (incluye formatos con monto entre el verbo y el comercio, p. ej. `Pagaste $109.00 en Carls Jr`), y es extensible vía Modo dev. Incluye alerta proactiva cuando una notificación de banco no se reconoce (umbral 3, enfriamiento 12 h).
