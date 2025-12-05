package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.Texture;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import obj.BCP;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGLForKtKt.texture;

public class SpaceOsFactory implements EntityFactory {

    private StackPane crearEtiqueta(String titulo, String subtitulo, Color colorTexto) {
        Text t1 = new Text(titulo);
        t1.setFont(Font.font("Consolas", 14));
        t1.setFill(colorTexto);
        t1.setStyle("-fx-font-weight: bold");

        Text t2 = new Text(subtitulo);
        t2.setFont(Font.font("Consolas", 10));
        t2.setFill(colorTexto.deriveColor(0, 1, 1, 0.7));

        javafx.scene.layout.VBox vb = new javafx.scene.layout.VBox(2, t1, t2);
        vb.setAlignment(javafx.geometry.Pos.CENTER);

        Rectangle bg = new Rectangle(120, 45, Color.rgb(0, 0, 0, 0.5)); // Fondo semitransparente
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        bg.setStroke(colorTexto.deriveColor(0, 1, 1, 0.3));

        return new StackPane(bg, vb);
    }

    @Spawns("SOL_CPU")
    public Entity newSolCpu(SpawnData data) {
        // Sol un poco más grande
        Texture texture = FXGL.texture("sol.png", 160, 160);
        texture.setEffect(new Glow(0.8));

        StackPane etiqueta = crearEtiqueta("CPU CORE", "Procesando", Color.GOLD);
        etiqueta.setTranslateY(95); // Bajamos la etiqueta para que no tape el sol

        return entityBuilder(data)
                .type(EntityType.SOL_CPU)
                .viewWithBBox(texture)
                .view(etiqueta)
                .zIndex(10)
                .build();
    }

    @Spawns("PLANETA_READY")
    public Entity newPlanetaReady(SpawnData data) {
        // Planeta azul
        Texture texture = FXGL.texture("planetaReady.png", 210, 110);

        StackPane etiqueta = crearEtiqueta("READY QUEUE", "En Espera", Color.CYAN);
        etiqueta.setTranslateY(70);

        return entityBuilder(data)
                .type(EntityType.PLANETA_READY)
                .viewWithBBox(texture)
                .view(etiqueta)
                .build();
    }

    @Spawns("PLANETA_BLOCKED")
    public Entity newPlanetaBlocked(SpawnData data) {
        // Agujero negro
        Texture texture = FXGL.texture("agujero_negro.png", 140, 140);
        //FXGL.animationBuilder().duration(Duration.seconds(30)).repeatInfinitely().rotate(texture).from(0).to(360).buildAndPlay();

        StackPane etiqueta = crearEtiqueta("BLOCKED", "I/O Wait", Color.RED);
        etiqueta.setTranslateY(80);

        return entityBuilder(data)
                .type(EntityType.PLANETA_BLOCKED)
                .viewWithBBox(texture)
                .view(etiqueta)
                .build();
    }

    // --- RECURSO (MODIFICADO PARA TENER DOS PLANETAS DISTINTOS) ---
    @Spawns("RECURSO")
    public Entity newRecurso(SpawnData data) {
        String nombre = data.get("nombre");
        int total = data.get("capacidad");

        // Selección dinámica de la imagen del planeta
        String imagenFile;

        // Si el recurso es Marte, usamos el planeta rojo (planeta2)
        // Si es Estacion-Alpha, usamos el planeta azul (planeta1) para variarlo
        if (nombre.contains("Marte")) {
            imagenFile = "planeta2.png";
        } else {
            imagenFile = "planeta1.png";
        }

        var view = texture(imagenFile, 165, 65);

        Text text = new Text(nombre + "\n[0/" + total + "]");
        text.setFill(Color.LIGHTGREEN);
        text.setFont(Font.font("Consolas", 10));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-10); // Ajuste para centrar texto
        text.setTranslateY(80);  // Texto debajo del planeta

