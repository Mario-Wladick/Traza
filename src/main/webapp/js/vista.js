/* =========================================================
   TRAZA · VISTA (MVC)
   Arma el HTML de cada página con lo que le pasa el controlador.
   La vista NO calcula ni guarda: solo muestra.
   ========================================================= */
var Vista = (function () {
  "use strict";

  var raiz = function () { return document.getElementById("app"); };

  // Escapa texto para insertarlo en HTML sin riesgo
  function e(texto) {
    return String(texto == null ? "" : texto).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  // Íconos de línea gruesa con puntas redondeadas (brief)
  var trazos = {
    cursos: '<path d="M4 5a2 2 0 0 1 2-2h12v16H6a2 2 0 0 0-2 2z"/><path d="M4 21V5"/><path d="M9 7h5"/>',
    asistencia: '<path d="M9 11l3 3 8-8"/><path d="M20 12v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9"/>',
    evidencias: '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 14l2 2 4-4"/>',
    portafolio: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M3 13h18"/>',
    subir: '<path d="M12 16V4"/><path d="M7 9l5-5 5 5"/><path d="M5 20h14"/>',
    matricula: '<circle cx="9" cy="8" r="4"/><path d="M2 21c0-4 3-6 7-6s7 2 7 6"/><path d="M19 8v6M16 11h6"/>',
    lista: '<path d="M9 6h11M9 12h11M9 18h11"/><circle cx="4.5" cy="6" r="1"/><circle cx="4.5" cy="12" r="1"/><circle cx="4.5" cy="18" r="1"/>',
    salir: '<path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3"/><path d="M10 17l-5-5 5-5"/><path d="M5 12h11"/>',
    casa: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>',
    menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
    check: '<path d="M5 12l5 5 9-10"/>',
    flecha: '<path d="M5 12h14"/><path d="M13 6l6 6-6 6"/>',
    atras: '<path d="M19 12H5"/><path d="M11 6l-6 6 6 6"/>',
    reiniciar: '<path d="M3 12a9 9 0 1 0 3-6.7"/><path d="M3 4v5h5"/>',
    solicitud: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/>',
    cruz: '<path d="M6 6l12 12M18 6L6 18"/>'
  };
  function icono(nombre, tam, grosor) {
    tam = tam || 20;
    return '<svg width="' + tam + '" height="' + tam + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="' + (grosor || 2.4) +
      '" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + trazos[nombre] + "</svg>";
  }
  function iniciales(nombre) {
    return nombre.split(" ").filter(Boolean).slice(0, 2).map(function (p) { return p[0]; }).join("").toUpperCase();
  }
  var nombresRol = { docente: "Docente", estudiante: "Estudiante", secretaria: "Secretaría académica", director: "Director de escuela" };
  function fechaLarga(iso) {
    var meses = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
    var p = iso.split("-");
    return Number(p[2]) + " de " + meses[Number(p[1]) - 1];
  }
  function chipNota(cal) {
    if (cal === null) return '<span class="chip por-calificar">Por calificar</span>';
    return '<span class="chip ' + (cal < 11 ? "desaprobada" : "calificada") + '">Calificada</span>';
  }
  function barraAvance(pct) {
    return '<div class="avance"><div class="barra"><i style="width:' + pct + '%"></i></div><b>' + pct + "%</b></div>";
  }
  function mensaje(tipo, texto) { return texto ? '<div class="mensaje ' + tipo + '" role="' + (tipo === "error" ? "alert" : "status") + '">' + e(texto) + "</div>" : ""; }

  // ---------- Marco de la aplicación ----------
  function menuDe(rol) {
    if (rol === "docente") return [{ ruta: "#/cursos", txt: "Mis cursos", ic: "cursos" }];
    if (rol === "estudiante") return [{ ruta: "#/portafolio", txt: "Mi portafolio", ic: "portafolio" }, { ruta: "#/portafolio/evidencias/nueva", txt: "Subir evidencia", ic: "subir" }];
    if (rol === "secretaria") return [{ ruta: "#/matriculas/nueva", txt: "Matricular estudiante", ic: "matricula" }, { ruta: "#/listas", txt: "Listas de cursos", ic: "lista", grupo: "/estudiantes" },
      { ruta: "#/solicitudes", txt: "Solicitudes de cuenta", ic: "solicitud", insignia: "insignia-solicitudes" }];
    return [{ ruta: "#/portafolio-cursos", txt: "Portafolio de cursos", ic: "portafolio", grupo: "/portafolio" }];
  }

  function marco(usuario, rutaActual, migas, contenido) {
    var items = menuDe(usuario.rol).map(function (m) {
      var activo = rutaActual === m.ruta || (m.grupo && rutaActual.indexOf(m.grupo) >= 0) ||
        (m.ruta === "#/cursos" && rutaActual.indexOf("#/cursos") === 0);
      return '<a href="' + m.ruta + '" class="' + (activo ? "activo" : "") + '">' + icono(m.ic) + e(m.txt) +
        (m.insignia ? '<span class="insignia oculto" id="' + m.insignia + '"></span>' : "") + "</a>";
    }).join("");
    var migasHtml = migas.map(function (m, i) {
      return i === migas.length - 1 ? "<b>" + e(m.txt) + "</b>" : '<a href="' + m.ruta + '">' + e(m.txt) + "</a> / ";
    }).join("");

    raiz().innerHTML =
      '<div class="app">' +
        '<aside class="lateral" id="lateral">' +
          '<a class="marca" href="index.html"><img src="img/isotipo.png" alt=""><span>TRAZA</span></a>' +
          '<nav class="nav-lateral" aria-label="Menú del sistema"><span class="nav-titulo">' + e(nombresRol[usuario.rol]) + "</span>" + items +
            '<span class="nav-titulo">TRAZA</span>' +
            '<a href="index.html">' + icono("casa") + "Página de inicio</a>" +
          "</nav>" +
          '<div class="usuario-lateral"><div class="avatar">' + e(iniciales(usuario.nombre)) + '</div>' +
            '<div class="datos"><div class="nombre">' + e(usuario.nombre) + '</div><div class="rol">' + e(nombresRol[usuario.rol]) + " · " + e(usuario.codigo) + "</div></div>" +
            '<button class="salir" data-accion="salir" title="Cerrar sesión" aria-label="Cerrar sesión">' + icono("salir") + "</button></div>" +
        "</aside>" +
        '<div class="principal">' +
          '<header class="encabezado">' +
            '<button class="menu-app-btn" data-accion="abrir-menu" aria-label="Abrir menú">' + icono("menu", 24) + "</button>" +
            '<img class="logo-movil" src="img/logo-horizontal.png" alt="TRAZA">' +
            '<div class="migas">' + migasHtml + "</div>" +
            '<span class="prototipo-chip">Semestre ' + e(Modelo.SEMESTRE) + "</span>" +
          "</header>" +
          '<main class="contenido" id="contenido">' + contenido + "</main>" +
        "</div>" +
      "</div>";
    window.scrollTo(0, 0);
  }

  function tituloPagina(titulo, sub, acciones) {
    return '<div class="titulo-pagina"><div><h1>' + e(titulo) + "</h1>" + (sub ? "<p>" + sub + "</p>" : "") + "</div>" + (acciones || "") + "</div>";
  }

  // ---------- 1 · Ingreso (/ingresar) ----------
  function ingreso(d) {
    var demos = [
      { rol: "docente", cod: "100001", txt: "Docente" },
      { rol: "estudiante", cod: "260001", txt: "Estudiante" },
      { rol: "secretaria", cod: "300001", txt: "Secretaría" },
      { rol: "director", cod: "400001", txt: "Director" }
    ].map(function (x) {
      return '<button type="button" class="demo-btn" data-accion="demo" data-codigo="' + x.cod + '" data-clave="' + x.rol + '123"><b>' + x.txt + "</b><small>" + x.cod + " · " + x.rol + "123</small></button>";
    }).join("");
    raiz().innerHTML =
      '<div class="ingreso">' +
        '<section class="ingreso-arte"><div class="cuadro"></div><div class="circulo"></div>' +
          '<a class="marca-ingreso" href="index.html"><img src="img/isotipo.png" alt=""><span>TRAZA</span></a>' +
          "<div><h2>Deja la <span>traza</span> de tu recorrido académico</h2><p>Asistencia, evidencias y portafolio en un solo lugar, desde tu celular o tu computadora.</p></div>" +
        "</section>" +
        '<section class="ingreso-form"><div class="ingreso-caja">' +
          '<a class="volver-inicio" href="index.html">' + icono("atras", 16) + " Volver al inicio</a>" +
          "<h1>Iniciar sesión</h1><p>Entra con tu código universitario y tu contraseña.</p>" +
          '<form class="form" id="form-ingreso" novalidate>' +
            '<div class="grupo"><label for="codigo">Código universitario</label>' +
              '<input class="campo' + (d.error ? " invalido" : "") + '" id="codigo" name="codigo" inputmode="numeric" autocomplete="username" maxlength="6" placeholder="Ej. 260001" value="' + e(d.codigo) + '"></div>' +
            '<div class="grupo"><label for="clave">Contraseña</label>' +
              '<input class="campo' + (d.error ? " invalido" : "") + '" id="clave" name="clave" type="password" autocomplete="current-password" placeholder="Tu contraseña" value="' + e(d.clave) + '"></div>' +
            mensaje("error", d.error) +
            '<button class="btn btn-verde btn-bloque" type="submit">Ingresar ' + icono("flecha", 18) + "</button>" +
          "</form>" +
          '<div class="demo"><span class="lbl">Cuentas de prueba (toca una para llenar)</span><div class="demo-grid">' + demos + "</div></div>" +
        "</div></section>" +
      "</div>";
    var foco = d.codigo ? document.getElementById("clave") : document.getElementById("codigo");
    if (foco && !d.codigo) foco.focus();
  }

  // ---------- 2 · Mis cursos (/cursos) ----------
  function misCursos(d) {
    var tarjetas = d.cursos.map(function (c) {
      return '<article class="curso">' +
        '<div class="curso-cabeza"><span class="codigo">' + e(c.codigo) + " · Ciclo " + e(c.ciclo) + "</span><h3>" + e(c.nombre) + "</h3></div>" +
        '<div class="curso-cuerpo">' +
          '<div class="curso-meta"><span>' + c.matriculados + (c.matriculados === 1 ? " estudiante" : " estudiantes") + "</span><span>" + c.sesiones + (c.sesiones === 1 ? " sesión" : " sesiones") + "</span><span>" + c.porCalificar + " por calificar</span></div>" +
          '<a class="btn btn-verde btn-bloque" href="#/cursos/' + e(c.id) + '/asistencia/' + c.siguiente + '">' + icono("asistencia", 18) + " Registrar asistencia</a>" +
          '<div class="curso-acciones">' +
            '<a class="btn btn-claro btn-chico" href="#/cursos/' + e(c.id) + '/evidencias">Evidencias</a>' +
            '<a class="btn btn-claro btn-chico" href="#/cursos/' + e(c.id) + '/portafolio">Portafolio</a>' +
          "</div>" +
          '<a class="btn btn-texto btn-chico" href="#/cursos/' + e(c.id) + '/estudiantes">Ver lista del curso</a>' +
        "</div></article>";
    }).join("");
    return tituloPagina("Mis cursos", "Hola, " + e(d.nombre) + ". Tienes " + d.cursos.length + " cursos este semestre.") +
      '<div class="resumen-grid">' +
        '<div class="dato destacado"><div class="valor">' + d.totalEstudiantes + '</div><div class="nombre">Estudiantes a cargo</div></div>' +
        '<div class="dato"><div class="valor">' + d.cursos.length + '</div><div class="nombre">Cursos</div></div>' +
        '<div class="dato"><div class="valor">' + d.totalPorCalificar + '</div><div class="nombre">Evidencias por calificar</div></div>' +
        '<div class="dato"><div class="valor">' + d.totalSesiones + '</div><div class="nombre">Sesiones registradas</div></div>' +
      "</div>" +
      '<div class="cursos-grid">' + tarjetas + "</div>";
  }

  // ---------- 3 · Registro de asistencia (/cursos/{id}/asistencia/{sesion}) ----------
  function filaEstudiante(est, i, estado) {
    var letra = { presente: "P", tardanza: "T", falta: "F" };
    var botones = ["presente", "tardanza", "falta"].map(function (s) {
      var sel = estado === s ? " sel-" + letra[s] : "";
      return '<button type="button" class="estado-btn' + sel + '" data-accion="marcar" data-codigo="' + e(est.codigo) + '" data-estado="' + s +
        '" aria-pressed="' + (estado === s) + '" title="' + s + '" aria-label="' + s + " " + e(est.nombre) + '">' + letra[s] + "</button>";
    }).join("");
    return '<li class="fila-est" id="fila-' + e(est.codigo) + '"><span class="num">' + (i + 1) + '</span>' +
      '<div class="info"><div class="n">' + e(est.nombre) + '</div><div class="c">' + e(est.codigo) + "</div></div>" +
      '<div class="estados">' + botones + "</div></li>";
  }

  function registroAsistencia(d) {
    if (!d.estudiantes.length) {
      return tituloPagina("Registro de asistencia", e(d.curso.codigo) + " · " + e(d.curso.nombre)) +
        '<div class="panel vacio"><b>El curso no tiene estudiantes</b>La secretaría académica todavía no matricula a nadie en este curso.<br><br>' +
        '<a class="btn btn-claro" href="#/cursos">' + icono("atras", 18) + " Volver a mis cursos</a></div>";
    }
    var opcionesSesion = d.sesiones.map(function (s) {
      return '<option value="' + s.sesion + '"' + (s.sesion === d.sesion ? " selected" : "") + ">Sesión " + s.sesion + " · " + fechaLarga(s.fecha) + (s.registrada ? " (registrada)" : " (hoy)") + "</option>";
    }).join("");
    return tituloPagina("Registro de asistencia", e(d.curso.codigo) + " · " + e(d.curso.nombre),
        '<div class="grupo" style="min-width:240px"><label for="sel-sesion">Sesión</label><select class="campo" id="sel-sesion" data-accion="cambiar-sesion">' + opcionesSesion + "</select></div>") +
      (d.yaRegistrada ? mensaje("info", "Esta sesión ya tiene asistencia registrada. Corrige lo que necesites y vuelve a guardar.") + "<br>" : "") +
      '<div class="panel">' +
        '<div class="barra-asistencia">' +
          '<button class="btn btn-negro btn-chico" data-accion="todos-presentes">' + icono("check", 16) + " Todos presentes</button>" +
          '<button class="btn btn-claro btn-chico" data-accion="limpiar">Limpiar</button>' +
          '<span class="contador" id="contador">' + d.marcados + " de " + d.total + " marcados</span>" +
        "</div>" +
        '<div class="leyenda"><span><i style="background:var(--ok)"></i>P · presente</span><span><i style="background:var(--alerta)"></i>T · tardanza</span><span><i style="background:var(--error)"></i>F · falta</span></div>' +
        '<ul class="lista-estudiantes">' + d.estudiantes.map(function (est, i) { return filaEstudiante(est, i, d.estados[est.codigo]); }).join("") + "</ul>" +
      "</div>" +
      '<div id="zona-error">' + mensaje("error", d.error) + "</div>" +
      '<div class="barra-guardar">' +
        '<label class="check" style="padding:8px 12px;margin-right:auto;font-size:13px"><input type="checkbox" id="sin-senal"' + (d.sinSenal ? " checked" : "") + '><span class="t">Simular sin señal</span></label>' +
        '<a class="btn btn-claro" href="#/cursos">Cancelar</a>' +
        '<button class="btn btn-verde" data-accion="guardar-asistencia">Guardar asistencia</button>' +
      "</div>";
  }
  function actualizarFila(codigo, estado) {
    var fila = document.getElementById("fila-" + codigo);
    if (!fila) return;
    var letra = { presente: "P", tardanza: "T", falta: "F" };
    fila.querySelectorAll(".estado-btn").forEach(function (b) {
      var sel = b.dataset.estado === estado;
      b.className = "estado-btn" + (sel ? " sel-" + letra[b.dataset.estado] : "");
      b.setAttribute("aria-pressed", sel);
    });
  }
  function actualizarContador(marcados, total) {
    var c = document.getElementById("contador");
    if (c) c.textContent = marcados + " de " + total + " marcados";
  }
  function mostrarErrorAsistencia(texto) {
    var z = document.getElementById("zona-error");
    if (z) { z.innerHTML = mensaje("error", texto); z.scrollIntoView({ behavior: "smooth", block: "center" }); }
  }

  // ---------- 4 · Resumen de la sesión ----------
  function resumenSesion(d) {
    return '<div class="panel resumen-sesion">' +
      '<div class="check-grande">' + icono("check", 44, 3) + "</div>" +
      "<h2>Asistencia guardada</h2>" +
      '<p style="color:var(--gris-texto);margin-top:6px">' + e(d.curso.codigo) + " · " + e(d.curso.nombre) + " · Sesión " + d.sesion + "</p>" +
      '<p class="frase">«' + e(d.resumen) + "»</p>" +
      '<div class="conteo"><div class="p"><b>' + d.conteo.presente + '</b>presentes</div><div class="t"><b>' + d.conteo.tardanza + '</b>tardanzas</div><div class="f"><b>' + d.conteo.falta + "</b>faltas</div></div>" +
      '<div class="acciones-centro">' +
        '<a class="btn btn-claro" href="#/cursos/' + e(d.curso.id) + "/asistencia/" + d.sesion + '">Corregir</a>' +
        '<a class="btn btn-negro" href="#/cursos/' + e(d.curso.id) + '/portafolio">Ver portafolio del curso</a>' +
        '<a class="btn btn-verde" href="#/cursos">Volver a mis cursos</a>' +
      "</div></div>";
  }

  // ---------- 5 · Evidencias del curso (/cursos/{id}/evidencias) ----------
  function itemEvidencia(ev, calificable) {
    var ext = ev.archivo.formato === "PDF" ? "PDF" : "IMG";
    var der = calificable
      ? '<form class="calificar" novalidate data-form="calificar" data-id="' + e(ev.id) + '"><input class="campo" name="nota" type="number" min="0" max="20" step="1" inputmode="numeric" placeholder="0–20" aria-label="Calificación de ' + e(ev.nombreEstudiante) + '" value="' + (ev.calificacion === null ? "" : ev.calificacion) + '">' +
        '<button class="btn btn-verde btn-chico" type="submit">Guardar</button></form>'
      : '<span class="nota' + (ev.calificacion < 11 ? " baja" : "") + '">' + ev.calificacion + "</span>";
    return '<li class="item"><div class="icono-archivo">' + ext + '</div>' +
      '<div class="info"><div class="n">' + e(ev.nombreEstudiante) + '</div><div class="c">' + e(ev.tipo) + " · Unidad " + e(ev.unidad) + " · " + e(ev.archivo.nombre) + " · " + (ev.archivo.tamanoMB < 0.1 ? "menos de 0.1" : ev.archivo.tamanoMB) + " MB · " + fechaLarga(ev.fecha) + "</div></div>" +
      (ev.tieneArchivo ? '<a class="btn btn-texto btn-chico" target="_blank" rel="noopener" href="api/evidencias/' + ev.id + '/archivo">Ver archivo</a>' : "") +
      chipNota(ev.calificacion) + der + "</li>";
  }
  function evidenciasCurso(d) {
    var pestañas = '<div class="barra-asistencia">' +
      '<button class="btn btn-chico ' + (d.filtro === "por" ? "btn-negro" : "btn-claro") + '" data-accion="filtro" data-filtro="por">Por calificar (' + d.porCalificar.length + ")</button>" +
      '<button class="btn btn-chico ' + (d.filtro === "cal" ? "btn-negro" : "btn-claro") + '" data-accion="filtro" data-filtro="cal">Calificadas (' + d.calificadas.length + ")</button>" +
      (d.filtro === "cal" ? '<span class="contador">Para corregir una nota, cámbiala y guarda</span>' : "") + "</div>";
    var lista = d.filtro === "por" ? d.porCalificar : d.calificadas;
    var cuerpo = lista.length
      ? '<ul class="lista">' + lista.map(function (ev) { return itemEvidencia(ev, true); }).join("") + "</ul>"
      : '<div class="vacio"><b>' + (d.filtro === "por" ? "Todo calificado" : "Aún no hay calificadas") + "</b>" + (d.filtro === "por" ? "No tienes evidencias pendientes en este curso." : "") + "</div>";
    return tituloPagina("Evidencias del curso", e(d.curso.codigo) + " · " + e(d.curso.nombre) + " · califica de 0 a 20") +
      '<div class="panel">' + pestañas + cuerpo + "</div>";
  }

  // ---------- 6 · Portafolio del curso (/cursos/{id}/portafolio) ----------
  function portafolioCurso(d) {
    var p = d.portafolio;
    var selector = d.selector
      ? '<div class="selector-curso"><label class="lbl" for="sel-curso" style="font-family:var(--titulo);font-weight:700;font-size:13px">Curso</label><select class="campo" id="sel-curso" data-accion="cambiar-curso" data-destino="portafolio">' +
        d.selector.map(function (c) { return '<option value="' + e(c.id) + '"' + (c.id === p.curso.id ? " selected" : "") + ">" + e(c.codigo) + " · " + e(c.nombre) + "</option>"; }).join("") + "</select></div>"
      : "";
    var filasEst = p.estudiantes.map(function (x) {
      return "<tr><td>" + e(x.estudiante.nombre) + '<div style="font-size:13px;color:var(--gris-texto)">' + e(x.estudiante.codigo) + "</div></td>" +
        '<td class="num">' + x.asistenciaPct + '%</td><td class="num ocultar-movil">' + x.faltas + '</td><td class="num ocultar-movil">' + x.entregadas + '</td><td class="num">' + (x.promedio === null ? "—" : x.promedio) + "</td>" +
        '<td style="min-width:120px">' + barraAvance(x.avance) + "</td></tr>";
    }).join("");
    var filasSes = p.sesiones.slice().reverse().map(function (s) {
      return "<tr><td>Sesión " + s.sesion + '</td><td class="ocultar-movil">' + fechaLarga(s.fecha) + '</td><td class="num">' + s.conteo.presente + '</td><td class="num">' + s.conteo.tardanza + '</td><td class="num">' + s.conteo.falta + "</td></tr>";
    }).join("");
    return tituloPagina("Portafolio del curso", "Armado automáticamente con lo registrado en el semestre " + e(p.semestre), selector) +
      '<div class="portafolio-cabeza"><div><span class="chip ' + e(p.curso.portafolio) + '">Portafolio ' + e(p.curso.portafolio) + "</span>" +
        '<h2 style="margin-top:12px">' + e(p.curso.codigo) + " · " + e(p.curso.nombre) + "</h2><p>Docente: " + e(p.docente) + " · " + p.matriculados + " estudiantes · Ciclo " + e(p.curso.ciclo) + "</p></div>" +
        '<div class="avance-grande"><b>' + p.avance + '%</b><span style="color:#c3c9cd;font-size:14px">avance de evidencias</span><div class="barra"><i style="width:' + p.avance + '%"></i></div></div></div>' +
      '<div class="resumen-grid">' +
        '<div class="dato"><div class="valor">' + p.asistenciaPct + '%</div><div class="nombre">Asistencia del curso</div></div>' +
        '<div class="dato"><div class="valor">' + p.sesiones.length + '</div><div class="nombre">Sesiones registradas</div></div>' +
        '<div class="dato"><div class="valor">' + p.calificadas + '</div><div class="nombre">Evidencias calificadas</div></div>' +
        '<div class="dato"><div class="valor">' + (p.promedio === null ? "—" : p.promedio) + '</div><div class="nombre">Promedio del curso</div></div>' +
      "</div>" +
      '<div class="panel"><h3 class="subtitulo" style="margin-top:0">Estudiantes</h3>' +
        (p.estudiantes.length ? '<table class="tabla-simple"><thead><tr><th>Estudiante</th><th style="text-align:right">Asist.</th><th class="ocultar-movil" style="text-align:right">Faltas</th><th class="ocultar-movil" style="text-align:right">Entregas</th><th style="text-align:right">Prom.</th><th>Avance</th></tr></thead><tbody>' + filasEst + "</tbody></table>"
          : '<div class="vacio"><b>Sin estudiantes</b>Este curso todavía no tiene matriculados.</div>') +
      "</div>" +
      '<div class="panel"><h3 class="subtitulo" style="margin-top:0">Asistencias por sesión</h3>' +
        (p.sesiones.length ? '<table class="tabla-simple"><thead><tr><th>Sesión</th><th class="ocultar-movil">Fecha</th><th style="text-align:right">Presentes</th><th style="text-align:right">Tardanzas</th><th style="text-align:right">Faltas</th></tr></thead><tbody>' + filasSes + "</tbody></table>"
          : '<div class="vacio"><b>Sin sesiones</b>Aún no se registra asistencia en este curso.</div>') +
      "</div>";
  }

  // ---------- 7 · Matricular estudiante (/matriculas/nueva) ----------
  function matricula(d) {
    var checks = d.cursos.map(function (c) {
      var marcado = d.valores.cursos.indexOf(c.id) >= 0;
      return '<label class="check"><input type="checkbox" name="cursos" value="' + e(c.id) + '"' + (marcado ? " checked" : "") + '><span class="t"><b>' + e(c.codigo) + "</b> · " + e(c.nombre) + "<small>Ciclo " + e(c.ciclo) + " · " + e(c.docente) + "</small></span></label>";
    }).join("");
    var resultado = "";
    if (d.resultado) {
      resultado = '<div class="panel"><h3 class="subtitulo" style="margin-top:0">Resultado · ' + e(d.resultado.estudiante.nombre) + "</h3><ul class=\"lista\">" +
        d.resultado.resultados.map(function (r) {
          return '<li class="item"><div class="info"><div class="n">' + e(r.curso.codigo) + " · " + e(r.curso.nombre) + "</div></div>" +
            '<span class="chip ' + (r.estado === "ya" ? "por-calificar" : "calificada") + '">' + e(r.mensaje) + "</span>" +
            '<a class="btn btn-claro btn-chico" href="#/cursos/' + e(r.curso.id) + '/estudiantes?nuevo=' + e(d.resultado.estudiante.codigo) + '">Ver lista del curso</a></li>';
        }).join("") + "</ul></div>";
    }
    return tituloPagina("Matricular estudiante", "Semestre " + e(Modelo.SEMESTRE) + ". Escribe el código del estudiante y elige sus cursos.") +
      '<div class="panel"><form class="form" id="form-matricula" novalidate>' +
        '<div class="fila-2">' +
          '<div class="grupo"><label for="mat-codigo">Código del estudiante</label><input class="campo" id="mat-codigo" name="codigo" inputmode="numeric" maxlength="6" placeholder="Ej. 260041" value="' + e(d.valores.codigo) + '">' +
            '<span class="ayuda" id="mat-nombre">' + e(d.nombreEncontrado || "") + "</span></div>" +
          '<div class="grupo"><label for="mat-semestre">Semestre</label><select class="campo" id="mat-semestre" name="semestre"><option>' + e(Modelo.SEMESTRE) + "</option></select></div>" +
        "</div>" +
        '<div class="grupo"><span class="lbl">Cursos</span><div class="checks">' + checks + "</div></div>" +
        mensaje("error", d.error) +
        '<div style="display:flex;justify-content:flex-end"><button class="btn btn-verde" type="submit">' + icono("matricula", 18) + " Matricular</button></div>" +
      "</form></div>" + resultado;
  }

  // ---------- 8 · Lista del curso (/cursos/{id}/estudiantes) ----------
  function listaCurso(d) {
    var selector = d.selector
      ? '<div class="selector-curso"><select class="campo" id="sel-curso" data-accion="cambiar-curso" data-destino="estudiantes" aria-label="Curso">' +
        d.selector.map(function (c) { return '<option value="' + e(c.id) + '"' + (c.id === d.curso.id ? " selected" : "") + ">" + e(c.codigo) + " · " + e(c.nombre) + "</option>"; }).join("") + "</select></div>"
      : "";
    var filas = d.estudiantes.map(function (x, i) {
      var nuevo = d.resaltar === x.codigo;
      return "<tr" + (nuevo ? ' style="background:var(--verde-pastel)"' : "") + '><td class="num" style="text-align:left;width:40px">' + (i + 1) + "</td><td>" + e(x.nombre) + (nuevo ? ' <span class="chip calificada">Nuevo</span>' : "") + "</td><td>" + e(x.codigo) + "</td></tr>";
    }).join("");
    return tituloPagina("Lista del curso", e(d.curso.codigo) + " · " + e(d.curso.nombre) + " · " + d.estudiantes.length + " estudiantes", selector) +
      '<div class="panel">' + (d.estudiantes.length
        ? '<table class="tabla-simple"><thead><tr><th>#</th><th>Estudiante</th><th>Código</th></tr></thead><tbody>' + filas + "</tbody></table>"
        : '<div class="vacio"><b>El curso no tiene estudiantes</b>Matricula a alguien desde «Matricular estudiante».</div>') + "</div>" +
      (d.puedeMatricular ? '<div style="margin-top:24px"><a class="btn btn-verde" href="#/matriculas/nueva">' + icono("matricula", 18) + " Matricular otro estudiante</a></div>" : "");
  }

  // ---------- 9 · Mi portafolio (/portafolio) ----------
  function miPortafolio(p) {
    var cursos = p.cursos.map(function (c) {
      var evs = c.evidencias.length
        ? '<ul class="lista">' + c.evidencias.map(function (ev) {
            return '<li class="item"><div class="icono-archivo">' + (ev.archivo.formato === "PDF" ? "PDF" : "IMG") + '</div><div class="info"><div class="n">' + e(ev.tipo.charAt(0).toUpperCase() + ev.tipo.slice(1)) + " · Unidad " + e(ev.unidad) + '</div><div class="c">' + e(ev.archivo.nombre) + " · " + fechaLarga(ev.fecha) + "</div></div>" +
              (ev.tieneArchivo ? '<a class="btn btn-texto btn-chico" target="_blank" rel="noopener" href="api/evidencias/' + ev.id + '/archivo">Ver</a>' : "") +
              chipNota(ev.calificacion) + (ev.calificacion === null ? "" : '<span class="nota' + (ev.calificacion < 11 ? " baja" : "") + '">' + ev.calificacion + "</span>") + "</li>";
          }).join("") + "</ul>"
        : '<div class="vacio"><b>Aún no subes evidencias</b>Cuando entregues un trabajo aparecerá aquí.</div>';
      return '<div class="panel">' +
        '<div class="titulo-pagina" style="margin-bottom:12px"><div><span class="chip ' + e(c.curso.portafolio) + '">' + (c.curso.portafolio === "cerrado" ? "Portafolio cerrado" : "Portafolio abierto") + '</span><h3 class="subtitulo" style="margin:8px 0 0">' + e(c.curso.codigo) + " · " + e(c.curso.nombre) + "</h3>" +
          '<p style="color:var(--gris-texto);font-size:14px">Asistencia: ' + c.asistencia.presente + " presentes, " + c.asistencia.tardanza + " tardanzas, " + c.asistencia.falta + " faltas · Promedio: " + (c.promedio === null ? "—" : c.promedio) + "</p></div>" +
          (c.curso.portafolio === "abierto" ? '<a class="btn btn-verde btn-chico" href="#/portafolio/evidencias/nueva?curso=' + e(c.curso.id) + '">' + icono("subir", 16) + " Subir evidencia</a>" : "") + "</div>" +
        barraAvance(c.avance) + '<div style="margin-top:8px">' + evs + "</div></div>";
    }).join("");
    return tituloPagina("Mi portafolio", "Hola, " + e(p.estudiante.nombre.split(" ")[0]) + ". " + (p.cursos.some(function (c) { return c.porCalificar; }) ? "Tienes evidencias esperando calificación." : "Tu portafolio está al día."),
        '<a class="btn btn-verde" href="#/portafolio/evidencias/nueva">' + icono("subir", 18) + " Subir evidencia</a>") +
      '<div class="portafolio-cabeza"><div><span class="chip abierto">Semestre ' + e(p.semestre) + '</span><h2 style="margin-top:12px">' + e(p.estudiante.nombre) + "</h2><p>Código " + e(p.estudiante.codigo) + " · " + p.cursos.length + " cursos</p></div>" +
        '<div class="avance-grande"><b>' + p.avance + '%</b><span style="color:#c3c9cd;font-size:14px">avance de tu portafolio</span><div class="barra"><i style="width:' + p.avance + '%"></i></div></div></div>' +
      '<div class="resumen-grid">' +
        '<div class="dato destacado"><div class="valor">' + (p.promedio === null ? "—" : p.promedio) + '</div><div class="nombre">Promedio</div></div>' +
        '<div class="dato"><div class="valor">' + p.asistenciaPct + '%</div><div class="nombre">Asistencia</div></div>' +
        '<div class="dato"><div class="valor">' + p.evidenciasEntregadas + '</div><div class="nombre">Evidencias entregadas</div></div>' +
        '<div class="dato"><div class="valor">' + p.cursos.length + '</div><div class="nombre">Cursos</div></div>' +
      "</div>" + cursos;
  }

  // ---------- 10 · Subir evidencia (/portafolio/evidencias/nueva) ----------
  function subirEvidencia(d) {
    var opcCursos = d.cursos.map(function (c) {
      return '<option value="' + e(c.id) + '"' + (d.valores.curso === c.id ? " selected" : "") + ">" + e(c.codigo) + " · " + e(c.nombre) + (c.portafolio === "cerrado" ? " (cerrado)" : "") + "</option>";
    }).join("");
    function opc(lista, sel, pref) {
      return lista.map(function (x) { return '<option value="' + e(x) + '"' + (sel === x ? " selected" : "") + ">" + pref + e(x.charAt(0).toUpperCase() + x.slice(1)) + "</option>"; }).join("");
    }
    var confirmar = d.confirmacion
      ? '<div class="mensaje info" role="alert" style="flex-direction:column;align-items:stretch;gap:12px"><span>' + e(d.confirmacion) + '</span><div style="display:flex;gap:8px;flex-wrap:wrap">' +
        '<button type="button" class="btn btn-verde btn-chico" data-accion="reemplazar">Sí, reemplazar</button><button type="button" class="btn btn-claro btn-chico" data-accion="conservar">No, conservar la anterior</button></div></div>'
      : "";
    return '<a class="volver-inicio" href="#/portafolio" style="margin-bottom:16px">' + icono("atras", 16) + " Mi portafolio</a>" +
      tituloPagina("Subir evidencia", "PDF o imagen, hasta " + Modelo.MAX_MB + " MB. Quedará «por calificar» en tu portafolio.") +
      '<div class="panel" style="max-width:720px"><form class="form" id="form-evidencia" novalidate>' +
        '<div class="grupo"><label for="ev-curso">Curso</label><select class="campo" id="ev-curso" name="curso">' + opcCursos + "</select></div>" +
        '<div class="fila-2">' +
          '<div class="grupo"><label for="ev-unidad">Unidad</label><select class="campo" id="ev-unidad" name="unidad">' + opc(Modelo.UNIDADES, d.valores.unidad, "Unidad ") + "</select></div>" +
          '<div class="grupo"><label for="ev-tipo">Tipo de evidencia</label><select class="campo" id="ev-tipo" name="tipo">' + opc(Modelo.TIPOS, d.valores.tipo, "") + "</select></div>" +
        "</div>" +
        '<div class="grupo"><span class="lbl">Archivo</span>' +
          '<label class="zona-archivo" for="ev-archivo">' + icono("subir", 32) + '<div><b>Toca para elegir un archivo</b></div><div class="ayuda">Desde tu celular puedes tomar una foto</div>' +
            '<div class="archivo-elegido" id="archivo-elegido">' + e(d.archivoTexto || "") + "</div></label>" +
          '<input class="oculto" id="ev-archivo" name="archivo" type="file" accept="application/pdf,image/*">' +
          '<div class="ayuda" style="display:flex;gap:6px;flex-wrap:wrap;align-items:center;margin-top:4px">Probar sin archivo real:' +
            '<button type="button" class="btn btn-claro btn-chico" data-accion="archivo-prueba" data-caso="pdf">PDF 2 MB</button>' +
            '<button type="button" class="btn btn-claro btn-chico" data-accion="archivo-prueba" data-caso="foto">Foto 1.3 MB</button>' +
            '<button type="button" class="btn btn-claro btn-chico" data-accion="archivo-prueba" data-caso="word">Word</button>' +
            '<button type="button" class="btn btn-claro btn-chico" data-accion="archivo-prueba" data-caso="pesado">PDF 12 MB</button></div></div>' +
        mensaje("error", d.error) + confirmar +
        '<div style="display:flex;gap:8px;justify-content:flex-end;flex-wrap:wrap"><a class="btn btn-claro" href="#/portafolio">Cancelar</a><button class="btn btn-verde" type="submit">' + icono("subir", 18) + " Entregar evidencia</button></div>" +
      "</form></div>";
  }

  // ---------- Solicitudes de cuenta (/solicitudes) ----------
  function solicitudes(lista) {
    var nombresRolCorto = { estudiante: "Estudiante", docente: "Docente" };
    var cuerpo = lista.length
      ? '<ul class="lista">' + lista.map(function (s) {
          var f = s.fecha.split("T");
          return '<li class="item"><div class="avatar">' + e(iniciales(s.nombre)) + '</div>' +
            '<div class="info"><div class="n">' + e(s.nombre) + '</div><div class="c">Código ' + e(s.codigo) + " · " + e(nombresRolCorto[s.rol] || s.rol) +
            " · pedida el " + fechaLarga(f[0]) + " a las " + e((f[1] || "").slice(0, 5)) + "</div></div>" +
            '<span class="chip por-calificar">Pendiente</span>' +
            '<button class="btn btn-claro btn-chico" data-accion="rechazar-solicitud" data-id="' + s.id + '" data-nombre="' + e(s.nombre) + '">' + icono("cruz", 16) + " Rechazar</button>" +
            '<button class="btn btn-verde btn-chico" data-accion="aprobar-solicitud" data-id="' + s.id + '" data-nombre="' + e(s.nombre) + '">' + icono("check", 16) + " Aprobar</button></li>";
        }).join("") + "</ul>"
      : '<div class="vacio"><b>No hay solicitudes pendientes</b>Cuando alguien use «Regístrate» en la página de inicio, su pedido aparecerá aquí.</div>';
    return tituloPagina("Solicitudes de cuenta", "Al aprobar, se crea la cuenta y la persona ingresa con su código y la contraseña que eligió.") +
      '<div class="panel">' + cuerpo + "</div>";
  }
  function insignia(id, n) {
    var el = document.getElementById(id);
    if (!el) return;
    el.textContent = n;
    el.classList.toggle("oculto", !n);
  }

  // ---------- Otras ----------
  function sinAcceso(texto, volver) {
    return '<div class="panel vacio"><b>No tienes acceso a esta página</b>' + e(texto) + '<br><br><a class="btn btn-verde" href="' + volver + '">Ir a mi inicio</a></div>';
  }

  function avisar(texto, tipo) {
    var caja = document.getElementById("avisos");
    var div = document.createElement("div");
    div.className = "aviso" + (tipo === "error" ? " error" : "");
    div.textContent = texto;
    caja.appendChild(div);
    setTimeout(function () { div.remove(); }, 3500);
  }

  return {
    marco: marco, ingreso: ingreso, misCursos: misCursos,
    registroAsistencia: registroAsistencia, actualizarFila: actualizarFila, actualizarContador: actualizarContador, mostrarErrorAsistencia: mostrarErrorAsistencia,
    resumenSesion: resumenSesion, evidenciasCurso: evidenciasCurso, portafolioCurso: portafolioCurso,
    matricula: matricula, listaCurso: listaCurso, miPortafolio: miPortafolio, subirEvidencia: subirEvidencia,
    solicitudes: solicitudes, insignia: insignia,
    sinAcceso: sinAcceso, avisar: avisar
  };
})();
