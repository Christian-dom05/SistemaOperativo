package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
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

    // Color técnico para la UI (cian desaturado)
    private final String TECH_COLOR = "#80C0D0";
    private final String BG_PANEL_COLOR = "rgba(0, 20, 30, 0.85)";

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("SIMULACIÓN DE ORBITADOR DE PROCESOS - v6.0");
        settings.setVersion("6.0.1 Realism Patch");
        // Opcional: si tienes un icono para la ventana
        // settings.setAppIcon("icon.png");
    }

    @Override
    protected void initUI() {
        // 1. Establecer FONDO REALISTA.
        // FXGL buscará automáticamente en assets/textures/
        FXGL.getGameScene().setBackgroundRepeat("fondo_espacio_realista.jpg");

        // 2. Configurar el panel de logs para que parezca un monitor de datos
        ListView<String> listViewLogs = new ListView<>();

        // Estilo del contenedor del ListView: Oscuro, bordes finos, fuente técnica
        listViewLogs.setStyle(
                "-fx-background-color: " + BG_PANEL_COLOR + "; " +
                        "-fx-background-radius: 2; " +
                        "-fx-border-color: " + TECH_COLOR + "; " +
                        "-fx-border-width: 0.5px; " +
                        "-fx-border-radius: 2;"
        );

        // Fábrica de celdas: Monocromática y técnica.
        // En una simulación realista, el texto no cambia de color como un árbol de navidad.
        // Se usa un solo color para datos estándar, quizás otro para errores críticos.
        listViewLogs.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("> " + item); // Añadir un prompt ">" para estilo terminal
                    setFont(Font.font("Consolas", 11));

                    // Color base monocromático para todo
                    String estiloBase = "-fx-background-color: transparent; -fx-text-fill: " + TECH_COLOR + ";";

                    // Opcional: Solo cambiar el color si es algo CRÍTICO (ej. bloqueo)
                    // para que parezca una alerta del sistema, pero mantenerlo sutil.
                    if (item.contains("BLOCKED") || item.contains("ColaBloqueados")) {
                        setStyle(estiloBase + " -fx-text-fill: #FF6060;"); // Rojo desaturado para alertas
                    } else {
                        setStyle(estiloBase);
                    }
                }
            }
        });

        UIAdapter.getInstance().setListView(listViewLogs);

        // Estilo del botón: Más industrial/militar
        Button btnLanzar = new Button("[ SECUENCIA DE LANZAMIENTO ]");
        btnLanzar.setStyle(
                "-fx-background-color: rgba(50, 80, 100, 0.5); " +
                        "-fx-text-fill: " + TECH_COLOR + "; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 12px; " +
                        "-fx-border-color: " + TECH_COLOR + "; " +
                        "-fx-border-width: 1px; " +
                        "-fx-cursor: hand;"
        );
        btnLanzar.setMaxWidth(Double.MAX_VALUE);
        btnLanzar.setOnAction(e -> crearYLanzarProceso());

        // Contenedor del panel izquierdo
        VBox leftPane = new VBox(10, btnLanzar, listViewLogs);
        // Fondo oscuro semitransparente para el panel lateral completo
        leftPane.setStyle("-fx-padding: 15; -fx-background-color: rgba(0, 10, 20, 0.6);");
        VBox.setVgrow(listViewLogs, Priority.ALWAYS);

        Pane gameOverlayPane = new Pane();
        gameOverlayPane.setStyle("-fx-background-color: transparent;");
        gameOverlayPane.setPickOnBounds(false);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, gameOverlayPane);
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        splitPane.setDividerPositions(0.28);

        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // ELIMINADO: El bucle for que creaba "ESTRELLA"s.
        // El fondo realista se encarga de esto ahora.

        // Posiciones (ligeramente ajustadas para dar más espacio)
        double gameWidth = FXGL.getAppWidth() * 0.72;
        double offsetX = FXGL.getAppWidth() * 0.28;
        double cx = offsetX + (gameWidth / 2);
        double cy = FXGL.getAppHeight() / 2.0;

        Entity sol = FXGL.spawn("SOL_CPU", cx, cy - 80);
        UIAdapter.getInstance().registrarUbicacion("CPU", sol.getPosition());

        Entity ready = FXGL.spawn("PLANETA_READY", cx - 280, cy - 80);
        UIAdapter.getInstance().registrarUbicacion("READY", ready.getPosition());

        Entity blocked = FXGL.spawn("PLANETA_BLOCKED", cx + 280, cy - 80);
        UIAdapter.getInstance().registrarUbicacion("BLOCKED", blocked.getPosition());

        // La nebulosa ahora es más grande y está más al fondo
        Entity nebula = FXGL.spawn("NEBULOSA_MEMORIA", cx - 400, cy - 300);
        UIAdapter.getInstance().registrarUbicacion("MEMORIA", nebula.getPosition().add(100, 100)); // Ajuste del punto de spawn

        // Recursos y Portales (Semáforos)
        spawnYRegistrar("Recurso-Alfa", "RECURSO", cx - 180, cy + 180, 1);
        spawnYRegistrar("Portal-Alfa", "SEMAFORO", cx - 180, cy + 250, 1);

        spawnYRegistrar("Recurso-Beta", "RECURSO", cx + 180, cy + 180, 2);
        spawnYRegistrar("Portal-Beta", "SEMAFORO", cx + 180, cy + 250, 2);

        inicializarKernel();
    }

    private void spawnYRegistrar(String nombre, String tipo, double x, double y, int val) {
        Entity e = FXGL.spawn(tipo, new SpawnData(x, y).put("nombre", nombre).put("capacidad", val).put("valor", val));
        UIAdapter.getInstance().registrarUbicacion(nombre, e.getPosition());
    }

    // --- Métodos del Kernel (Sin cambios lógicos, solo nombres de recursos) ---
    private void inicializarKernel() {
        memoria = new Memoria(50, 4);
        colaListos = new ColaListos();
        colaBloq = new ColaBloqueados();
        cpu = new CPU();

        // Cambié nombres como "Marte" a algo más genérico para que no choque si usas una imagen de asteroide
        recursos = new HashMap<>();
        recursos.put("Recurso-Alfa", new Recurso("Recurso-Alfa", 1));
        recursos.put("Recurso-Beta", new Recurso("Recurso-Beta", 2));

        semaforos = new HashMap<>();
        semaforos.put("Portal-Alfa", new SemaphoroGalactico("Portal-Alfa", 1));
        semaforos.put("Portal-Beta", new SemaphoroGalactico("Portal-Beta", 2));

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
        if (rnd.nextBoolean()) misRecursos.add("Recurso-Alfa");
        if (rnd.nextBoolean()) misRecursos.add("Recurso-Beta");

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