package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.LoadingScene;
import com.almasb.fxgl.app.scene.SceneFactory;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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

    // --- PANTALLA DE CARGA MEJORADA ---
    public static class NasaLoadingScene extends LoadingScene {
        public NasaLoadingScene() {
            getContentRoot().getChildren().add(new Rectangle(getAppWidth(), getAppHeight(), Color.BLACK));
            Text t = new Text("INICIALIZANDO SISTEMA DE SIMULACIÓN...");
            t.setFont(Font.font("Consolas", 28));
            t.setFill(Color.LIME);
            t.setTranslateX(100);
            t.setTranslateY(getAppHeight() / 2.0);
            getContentRoot().getChildren().add(t);
        }
    }
    public static class NasaSceneFactory extends SceneFactory {
        @Override
        public LoadingScene newLoadingScene() { return new NasaLoadingScene(); }
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Simulación SO - NASA Mode");
        settings.setVersion("Sim v9.0");
        settings.setSceneFactory(new NasaSceneFactory());
        settings.setMainMenuEnabled(false);
    }

    @Override
    protected void initUI() {
        FXGL.getGameScene().setBackgroundRepeat("fondo_original.png");

        // --- CSS PARA HACER LA BARRA DEL SPLITPANE VISIBLE Y GRUESA ---
        String cssSplitPane =
                ".split-pane > .split-pane-divider { " +
                        "   -fx-background-color: #00ff00; " + // Verde brillante
                        "   -fx-border-color: #004400; " +
                        "   -fx-pref-width: 10px; " + // Barra ancha para agarrar facil
                        "} " +
                        ".split-pane > .split-pane-divider:hover { " +
                        "   -fx-background-color: #00ff00; " +
                        "   -fx-cursor: h-resize; " +
                        "}";

        ListView<String> listViewLogs = new ListView<>();
        listViewLogs.setStyle("-fx-background-color: rgba(0, 10, 0, 0.9); -fx-border-color: #00ff00;");
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
                    setFont(Font.font("Consolas", 13));
                    // Colores por evento
                    String base = "-fx-background-color: transparent; ";
                    if (item.contains("CPU")) setStyle(base + "-fx-text-fill: yellow;");
                    else if (item.contains("READY")) setStyle(base + "-fx-text-fill: cyan;");
                    else if (item.contains("BLOCKED")) setStyle(base + "-fx-text-fill: #ff5555;");
                    else setStyle(base + "-fx-text-fill: #00ff00;");
                }
            }
        });

        UIAdapter.getInstance().setListView(listViewLogs);

        Button btnLanzar = new Button(">> LANZAR PROCESO <<");
        btnLanzar.setStyle("-fx-background-color: #003300; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnLanzar.setMaxWidth(Double.MAX_VALUE);
        btnLanzar.setOnAction(e -> crearYLanzarProceso());

        VBox leftPane = new VBox(10, btnLanzar, listViewLogs);
        leftPane.setStyle("-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.6);");
        VBox.setVgrow(listViewLogs, Priority.ALWAYS);

        Pane gamePane = new Pane();
        gamePane.setStyle("-fx-background-color: transparent;");
        gamePane.setPickOnBounds(false);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, gamePane);

        // Aplicar estilos inline (o podrías cargarlos de un archivo .css)
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        // Inyectamos el estilo del divider directamente en la escena cuando esté lista,
        // o añadimos una hoja de estilo global. Como solución rápida:
        if (FXGL.getGameScene().getRoot().getStylesheets().isEmpty()) {
            // Crear un archivo temporal o usar setStyle en nodos no funciona para selectores hijos complejos.
            // Lo mejor es añadir el archivo "estilos.css" que te di antes.
            // Asumimos que existe src/ui/estilos.css con el código que te pasé.
            try {
                FXGL.getGameScene().getRoot().getStylesheets().add(getClass().getResource("/ui/estilos.css").toExternalForm());
            } catch(Exception e) { System.out.println("Nota: Crea src/ui/estilos.css para ver la barra verde divisoria."); }
        }

        // Posición inicial del divisor (más espacio para logs)
        splitPane.setDividerPositions(0.25);

        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // --- DISTRIBUCIÓN ESPACIAL MEJORADA (GRID EXPANDIDO) ---

        // Calculamos el centro de la zona visible del juego (derecha del splitpane)
        double anchoJuego = FXGL.getAppWidth() * 0.75;
        double inicioJuegoX = FXGL.getAppWidth() * 0.25;

        double cx = inicioJuegoX + (anchoJuego / 2);
        double cy = FXGL.getAppHeight() / 2.0;

        // 1. SOL (Centro, ligeramente arriba)
        Entity sol = FXGL.spawn("SOL_CPU", cx, cy - 80);
        UIAdapter.getInstance().registrarUbicacion("CPU", sol.getPosition());

        // 2. PLANETAS (Más separados horizontalmente)
        // Ready a la izquierda
        Entity ready = FXGL.spawn("PLANETA_READY", cx - 350, cy - 80);
        UIAdapter.getInstance().registrarUbicacion("READY", ready.getPosition());

        // Blocked a la derecha
        Entity blocked = FXGL.spawn("PLANETA_BLOCKED", cx + 350, cy - 80);
        UIAdapter.getInstance().registrarUbicacion("BLOCKED", blocked.getPosition());

        // 3. MEMORIA (Arriba a la izquierda, bien separada)
        Entity memoriaEnt = FXGL.spawn("NEBULOSA_MEMORIA", cx - 450, cy - 280);
        UIAdapter.getInstance().registrarUbicacion("MEMORIA", memoriaEnt.getPosition());

        // 4. RECURSOS Y SEMAFOROS (Abajo, distribuidos en el ancho)
        double yRecursos = cy + 180;
        double yPortales = cy + 280;

        // Grupo Marte (Izquierda)
        spawnYRegistrar("Marte", "RECURSO", cx - 200, yRecursos, 1);
        spawnYRegistrar("Portal-Marte", "SEMAFORO", cx - 200, yPortales, 1);

        // Grupo Estación (Derecha)
        spawnYRegistrar("Estacion-Alpha", "RECURSO", cx + 200, yRecursos, 2);
        spawnYRegistrar("Portal-EstAlpha", "SEMAFORO", cx + 200, yPortales, 2);

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
        if (memoria.asignar(nuevoBcp)) { colaListos.enlistar(nuevoBcp); }
        else { colaBloq.enlistar(nuevoBcp); }
    }

    public static void main(String[] args) { launch(args); }
}