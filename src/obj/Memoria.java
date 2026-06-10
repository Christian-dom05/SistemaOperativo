package obj;
import ui.UIAdapter;
import java.util.HashMap;
import java.util.Map;

public class Memoria {
    private final int marcosTotales;
    private final int tamMarcoKb;
    private int marcosUsados = 0;
    private final Map<Integer,Integer> asignacion = new HashMap<>();

    public Memoria(int marcosTotales, int tamMarcoKb) {
        this.marcosTotales = marcosTotales;
        this.tamMarcoKb = tamMarcoKb;
        UIAdapter.getInstance().actualizarMemoriaUI(0, marcosTotales);
    }

    public synchronized boolean asignar(BCP bcp) {
        if (bcp.paginasMemoria <= (marcosTotales - marcosUsados)) {
            asignacion.put(bcp.pid, bcp.paginasMemoria);
            marcosUsados += bcp.paginasMemoria;
            UIAdapter.getInstance().actualizarMemoriaUI(marcosUsados, marcosTotales);

            // Animación breve en memoria
            UIAdapter.getInstance().moverNave(bcp, "MEMORIA");
            CentroControl.registrar(String.format("Memoria: Cargado %s.", bcp.nombre));
            return true;
        }
        return false;
    }
    public synchronized void liberar(BCP bcp) {
        Integer m = asignacion.remove(bcp.pid);
        if (m != null) {
            marcosUsados -= m;
            UIAdapter.getInstance().actualizarMemoriaUI(marcosUsados, marcosTotales);
        }
    }
}