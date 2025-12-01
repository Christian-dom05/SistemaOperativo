package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Point2D;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ui.EntityType;
import ui.SpaceOsFactory;
import ui.UIAdapter;
import obj.*;

import java.util.*;

public class SpaceOsApp extends GameApplication {

    private Planificador planificadorRef;
    private Thread hiloPlanificadorRef;
    private Thread hiloEventosRef;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("GalaxyOS - Simulador SO");
        settings.setVersion("1.0");
        settings.setBgColor(Color.web("#0a0a2a"));
    }

    @Override
    protected void initUI() {
        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: limegreen; -fx-control-inner-background: black;");
        VBox.setVgrow(logTextArea, javafx.scene.layout.Priority.ALWAYS);

        UIAdapter.getInstance().setLogArea(logTextArea);

        SplitPane splitPane = new SplitPane();
        splitPane.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        splitPane.setDividerPositions(0.33);
        splitPane.getItems().addAll(new VBox(logTextArea), FXGL.getGameViewPane());

        FXGL.getGameScene().addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        double gameCenterX = (FXGL.getAppWidth() * 0.66) / 2 + (FXGL.getAppWidth() * 0.33);
        double gameCenterY = FXGL.getAppHeight() / 2.0;

        UIAdapter.getInstance().posSol = new Point2D(gameCenterX, gameCenterY);
        UIAdapter.getInstance().posReady = new Point2D(gameCenterX - 250, gameCenterY);
        UIAdapter.getInstance().posBlocked = new Point2D(gameCenterX + 250, gameCenterY);

        FXGL.spawn("SOL_CPU", UIAdapter.getInstance().posSol);
        FXGL.spawn("PLANETA_READY", UIAdapter.getInstance().posReady);
        FXGL.spawn("PLANETA_BLOCKED", UIAdapter.getInstance().posBlocked);

        new Thread(this::iniciarSimulacionBackend, "Hilo-Simulacion").start();
    }

    private void iniciarSimulacionBackend() {
        CentroControl.registrar("=== Iniciando Simulacion Space OS (Grafica) ===");

        Memoria memoria = new Memoria(50, 4);
        ColaListos colaListos = new ColaListos();
        ColaBloqueados colaBloq = new ColaBloqueados();
        CPU cpu = new CPU();

        Map<String, Recurso> recursos = new HashMap<>();
        recursos.put("Marte", new Recurso("Portal-Marte", 1));
        recursos.put("Estacion-Alpha", new Recurso("Estacion-Alpha", 2));
        recursos.put("Planeta-Y", new Recurso("Planeta-Y", 1));

        Map<String, SemaphoroGalactico> semaforos = new HashMap<>();
        semaforos.put("Marte", new SemaphoroGalactico("Portal-Marte", 1));
        semaforos.put("Estacion-Alpha", new SemaphoroGalactico("Portal-EstAlpha", 2));

        List<BCP> naves = new ArrayList<>();
        naves.add(new BCP(1, "Nave-Dory", 1200, 5, Arrays.asList("Marte")));
        naves.add(new BCP(2, "Nave-Borealis", 800, 3, Arrays.asList("Estacion-Alpha")));
        naves.add(new BCP(3, "Nave-Cassiopeia", 1500, 7, Arrays.asList("Planeta-Y", "Estacion-Alpha")));
        naves.add(new BCP(4, "Nave-Draco", 600, 2, Collections.emptyList()));
        naves.add(new BCP(5, "Nave-Equinox", 400, 1, Arrays.asList("Estacion-Alpha")));

        for (BCP bcp : naves) {
            bcp.estado = BCP.EstadoProceso.NUEVO;
            UIAdapter.getInstance().crearNaveVisual(bcp);

            if (memoria.asignar(bcp)) {
                colaListos.enlistar(bcp);
            } else {
                colaBloq.enlistar(bcp);
                CentroControl.registrar(String.format("%s (pid=%d) no puede iniciar por memoria.", bcp.nombre, bcp.pid));
            }
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }

        planificadorRef = new Planificador(colaListos, colaBloq, memoria, cpu, recursos, semaforos, 200);
        hiloPlanificadorRef = new Thread(planificadorRef, "HiloPlanificador");
        hiloPlanificadorRef.start();

        hiloEventosRef = new Thread(() -> {
            Random rnd = new Random();
            while (true) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                if (rnd.nextDouble() < 0.4) {
                    for (SemaphoroGalactico s : semaforos.values()) s.signalSem(colaListos, colaBloq);
                }
                if (rnd.nextDouble() < 0.2) {
                    for (Recurso r : recursos.values()) r.liberar();
                }
                synchronized (colaBloq) {
                    List<BCP> b = colaBloq.snapshot();
                    for (BCP p : b) if (Math.random() < 0.3) {
                        colaBloq.remover(p);
                        colaListos.enlistar(p);
                    }
                }
            }
        }, "HiloEventos");
        hiloEventosRef.start();
    }

    @Override
    protected void onExit() {
        if (planificadorRef != null) planificadorRef.detener();
        if (hiloPlanificadorRef != null) hiloPlanificadorRef.interrupt();
        if (hiloEventosRef != null) hiloEventosRef.interrupt();
        CentroControl.cerrar();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}