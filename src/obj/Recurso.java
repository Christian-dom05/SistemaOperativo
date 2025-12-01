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

    public synchronized boolean solicitar(BCP bcp) { // Aceptamos BCP para animar
        // Nave intenta ir al recurso visualmente
        UIAdapter.getInstance().moverNave(bcp, nombre);

        if (enUso < cantidadTotal) {
            enUso++;
            UIAdapter.getInstance().actualizarRecursoUI(nombre, enUso, cantidadTotal);
            CentroControl.registrar(String.format("%s recolectó %s.", bcp.nombre, nombre));
            return true;
        }
        return false;
    }

    public synchronized void liberar() {
        if (enUso > 0) {
            enUso--;
            UIAdapter.getInstance().actualizarRecursoUI(nombre, enUso, cantidadTotal);
        }
    }
    public synchronized int getEnUso() { return enUso; }
}