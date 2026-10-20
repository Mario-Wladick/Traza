/* =========================================================
   TRAZA · Acceso al MODELO desde el navegador
   Ya no guarda nada en el navegador: cada función llama a una
   ruta del servidor (servlets de Java) y el modelo Java hace el
   trabajo con PostgreSQL. Todas las funciones son asíncronas
   (devuelven una promesa): se usan con await.

   Cada respuesta trae { ok: true, ... } o { ok: false, mensaje }.
   ========================================================= */
var Modelo = (function () {
  "use strict";

  var SEMESTRE = "2026-II";
  var UNIDADES = ["I", "II", "III"];
  var TIPOS = ["trabajo", "práctica", "proyecto", "examen"];
  var MAX_MB = 10;

  /* Envía un pedido al servidor. datos: objeto simple (se manda como formulario) o FormData (archivos). */
  async function pedir(metodo, ruta, datos) {
    var opciones = { method: metodo, credentials: "same-origin", headers: { "Accept": "application/json" } };
    if (datos instanceof FormData) {
      opciones.body = datos;
    } else if (datos) {
      var form = new URLSearchParams();
      Object.keys(datos).forEach(function (k) {
        var v = datos[k];
        if (Array.isArray(v)) v.forEach(function (x) { form.append(k, x); });
        else if (v !== undefined && v !== null) form.append(k, v);
      });
      opciones.body = form;
    }
    var resp;
    try {
      resp = await fetch("api/" + ruta, opciones);
    } catch (e) {
      // Sin conexión con el servidor (flujo 4a): lo marcado se conserva en la pantalla
      return { ok: false, estado: 0, mensaje: "No se pudo guardar. Intente de nuevo" };
    }
    var json;
    try { json = await resp.json(); }
    catch (e) { json = { ok: false, mensaje: "El servidor no respondió bien. Intente de nuevo" }; }
    json.estado = resp.status;
    return json;
  }

  return {
    SEMESTRE: SEMESTRE, UNIDADES: UNIDADES, TIPOS: TIPOS, MAX_MB: MAX_MB,

    // OPERACIÓN 1 · iniciarSesion (RF-06)
    iniciarSesion: function (codigo, clave) { return pedir("POST", "ingresar", { codigo: codigo, clave: clave }); },
    sesion: function () { return pedir("GET", "sesion"); },
    salir: function () { return pedir("POST", "salir", {}); },

    // Cursos
    cursosPublicos: function () { return pedir("GET", "cursos/publicos"); },
    misCursos: function () { return pedir("GET", "cursos"); },
    listaCurso: function (id) { return pedir("GET", "cursos/" + id + "/estudiantes"); },

    // OPERACIÓN 2 · matricularEstudiante (RF-02)
    buscarEstudiante: function (codigo) { return pedir("GET", "usuarios/" + encodeURIComponent(codigo)); },
    matricularEstudiante: function (codigo, semestre, cursos) {
      return pedir("POST", "matriculas", { codigo: codigo, semestre: semestre, cursos: cursos });
    },

    // OPERACIÓN 3 · registrarAsistenciaSesion (RF-01)
    siguienteSesion: function (id) { return pedir("GET", "cursos/" + id + "/asistencia"); },
    asistencia: function (id, sesion) { return pedir("GET", "cursos/" + id + "/asistencia/" + sesion); },
    registrarAsistenciaSesion: function (id, sesion, estados) {
      var datos = {};
      Object.keys(estados).forEach(function (codigo) { datos["e." + codigo] = estados[codigo]; });
      return pedir("POST", "cursos/" + id + "/asistencia/" + sesion, datos);
    },
    resumenSesion: function (id, sesion) { return pedir("GET", "cursos/" + id + "/asistencia/" + sesion + "/resumen"); },

    // OPERACIÓN 4 · subirEvidencia (RF-04)
    subirEvidencia: function (curso, unidad, tipo, archivo, reemplazar) {
      var fd = new FormData();
      fd.append("curso", curso);
      fd.append("unidad", unidad);
      fd.append("tipo", tipo);
      fd.append("reemplazar", reemplazar ? "true" : "false");
      if (archivo) fd.append("archivo", archivo, archivo.name);
      return pedir("POST", "portafolio/evidencias", fd);
    },

    // OPERACIÓN 5 · calificarEvidencia (RF-03, RNF-04)
    evidenciasCurso: function (id) { return pedir("GET", "cursos/" + id + "/evidencias"); },
    calificarEvidencia: function (id, nota) { return pedir("POST", "evidencias/" + id + "/calificacion", { nota: nota }); },

    // OPERACIÓN 6 · armarPortafolio (RF-05)
    miPortafolio: function () { return pedir("GET", "portafolio"); },
    portafolioCurso: function (id) { return pedir("GET", "cursos/" + id + "/portafolio"); }
  };
})();
