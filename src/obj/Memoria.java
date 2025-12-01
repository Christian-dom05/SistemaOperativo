package obj;

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

    public synchronized boolean asignar(BCP bcp) {
        if (bcp.paginasMemoria <= (marcosTotales - marcosUsados)) {
            asignacion.put(bcp.pid, bcp.paginasMemoria);
            marcosUsados += bcp.paginasMemoria;
            CentroControl.registrar(String.format("Memoria: asignados %d marcos a %s (pid=%d). Usados %d/%d", bcp.paginasMemoria, bcp.nombre, bcp.pid, marcosUsados, marcosTotales));
            return true;
        }
        CentroControl.registrar(String.format("Memoria: no hay suficiente para %s (pid=%d). Necesita %d, libres %d", bcp.nombre, bcp.pid, bcp.paginasMemoria, marcosTotales - marcosUsados));
        return false;
    }

    public synchronized void liberar(BCP BCP) {
        Integer m = asignacion.remove(BCP.pid);
        if (m != null) {
            marcosUsados -= m;
            CentroControl.registrar(String.format("Memoria: liberados %d marcos de %s (pid=%d). Usados %d/%d", m, BCP.nombre, BCP.pid, marcosUsados, marcosTotales));
        }
    }

    public synchronized int getMarcosLibres() { return marcosTotales - marcosUsados; }
}