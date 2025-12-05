package obj;
import ui.UIAdapter;

public class Recurso {
    public final String nombre;
    private final int cantidadTotal;
    private int enUso = 0;

    public Recurso(String nombre, int cantidad) {
        this.nombre = nombre;
        this.cantidadTotal = cantidad;
    }

    public boolean solicitar(BCP bcp) {
        // 1. Animación (Fuera del bloqueo)
        UIAdapter.getInstance().moverNave(bcp, nombre);

        // 2. Lógica crítica (Dentro del bloqueo)
        synchronized (this) {
            if (enUso < cantidadTotal) {
                enUso++;
                UIAdapter.getInstance().actualizarRecursoUI(nombre, enUso, cantidadTotal);
                CentroControl.registrar(String.format("%s recolectó %s.", bcp.nombre, nombre));
                return true;
            }
            return false;
        }
    }

    public synchronized void liberar() {
        if (enUso > 0) {
            enUso--;
            UIAdapter.getInstance().actualizarRecursoUI(nombre, enUso, cantidadTotal);
        }
    }

    public synchronized int getEnUso() { return enUso; }
}