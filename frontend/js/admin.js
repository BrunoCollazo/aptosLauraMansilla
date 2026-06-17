const API_BASE = "http://localhost:8080";

let apartments = [];

async function cargarApartamentos() {
  const tbody = document.getElementById("adminTableBody");

  try {
    const response = await fetch(`${API_BASE}/api/admin/apartments`);
    if (!response.ok) throw new Error("Error al cargar apartamentos");

    const data = await response.json();

    apartments = data.map((apt) => ({
      id: apt.id,
      name: apt.name,
      available: apt.available,
      pricePerNight: apt.pricePerNight,
      discount3: 0,
      discount7: 0,
    }));

    renderTabla();
  } catch (error) {
    console.error(error);
    tbody.innerHTML = `
      <tr>
        <td class="admin-feedback" colspan="5">
          No se pudo conectar con el servidor. Verificá que el backend esté
          corriendo en ${API_BASE}.
        </td>
      </tr>
    `;
  }
}

function renderTabla() {
  const tbody = document.getElementById("adminTableBody");
  tbody.innerHTML = apartments.map(filaHTML).join("");
}

function filaHTML(apt) {
  return `
    <tr data-id="${apt.id}">
      <td class="admin-cell-name">${apt.name}</td>
      <td>
        <label class="switch">
          <input
            type="checkbox"
            class="switch-input"
            data-field="available"
            ${apt.available ? "checked" : ""}
          >
          <span class="switch-slider"></span>
        </label>
      </td>
      <td>
        <div class="admin-input-group">
          <span class="admin-input-symbol">$</span>
          <input
            type="number"
            class="admin-input"
            data-field="pricePerNight"
            min="1"
            step="1"
            value="${apt.pricePerNight}"
          >
        </div>
      </td>
      <td>
        <div class="admin-input-group">
          <input
            type="number"
            class="admin-input admin-input--percent"
            data-field="discount3"
            min="0"
            max="100"
            step="1"
            value="${apt.discount3}"
          >
          <span class="admin-input-symbol">%</span>
        </div>
      </td>
      <td>
        <div class="admin-input-group">
          <input
            type="number"
            class="admin-input admin-input--percent"
            data-field="discount7"
            min="0"
            max="100"
            step="1"
            value="${apt.discount7}"
          >
          <span class="admin-input-symbol">%</span>
        </div>
      </td>
    </tr>
  `;
}

function buscarApartamento(id) {
  return apartments.find((apt) => apt.id === id);
}

document.getElementById("adminTableBody").addEventListener("change", (e) => {
  const input = e.target;
  const fila = input.closest("tr");
  if (!fila) return;

  const id = parseInt(fila.dataset.id);
  const apt = buscarApartamento(id);
  if (!apt) return;

  const field = input.dataset.field;

  if (field === "available") {
    actualizarDisponibilidad(apt, input);
  } else if (field === "pricePerNight") {
    actualizarPrecio(apt, input);
  } else if (field === "discount3" || field === "discount7") {
    actualizarDescuento(apt, field, input);
  }
});

async function actualizarDisponibilidad(apt, checkbox) {
  const nuevoValor = checkbox.checked;
  const valorAnterior = apt.available;
  apt.available = nuevoValor;

  try {
    const response = await fetch(
      `${API_BASE}/api/admin/apartments/${apt.id}/availability?available=${nuevoValor}`,
      { method: "PUT" }
    );
    if (!response.ok) throw new Error("Error al actualizar disponibilidad");
  } catch (error) {
    console.error(error);
    apt.available = valorAnterior;
    checkbox.checked = valorAnterior;
    alert("No se pudo actualizar la disponibilidad. Intentá nuevamente.");
  }
}

async function actualizarPrecio(apt, input) {
  const valor = parseFloat(input.value);
  const valorAnterior = apt.pricePerNight;

  if (isNaN(valor) || valor <= 0) {
    input.value = valorAnterior;
    alert("El precio por noche debe ser un valor positivo.");
    return;
  }

  apt.pricePerNight = valor;

  try {
    const response = await fetch(
      `${API_BASE}/api/admin/apartments/${apt.id}/price?price=${valor}`,
      { method: "PUT" }
    );
    if (!response.ok) throw new Error("Error al actualizar el precio");
  } catch (error) {
    console.error(error);
    apt.pricePerNight = valorAnterior;
    input.value = valorAnterior;
    alert("No se pudo actualizar el precio. Intentá nuevamente.");
  }
}

// El backend todavía no expone un endpoint para editar descuentos por apartamento
// (el Discount actual es global y no está asociado a un Apartment). El valor se
// guarda por ahora en el estado local, ya estructurado para enviarse al backend
// en cuanto exista el endpoint correspondiente.
function actualizarDescuento(apt, field, input) {
  const valor = parseInt(input.value, 10);
  const valorAnterior = apt[field];

  if (isNaN(valor) || valor < 0 || valor > 100) {
    input.value = valorAnterior;
    alert("El descuento debe ser un valor entre 0 y 100.");
    return;
  }

  apt[field] = valor;
}

cargarApartamentos();
