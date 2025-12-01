package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea; // Ya no se usa mucho, pero lo dejamos por compatibilidad
import javafx.scene.text.Text;
import obj.BCP;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class UIAdapter {

    private static UIAdapter instance;

    // --- NUEVO: Manejo de logs con lista observable para mejor UI ---
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

    // Método de configuración nuevo para el ListView
    public void setListView(ListView<String> listView) {
        this.listViewLogs = listView;
        this.listViewLogs.setItems(listaLogs);
    }

    // Mantenemos este por compatibilidad, pero ya no hace nada crítico
    public void setLogArea(TextArea area) {}

    public void registrarUbicacion(String id, Point2D pos) {
        ubicaciones.put(id, pos);
    }

    public void agregarLog(String mensaje) {
        Platform.runLater(() -> {
            listaLogs.add(mensaje);
            // Mantener solo los últimos 50 mensajes para armonía visual
            if (listaLogs.size() > 50) {
                listaLogs.remove(0);
            }
            // Auto-scroll al último elemento
            if (listViewLogs != null) {
                listViewLogs.scrollTo(listaLogs.size() - 1);
            }
        });
    }

    // ... (El resto de métodos moverNave, moverNaveAsync, etc. se mantienen IGUAL que en la versión anterior) ...
    // COPIA AQUÍ LOS MÉTODOS moverNave, moverNaveAsync, animarInternal, crearNaveVisual, destruirNaveVisual
    // y los actualizadores de UI (actualizarRecursoUI, etc.) DEL CÓDIGO ANTERIOR.
    // Para no hacer el código gigante aquí, asumo que mantienes esa lógica.

    public void crearNaveVisual(BCP bcp) {
        Point2D posInicio = ubicaciones.getOrDefault("MEMORIA", new Point2D(0,0));
        Platform.runLater(() -> {
            Entity nave = FXGL.spawn("NAVE_PROCESO", new SpawnData(posInicio.getX(), posInicio.getY()).put("pcb", bcp));
            navesVisuales.put(bcp.pid, nave);
        });
    }

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
        double offsetX = Math.random() * 20 - 10;
        double offsetY = Math.random() * 20 - 10;
        Point2D destinoFinal = new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY);

        FXGL.animationBuilder()
                .duration(javafx.util.Duration.seconds(0.8))
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