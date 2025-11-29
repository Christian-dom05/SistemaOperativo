public class Recurso {
    public final String nombre;
    private final int cantidadTotal;
    private int enUso = 0;

    public Recurso(String nombre, int cantidad) {
        this.nombre = nombre;
        this.cantidadTotal = cantidad;
    }

    public synchronized boolean solicitar() {
        if (enUso < cantidadTotal) { enUso++; return true; }
        return false;
    }

    public synchronized void liberar() {
        if (enUso > 0) enUso--;
    }

    public synchronized int getEnUso() { return enUso; }
}