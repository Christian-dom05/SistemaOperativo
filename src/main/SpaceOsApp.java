package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ui.SpaceOsFactory;
import ui.UIAdapter;
import obj.*;

import java.util.*;

public class SpaceOsApp extends GameApplication {

    private Memoria memoria;
    private ColaListos colaListos;
    private ColaBloqueados colaBloq;
    private CPU cpu;
    private Map<String, Recurso> recursos;
    private Map<String, SemaphoroGalactico> semaforos;
    private int pidCounter = 1;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("GalaxyOS - Flujo Fluido");
        settings.setVersion("4.0");
    }

    @Override
    protected void initUI() {

        // Si tienes una imagen llamada "fondo.jpg" en src/assets/textures/, descomenta esto:
        // FXGL.getGameScene().setBackgroundRepeat("fondo.jpg");

        // Si no hay imagen, usamos el color espacio profundo:
        FXGL.getGameScene().setBackgroundColor(Color.web("#050510"));
        //FXGL.getGameScene().setBackgroundColor(Color.web("#0a0a2a"));
        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: limegreen; -fx-control-inner-background: black;");
        UIAdapter.getInstance().setLogArea(logTextArea);

        Button btnLanzar = new Button("LANZAR NAVE");
        btnLanzar.setStyle("-fx-font-size: 16px; -fx-base: #4444aa; -fx-text-fill: white;");
        btnLanzar.setMaxWidth(Double.MAX_VALUE);
        btnLanzar.setOnAction(e -> crearYLanzarProceso());

        VBox leftPane = new VBox(10, btnLanzar, logTextArea);
        leftPane.setStyle("-fx-padding: 10; -fx-background-color: #111;");
        VBox.setVgrow(logTextArea, javafx.scene.layout.Priority.ALWAYS);

        Pane gameOverlayPane = new Pane();
        gameOverlayPane.setStyle("-fx-background-color: transparent;");
        gameOverlayPane.setPickOnBounds(false);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, gameOverlayPane);
        splitPane.setStyle("-fx-background-color: transparent;");
        splitPane.setDividerPositions(0.33);

        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // --- GENERACIÓN DE ESTRELLAS DE FONDO ---
        for (int i = 0; i < 150; i++) {
            double x = Math.random() * FXGL.getAppWidth();
            double y = Math.random() * FXGL.getAppHeight();
            FXGL.spawn("ESTRELLA", x, y);
        }

        double cx = (FXGL.getAppWidth() * 0.66) / 2 + (FXGL.getAppWidth() * 0.33);
        double cy = FXGL.getAppHeight() / 2.0;

        // --- SPAWN Y REGISTRO DE UBICACIONES ---
        // 1. Entidades Base
        Entity sol = FXGL.spawn("SOL_CPU", cx, cy);
        UIAdapter.getInstance().registrarUbicacion("CPU", sol.getPosition());

        Entity ready = FXGL.spawn("PLANETA_READY", cx - 200, cy);
        UIAdapter.getInstance().registrarUbicacion("READY", ready.getPosition());

        Entity blocked = FXGL.spawn("PLANETA_BLOCKED", cx + 200, cy);
        UIAdapter.getInstance().registrarUbicacion("BLOCKED", blocked.getPosition());

        Entity nebula = FXGL.spawn("NEBULOSA_MEMORIA", cx - 300, cy - 250);
        UIAdapter.getInstance().registrarUbicacion("MEMORIA", nebula.getPosition());

        // 2. Recursos y Semáforos
        spawnYRegistrar("Marte", "RECURSO", cx + 150, cy - 200, 1);
        spawnYRegistrar("Portal-Marte", "SEMAFORO", cx + 250, cy - 200, 1);

        spawnYRegistrar("Estacion-Alpha", "RECURSO", cx + 150, cy + 200, 2);
        spawnYRegistrar("Portal-EstAlpha", "SEMAFORO", cx + 250, cy + 200, 2);

        inicializarKernel();
    }

    // Helper para registrar
    private void spawnYRegistrar(String nombre, String tipo, double x, double y, int val) {
        Entity e = FXGL.spawn(tipo, new SpawnData(x, y).put("nombre", nombre).put("capacidad", val).put("valor", val));
        UIAdapter.getInstance().registrarUbicacion(nombre, e.getPosition());
    }

    private void inicializarKernel() {
        memoria = new Memoria(50, 4);
        colaListos = new ColaListos();
        colaBloq = new ColaBloqueados();
        cpu = new CPU();

        recursos = new HashMap<>();
        recursos.put("Marte", new Recurso("Marte", 1));
        recursos.put("Estacion-Alpha", new Recurso("Estacion-Alpha", 2));

        semaforos = new HashMap<>();
        semaforos.put("Portal-Marte", new SemaphoroGalactico("Portal-Marte", 1));
        semaforos.put("Portal-EstAlpha", new SemaphoroGalactico("Portal-EstAlpha", 2));

        Planificador planificador = new Planificador(colaListos, colaBloq, memoria, cpu, recursos, semaforos, 200);
        Thread hiloPlanificador = new Thread(planificador, "HiloPlanificador");
        hiloPlanificador.setDaemon(true);
        hiloPlanificador.start();

        Thread hiloEventos = new Thread(() -> {
            Random rnd = new Random();
            while (true) {
                try { Thread.sleep(800); } catch (InterruptedException e) { break; }
                if (rnd.nextDouble() < 0.3) {
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
        hiloEventos.setDaemon(true);
        hiloEventos.start();
    }

    private void crearYLanzarProceso() {
        Random rnd = new Random();
        int pid = pidCounter++;
        String nombre = "Nave-" + pid;
        int tiempoCpu = 2000 + rnd.nextInt(3000);
        int paginas = 2 + rnd.nextInt(8);

        List<String> misRecursos = new ArrayList<>();
        if (rnd.nextBoolean()) misRecursos.add("Marte");
        if (rnd.nextBoolean()) misRecursos.add("Estacion-Alpha");

        BCP nuevoBcp = new BCP(pid, nombre, tiempoCpu, paginas, misRecursos);

        // Crear visualmente (aparece en Memoria)
        UIAdapter.getInstance().crearNaveVisual(nuevoBcp);

        if (memoria.asignar(nuevoBcp)) {
            colaListos.enlistar(nuevoBcp);
        } else {
            colaBloq.enlistar(nuevoBcp);
        }
    }

    public static void main(String[] args) { launch(args); }
}