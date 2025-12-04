package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape; // Importante: Importar esto
import com.almasb.fxgl.physics.HitBox;       // Importante: Importar esto
import javafx.geometry.Point2D;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import obj.BCP;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

public class SpaceOsFactory implements EntityFactory {

    // --- SOL / CPU ---
    @Spawns("SOL_CPU")
    public Entity newSolCpu(SpawnData data) {
        RadialGradient gradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.2, Color.YELLOW),
                new Stop(1, Color.ORANGE)
        );

        Circle cuerpo = new Circle(70);
        cuerpo.setFill(gradient);
        cuerpo.setEffect(new Bloom(0.8));
        cuerpo.setStroke(Color.ORANGERED);
        cuerpo.setStrokeWidth(2);

        Text text = new Text("CPU\n(Core)");
        text.setFill(Color.BLACK);
        text.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-20);
        text.setTranslateY(5);

        return entityBuilder(data)
                .type(EntityType.SOL_CPU)
                .view(cuerpo) // Usamos view() en lugar de viewWithBBox() para definir la caja manualmente
                .view(text)
                // CORRECCIÓN: Usar BoundingShape.circle para colisión circular
                .bbox(new HitBox(BoundingShape.circle(70)))
                .build();
    }

    @Spawns("PLANETA_READY")
    public Entity newPlanetaReady(SpawnData data) {
        RadialGradient gradient = new RadialGradient(
                0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.CYAN),
                new Stop(1, Color.DARKBLUE)
        );

        Circle planeta = new Circle(45);
        planeta.setFill(gradient);
        planeta.setEffect(new DropShadow(20, Color.DODGERBLUE));

        Text text = new Text("READY");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        text.setTranslateX(-18);

        return entityBuilder(data)
                .type(EntityType.PLANETA_READY)
                .view(planeta)
                .view(text)
                // CORRECCIÓN: Colisión circular de radio 45
                .bbox(new HitBox(BoundingShape.circle(45)))
                .build();
    }

    // --- PLANETA BLOCKED ---
    @Spawns("PLANETA_BLOCKED")
    public Entity newPlanetaBlocked(SpawnData data) {
        RadialGradient gradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.DARKRED),
                new Stop(1, Color.BLACK)
        );

        Circle planeta = new Circle(45);
        planeta.setFill(gradient);
        planeta.setStroke(Color.RED);
        planeta.setStrokeWidth(1);
        planeta.setEffect(new DropShadow(15, Color.RED));

        Text text = new Text("BLOCKED");
        text.setFill(Color.RED);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        text.setTranslateX(-24);

        return entityBuilder(data)
                .type(EntityType.PLANETA_BLOCKED)
                .view(planeta)
                .view(text)
                // CORRECCIÓN: Colisión circular de radio 45
                .bbox(new HitBox(BoundingShape.circle(45)))
                .build();
    }

    // --- RECURSO ---
    @Spawns("RECURSO")
    public Entity newRecurso(SpawnData data) {
        String nombre = data.get("nombre");
        int total = data.get("capacidad");

        Circle shape = new Circle(30, Color.rgb(0, 255, 100, 0.3));
        shape.setStroke(Color.LIME);
        shape.setStrokeWidth(2);
        shape.setEffect(new Glow(0.5));

        Text text = new Text(nombre + "\n[0/" + total + "]");
        text.setFill(Color.LIGHTGREEN);
        text.setFont(Font.font("Consolas", 10));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-20);
        text.setTranslateY(5);

        UIAdapter.getInstance().textosRecursos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.RECURSO)
                .view(shape)
                .view(text)
                // Colisión circular
                .bbox(new HitBox(BoundingShape.circle(30)))
                .build();
    }

    // --- SEMAFORO ---
    @Spawns("SEMAFORO")
    public Entity newSemaforo(SpawnData data) {
        String nombre = data.get("nombre");
        int valorInicial = data.get("valor");

        Circle anillo = new Circle(35);
        anillo.setFill(Color.TRANSPARENT);
        anillo.setStroke(Color.CYAN);
        anillo.setStrokeWidth(4);
        anillo.setEffect(new Bloom(0.7));

        Circle fondo = new Circle(30, Color.rgb(0, 0, 50, 0.7));

        Text text = new Text("PORTAL\n" + valorInicial);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        text.setFill(Color.CYAN);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-20);
        text.setTranslateY(5);

        UIAdapter.getInstance().textosSemaforos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.SEMAFORO)
                .view(anillo)
                .view(fondo)
                .view(text)
                // Colisión circular
                .bbox(new HitBox(BoundingShape.circle(35)))
                .build();
    }

    // --- MEMORIA (Fondo) ---
    @Spawns("NEBULOSA_MEMORIA")
    public Entity newNebulosa(SpawnData data) {
        Circle core = new Circle(60, Color.rgb(100, 0, 200, 0.2));
        core.setEffect(new GaussianBlur(30));

        Circle outer = new Circle(90, Color.rgb(50, 0, 150, 0.1));
        outer.setEffect(new GaussianBlur(50));

        Text text = new Text("MEMORIA RAM\nSistema");
        text.setFill(Color.LAVENDER);
        text.setFont(Font.font("Verdana", 11));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-40);

        UIAdapter.getInstance().textoMemoria = text;

        return entityBuilder(data)
                .type(EntityType.MEMORIA)
                .view(outer)
                .view(core)
                .view(text)
                .zIndex(-5)
                .build();
    }

    // --- NAVE PROCESO ---
    @Spawns("NAVE_PROCESO")
    public Entity newNaveProceso(SpawnData data) {
        BCP bcp = data.get("pcb");

        Rectangle naveShape = new Rectangle(36, 36, Color.SILVER);
        naveShape.setArcWidth(10);
        naveShape.setArcHeight(10);
        naveShape.setStroke(Color.WHITE);
        naveShape.setEffect(new DropShadow(5, Color.CYAN));

        Text pidText = new Text("P" + bcp.pid);
        pidText.setFill(Color.BLACK);
        pidText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        pidText.setTranslateX(8);
        pidText.setTranslateY(22);

        return entityBuilder(data)
                .type(EntityType.NAVE_PROCESO)
                // Aquí usamos viewWithBBox porque la nave es rectangular,
                // así que la caja automática funciona bien.
                .viewWithBBox(naveShape)
                .view(pidText)
                .with("pcb", bcp)
                .build();
    }

    @Spawns("ESTRELLA")
    public Entity newEstrella(SpawnData data) {
        Circle shape = new Circle(1.0, Color.WHITE);
        shape.setEffect(new Bloom(0.5));

        Entity e = entityBuilder(data)
                .view(shape)
                .zIndex(-100)
                .build();

        FXGL.animationBuilder()
                .duration(Duration.seconds(1 + Math.random() * 3))
                .repeatInfinitely()
                .autoReverse(true)
                .fadeIn(e)
                .buildAndPlay();

        return e;
    }
}