        UIAdapter.getInstance().textosRecursos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.RECURSO)
                .view(view)
                .view(text)
                .bbox(new HitBox(BoundingShape.circle(32)))
                .build();
    }

    @Spawns("SEMAFORO")
    public Entity newSemaforo(SpawnData data) {
        String nombre = data.get("nombre");
        int valor = data.get("valor");

        // Estación un poco más ancha para evitar deformación
        Texture texture = FXGL.texture("estacion_espacial.png", 100, 80);
        FXGL.animationBuilder().duration(Duration.seconds(5)).repeatInfinitely().autoReverse(true).translate(texture).from(new Point2D(0,0)).to(new Point2D(0, -5)).buildAndPlay();

        Text textVal = new Text("VAL: " + valor);
        textVal.setFont(Font.font("Consolas", 11));
        textVal.setFill(Color.CYAN);
        UIAdapter.getInstance().textosSemaforos.put(nombre, textVal);

        StackPane etiqueta = crearEtiqueta(nombre, "Semaforo", Color.LIGHTBLUE);
        etiqueta.setTranslateY(50);
        ((javafx.scene.layout.VBox) etiqueta.getChildren().get(1)).getChildren().add(textVal);

        return entityBuilder(data)
                .type(EntityType.SEMAFORO)
                .viewWithBBox(texture)
                .view(etiqueta)
                .build();
    }

    @Spawns("NAVE_PROCESO")
    public Entity newNaveProceso(SpawnData data) {
        BCP bcp = data.get("pcb");

        // --- NAVE LIMPIA SIN TEXTO ROTATORIO ---
        Texture texture = FXGL.texture("nave_espacial.png", 45, 45);
        texture.setEffect(new DropShadow(15, Color.CYAN));

        // NOTA: Hemos quitado el PID text de aquí para que no gire feo.
        // La información del proceso se ve en los logs.

        return entityBuilder(data)
                .type(EntityType.NAVE_PROCESO)
                .viewWithBBox(texture)
                .with("pcb", bcp)
                .zIndex(100)
                .build();
    }

    // --- MATRIZ DE MEMORIA (Se mantiene igual, es código puro) ---
    @Spawns("NEBULOSA_MEMORIA")
    public Entity newNebulosa(SpawnData data) {
        // Grupo visual para contener los hexágonos
        javafx.scene.Group matriz = new javafx.scene.Group();

        // Color base: Violeta digital
        Color colorBase = Color.rgb(180, 80, 255, 0.4);
        Color colorBorde = Color.rgb(220, 150, 255, 0.8);

        // Crear un patrón de hexágonos (o cuadros)
        // Dibujamos 7 celdas simulando marcos de memoria
        Point2D[] offsets = {
                new Point2D(0, 0),      // Centro
                new Point2D(30, -17),   // Arriba Der
                new Point2D(30, 17),    // Abajo Der
                new Point2D(0, 34),     // Abajo
                new Point2D(-30, 17),   // Abajo Izq
                new Point2D(-30, -17),  // Arriba Izq
                new Point2D(0, -34)     // Arriba
        };

        for (Point2D p : offsets) {
            // Dibujar Hexágono usando Polygon
            // Puntos para un hexágono de radio ~20
            javafx.scene.shape.Polygon hex = new javafx.scene.shape.Polygon(
                    -10.0, -17.0,
                    10.0, -17.0,
                    20.0, 0.0,
                    10.0, 17.0,
                    -10.0, 17.0,
                    -20.0, 0.0
            );

            hex.setFill(colorBase);
            hex.setStroke(colorBorde);
            hex.setStrokeWidth(2);
            hex.setTranslateX(p.getX());
            hex.setTranslateY(p.getY());

            // Efecto de neón individual
            hex.setEffect(new javafx.scene.effect.DropShadow(10, colorBase));

            matriz.getChildren().add(hex);
        }

        // Texto descriptivo
        Text text = new Text("RAM\nMATRIX");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-15);
        text.setTranslateY(5);

        // Añadir animación de "respiración" a toda la matriz
        Entity entidad = entityBuilder(data)
                .type(EntityType.MEMORIA)
                .view(matriz)
                .view(text)
                .zIndex(-5)
                .build();

        // Animación suave de rotación y escala
        FXGL.animationBuilder()
                .duration(Duration.seconds(4))
                .repeatInfinitely()
                .autoReverse(true)
                .scale(entidad)
                .from(new Point2D(1, 1))
                .to(new Point2D(1.1, 1.1))
                .buildAndPlay();

        // Animación de rotación lenta constante
        FXGL.animationBuilder()
                .duration(Duration.seconds(20))
                .repeatInfinitely()
                .rotate(entidad)
                .from(0)
                .to(360)
                .buildAndPlay();

        // Registrar referencia para textos
        UIAdapter.getInstance().textoMemoria = text;

        return entidad;
    }
}