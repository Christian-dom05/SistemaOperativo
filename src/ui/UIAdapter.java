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

    // Almacén de coordenadas de todas las entidades (Planetas, Recursos, Semáforos)
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

    public void agregarLog(String mensaje) {
        if (logArea == null) return;
        Platform.runLater(() -> {
            logArea.appendText(mensaje + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // --- REGISTRO DE UBICACIONES ---
    public void registrarUbicacion(String id, Point2D pos) {
        ubicaciones.put(id, pos);
    }

    // --- GESTIÓN VISUAL DE NAVES ---

    public void crearNaveVisual(BCP bcp) {
        // Las naves nacen visualmente en la Memoria (Nebulosa)
        Point2D posInicio = ubicaciones.getOrDefault("MEMORIA", new Point2D(0,0));
        Platform.runLater(() -> {
            Entity nave = FXGL.spawn("NAVE_PROCESO", new SpawnData(posInicio.getX(), posInicio.getY()).put("pcb", bcp));
            navesVisuales.put(bcp.pid, nave);
        });
    }

    /**
     * Mueve la nave al destino y ESPERA a que llegue (si se llama desde hilo de fondo).
     * Esto garantiza la fluidez: Lógica -> Animación -> Fin Animación -> Siguiente paso Lógica.
     */
    public void moverNave(BCP bcp, String destinoId) {
        Point2D target = ubicaciones.get(destinoId);
        if (target == null) return; // Destino no registrado

        if (Platform.isFxApplicationThread()) {
            // Si lo llama el botón (UI), no podemos bloquear, solo animar
            animarInternal(bcp, target, null);
        } else {
            // Si lo llama el Planificador, bloqueamos hasta que llegue
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> animarInternal(bcp, target, latch));
            try {
                latch.await(); // El hilo lógico se duerme aquí
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void animarInternal(BCP bcp, Point2D targetPos, CountDownLatch latch) {
        Entity nave = navesVisuales.get(bcp.pid);
        if (nave == null) {
            if (latch != null) latch.countDown();
            return;
        }

        // Pequeña variabilidad para que no se superpongan exacto
        double offsetX = Math.random() * 20 - 10;
        double offsetY = Math.random() * 20 - 10;
        Point2D destinoFinal = new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY);

        // Animación de 0.8 segundos
        FXGL.animationBuilder()
                .duration(javafx.util.Duration.seconds(0.8))
                .onFinished(() -> {
                    if (latch != null) latch.countDown(); // Avisar al backend que llegamos
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

    // --- ACTUALIZACIONES DE TEXTO (Igual que antes) ---
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