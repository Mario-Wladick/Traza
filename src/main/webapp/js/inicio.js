/* =========================================================
   TRAZA · Página de inicio
   Botones del menú, menú del celular, modal de registro y avisos
   ========================================================= */
(function () {
  "use strict";

  // Aviso flotante
  function avisar(texto, tipo) {
    var caja = document.getElementById("avisos");
    var div = document.createElement("div");
    div.className = "aviso" + (tipo === "error" ? " error" : "");
    div.textContent = texto;
    caja.appendChild(div);
    setTimeout(function () { div.remove(); }, 3500);
  }

  // Menú del celular
  var btnMenu = document.getElementById("btn-hamburguesa");
  var menuMovil = document.getElementById("menu-movil");
  btnMenu.addEventListener("click", function () {
    var abierto = menuMovil.classList.toggle("abierto");
    btnMenu.setAttribute("aria-expanded", abierto);
  });
  menuMovil.addEventListener("click", function (e) {
    if (e.target.tagName === "A") menuMovil.classList.remove("abierto");
  });

  // Marcar en el menú la sección que se está viendo
  var enlaces = document.querySelectorAll(".menu-links a");
  var secciones = ["inicio", "cursos", "blog"].map(function (id) { return document.getElementById(id); });
  function marcarSeccion() {
    var actual = "inicio";
    secciones.forEach(function (s) {
      if (s.getBoundingClientRect().top < 140) actual = s.id;
    });
    enlaces.forEach(function (a) { a.classList.toggle("activo", a.dataset.seccion === actual); });
  }
  window.addEventListener("scroll", marcarSeccion, { passive: true });

  // Cursos del semestre (los pide al servidor)
  var lista = document.getElementById("lista-cursos-inicio");
  Modelo.cursosPublicos().then(function (resp) { (resp.cursos || []).forEach(function (c) {
    var art = document.createElement("article");
    art.className = "tarjeta-curso";
    art.innerHTML =
      '<span class="codigo"></span><h3></h3><p></p>' +
      '<a class="btn btn-claro btn-chico" href="app.html#/ingresar" style="align-self:flex-start;margin-top:8px">Ver en TRAZA</a>';
    art.querySelector(".codigo").textContent = c.codigo + " · Ciclo " + c.ciclo;
    art.querySelector("h3").textContent = c.nombre;
    art.querySelector("p").textContent = c.matriculados + " estudiantes · " + c.docenteNombre;
    lista.appendChild(art);
  }); });

  // Modal de registro
  var modal = document.getElementById("modal-registro");
  function abrirModal() { modal.classList.remove("oculto"); document.getElementById("reg-nombre").focus(); menuMovil.classList.remove("abierto"); }
  function cerrarModal() { modal.classList.add("oculto"); }
  document.querySelectorAll('[data-abrir="modal-registro"]').forEach(function (b) { b.addEventListener("click", abrirModal); });
  modal.addEventListener("click", function (e) { if (e.target === modal || e.target.hasAttribute("data-cerrar")) cerrarModal(); });
  document.addEventListener("keydown", function (e) { if (e.key === "Escape") cerrarModal(); });

  document.getElementById("form-registro").addEventListener("submit", async function (e) {
    e.preventDefault();
    var form = this;
    var nombre = document.getElementById("reg-nombre").value.trim();
    var codigo = document.getElementById("reg-codigo").value.trim();
    var rol = document.getElementById("reg-rol").value;
    var clave = document.getElementById("reg-clave").value;
    var clave2 = document.getElementById("reg-clave2").value;
    var error = document.getElementById("reg-error");
    function mostrar(texto) { error.textContent = texto; error.classList.remove("oculto"); }
    if (!nombre) return mostrar("Escribe tu nombre completo.");
    if (!/^\d{6}$/.test(codigo)) return mostrar("El código universitario tiene 6 dígitos.");
    if (clave.length < 6) return mostrar("La contraseña debe tener al menos 6 caracteres.");
    if (clave !== clave2) return mostrar("Las contraseñas no coinciden.");
    var boton = form.querySelector("button[type=submit]");
    boton.disabled = true;
    var resp = await Modelo.solicitarCuenta(codigo, nombre, rol, clave);
    boton.disabled = false;
    if (!resp.ok) return mostrar(resp.mensaje);
    error.classList.add("oculto");
    form.reset();
    cerrarModal();
    avisar("Listo, " + nombre.split(" ")[0] + ". Tu solicitud llegó a la secretaría académica. Cuando la apruebe, ingresa con tu código " + codigo + ".");
  });

  // Botones que aún no tienen pantalla
  document.querySelectorAll("[data-aviso]").forEach(function (b) {
    b.addEventListener("click", function () { avisar(b.dataset.aviso); });
  });
})();
