/* =========================================================
   TRAZA · CONTROLADOR del navegador
   Lee la ruta (#/...), pide los datos al servidor a través de
   Modelo, y elige qué vista mostrar. No decide reglas del negocio:
   eso lo hace el modelo Java en el servidor.

   Rutas de la Práctica 4:
     #/ingresar
     #/cursos
     #/cursos/{id}/asistencia/{sesion}
     #/cursos/{id}/asistencia/{sesion}/resumen
     #/cursos/{id}/evidencias
     #/cursos/{id}/portafolio
     #/cursos/{id}/estudiantes
     #/matriculas/nueva
     #/portafolio
     #/portafolio/evidencias/nueva
   ========================================================= */
(function () {
  "use strict";

  var usuario = null;        // quién tiene la sesión abierta
  var cursosSesion = [];     // sus cursos (para los menús)
  var turno = 0;             // evita pintar una página vieja si el usuario ya cambió de ruta
  var estado = {
    asistencia: null,        // { curso, sesion, estados, estudiantes, error }
    sinSenal: false,
    filtroEvidencias: "por",
    matricula: { valores: { codigo: "", cursos: [] }, resultado: null, error: "" },
    subida: { valores: {}, archivo: null, confirmacion: "", error: "" },
    ingreso: { codigo: "", clave: "", error: "" }
  };

  function inicioDe(rol) {
    return { docente: "#/cursos", estudiante: "#/portafolio", secretaria: "#/matriculas/nueva", director: "#/portafolio-cursos" }[rol];
  }
  function ir(ruta) { if (location.hash === ruta) atender(); else location.hash = ruta; }
  function primerCurso() { return cursosSesion.length ? cursosSesion[0].id : null; }
  function migaInicio() {
    return { docente: { txt: "Mis cursos", ruta: "#/cursos" }, estudiante: { txt: "Mi portafolio", ruta: "#/portafolio" },
      secretaria: { txt: "Matrícula", ruta: "#/matriculas/nueva" }, director: { txt: "Portafolios", ruta: "#/portafolio-cursos" } }[usuario.rol];
  }
  function tamano(bytes) { return bytes < 104858 ? Math.max(1, Math.round(bytes / 1024)) + " KB" : (bytes / 1048576).toFixed(1) + " MB"; }

  /* Si el servidor rechazó el pedido, muestra el motivo. Devuelve true si hubo problema. */
  function problema(r, resp) {
    if (resp.ok) return false;
    if (resp.estado === 401) {
      usuario = null;
      Vista.avisar("Tu sesión terminó. Vuelve a ingresar.", "error");
      location.hash = "#/ingresar";
      return true;
    }
    var volver = usuario ? inicioDe(usuario.rol) : "#/ingresar";
    Vista.marco(usuario, r.ruta, [{ txt: resp.estado === 404 ? "No encontrado" : "Sin acceso" }],
      Vista.sinAcceso(resp.mensaje || "No se pudo cargar la página.", volver));
    return true;
  }

  // ---------- Enrutador ----------
  function leerRuta() {
    var h = location.hash || "#/ingresar";
    var partes = h.split("?");
    var query = {};
    (partes[1] || "").split("&").forEach(function (par) { if (par) { var kv = par.split("="); query[decodeURIComponent(kv[0])] = decodeURIComponent(kv[1] || ""); } });
    return { ruta: partes[0], segmentos: partes[0].replace(/^#\//, "").split("/").filter(Boolean), query: query };
  }

  async function atender() {
    var r = leerRuta();
    var s = r.segmentos;
    var miTurno = ++turno;
    r.vigente = function () { return miTurno === turno; };

    if (s[0] === "ingresar" || !s.length) return paginaIngreso(r);
    if (!usuario) { location.hash = "#/ingresar"; return; }

    if (s[0] === "portafolio-cursos") return primerCurso() ? ir("#/cursos/" + primerCurso() + "/portafolio") : ir("#/ingresar");
    if (s[0] === "listas") return primerCurso() ? ir("#/cursos/" + primerCurso() + "/estudiantes") : ir("#/matriculas/nueva");
    if (s[0] === "cursos" && s.length === 1) return paginaMisCursos(r);
    if (s[0] === "cursos" && s[2] === "asistencia" && !s[3]) {
      var sig = await Modelo.siguienteSesion(s[1]);
      if (problema(r, sig)) return;
      return ir("#/cursos/" + s[1] + "/asistencia/" + sig.siguiente);
    }
    if (s[0] === "cursos" && s[2] === "asistencia" && s[4] === "resumen") return paginaResumen(r, s[1], Number(s[3]));
    if (s[0] === "cursos" && s[2] === "asistencia") return paginaAsistencia(r, s[1], Number(s[3]));
    if (s[0] === "cursos" && s[2] === "evidencias") return paginaEvidencias(r, s[1]);
    if (s[0] === "cursos" && s[2] === "portafolio") return paginaPortafolioCurso(r, s[1]);
    if (s[0] === "cursos" && s[2] === "estudiantes") return paginaListaCurso(r, s[1]);
    if (s[0] === "matriculas" && s[1] === "nueva") return paginaMatricula(r);
    if (s[0] === "portafolio" && s[1] === "evidencias") return paginaSubirEvidencia(r);
    if (s[0] === "portafolio") return paginaMiPortafolio(r);
    ir(inicioDe(usuario.rol));
  }

  // ---------- 1 · Ingreso (POST /api/ingresar) ----------
  async function paginaIngreso(r) {
    var q = r.query;
    if (usuario && !q.demo) return ir(inicioDe(usuario.rol));
    if (q.demo) {
      if (usuario) { await Modelo.salir(); usuario = null; }
      var demo = { docente: "100001", estudiante: "260001", secretaria: "300001", director: "400001" }[q.demo];
      if (demo) estado.ingreso = { codigo: demo, clave: q.demo + "123", error: "" };
    }
    Vista.ingreso(estado.ingreso);
  }
  async function enviarIngreso(form) {
    var boton = form.querySelector("button[type=submit]");
    boton.disabled = true;
    var resp = await Modelo.iniciarSesion(form.codigo.value, form.clave.value);
    if (!resp.ok) {
      estado.ingreso = { codigo: form.codigo.value, clave: "", error: resp.mensaje };
      return Vista.ingreso(estado.ingreso);
    }
    estado.ingreso = { codigo: "", clave: "", error: "" };
    usuario = resp.usuario;
    cursosSesion = resp.cursos;
    Vista.avisar("Bienvenido, " + usuario.nombre.split(" ")[0]);
    location.hash = resp.inicio;
  }

  // ---------- 2 · Mis cursos ----------
  async function paginaMisCursos(r) {
    if (usuario.rol !== "docente") return ir(inicioDe(usuario.rol));
    var resp = await Modelo.misCursos();
    if (!r.vigente() || problema(r, resp)) return;
    var cursos = resp.cursos;
    Vista.marco(usuario, r.ruta, [{ txt: "Mis cursos" }], Vista.misCursos({
      nombre: usuario.nombre.split(" ")[0], cursos: cursos,
      totalEstudiantes: cursos.reduce(function (s, c) { return s + c.matriculados; }, 0),
      totalPorCalificar: cursos.reduce(function (s, c) { return s + c.porCalificar; }, 0),
      totalSesiones: cursos.reduce(function (s, c) { return s + c.sesiones; }, 0)
    }));
  }

  // ---------- 3 · Registro de asistencia (POST /api/cursos/{id}/asistencia/{sesion}) ----------
  async function paginaAsistencia(r, cursoId, sesion) {
    var resp = await Modelo.asistencia(cursoId, sesion);
    if (!r.vigente()) return;
    if (!resp.ok && resp.estado === 404 && resp.siguiente) return ir("#/cursos/" + cursoId + "/asistencia/" + resp.siguiente);
    if (problema(r, resp)) return;
    var a = estado.asistencia;
    if (!a || a.curso !== String(cursoId) || a.sesion !== sesion) {
      // flujo 2b: si la sesión ya tiene asistencia, se muestran los estados guardados
      a = estado.asistencia = { curso: String(cursoId), sesion: sesion, estados: Object.assign({}, resp.estados), error: "" };
    }
    a.estudiantes = resp.estudiantes;
    Vista.marco(usuario, r.ruta, [migaInicio(), { txt: resp.curso.codigo }, { txt: "Asistencia · sesión " + sesion }], Vista.registroAsistencia({
      curso: resp.curso, sesion: sesion, sesiones: resp.sesiones, estudiantes: resp.estudiantes, estados: a.estados,
      marcados: Object.keys(a.estados).length, total: resp.estudiantes.length,
      yaRegistrada: resp.yaRegistrada, error: a.error, sinSenal: estado.sinSenal
    }));
  }
  function marcar(codigo, valor) {
    var a = estado.asistencia;
    a.estados[codigo] = valor;
    Vista.actualizarFila(codigo, valor);
    Vista.actualizarContador(Object.keys(a.estados).length, a.estudiantes.length);
  }
  async function guardarAsistencia(boton) {
    var a = estado.asistencia;
    estado.sinSenal = document.getElementById("sin-senal").checked;
    if (estado.sinSenal) { // prueba del flujo 4a sin desconectar el wifi
      a.error = "No se pudo guardar. Intente de nuevo";
      return Vista.mostrarErrorAsistencia(a.error);
    }
    boton.disabled = true;
    boton.textContent = "Guardando…";
    var resp = await Modelo.registrarAsistenciaSesion(a.curso, a.sesion, a.estados);
    if (!resp.ok) {
      boton.disabled = false;
      boton.textContent = "Guardar asistencia";
      a.error = resp.mensaje;
      if (resp.estado === 401) return problema(leerRuta(), resp);
      return Vista.mostrarErrorAsistencia(resp.mensaje); // lo marcado queda intacto
    }
    estado.asistencia = null;
    location.hash = resp.destino;
  }

  // ---------- 4 · Resumen de la sesión ----------
  async function paginaResumen(r, cursoId, sesion) {
    var resp = await Modelo.resumenSesion(cursoId, sesion);
    if (!r.vigente() || problema(r, resp)) return;
    Vista.marco(usuario, r.ruta, [migaInicio(), { txt: resp.curso.codigo }, { txt: "Resumen de la sesión " + sesion }],
      Vista.resumenSesion({ curso: resp.curso, sesion: sesion, conteo: resp.conteo, resumen: resp.resumen }));
  }

  // ---------- 5 · Evidencias del curso (POST /api/evidencias/{id}/calificacion) ----------
  async function paginaEvidencias(r, cursoId) {
    var resp = await Modelo.evidenciasCurso(cursoId);
    if (!r.vigente() || problema(r, resp)) return;
    Vista.marco(usuario, r.ruta, [migaInicio(), { txt: resp.curso.codigo }, { txt: "Evidencias" }], Vista.evidenciasCurso({
      curso: resp.curso, filtro: estado.filtroEvidencias, porCalificar: resp.porCalificar, calificadas: resp.calificadas
    }));
  }
  async function enviarCalificacion(form) {
    var resp = await Modelo.calificarEvidencia(form.dataset.id, form.nota.value);
    if (!resp.ok) { form.nota.classList.add("invalido"); return Vista.avisar(resp.mensaje, "error"); }
    Vista.avisar("Calificación guardada: " + resp.evidencia.calificacion + ". Ya está en el portafolio del estudiante.");
    atender();
  }

  // ---------- 6 · Portafolio del curso (GET /api/cursos/{id}/portafolio) ----------
  async function paginaPortafolioCurso(r, cursoId) {
    var resp = await Modelo.portafolioCurso(cursoId);
    if (!r.vigente() || problema(r, resp)) return;
    Vista.marco(usuario, r.ruta, [migaInicio(), { txt: resp.portafolio.curso.codigo }, { txt: "Portafolio del curso" }],
      Vista.portafolioCurso({ portafolio: resp.portafolio, selector: resp.selector }));
  }

  // ---------- 7 · Matricular estudiante (POST /api/matriculas) ----------
  async function paginaMatricula(r) {
    if (usuario.rol !== "secretaria") return ir(inicioDe(usuario.rol));
    var resp = await Modelo.misCursos();
    if (!r.vigente() || problema(r, resp)) return;
    var m = estado.matricula;
    Vista.marco(usuario, r.ruta, [{ txt: "Matricular estudiante" }], Vista.matricula({
      cursos: resp.cursos.map(function (c) { return Object.assign({}, c, { id: String(c.id), docente: c.docenteNombre }); }),
      valores: m.valores, resultado: m.resultado, error: m.error, nombreEncontrado: m.nombreEncontrado || ""
    }));
  }
  async function enviarMatricula(form) {
    var cursos = Array.prototype.filter.call(form.querySelectorAll('input[name="cursos"]'), function (c) { return c.checked; }).map(function (c) { return c.value; });
    var m = estado.matricula;
    m.valores = { codigo: form.codigo.value.trim(), cursos: cursos };
    var resp = await Modelo.matricularEstudiante(m.valores.codigo, form.semestre.value, cursos);
    m.error = resp.ok ? "" : resp.mensaje;
    m.resultado = resp.ok ? resp.resultado : null;
    if (resp.ok) {
      var nuevos = resp.resultado.resultados.filter(function (x) { return x.estado === "matriculado"; }).length;
      Vista.avisar(nuevos ? "Matrícula guardada en " + nuevos + (nuevos === 1 ? " curso" : " cursos") : "Ya está matriculado en este curso", nuevos ? "" : "error");
      m.valores.cursos = [];
      m.nombreEncontrado = "✓ " + resp.resultado.estudiante.nombre;
    }
    atender();
  }
  var esperaBusqueda;
  function mostrarNombreEstudiante(input) {
    var span = document.getElementById("mat-nombre");
    var codigo = input.value.trim();
    clearTimeout(esperaBusqueda);
    if (codigo.length !== 6) { span.textContent = ""; estado.matricula.nombreEncontrado = ""; return; }
    esperaBusqueda = setTimeout(async function () {
      var resp = await Modelo.buscarEstudiante(codigo);
      if (input.value.trim() !== codigo) return;
      span.textContent = resp.ok ? "✓ " + resp.usuario.nombre : resp.mensaje;
      estado.matricula.nombreEncontrado = resp.ok ? span.textContent : "";
    }, 250);
  }

  // ---------- 8 · Lista del curso ----------
  async function paginaListaCurso(r, cursoId) {
    var resp = await Modelo.listaCurso(cursoId);
    if (!r.vigente() || problema(r, resp)) return;
    Vista.marco(usuario, r.ruta, [migaInicio(), { txt: resp.curso.codigo }, { txt: "Lista del curso" }], Vista.listaCurso({
      curso: resp.curso, estudiantes: resp.estudiantes, selector: resp.selector,
      resaltar: r.query.nuevo || "", puedeMatricular: usuario.rol === "secretaria"
    }));
  }

  // ---------- 9 · Mi portafolio (GET /api/portafolio) ----------
  async function paginaMiPortafolio(r) {
    if (usuario.rol !== "estudiante") return ir(inicioDe(usuario.rol));
    var resp = await Modelo.miPortafolio();
    if (!r.vigente() || problema(r, resp)) return;
    Vista.marco(usuario, r.ruta, [{ txt: "Mi portafolio" }], Vista.miPortafolio(resp.portafolio));
  }

  // ---------- 10 · Subir evidencia (POST /api/portafolio/evidencias) ----------
  async function paginaSubirEvidencia(r) {
    if (usuario.rol !== "estudiante") return ir(inicioDe(usuario.rol));
    var resp = await Modelo.misCursos();
    if (!r.vigente() || problema(r, resp)) return;
    var sub = estado.subida;
    var cursos = resp.cursos;
    if (r.query.curso) sub.valores.curso = r.query.curso;
    if (!sub.valores.curso) sub.valores.curso = String((cursos.filter(function (c) { return c.portafolio === "abierto"; })[0] || cursos[0] || {}).id);
    Vista.marco(usuario, r.ruta, [migaInicio(), { txt: "Subir evidencia" }], Vista.subirEvidencia({
      cursos: cursos.map(function (c) { return Object.assign({}, c, { id: String(c.id) }); }),
      valores: sub.valores, error: sub.error, confirmacion: sub.confirmacion,
      archivoTexto: sub.archivo ? sub.archivo.name + " · " + tamano(sub.archivo.size) : ""
    }));
  }
  function leerFormEvidencia() {
    var f = document.getElementById("form-evidencia");
    estado.subida.valores = { curso: f.curso.value, unidad: f.unidad.value, tipo: f.tipo.value };
  }
  async function enviarEvidencia(reemplazar) {
    leerFormEvidencia();
    var sub = estado.subida, v = sub.valores;
    var boton = document.querySelector("#form-evidencia button[type=submit]");
    if (boton) { boton.disabled = true; boton.textContent = "Subiendo…"; }
    var resp = await Modelo.subirEvidencia(v.curso, v.unidad, v.tipo, sub.archivo, reemplazar);
    if (!resp.ok) {
      if (resp.estado === 401) return problema(leerRuta(), resp);
      sub.error = resp.requiereConfirmacion ? "" : resp.mensaje;
      sub.confirmacion = resp.requiereConfirmacion ? resp.mensaje : "";
      return atender();
    }
    estado.subida = { valores: {}, archivo: null, confirmacion: "", error: "" };
    Vista.avisar("Tu portafolio se actualizó. La evidencia quedó «por calificar».");
    location.hash = "#/portafolio";
  }
  function elegirArchivo(input) {
    var f = input.files[0];
    if (!f) return;
    estado.subida.archivo = f;
    estado.subida.error = ""; estado.subida.confirmacion = "";
    document.getElementById("archivo-elegido").textContent = f.name + " · " + tamano(f.size);
  }
  /* Botones de prueba: crean un archivo de ese tipo y tamaño para probar las validaciones del servidor. */
  function archivoDePrueba(caso) {
    var casos = {
      pdf: ["trabajo-unidad.pdf", 2 * 1048576, "application/pdf"],
      foto: ["foto-practica.jpg", 1.3 * 1048576, "image/jpeg"],
      word: ["informe.docx", 0.6 * 1048576, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"],
      pesado: ["exposicion.pdf", 12.4 * 1048576, "application/pdf"]
    };
    var c = casos[caso];
    leerFormEvidencia();
    estado.subida.archivo = new File([new Uint8Array(Math.round(c[1]))], c[0], { type: c[2] });
    estado.subida.error = ""; estado.subida.confirmacion = "";
    atender();
  }

  // ---------- Eventos ----------
  document.addEventListener("click", async function (ev) {
    var lateral = document.getElementById("lateral");
    var btn = ev.target.closest("[data-accion]");
    if (lateral && lateral.classList.contains("abierto") && !ev.target.closest("#lateral") && !(btn && btn.dataset.accion === "abrir-menu")) lateral.classList.remove("abierto");
    if (!btn || btn.tagName === "SELECT") return;
    var a = btn.dataset.accion;

    if (a === "demo") { document.getElementById("codigo").value = btn.dataset.codigo; document.getElementById("clave").value = btn.dataset.clave; document.querySelector("#form-ingreso button[type=submit]").focus(); }
    else if (a === "salir") {
      await Modelo.salir();
      usuario = null; cursosSesion = []; estado.asistencia = null;
      estado.matricula = { valores: { codigo: "", cursos: [] }, resultado: null, error: "" };
      Vista.avisar("Sesión cerrada");
      location.hash = "#/ingresar";
    }
    else if (a === "abrir-menu") lateral.classList.toggle("abierto");
    else if (a === "marcar") marcar(btn.dataset.codigo, btn.dataset.estado);
    else if (a === "todos-presentes") estado.asistencia.estudiantes.forEach(function (e) { marcar(e.codigo, "presente"); });
    else if (a === "limpiar") { estado.asistencia.estados = {}; estado.asistencia.error = ""; estado.sinSenal = document.getElementById("sin-senal").checked; atender(); }
    else if (a === "guardar-asistencia") guardarAsistencia(btn);
    else if (a === "filtro") { estado.filtroEvidencias = btn.dataset.filtro; atender(); }
    else if (a === "reemplazar") enviarEvidencia(true);
    else if (a === "conservar") { estado.subida = { valores: {}, archivo: null, confirmacion: "", error: "" }; Vista.avisar("Conservamos tu entrega anterior"); location.hash = "#/portafolio"; }
    else if (a === "archivo-prueba") archivoDePrueba(btn.dataset.caso);
  });

  document.addEventListener("change", function (ev) {
    var t = ev.target;
    if (t.dataset.accion === "cambiar-sesion") location.hash = "#/cursos/" + estado.asistencia.curso + "/asistencia/" + t.value;
    else if (t.dataset.accion === "cambiar-curso") location.hash = "#/cursos/" + t.value + "/" + t.dataset.destino;
    else if (t.id === "ev-archivo") elegirArchivo(t);
    else if (t.id === "sin-senal") estado.sinSenal = t.checked;
  });

  document.addEventListener("input", function (ev) {
    if (ev.target.id === "mat-codigo") mostrarNombreEstudiante(ev.target);
  });

  document.addEventListener("submit", function (ev) {
    var f = ev.target;
    ev.preventDefault();
    if (f.id === "form-ingreso") enviarIngreso(f);
    else if (f.id === "form-matricula") enviarMatricula(f);
    else if (f.id === "form-evidencia") enviarEvidencia(false);
    else if (f.dataset.form === "calificar") enviarCalificacion(f);
  });

  window.addEventListener("hashchange", atender);

  // Al abrir la página: ¿ya había una sesión abierta en el servidor?
  (async function iniciar() {
    var resp = await Modelo.sesion();
    if (resp.ok) { usuario = resp.usuario; cursosSesion = resp.cursos; }
    atender();
  })();
})();
