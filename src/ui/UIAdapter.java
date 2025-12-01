package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.TextArea;
import obj.BCP; // <--- IMPORTANTE: Usar BCP

import java.util.HashMap;
import java.util.Map;

public class UIAdapter {

    private static UIAdapter instance;
    private TextArea logArea;
    private final Map<Integer, Entity> navesVisuales = new HashMap<>();

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

    public void agregarLog(String mensaje) {
        if (logArea == null) return;
        Platform.runLater(() -> {
            logArea.appendText(mensaje + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // AQUI CAMBIAMOS PCB POR BCP
    public void crearNaveVisual(BCP bcp) {
        Platform.runLater(() -> {
            if (posReady != null) {
                Entity nave = FXGL.spawn("NAVE_PROCESO", new SpawnData(posReady.getX(), posReady.getY() + 50).put("pcb", bcp));
                navesVisuales.put(bcp.pid, nave);
            }
        });
    }

    public void moverNaveA(BCP bcp, EntityType destino) {
        Platform.runLater(() -> {
            Entity nave = navesVisuales.get(bcp.pid);
            if (nave == null) return;

            Point2D targetPos = switch (destino) {
                case SOL_CPU -> posSol;
                case PLANETA_READY -> posReady;
                case PLANETA_BLOCKED -> posBlocked;
                default -> posReady;
            };

            double offsetX = Math.random() * 40 - 20;
            double offsetY = Math.random() * 40 - 20;

            FXGL.animationBuilder()
                    .duration(javafx.util.Duration.seconds(0.5))
                    .translate(nave)
                    .to(new Point2D(targetPos.getX() + offsetX, targetPos.getY() + offsetY))
                    .buildAndPlay();
        });
    }

    public void destruirNaveVisual(BCP bcp) {
        Platform.runLater(() -> {
            Entity nave = navesVisuales.remove(bcp.pid);
            if (nave != null) {
                nave.removeFromWorld();
            }
        });
    }
}