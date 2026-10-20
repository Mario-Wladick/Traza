# TRAZA · Sistema de Control Educativo con Portafolio Automático

EST323 Ingeniería de Software II · UNA Puno · Vuelta 01 (RF-06, RF-02, RF-01, RF-04, RF-03)

**Java 21+ (funciona con JDK 24) · Servlets Jakarta 6.0 · PostgreSQL · HTML + CSS + JavaScript puro**

---

## 1 · Crear la base de datos (una sola vez)

En **pgAdmin** (o `psql`), conectado como `postgres`:

1. Abre y ejecuta `sql/00_crear_base.sql`. Crea el usuario `traza_app` (clave `traza2026`) y la base `traza`.
2. Conéctate a la base **traza** y ejecuta, en orden:
   - `sql/01_esquema.sql`: las tablas, claves y reglas.
   - `sql/02_datos_prueba.sql`: 45 usuarios, 4 cursos, asistencias y evidencias de prueba.

Para volver a los datos iniciales, ejecuta otra vez `01_esquema.sql` y `02_datos_prueba.sql`.

> Si prefieres usar tu usuario `postgres`, cambia `db.usuario` y `db.clave` en
> `src/main/resources/traza.properties`.

## 2 · Abrir y correr en NetBeans

1. **File → Open Project** y elige la carpeta `traza` (NetBeans la reconoce como proyecto Maven).
2. La primera vez, NetBeans descarga las dependencias (necesita internet).
3. **Clic derecho en el proyecto → Run.** Si te pide servidor:
   - Con **JDK 24**, usa **Apache Tomcat 10.1 u 11** (*Tools → Servers → Add Server*).
   - Con **JDK 21**, también sirve **GlassFish 7**.
4. Se abre `http://localhost:8080/traza/`.

## 3 · Cuentas de prueba

| Rol | Código | Contraseña |
| --- | --- | --- |
| Docente | 100001 | docente123 |
| Estudiante | 260001 | estudiante123 |
| Secretaría académica | 300001 | secretaria123 |
| Director de escuela | 400001 | director123 |

---

## Cómo está organizado (MVC)

```
traza/
├── pom.xml                         Maven: servlet-api 6.0 y driver de PostgreSQL
├── sql/                            00 crear base · 01 esquema · 02 datos de prueba
└── src/main/
    ├── resources/traza.properties  conexión a la base y carpeta de evidencias
    ├── java/pe/edu/unap/traza/
    │   ├── modelo/                 MODELO: objetos y las 6 operaciones con JDBC
    │   │   ├── Objetos.java            Usuario, Curso, Asistencia, Evidencia, Portafolio
    │   │   ├── UsuarioModelo.java      1 iniciarSesion
    │   │   ├── MatriculaModelo.java    2 matricularEstudiante
    │   │   ├── AsistenciaModelo.java   3 registrarAsistenciaSesion (todo o nada)
    │   │   ├── EvidenciaModelo.java    4 subirEvidencia · 5 calificarEvidencia
    │   │   ├── PortafolioModelo.java   6 armarPortafolio
    │   │   ├── CursoModelo.java        cursos y listarMatriculados
    │   │   ├── Conexion.java · Clave.java (PBKDF2) · ReglaException.java
    │   └── controlador/            CONTROLADOR: un servlet por grupo de rutas
    │       ├── IngresoServlet.java     /api/ingresar · /api/sesion · /api/salir
    │       ├── CursoServlet.java       /api/cursos/...
    │       ├── MatriculaServlet.java   /api/matriculas · /api/usuarios/{codigo}
    │       ├── EvidenciaServlet.java   /api/evidencias/{id}/calificacion · /archivo
    │       ├── PortafolioServlet.java  /api/portafolio · /api/portafolio/evidencias
    │       └── BaseServlet.java · Json.java
    └── webapp/                     VISTA: index.html, app.html, css, img, js
```

### Rutas → operaciones → requisitos

| Ruta (servlet) | Operación del modelo | Requisito |
| --- | --- | --- |
| POST /api/ingresar | iniciarSesion | RF-06 |
| POST /api/matriculas | matricularEstudiante | RF-02 |
| POST /api/cursos/{id}/asistencia/{sesion} | registrarAsistenciaSesion | RF-01, RNF-01 |
| POST /api/portafolio/evidencias | subirEvidencia | RF-04 |
| POST /api/evidencias/{id}/calificacion | calificarEvidencia | RF-03, RNF-04 |
| GET /api/portafolio · /api/cursos/{id}/portafolio | armarPortafolio | RF-05 |
| GET /api/cursos/{id}/estudiantes | listarMatriculados (hueco de la Práctica 4) | RF-01, RF-02 |
| GET /api/cursos/{id}/evidencias | listarPorCalificar (hueco de la Práctica 4) | RF-03 |

### Reglas del análisis que cuida la base de datos

- **«Ya está matriculado en este curso»:** clave única `uq_matricula (estudiante, curso)`.
- **Asistencia todo o nada:** una transacción; si algo falla, no queda nada a medias (flujo 4a).
- **Una evidencia por tipo y unidad:** clave única `uq_evidencia` (flujo 3b, pregunta si reemplaza).
- **Calificación de 0 a 20:** `CHECK (calificacion BETWEEN 0 AND 20)`.
- **Solo estudiantes matriculados tienen asistencia y evidencias:** clave foránea a `matricula`.
- **El portafolio se arma solo:** vista `v_portafolio_estudiante_curso`, sin tabla aparte.
- **Contraseñas cifradas** con PBKDF2-SHA256 (Ley N.º 29733).

### Dónde quedan los archivos de las evidencias

En la carpeta `traza-evidencias` dentro de tu usuario (por ejemplo `C:\Users\Mario\traza-evidencias`).
Se cambia en `traza.properties` → `evidencias.carpeta`. Las evidencias de los datos de prueba no tienen archivo real.

## Pendiente para las siguientes vueltas

- RF-05: operación «Revisar portafolio» (estado *revisado* con la observación del director).
- RF-07: objeto Competencia y avance por competencias (vuelta 03).
- RF-08: objeto Alerta y operación Detectar riesgo (vuelta 02).
- Crear cuentas de docentes y estudiantes desde la app (pregunta abierta: hoy se cargan por SQL).
- Quitar las «Cuentas de prueba» de la pantalla de ingreso antes de publicar.
