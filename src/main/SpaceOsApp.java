package main;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Point2D;
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

    // Ya no necesitamos guardar referencias a los hilos para cerrarlos manualmente
    // porque usaremos setDaemon(true)

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("GalaxyOS - Simulador SO");
        settings.setVersion("1.0");
        // El color de fondo se define mejor en initUI o initGame para esta versión
    }

    @Override
    protected void initUI() {
        // 1. Establecer el color de fondo del espacio
        FXGL.getGameScene().setBackgroundColor(Color.web("#0a0a2a"));

        // 2. Crear el área de logs (Izquierda)
        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        // Estilo "Hacker" para los logs
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: limegreen; -fx-control-inner-background: black;");
        VBox logBox = new VBox(logTextArea);
        VBox.setVgrow(logTextArea, javafx.scene.layout.Priority.ALWAYS);

        // Conectar logs
        UIAdapter.getInstance().setLogArea(logTextArea);

        // 3. Crear un panel transparente para la derecha (donde se verá el juego)
        Pane gameOverlayPane = new Pane();
        gameOverlayPane.setStyle("-fx-background-color: transparent;");
        // Permitir que los clics pasen a través del panel derecho hacia el juego
        gameOverlayPane.setPickOnBounds(false);

        // 4. Crear el SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        splitPane.getItems().addAll(logBox, gameOverlayPane);

        // Configurar el SplitPane para que sea transparente y deje ver el juego de fondo
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        // Posición inicial del divisor (33% logs, 66% juego)
        splitPane.setDividerPositions(0.33);

        // Añadir la interfaz SOBRE el juego
        FXGL.addUINode(splitPane);
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new SpaceOsFactory());

        // Calcular coordenadas para el "Radar" (la parte derecha de la pantalla)
        double gameCenterX = (FXGL.getAppWidth() * 0.66) / 2 + (FXGL.getAppWidth() * 0.33);
        double gameCenterY = FXGL.getAppHeight() / 2.0;

        UIAdapter.getInstance().posSol = new Point2D(gameCenterX, gameCenterY);
        UIAdapter.getInstance().posReady = new Point2D(gameCenterX - 250, gameCenterY);
        UIAdapter.getInstance().posBlocked = new Point2D(gameCenterX + 250, gameCenterY);

        // Spawnear entidades estáticas
        FXGL.spawn("SOL_CPU", UIAdapter.getInstance().posSol);
        FXGL.spawn("PLANETA_READY", UIAdapter.getInstance().posReady);
        FXGL.spawn("PLANETA_BLOCKED", UIAdapter.getInstance().posBlocked);

        // Iniciar la lógica en un hilo separado
        Thread hiloSimulacion = new Thread(this::iniciarSimulacionBackend, "Hilo-Simulacion");
        hiloSimulacion.setDaemon(true); // IMPORTANTE: Se cierra solo al cerrar la ventana
        hiloSimulacion.start();
    }

    private void iniciarSimulacionBackend() {
        CentroControl.registrar("=== Iniciando Simulacion Space OS (Grafica) ===");

        // Crear componentes del SO
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

        // Carga inicial
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

        // Hilo del Planificador
        Planificador planificador = new Planificador(colaListos, colaBloq, memoria, cpu, recursos, semaforos, 200);
        Thread hiloPlanificador = new Thread(planificador, "HiloPlanificador");
        hiloPlanificador.setDaemon(true); // Se cierra automático
        hiloPlanificador.start();

        // Hilo de Eventos Aleatorios
        Thread hiloEventos = new Thread(() -> {
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
        hiloEventos.setDaemon(true); // Se cierra automático
        hiloEventos.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}