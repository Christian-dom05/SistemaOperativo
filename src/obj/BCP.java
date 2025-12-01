package obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BCP {
    public final int pid;
    public String nombre;
    public EstadoProceso estado;
    public int contadorPrograma;
    public Map<String,Integer> registros = new HashMap<>();
    public int tiempoCpuTotalMs;
    public int tiempoCpuRestanteMs;
    public List<String> recursosNecesarios = new ArrayList<>();
    public int paginasMemoria;

    public BCP(int pid, String nombre, int cpuMs, int paginas, List<String> recursos) {
        this.pid = pid;
        this.nombre = nombre;
        this.estado = EstadoProceso.NUEVO;
        this.contadorPrograma = 0;
        this.tiempoCpuTotalMs = cpuMs;
        this.tiempoCpuRestanteMs = cpuMs;
        this.paginasMemoria = paginas;
        if (recursos != null) this.recursosNecesarios.addAll(recursos);
    }

    @Override
    public String toString() {
        return String.format("obj.BCP(pid=%d,nombre=%s,estado=%s,pc=%d,restante=%d)", pid, nombre, estado, contadorPrograma, tiempoCpuRestanteMs);
    }

    public enum EstadoProceso { NUEVO, LISTO, EJECUTANDO, BLOQUEADO, TERMINADO }
}