const selector = document.getElementById("opcion");
const titulo = document.getElementById("titulo");
const descripcion = document.getElementById("descripcion");

const descripciones = {
  1: "Apartamento cómodo con cama individual y sofa cama de plaza y media.",
  2: "Apartamento para dos personas con Sommier de 2 plazas",
  3: "Apartamento para dos personas con Sommier de 2 plazas",
  4: "Apartamento amplio con Sommier de 2 plazas y 2 sommier individual"
};

selector.addEventListener("change", () => {
  const opcion = selector.value;

  titulo.textContent = `Apartamento – Opción ${opcion}`;
  descripcion.textContent = descripciones[opcion];
});
const track = document.querySelector(".carousel-track");
const slides = Array.from(document.querySelectorAll(".slide"));
const nextBtn = document.querySelector(".nav.next");
const prevBtn = document.querySelector(".nav.prev");

let currentIndex = 0;

function updateCarousel() {
  track.style.transform = `translateX(-${currentIndex * 100}%)`;

  // Pausar videos que no están activos
  slides.forEach((slide, index) => {
    const video = slide.querySelector("video");
    if (video && index !== currentIndex) {
      video.pause();
    }
  });
}

nextBtn.addEventListener("click", () => {
  currentIndex = (currentIndex + 1) % slides.length;
  updateCarousel();
});

prevBtn.addEventListener("click", () => {
  currentIndex =
    (currentIndex - 1 + slides.length) % slides.length;
  updateCarousel();
});

/* Swipe para mobile */
let startX = 0;

track.addEventListener("touchstart", (e) => {
  startX = e.touches[0].clientX;
});

track.addEventListener("touchend", (e) => {
  const endX = e.changedTouches[0].clientX;
  const diff = startX - endX;

  if (diff > 50) {
    nextBtn.click();
  } else if (diff < -50) {
    prevBtn.click();
  }
});
const backTitle = document.getElementById("backTitle");

backTitle.addEventListener("click", () => {
  if (window.history.length > 1) {
    window.history.back();
  } else {
    window.location.href = "index.html";
  }
});
const lightbox = document.getElementById("lightbox");
const lightboxImg = document.getElementById("lightbox-img");
const lightboxVideo = document.getElementById("lightbox-video");

document.querySelectorAll(".slide img, .slide video").forEach(media => {
  media.addEventListener("click", () => {
    lightbox.classList.remove("hidden");

    if (media.tagName === "IMG") {
      lightboxVideo.style.display = "none";
      lightboxImg.style.display = "block";
      lightboxImg.src = media.src;
    } else {
      lightboxImg.style.display = "none";
      lightboxVideo.style.display = "block";
      lightboxVideo.src = media.querySelector("source").src;
    }
  });
});

lightbox.addEventListener("click", () => {
  lightbox.classList.add("hidden");
  lightboxVideo.pause();
});
const API_BASE = ["localhost", "127.0.0.1"].includes(window.location.hostname)
  ? "http://localhost:8080"
  : "";
const precioPorNoche = 2500;
const nochesSelect = document.getElementById("noches");
const totalElement = document.getElementById("total");
const btnPago = document.getElementById("btnPago");

function actualizarTotal() {
  const noches = parseInt(nochesSelect.value);
  const total = noches * precioPorNoche;

  totalElement.textContent = `$${total.toLocaleString("es-UY")}`;
}

nochesSelect.addEventListener("input", actualizarTotal);

// Inicializar
actualizarTotal();

btnPago.addEventListener("click", () => {
  const noches = nochesSelect.value;
  const opcion = selector.value;

  // Más adelante acá va la redirección a Fiserv
});
function actualizarTotal() {
  const noches = parseInt(nochesSelect.value);

  if (!isNaN(noches) && noches > 0) {
    const total = noches * precioPorNoche;
    totalElement.textContent = `$${total.toLocaleString("es-UY")}`;
  } else {
    totalElement.textContent = "$0";
  }
}


const modal = document.getElementById("legalModal");
const aceptarCheckbox = document.getElementById("aceptoLegal");
const confirmarPagoBtn = document.getElementById("confirmarPago");

btnPago.addEventListener("click", () => {
  const noches = parseInt(nochesSelect.value);

  if (isNaN(noches) || noches < 1) {
    alert("Por favor ingresá una cantidad válida de noches (mínimo 1).");
    nochesSelect.focus();
    return;
  }

  modal.classList.remove("hidden");
});


aceptarCheckbox.addEventListener("change", () => {
  confirmarPagoBtn.disabled = !aceptarCheckbox.checked;
});

confirmarPagoBtn.addEventListener("click", async () => {
  modal.classList.add("hidden");

  const noches = parseInt(nochesSelect.value);
  const opcion = parseInt(selector.value);

  try {

    const response = await fetch(`${API_BASE}/api/payments`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        apartmentId: opcion,
        nights: noches,
        clientEmail: "cliente@test.com"
      })
    });

    if (!response.ok) {
      throw new Error("Error al crear el pago");
    }

    const data = await response.json();

    // Redirige al gateway de pago
    window.location.href = data.redirectUrl;

  } catch (error) {

    console.error(error);
    alert("Hubo un problema al iniciar el pago. Intenta nuevamente.");

  }
});
