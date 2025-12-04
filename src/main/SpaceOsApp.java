package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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
        settings.setTitle("GalaxyOS - Command Center");
        settings.setVersion("6.0");
    }

    @Override
    protected void initUI() {
        FXGL.getGameScene().setBackgroundRepeat("fondo_original.png");

        ListView<String> listViewLogs = new ListView<>();

        listViewLogs.setStyle("-fx-background-color: rgba(0, 20, 0, 0.7); -fx-background-radius: 10; -fx-border-color: #004400; -fx-border-radius: 10;");

        listViewLogs.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setFont(Font.font("Consolas", 12));


                    String estiloBase = "-fx-background-color: transparent; -fx-font-weight: bold; ";

                    if (item.contains("CPU") || item.contains("SOL")) {
                        setStyle(estiloBase + "-fx-text-fill: #ffd700;"); // Dorado (CPU)
                    } else if (item.contains("READY") || item.contains("ColaListos")) {
                        setStyle(estiloBase + "-fx-text-fill: #00bfff;"); // Azul (Ready)
                    } else if (item.contains("BLOCKED") || item.contains("ColaBloqueados") || item.contains("bloqueado")) {
                        setStyle(estiloBase + "-fx-text-fill: #ff4500;"); // Rojo (Blocked/Error)
                    } else if (item.contains("Semaforo") || item.contains("Portal")) {
                        setStyle(estiloBase + "-fx-text-fill: #d8bfd8;"); // Violeta (Semáforos)
                    } else if (item.contains("recolectó") || item.contains("Recurso")) {
                        setStyle(estiloBase + "-fx-text-fill: #32cd32;"); // Verde (Recursos)
                    } else {
                        setStyle(estiloBase + "-fx-text-fill: #00ff00;"); // Verde terminal por defecto
                    }
                }
            }
        });

        // Conectar al adaptador
        UIAdapter.getInstance().setListView(listViewLogs);

        Button btnLanzar = new Button("[ INICIAR NUEVA MISION ]");
        btnLanzar.setStyle(
                "-fx-background-color: rgba(0, 100, 255, 0.2); " +
                        "-fx-text-fill: cyan; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: cyan; " +
                        "-fx-border-width: 1px; " +
                        "-fx-cursor: hand;"
        );
        btnLanzar.setMaxWidth(Double.MAX_VALUE);
        btnLanzar.setOnAction(e -> crearYLanzarProceso());

        // Contenedor del panel izquierdo (Transparente)
        VBox leftPane = new VBox(10, btnLanzar, listViewLogs);
        leftPane.setStyle("-fx-padding: 15; -fx-background-color: rgba(0, 0, 0, 0.3);");
        VBox.setVgrow(listViewLogs, Priority.ALWAYS);

        Pane gameOverlayPane = new Pane();
        gameOverlayPane.setStyle("-fx-background-color: transparent;");
        gameOverlayPane.setPickOnBounds(false);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, gameOverlayPane);
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        splitPane.setDividerPositions(0.30); // 30% Logs, 70% Juego

        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // Posiciones simétricas
        double gameWidth = FXGL.getAppWidth() * 0.70;
        double offsetX = FXGL.getAppWidth() * 0.30;
        double cx = offsetX + (gameWidth / 2);
        double cy = FXGL.getAppHeight() / 2.0;

        Entity sol = FXGL.spawn("SOL_CPU", cx, cy - 50);
        UIAdapter.getInstance().registrarUbicacion("CPU", sol.getPosition());

        Entity ready = FXGL.spawn("PLANETA_READY", cx - 250, cy - 50);
        UIAdapter.getInstance().registrarUbicacion("READY", ready.getPosition());

        Entity blocked = FXGL.spawn("PLANETA_BLOCKED", cx + 250, cy - 50);
        UIAdapter.getInstance().registrarUbicacion("BLOCKED", blocked.getPosition());

        Entity nebula = FXGL.spawn("NEBULOSA_MEMORIA", cx - 350, cy - 250);
        UIAdapter.getInstance().registrarUbicacion("MEMORIA", nebula.getPosition());

        spawnYRegistrar("Marte", "RECURSO", cx - 150, cy + 180, 1);
        spawnYRegistrar("Portal-Marte", "SEMAFORO", cx - 150, cy + 260, 1);

        spawnYRegistrar("Estacion-Alpha", "RECURSO", cx + 150, cy + 180, 2);
        spawnYRegistrar("Portal-EstAlpha", "SEMAFORO", cx + 150, cy + 260, 2);

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