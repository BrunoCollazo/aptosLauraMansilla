# Estilos personalizados de la página de pago (Carat Gateway)

La página de pago de Fiserv es **hosted** (vive en su dominio), así que no se toca su HTML.
La única forma de personalizarla es modificar los CSS que Fiserv provee y devolvérselos.

## Flujo (confirmado por Rodrigo, jun 2026)

1. Fiserv manda los CSS base (`payment.css`, `addCard.css`) — los originales están en `Downloads`.
2. Nosotros devolvemos la **versión modificada** (estos archivos) vía virtu.
3. En un **despliegue programado de Carat** impactan en el ambiente (test o prod).
   → Conviene pedir que **primero lo apliquen en TEST** para verlo antes de prod.
4. Fiserv nos pasa un **código de estilo** que hay que enviar en el request de `/payment`
   (pendiente de cablear: iría en una env var tipo `FISERV_STYLE_CODE`, sin hardcodear).

Se personalizan **colores, logo y fuentes**. Hoy: **colores + fuentes** (Laura no tiene logo todavía).

## Qué se cambió

### Colores
Se mapearon los 3 colores del tema naranja de fábrica a la paleta del sitio
(lauramansilla.com, paleta cálida terracota/oliva):

| Fiserv (default) | Rol | Laura (sitio) |
|---|---|---|
| `#FF6600` | acento principal (título, encabezados, botones, monto, foco) | `#c4683f` terracota |
| `#1abc9c` | acento secundario (headers de tabla, fila/pestaña activa) | `#6b7257` oliva |
| `#BC0D41` | botón estado activo | `#a8552f` terracota oscuro |
| `#222` | hover de botón | `#3a322c` tinta |
| `rgba(26,188,156,…)` | sombra de foco (era el verde agua) | `rgba(107,114,87,…)` oliva |

### Fuentes
- **Encabezados** (`.title h1`, `h3`, `.paymentInfo h3`) → `Georgia, 'Times New Roman', serif`.
- **Cuerpo, botones y validaciones** → `system-ui, -apple-system, 'Segoe UI', Roboto, Arial, sans-serif`
  (antes referenciaban `'Fiserv Font'`).

Se usa **Georgia** (serif web-safe) en vez de la Playfair Display del sitio porque la web-font
solo cargaría si la página de Fiserv la importa, y un `@import` externo puede quedar bloqueado por
su CSP. Georgia da la misma elegancia serif sin depender de nada externo.

## Preview local

`preview.html` enlaza estos dos CSS y reproduce los elementos clave (barra de título, monto,
formulario de tarjeta, botones, pestañas, tabla) para juzgar **colores y tipografía** sin depender
de Fiserv. El layout es aproximado (falta Bootstrap, que sí está en la página real). Para una prueba
1:1 sobre la página real, usar **Local Overrides** de Chrome DevTools sobre `payment.css`/`addCard.css`.
