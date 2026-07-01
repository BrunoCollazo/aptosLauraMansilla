const API_BASE = ["localhost", "127.0.0.1"].includes(window.location.hostname)
  ? "http://localhost:8080"
  : "";

const container = document.getElementById("adminApartments");
const ivaBanner = document.getElementById("ivaBanner");

// Fecha ISO (YYYY-MM-DD) -> "5 de abril de 2026". Parseo por partes para no comerme un día por
// zona horaria (new Date("2026-04-05") es UTC y puede correrse).
function fmtFecha(iso) {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString("es-UY", {
    day: "numeric", month: "long", year: "numeric",
  });
}

// Cartel: le avisa a Laura si hoy se cobra IVA y cuándo cambia. El estado lo calcula el backend
// (misma lógica que el cobro), así no duplicamos las fechas de temporada acá.
async function cargarEstadoIva() {
  try {
    const res = await fetch(`${API_BASE}/api/iva-status`);
    if (!res.ok) return;
    const s = await res.json();

    if (s.applyingIva) {
      ivaBanner.className = "iva-banner iva-banner--on";
      ivaBanner.innerHTML =
        `<strong>Se está cobrando IVA (10%).</strong> Estás en temporada alta. ` +
        `El IVA se aplica hasta el <strong>${fmtFecha(s.seasonEnd)}</strong> (Domingo de Pascua); ` +
        `después los alquileres quedan exonerados hasta el próximo 15 de noviembre.`;
    } else {
      ivaBanner.className = "iva-banner iva-banner--off";
      ivaBanner.innerHTML =
        `<strong>No se está cobrando IVA (temporada baja).</strong> Los alquileres están exonerados. ` +
        `El IVA (10%) vuelve a aplicarse el <strong>${fmtFecha(s.seasonStart)}</strong> (inicio de temporada).`;
    }
    ivaBanner.hidden = false;
  } catch (error) {
    console.error(error);
  }
}

async function cargarApartamentos() {
  try {
    const res = await fetch(`${API_BASE}/api/admin/apartments`);
    if (!res.ok) throw new Error("Error al cargar apartamentos");

    const apartments = await res.json();
    container.innerHTML = "";
    apartments
      .sort((a, b) => a.id - b.id)
      .forEach((apt) => container.appendChild(cardApartamento(apt)));
  } catch (error) {
    console.error(error);
    container.innerHTML = `
      <p class="admin-feedback">
        No se pudo conectar con el servidor. Verificá que el backend esté corriendo en ${API_BASE}.
      </p>`;
  }
}

// Pinta el valor si viene, o vacío si es null (sin ese descuento).
function val(v) {
  return v === null || v === undefined ? "" : v;
}

function cardApartamento(apt) {
  const card = document.createElement("article");
  card.className = "apt-admin";
  card.dataset.id = apt.id;

  card.innerHTML = `
    <header class="apt-admin__head">
      <h2 class="apt-admin__name">${apt.name}</h2>
      <label class="switch" title="Disponibilidad">
        <input type="checkbox" class="switch-input" data-field="available" ${apt.available ? "checked" : ""}>
        <span class="switch-slider"></span>
      </label>
    </header>

    <div class="apt-admin__field">
      <label>Precio por noche</label>
      <div class="admin-input-group">
        <span class="admin-input-symbol">$</span>
        <input type="number" class="admin-input" data-field="pricePerNight" min="1" step="1" value="${val(apt.pricePerNight)}">
      </div>
    </div>

    <fieldset class="apt-admin__discount">
      <legend>Descuento por porcentaje</legend>
      <div class="apt-admin__discount-row">
        <div class="admin-input-group">
          <input type="number" class="admin-input admin-input--percent" data-field="percentDiscount" min="0" max="100" step="1" value="${val(apt.percentDiscount)}">
          <span class="admin-input-symbol">%</span>
        </div>
        <span class="apt-admin__sep">a partir de</span>
        <div class="admin-input-group">
          <input type="number" class="admin-input admin-input--nights" data-field="percentDiscountMinNights" min="1" step="1" value="${val(apt.percentDiscountMinNights)}">
          <span class="admin-input-symbol">noches</span>
        </div>
      </div>
    </fieldset>

    <fieldset class="apt-admin__discount">
      <legend>Descuento de monto fijo</legend>
      <div class="apt-admin__discount-row">
        <div class="admin-input-group">
          <span class="admin-input-symbol">$</span>
          <input type="number" class="admin-input" data-field="amountDiscount" min="0" step="1" value="${val(apt.amountDiscount)}">
        </div>
        <span class="apt-admin__sep">a partir de</span>
        <div class="admin-input-group">
          <input type="number" class="admin-input admin-input--nights" data-field="amountDiscountMinNights" min="1" step="1" value="${val(apt.amountDiscountMinNights)}">
          <span class="admin-input-symbol">noches</span>
        </div>
      </div>
    </fieldset>

    <footer class="apt-admin__foot">
      <span class="apt-admin__status apt-admin__status--ok" data-status>Guardado</span>
      <button class="btn-warm apt-admin__save" data-save disabled>Guardar</button>
    </footer>
  `;

  return card;
}

