package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.util.Duration; // Importante para la animación
import obj.BCP;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class UIAdapter {

    private static UIAdapter instance;

    // --- AJUSTE DE ROTACIÓN ---
    // Cambia este valor si la nave sigue volando raro:
    // 0   -> Si tu imagen apunta a la DERECHA
    // 90  -> Si tu imagen apunta ARRIBA
    // 180 -> Si tu imagen apunta a la IZQUIERDA
    // -90 -> Si tu imagen apunta ABAJO
    private static final double AJUSTE_ROTACION = 0;

    private final ObservableList<String> listaLogs = FXCollections.observableArrayList();
    private ListView<String> listViewLogs;

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

    public void setListView(ListView<String> listView) {
        this.listViewLogs = listView;
        this.listViewLogs.setItems(listaLogs);
    }

    // Compatibilidad
    public void setLogArea(TextArea area) {}

    public void registrarUbicacion(String id, Point2D pos) {
        ubicaciones.put(id, pos);
    }

    public void agregarLog(String mensaje) {
        Platform.runLater(() -> {
            listaLogs.add(mensaje);
            if (listaLogs.size() > 100) listaLogs.remove(0); // Guardamos un poco más de historial
            if (listViewLogs != null) listViewLogs.scrollTo(listaLogs.size() - 1);
        });
    }

    public void crearNaveVisual(BCP bcp) {
        Point2D posInicio = ubicaciones.getOrDefault("MEMORIA", new Point2D(0,0));
        Platform.runLater(() -> {
            Entity nave = FXGL.spawn("NAVE_PROCESO", new SpawnData(posInicio.getX(), posInicio.getY()).put("pcb", bcp));
            navesVisuales.put(bcp.pid, nave);
        });
    }

    // --- LÓGICA DE MOVIMIENTO MEJORADA ---

    public void moverNave(BCP bcp, String destinoId) {
        Point2D target = ubicaciones.get(destinoId);
        if (target == null) return;
        if (Platform.isFxApplicationThread()) {
            animarInternal(bcp, target, null);
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> animarInternal(bcp, target, latch));
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public void moverNaveAsync(BCP bcp, String destinoId) {
        Point2D target = ubicaciones.get(destinoId);
        if (target == null) return;
        Platform.runLater(() -> animarInternal(bcp, target, null));
    }

    private void animarInternal(BCP bcp, Point2D targetPos, CountDownLatch latch) {
        Entity nave = navesVisuales.get(bcp.pid);
        if (nave == null) {
            if (latch != null) latch.countDown();
            return;
        }

        // Variación aleatoria para que no se amontonen exactamente en el mismo pixel
        double offsetX = Math.random() * 40 - 20;
        double offsetY = Math.random() * 40 - 20;
        Point2D destinoFinal = new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY);

        // Calcular ángulo de rotación
        Point2D vectorDireccion = destinoFinal.subtract(nave.getPosition());
        double angle = Math.toDegrees(Math.atan2(vectorDireccion.getY(), vectorDireccion.getX()));
        double rotacionFinal = angle + AJUSTE_ROTACION;

        // 1. Rotar primero (rápido)
        FXGL.animationBuilder()
                .duration(Duration.seconds(0.3))
                .rotate(nave)
                .to(rotacionFinal)
                .buildAndPlay();

        // 2. Moverse después (o durante)
        FXGL.animationBuilder()
                .duration(Duration.seconds(1.2)) // Un poco más lento para que se aprecie el viaje
                .onFinished(() -> { if (latch != null) latch.countDown(); })
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

    // Métodos de actualización de UI (Recursos, Semáforos, Memoria)
    // Se mantienen igual que antes para actualizar los textos fijos
    public void actualizarRecursoUI(String nombre, int enUso, int total) {
        Platform.runLater(() -> {
            Text t = textosRecursos.get(nombre);
            if (t != null) t.setText(nombre + "\n[" + enUso + "/" + total + "]");
        });
    }

    public void actualizarSemaforoUI(String nombre, int valor) {
        Platform.runLater(() -> {
            Text t = textosSemaforos.get(nombre);
            if (t != null) t.setText("Semaforo\n" + nombre + "\nVAL: " + valor);
        });
    }

    public void actualizarMemoriaUI(int usados, int total) {
        Platform.runLater(() -> {
            if (textoMemoria != null) {
                double porcentaje = (double) usados / total * 100;
                textoMemoria.setText(String.format("MEMORIA RAM\nUso: %d/%d (%.0f%%)", usados, total, porcentaje));
            }
        });
    }
}