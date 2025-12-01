package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
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

    // --- VARIABLES GLOBALES DEL SISTEMA OPERATIVO ---
    // Las necesitamos aquí para que el botón pueda acceder a ellas
    private Memoria memoria;
    private ColaListos colaListos;
    private ColaBloqueados colaBloq;
    private CPU cpu;
    private Map<String, Recurso> recursos;
    private Map<String, SemaphoroGalactico> semaforos;

    // Contador para generar PIDs únicos
    private int pidCounter = 1;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("GalaxyOS - Control Manual");
        settings.setVersion("2.1");
    }

    @Override
    protected void initUI() {
        FXGL.getGameScene().setBackgroundColor(Color.web("#0a0a2a"));

        // 1. Área de Logs
        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: limegreen; -fx-control-inner-background: black;");

        UIAdapter.getInstance().setLogArea(logTextArea);

        // 2. Botón "Lanzar Nave"
        Button btnLanzar = new Button("LANZAR NAVE (NUEVO PROCESO)");
        btnLanzar.setStyle("-fx-font-size: 16px; -fx-base: #4444aa; -fx-text-fill: white;");
        btnLanzar.setMaxWidth(Double.MAX_VALUE); // Que ocupe todo el ancho disponible

        // --- ACCIÓN DEL BOTÓN ---
        btnLanzar.setOnAction(e -> crearYLanzarProceso());

        // Contenedor izquierdo (Botón + Logs)
        VBox leftPane = new VBox(10, btnLanzar, logTextArea);
        leftPane.setStyle("-fx-padding: 10; -fx-background-color: #111;");
        VBox.setVgrow(logTextArea, javafx.scene.layout.Priority.ALWAYS);

        // 3. Panel derecho transparente para el juego
        Pane gameOverlayPane = new Pane();
        gameOverlayPane.setStyle("-fx-background-color: transparent;");
        gameOverlayPane.setPickOnBounds(false);

        // 4. SplitPane principal
        SplitPane splitPane = new SplitPane();
        splitPane.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        splitPane.getItems().addAll(leftPane, gameOverlayPane);
        splitPane.setStyle("-fx-background-color: transparent;");
        splitPane.setDividerPositions(0.33);

        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // Configurar posiciones del radar
        double cx = (FXGL.getAppWidth() * 0.66) / 2 + (FXGL.getAppWidth() * 0.33);
        double cy = FXGL.getAppHeight() / 2.0;

        UIAdapter.getInstance().posSol = new Point2D(cx, cy);
        UIAdapter.getInstance().posReady = new Point2D(cx - 250, cy);
        UIAdapter.getInstance().posBlocked = new Point2D(cx + 250, cy);

        FXGL.spawn("SOL_CPU", UIAdapter.getInstance().posSol);
        FXGL.spawn("PLANETA_READY", UIAdapter.getInstance().posReady);
        FXGL.spawn("PLANETA_BLOCKED", UIAdapter.getInstance().posBlocked);

        // INICIALIZAR EL KERNEL DEL SO (Sin crear naves todavía)
        inicializarKernel();
    }

    private void inicializarKernel() {
        CentroControl.registrar("=== Inicializando Kernel Space OS ===");

        // Instanciar componentes del SO
        memoria = new Memoria(50, 4); // 50 marcos totales
        colaListos = new ColaListos();
        colaBloq = new ColaBloqueados();
        cpu = new CPU();

        recursos = new HashMap<>();
        recursos.put("Marte", new Recurso("Portal-Marte", 1));
        recursos.put("Estacion-Alpha", new Recurso("Estacion-Alpha", 2));

        semaforos = new HashMap<>();
        semaforos.put("Marte", new SemaphoroGalactico("Portal-Marte", 1));
        semaforos.put("Estacion-Alpha", new SemaphoroGalactico("Portal-EstAlpha", 2));

        // Arrancar hilos del sistema (Planificador y Eventos)
        // NOTA: Ya no creamos naves aquí. Se esperan al botón.

        Planificador planificador = new Planificador(colaListos, colaBloq, memoria, cpu, recursos, semaforos, 200);
        Thread hiloPlanificador = new Thread(planificador, "HiloPlanificador");
        hiloPlanificador.setDaemon(true);
        hiloPlanificador.start();

        Thread hiloEventos = new Thread(() -> {
            Random rnd = new Random();
            while (true) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                // Liberar semáforos aleatoriamente
                if (rnd.nextDouble() < 0.4) {
                    for (SemaphoroGalactico s : semaforos.values()) s.signalSem(colaListos, colaBloq);
                }
                // Liberar recursos aleatoriamente
                if (rnd.nextDouble() < 0.2) {
                    for (Recurso r : recursos.values()) r.liberar();
                }
                // Desbloquear procesos aleatoriamente (simula fin de I/O)
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

        CentroControl.registrar("Kernel listo. Esperando comandos...");
    }

    // --- LÓGICA DEL BOTÓN ---
    private void crearYLanzarProceso() {
        // 1. Generar datos aleatorios para el proceso
        Random rnd = new Random();
        int pid = pidCounter++;
        String nombre = "Nave-" + pid;
        int tiempoCpu = 1000 + rnd.nextInt(2000); // Entre 1 y 3 segundos
        int paginas = 1 + rnd.nextInt(5); // Entre 1 y 5 páginas de memoria

        // 2. Asignar recursos necesarios aleatoriamente
        List<String> misRecursos = new ArrayList<>();
        if (rnd.nextBoolean()) misRecursos.add("Marte");
        if (rnd.nextBoolean()) misRecursos.add("Estacion-Alpha");

        // 3. Crear el BCP (Bloque de Control de Proceso)
        BCP nuevoBcp = new BCP(pid, nombre, tiempoCpu, paginas, misRecursos);

        CentroControl.registrar("BOTON: Solicitando lanzamiento de " + nombre + " (" + paginas + " pags)");

        // 4. Crear visualmente (aparece en la UI)
        UIAdapter.getInstance().crearNaveVisual(nuevoBcp);

        // 5. Intentar cargar en memoria e iniciar
        if (memoria.asignar(nuevoBcp)) {
            colaListos.enlistar(nuevoBcp);
            CentroControl.registrar("SISTEMA: " + nombre + " admitido en READY.");
        } else {
            colaBloq.enlistar(nuevoBcp);
            CentroControl.registrar("SISTEMA: " + nombre + " enviado a BLOCKED (Memoria llena).");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}