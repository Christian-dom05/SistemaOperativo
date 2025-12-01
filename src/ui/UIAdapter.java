package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.TextArea;
import obj.PCB;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase Singleton thread-safe que actúa como puente entre los hilos de simulación
 * y el hilo de interfaz gráfica (JavaFX Application Thread).
 */
public class UIAdapter {

    private static UIAdapter instance;
    private TextArea logArea; // Referencia al área de texto en la UI
    private final Map<Integer, Entity> navesVisuales = new HashMap<>();

    // Posiciones fijas en el radar (se configurarán al iniciar la App)
    public Point2D posSol;
    public Point2D posReady;
    public Point2D posBlocked;

    private UIAdapter() {}

    public static synchronized UIAdapter getInstance() {
        if (instance == null) {
            instance = new UIAdapter();
        }
        return instance;
    }

    public void setLogArea(TextArea area) {
        this.logArea = area;
    }

    // --- Métodos llamados desde el Backend (Simulación) ---

    // 1. Logs
    public void agregarLog(String mensaje) {
        if (logArea == null) return;
        // IMPORTANTE: Usar Platform.runLater para tocar la UI
        Platform.runLater(() -> {
            logArea.appendText(mensaje + "\n");
            // Auto-scroll hacia abajo
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // 2. Gestión de Naves/Procesos
    public void crearNaveVisual(PCB pcb) {
        Platform.runLater(() -> {
            // La nave aparece inicialmente cerca del planeta Ready
            Entity nave = FXGL.spawn("NAVE_PROCESO", new SpawnData(posReady.getX(), posReady.getY() + 50).put("pcb", pcb));
            navesVisuales.put(pcb.pid, nave);
        });
    }

    public void moverNaveA(PCB pcb, EntityType destino) {
        Platform.runLater(() -> {
            Entity nave = navesVisuales.get(pcb.pid);
            if (nave == null) return;

            Point2D targetPos = switch (destino) {
                case SOL_CPU -> posSol;
                case PLANETA_READY -> posReady;
                case PLANETA_BLOCKED -> posBlocked;
                default -> posReady;
            };

            // Animación simple de movimiento hacia el destino
            // Se le da un pequeño offset aleatorio para que no se apilen todas en el mismo pixel
            double offsetX = Math.random() * 40 - 20;
            double offsetY = Math.random() * 40 - 20;

            nave.translateTowards(new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY), 5);
            // Nota: Para una animación real fluida, se necesitaría un Component de FXGL,
            // esto es un "teletransporte con slide" básico por ahora.
            FXGL.animationBuilder()
                    .duration(javafx.util.Duration.seconds(0.5))
                    .translate(nave)
                    .to(new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY))
                    .buildAndPlay();
        });
    }

    public void destruirNaveVisual(PCB pcb) {
        Platform.runLater(() -> {
            Entity nave = navesVisuales.remove(pcb.pid);
            if (nave != null) {
                nave.removeFromWorld();
            }
        });
    }
}