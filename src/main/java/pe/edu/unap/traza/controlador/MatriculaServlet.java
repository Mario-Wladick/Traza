package pe.edu.unap.traza.controlador;

import jakarta.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unap.traza.modelo.CursoModelo;
import pe.edu.unap.traza.modelo.MatriculaModelo;
import pe.edu.unap.traza.modelo.ReglaException;
import pe.edu.unap.traza.modelo.UsuarioModelo;
import pe.edu.unap.traza.modelo.Objetos.Usuario;

/**
 * POST /api/matriculas        → matricularEstudiante (RF-02). No revisa duplicados: eso lo hace el modelo.
 * GET  /api/usuarios/{codigo}  busca al estudiante para mostrar su nombre en el formulario
 */
@WebServlet(urlPatterns = {"/api/matriculas", "/api/usuarios/*"})
public class MatriculaServlet extends BaseServlet {

    private final MatriculaModelo matriculas = new MatriculaModelo();
    private final UsuarioModelo usuarios = new UsuarioModelo();

    @Override
    protected Object get(Peticion p) throws Exception {
        p.exigirRol("secretaria");
        Usuario u = usuarios.buscar(p.parte(0));
        if (u == null || !u.es("estudiante")) throw new ReglaException(404, "No hay un estudiante con ese código");
        return json("ok", true, "usuario", u);
    }

    @Override
    protected Object post(Peticion p) throws Exception {
        p.exigirRol("secretaria");
        List<Integer> cursos = new ArrayList<>();
        String[] elegidos = p.req().getParameterValues("cursos");
        if (elegidos != null) for (String c : elegidos) {
            try { cursos.add(Integer.parseInt(c)); } catch (NumberFormatException e) { /* se ignora */ }
        }
        String semestre = p.param("semestre") == null ? CursoModelo.SEMESTRE_ACTUAL : p.param("semestre");
        return json("ok", true, "resultado", matriculas.matricularEstudiante(p.param("codigo"), semestre, cursos));
    }
}
