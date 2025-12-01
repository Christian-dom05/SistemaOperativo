package obj;
import ui.UIAdapter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaBloqueados {
    private final Deque<BCP> cola = new ArrayDeque<>();

    public synchronized void enlistar(BCP bcp) {
        bcp.estado = BCP.EstadoProceso.BLOQUEADO;
        cola.addLast(bcp);
        // Nave vuela al Planeta Rojo y espera
        UIAdapter.getInstance().moverNave(bcp, "BLOCKED");
        CentroControl.registrar(String.format("ColaBloqueados: %s atrapado en gravedad roja.", bcp.nombre));
    }

    public synchronized void remover(BCP bcp) {
        cola.remove(bcp);
    }
    public synchronized boolean estaVacia() { return cola.isEmpty(); }
    public synchronized List<BCP> snapshot() { return new ArrayList<>(cola); }
}