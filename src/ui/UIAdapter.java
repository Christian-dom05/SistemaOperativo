package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import obj.BCP;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class UIAdapter {

    private static UIAdapter instance;
    private TextArea logArea;
    private final Map<Integer, Entity> navesVisuales = new HashMap<>();
    private final Map<String, Point2D> ubicaciones = new HashMap<>();

    public final Map<String, Text> textosRecursos = new HashMap<>();
    public final Map<String, Text> textosSemaforos = new HashMap<>();
    public Text textoMemoria;

    private UIAdapter() {}

    public static synchronized UIAdapter getInstance() {
        if (instance == null) instance = new UIAdapter();
        return instance;
    }

    public void setLogArea(TextArea area) { this.logArea = area; }

    public void registrarUbicacion(String id, Point2D pos) {
        ubicaciones.put(id, pos);
    }

    public void agregarLog(String mensaje) {
        if (logArea == null) return;
        Platform.runLater(() -> {
            logArea.appendText(mensaje + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void crearNaveVisual(BCP bcp) {
        Point2D posInicio = ubicaciones.getOrDefault("MEMORIA", new Point2D(0,0));
        Platform.runLater(() -> {
            Entity nave = FXGL.spawn("NAVE_PROCESO", new SpawnData(posInicio.getX(), posInicio.getY()).put("pcb", bcp));
            navesVisuales.put(bcp.pid, nave);
        });
    }

    // --- MÉTODOS DE MOVIMIENTO ---

    /**
     * Mueve la nave y BLOQUEA el hilo hasta que llegue (Síncrono).
     * Úsalo para acciones importantes: CPU, Recursos, Semáforos.
     */
    public void moverNave(BCP bcp, String destinoId) {
        Point2D target = ubicaciones.get(destinoId);
        if (target == null) return;

        if (Platform.isFxApplicationThread()) {
            animarInternal(bcp, target, null);
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> animarInternal(bcp, target, latch));
            try {
                latch.await(); // El hilo lógico espera aquí
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Mueve la nave SIN BLOQUEAR el hilo (Asíncrono).
     * Úsalo para movimientos de rutina: Ir a Cola Listos, Ir a Bloqueados.
     */
    public void moverNaveAsync(BCP bcp, String destinoId) {
        Point2D target = ubicaciones.get(destinoId);
        if (target == null) return;
        // Enviamos null como latch para que no intente avisar a nadie
        Platform.runLater(() -> animarInternal(bcp, target, null));
    }

    private void animarInternal(BCP bcp, Point2D targetPos, CountDownLatch latch) {
        Entity nave = navesVisuales.get(bcp.pid);
        if (nave == null) {
            if (latch != null) latch.countDown();
            return;
        }

        double offsetX = Math.random() * 20 - 10;
        double offsetY = Math.random() * 20 - 10;
        Point2D destinoFinal = new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY);

        FXGL.animationBuilder()
                .duration(javafx.util.Duration.seconds(0.8))
                .onFinished(() -> {
                    if (latch != null) latch.countDown();
                })
                .translate(nave)
                .to(destinoFinal)
                .buildAndPlay();
    }

    public void destruirNaveVisual(BCP bcp) {
        Platform.runLater(() -> {
            Entity nave = navesVisuales.remove(bcp.pid);
            if (nave != null) nave.removeFromWorld();
        });
    }

    // --- ACTUALIZADORES UI ---
    public void actualizarRecursoUI(String nombre, int enUso, int total) {
        Platform.runLater(() -> {
            Text t = textosRecursos.get(nombre);
            if (t != null) t.setText(nombre + "\nUso: " + enUso + "/" + total);
        });
    }

    public void actualizarSemaforoUI(String nombre, int valor) {
        Platform.runLater(() -> {
            Text t = textosSemaforos.get(nombre);
            if (t != null) t.setText("Semaforo\n" + nombre + "\nValor: " + valor);
        });
    }

    public void actualizarMemoriaUI(int usados, int total) {
        Platform.runLater(() -> {
            if (textoMemoria != null) {
                int libres = total - usados;
                double porcentaje = (double) usados / total * 100;
                textoMemoria.setText(String.format("MEMORIA RAM\nLibre: %d/%d\nUso: %.1f%%", libres, total, porcentaje));
            }
        });
    }
}