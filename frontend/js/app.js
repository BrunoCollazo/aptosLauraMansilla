// En local pegamos al backend en :8080; en producción va relativo (lo proxea nginx).
const API_BASE = ["localhost", "127.0.0.1"].includes(window.location.hostname)
  ? "http://localhost:8080"
  : "";

// El backend solo guarda nombre/precio/disponibilidad. La descripción y la foto de cada
// unidad las tenemos acá hasta que el back las soporte. Si aparece un id que no está en el
// mapa, usamos un texto/imagen por defecto.
const APT_INFO = {
  1: { desc: "Cómodo, con cama individual y sofá cama de plaza y media.", img: "Fachada.jpg" },
  2: { desc: "Para dos personas, con sommier de dos plazas.", img: "Cama.jpg" },
  3: { desc: "Para dos personas, con sommier de dos plazas.", img: "CamaConToallas.jpg" },
  4: { desc: "Amplio, con sommier de dos plazas y dos camas individuales.", img: "Fachada.jpg" }
};

const FALLBACK_INFO = { desc: "Apartamento equipado en Castillos, Rocha.", img: "Fachada.jpg" };

const grid = document.getElementById("apartmentsGrid");

function formatPrecio(valor) {
  return `$${Number(valor).toLocaleString("es-UY")}`;
}

function tarjetaApartamento(apto) {
  const info = APT_INFO[apto.id] || FALLBACK_INFO;
  const disponible = apto.available;

  const card = document.createElement("article");
  card.className = "apt-card" + (disponible ? "" : " apt-card--off");

  card.innerHTML = `
    <div class="apt-card__media">
      <img src="assets/images/${info.img}" alt="${apto.name}" loading="lazy" />
      ${disponible ? "" : '<span class="apt-card__badge">No disponible</span>'}
    </div>
    <div class="apt-card__body">
      <h3 class="apt-card__name">${apto.name}</h3>
      <p class="apt-card__desc">${info.desc}</p>
      <p class="apt-card__price">
        <strong>${formatPrecio(apto.pricePerNight)}</strong> <span>/ noche + IVA</span>
      </p>
      ${disponible
        ? `<a class="btn-warm" href="detalle.html?id=${apto.id}">Ver y reservar →</a>`
        : `<span class="btn-warm btn-warm--disabled">No disponible</span>`}
    </div>
  `;

  return card;
}

async function cargarApartamentos() {
  try {
    const res = await fetch(`${API_BASE}/api/apartments`);
    if (!res.ok) throw new Error("No se pudo cargar la lista");

    const apartamentos = await res.json();
    grid.innerHTML = "";

    if (!apartamentos.length) {
      grid.innerHTML = '<p class="public-feedback">No hay apartamentos disponibles por el momento.</p>';
      return;
    }

    apartamentos
      .sort((a, b) => a.id - b.id)
      .forEach((apto) => grid.appendChild(tarjetaApartamento(apto)));
  } catch (error) {
    console.error(error);
    grid.innerHTML = '<p class="public-feedback">No pudimos cargar los apartamentos. Probá recargar la página.</p>';
  }
}

cargarApartamentos();
