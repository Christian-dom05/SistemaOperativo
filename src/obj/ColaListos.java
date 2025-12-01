package obj;

import ui.UIAdapter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaListos {
    private final Deque<BCP> cola = new ArrayDeque<>();

    public synchronized void enlistar(BCP bcp) {
        bcp.estado = BCP.EstadoProceso.LISTO;
        cola.addLast(bcp);
        // Animación: Nave vuela al Planeta Azul (READY)
        UIAdapter.getInstance().moverNave(bcp, "READY");
        CentroControl.registrar(String.format("ColaListos: %s en órbita de espera.", bcp.nombre));
    }

    // --- CORRECCIÓN: Renombrado de desencolar() a desenlistar() ---
    public synchronized BCP desenlistar() {
        // No necesitamos animación aquí, la animación ocurre
        // cuando el Planificador lo manda a la CPU.
        return cola.pollFirst();
    }

    public synchronized boolean estaVacia() { return cola.isEmpty(); }

    public synchronized List<BCP> snapshot() { return new ArrayList<>(cola); }
}