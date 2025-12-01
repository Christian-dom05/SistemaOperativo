package obj;
import ui.UIAdapter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaBloqueados {
    private final Deque<BCP> cola = new ArrayDeque<>();

    public void enlistar(BCP bcp) {
        // 1. Lógica protegida
        synchronized (this) {
            bcp.estado = BCP.EstadoProceso.BLOQUEADO;
            cola.addLast(bcp);
        }
        // 2. Animación ASÍNCRONA (No bloquea el hilo)
        UIAdapter.getInstance().moverNaveAsync(bcp, "BLOCKED");
        CentroControl.registrar(String.format("ColaBloqueados: %s atrapado en gravedad roja.", bcp.nombre));
    }

    public synchronized void remover(BCP bcp) {
        cola.remove(bcp);
    }
    public synchronized boolean estaVacia() { return cola.isEmpty(); }
    public synchronized List<BCP> snapshot() { return new ArrayList<>(cola); }
}