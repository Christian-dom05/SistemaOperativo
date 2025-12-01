package obj;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaBloqueados {
    private final Deque<BCP> cola = new ArrayDeque<>();

    public synchronized void enlistar(BCP bcp) {
        bcp.estado = BCP.EstadoProceso.BLOQUEADO;
        cola.addLast(bcp);
        CentroControl.registrar(String.format("ColaBloqueados: %s (pid=%d) enlistado. Tamaño=%d", bcp.nombre, bcp.pid, cola.size()));
    }

    public synchronized void remover(BCP bcp) {
        if (cola.remove(bcp)) {
            CentroControl.registrar(String.format("ColaBloqueados: removido %s (pid=%d). Tamaño=%d", bcp.nombre, bcp.pid, cola.size()));
        }
    }

    public synchronized boolean estaVacia() { return cola.isEmpty(); }

    public synchronized List<BCP> snapshot() { return new ArrayList<>(cola); }
}