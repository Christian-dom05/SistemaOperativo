import java.util.HashMap;
import java.util.Map;

public class Memoria {
    private final int marcosTotales;
    private final int tamMarcoKb;
    private int marcosUsados = 0;
    private final Map<Integer,Integer> asignacion = new HashMap<>(); // pid -> marcos

    public Memoria(int marcosTotales, int tamMarcoKb) {
        this.marcosTotales = marcosTotales;
        this.tamMarcoKb = tamMarcoKb;
    }

    public synchronized boolean asignar(PCB pcb) {
        if (pcb.paginasMemoria <= (marcosTotales - marcosUsados)) {
            asignacion.put(pcb.pid, pcb.paginasMemoria);
            marcosUsados += pcb.paginasMemoria;
            CentroControl.registrar(String.format("Memoria: asignados %d marcos a %s (pid=%d). Usados %d/%d", pcb.paginasMemoria, pcb.nombre, pcb.pid, marcosUsados, marcosTotales));
            return true;
        }
        CentroControl.registrar(String.format("Memoria: no hay suficiente para %s (pid=%d). Necesita %d, libres %d", pcb.nombre, pcb.pid, pcb.paginasMemoria, marcosTotales - marcosUsados));
        return false;
    }

    public synchronized void liberar(PCB pcb) {
        Integer m = asignacion.remove(pcb.pid);
        if (m != null) {
            marcosUsados -= m;
            CentroControl.registrar(String.format("Memoria: liberados %d marcos de %s (pid=%d). Usados %d/%d", m, pcb.nombre, pcb.pid, marcosUsados, marcosTotales));
        }
    }

    public synchronized int getMarcosLibres() { return marcosTotales - marcosUsados; }
}