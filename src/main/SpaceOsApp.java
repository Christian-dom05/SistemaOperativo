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
        settings.setTitle("GalaxyOS - Ultimate UI");
        settings.setVersion("5.0");
    }

    @Override
    protected void initUI() {
        FXGL.getGameScene().setBackgroundColor(Color.web("#020205")); // Negro profundo

        // Estilo Matrix/Cyberpunk para los logs
        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle(
                "-fx-control-inner-background: #000000; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-highlight-fill: #00ff00; " +
                        "-fx-highlight-text-fill: #000000; " +
                        "-fx-text-fill: #00ff00; " +
                        "-fx-border-color: #004400;"
        );
        UIAdapter.getInstance().setLogArea(logTextArea);

        Button btnLanzar = new Button(">>> LANZAR MISION <<<");
        btnLanzar.setStyle(
                "-fx-background-color: #0044aa; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #0088ff; " +
                        "-fx-cursor: hand;"
        );
        btnLanzar.setMaxWidth(Double.MAX_VALUE);
        btnLanzar.setOnAction(e -> crearYLanzarProceso());

        VBox leftPane = new VBox(15, btnLanzar, logTextArea);
        leftPane.setStyle("-fx-padding: 15; -fx-background-color: rgba(0, 0, 0, 0.8);");
        VBox.setVgrow(logTextArea, javafx.scene.layout.Priority.ALWAYS);

        Pane gameOverlayPane = new Pane();
        gameOverlayPane.setStyle("-fx-background-color: transparent;");
        gameOverlayPane.setPickOnBounds(false);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, gameOverlayPane);
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        splitPane.setDividerPositions(0.25); // Menos espacio para logs, más para gráficos

        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // --- FONDO ESTELAR ---
        for (int i = 0; i < 200; i++) {
            FXGL.spawn("ESTRELLA", Math.random() * FXGL.getAppWidth(), Math.random() * FXGL.getAppHeight());
        }

        // --- CALCULO DE POSICIONES SIMÉTRICAS ---
        // Centro del área de juego (aprox, descontando el panel izquierdo)
        double gameWidth = FXGL.getAppWidth() * 0.75;
        double offsetX = FXGL.getAppWidth() * 0.25;

        double cx = offsetX + (gameWidth / 2);
        double cy = FXGL.getAppHeight() / 2.0;

        // 1. SOL (Centro)
        Entity sol = FXGL.spawn("SOL_CPU", cx, cy - 50); // Un poco arriba del centro
        UIAdapter.getInstance().registrarUbicacion("CPU", sol.getPosition());

        // 2. PLANETAS (Lados)
        Entity ready = FXGL.spawn("PLANETA_READY", cx - 250, cy - 50);
        UIAdapter.getInstance().registrarUbicacion("READY", ready.getPosition());

        Entity blocked = FXGL.spawn("PLANETA_BLOCKED", cx + 250, cy - 50);
        UIAdapter.getInstance().registrarUbicacion("BLOCKED", blocked.getPosition());

        // 3. MEMORIA (Arriba Izquierda, como "origen")
        Entity nebula = FXGL.spawn("NEBULOSA_MEMORIA", cx - 350, cy - 250);
        UIAdapter.getInstance().registrarUbicacion("MEMORIA", nebula.getPosition());

        // 4. CINTURÓN DE RECURSOS (Abajo en arco)
        // Grupo Marte (Izquierda abajo)
        spawnYRegistrar("Marte", "RECURSO", cx - 150, cy + 150, 1);
        spawnYRegistrar("Portal-Marte", "SEMAFORO", cx - 150, cy + 230, 1);

        // Grupo Estación Alpha (Derecha abajo)
        spawnYRegistrar("Estacion-Alpha", "RECURSO", cx + 150, cy + 150, 2);
        spawnYRegistrar("Portal-EstAlpha", "SEMAFORO", cx + 150, cy + 230, 2);

        inicializarKernel();
    }

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
                if (rnd.nextDouble() < 0.3) for (SemaphoroGalactico s : semaforos.values()) s.signalSem(colaListos, colaBloq);
                if (rnd.nextDouble() < 0.2) for (Recurso r : recursos.values()) r.liberar();
                synchronized (colaBloq) {
                    List<BCP> b = colaBloq.snapshot();
                    for (BCP p : b) if (Math.random() < 0.3) { colaBloq.remover(p); colaListos.enlistar(p); }
                }
            }
        }, "HiloEventos");
        hiloEventos.setDaemon(true);
        hiloEventos.start();
    }

    private void crearYLanzarProceso() {
        Random rnd = new Random();
        int pid = pidCounter++;
        String nombre = "Mision-" + pid;
        int tiempoCpu = 2000 + rnd.nextInt(3000);
        int paginas = 2 + rnd.nextInt(8);

        List<String> misRecursos = new ArrayList<>();
        if (rnd.nextBoolean()) misRecursos.add("Marte");
        if (rnd.nextBoolean()) misRecursos.add("Estacion-Alpha");

        BCP nuevoBcp = new BCP(pid, nombre, tiempoCpu, paginas, misRecursos);

        UIAdapter.getInstance().crearNaveVisual(nuevoBcp);

        if (memoria.asignar(nuevoBcp)) {
            colaListos.enlistar(nuevoBcp);
        } else {
            colaBloq.enlistar(nuevoBcp);
        }
    }

    public static void main(String[] args) { launch(args); }
}