// Cualquier edición marca la tarjeta como "sin guardar" y habilita el botón.
function marcarSinGuardar(card) {
  card.querySelector("[data-save]").disabled = false;
  const status = card.querySelector("[data-status]");
  status.textContent = "Sin guardar";
  status.className = "apt-admin__status apt-admin__status--dirty";
}

container.addEventListener("input", (e) => {
  const card = e.target.closest(".apt-admin");
  if (card) marcarSinGuardar(card);
});

container.addEventListener("change", (e) => {
  // El switch dispara "change", no "input".
  const card = e.target.closest(".apt-admin");
  if (card) marcarSinGuardar(card);
});

container.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-save]");
  if (btn) guardar(btn.closest(".apt-admin"), btn);
});

// Lee un input numérico: vacío => null (sin valor), si no el número.
function leerNum(card, field) {
  const value = card.querySelector(`[data-field="${field}"]`).value.trim();
  return value === "" ? null : Number(value);
}

async function guardar(card, btn) {
  const id = parseInt(card.dataset.id);

  const pricePerNight = leerNum(card, "pricePerNight");
  if (pricePerNight === null || pricePerNight <= 0) {
    alert("El precio por noche debe ser un valor positivo.");
    return;
  }

  const percentDiscount = leerNum(card, "percentDiscount");
  const percentMin = leerNum(card, "percentDiscountMinNights");
  if (percentDiscount && (!percentMin || percentMin < 1)) {
    alert("Indicá a partir de cuántas noches aplica el descuento por porcentaje.");
    return;
  }

  const amountDiscount = leerNum(card, "amountDiscount");
  const amountMin = leerNum(card, "amountDiscountMinNights");
  if (amountDiscount && (!amountMin || amountMin < 1)) {
    alert("Indicá a partir de cuántas noches aplica el descuento de monto fijo.");
    return;
  }

  const body = {
    available: card.querySelector('[data-field="available"]').checked,
    pricePerNight,
    percentDiscount,
    percentDiscountMinNights: percentMin,
    amountDiscount,
    amountDiscountMinNights: amountMin,
  };

  const status = card.querySelector("[data-status]");
  btn.disabled = true;
  status.textContent = "Guardando…";
  status.className = "apt-admin__status";

  try {
    const res = await fetch(`${API_BASE}/api/admin/apartments/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error("Error al guardar");

    status.textContent = "Guardado";
    status.className = "apt-admin__status apt-admin__status--ok";
  } catch (error) {
    console.error(error);
    status.textContent = "Error al guardar";
    status.className = "apt-admin__status apt-admin__status--dirty";
    btn.disabled = false;
    alert("No se pudo guardar. Intentá nuevamente.");
  }
}

cargarEstadoIva();
cargarApartamentos();
