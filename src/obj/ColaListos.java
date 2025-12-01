package obj;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaListos {
    private final Deque<BCP> cola = new ArrayDeque<>();

    public synchronized void enlistar(BCP bcp) {
        bcp.estado = BCP.EstadoProceso.LISTO;
        cola.addLast(bcp);
        CentroControl.registrar(String.format("obj.ColaListos: %s (pid=%d) enlistado. Tamaño=%d", bcp.nombre, bcp.pid, cola.size()));
    }

    public synchronized BCP desenlistar() {
        BCP p = cola.pollFirst();
        if (p != null) {
            CentroControl.registrar(String.format("obj.ColaListos: desenlistado %s (pid=%d). Tamaño=%d", p.nombre, p.pid, cola.size()));
        }
        return p;
    }

    public synchronized boolean estaVacia() { return cola.isEmpty(); }

    public synchronized List<BCP> snapshot() { return new ArrayList<>(cola); }
}