package obj;
import ui.UIAdapter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaListos {
    private final Deque<BCP> cola = new ArrayDeque<>();

    public void enlistar(BCP bcp) {
        // 1. Lógica protegida
        synchronized (this) {
            bcp.estado = BCP.EstadoProceso.LISTO;
            cola.addLast(bcp);
        }
        // 2. Animación ASÍNCRONA (No bloquea el hilo)
        UIAdapter.getInstance().moverNaveAsync(bcp, "READY");
        CentroControl.registrar(String.format("ColaListos: %s en órbita de espera.", bcp.nombre));
    }

    public synchronized BCP desenlistar() {
        return cola.pollFirst();
    }
    public synchronized boolean estaVacia() { return cola.isEmpty(); }
    public synchronized List<BCP> snapshot() { return new ArrayList<>(cola); }
}