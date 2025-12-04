package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import obj.BCP;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.texture; // Importante para cargar las imágenes

public class SpaceOsFactory implements EntityFactory {

    // --- SOL / CPU ---
    @Spawns("SOL_CPU")
    public Entity newSolCpu(SpawnData data) {
        // Usamos sol.png, tamaño ajustado a 150x150
        var view = texture("sol.png", 150, 150);

        Text text = new Text("CPU\n(Core)");
        text.setFill(Color.WHITE); // Ajustado a blanco para contraste
        text.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(40); // Centrar texto a ojo sobre la imagen
        text.setTranslateY(80);

        return entityBuilder(data)
                .type(EntityType.SOL_CPU)
                .view(view)
                .view(text)
                // Colisión circular ajustada al tamaño de la imagen
                .bbox(new HitBox(BoundingShape.circle(75)))
                .build();
    }

    // --- PLANETA READY ---
    @Spawns("PLANETA_READY")
    public Entity newPlanetaReady(SpawnData data) {
        // Usamos planeta1.png
        var view = texture("planeta1.png", 100, 100);

        Text text = new Text("READY");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        text.setTranslateX(30);
        text.setTranslateY(55);

        return entityBuilder(data)
                .type(EntityType.PLANETA_READY)
                .view(view)
                .view(text)
                .bbox(new HitBox(BoundingShape.circle(50)))
                .build();
    }

    // --- PLANETA BLOCKED (AGUJERO NEGRO) ---
    @Spawns("PLANETA_BLOCKED")
    public Entity newPlanetaBlocked(SpawnData data) {
        // Usamos agujero_negro.png
        var view = texture("agujero_negro.png", 110, 110);

        Text text = new Text("BLOCKED");
        text.setFill(Color.ORANGERED);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        text.setTranslateX(25);
        text.setTranslateY(60);

        return entityBuilder(data)
                .type(EntityType.PLANETA_BLOCKED)
                .view(view)
                .view(text)
                .bbox(new HitBox(BoundingShape.circle(55)))
                .build();
    }

    // --- RECURSO ---
    @Spawns("RECURSO")
    public Entity newRecurso(SpawnData data) {
        String nombre = data.get("nombre");
        int total = data.get("capacidad");

        // Usamos planeta2.png para los recursos
        var view = texture("planeta2.png", 60, 60);

        Text text = new Text(nombre + "\n[0/" + total + "]");
        text.setFill(Color.LIGHTGREEN);
        text.setFont(Font.font("Consolas", 10));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-5);
        text.setTranslateY(75); // Texto debajo del planeta

        UIAdapter.getInstance().textosRecursos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.RECURSO)
                .view(view)
                .view(text)
                .bbox(new HitBox(BoundingShape.circle(30)))
                .build();
    }

    // --- SEMAFORO ---
    @Spawns("SEMAFORO")
    public Entity newSemaforo(SpawnData data) {
        String nombre = data.get("nombre");
        int valorInicial = data.get("valor");

        // Usamos estacion_espacial.png para los semáforos/portales
        var view = texture("estacion_espacial.png", 80, 80);

        Text text = new Text("PORTAL\n" + valorInicial);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        text.setFill(Color.CYAN);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(15);
        text.setTranslateY(-10);

        UIAdapter.getInstance().textosSemaforos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.SEMAFORO)
                .view(view)
                .view(text)
                .bbox(new HitBox(BoundingShape.circle(40)))
                .build();
    }

    // --- MEMORIA (Sin imagen específica, usamos panel) ---
    @Spawns("NEBULOSA_MEMORIA")
    public Entity newNebulosa(SpawnData data) {
        // Usamos un rectángulo translúcido elegante
        Rectangle panel = new Rectangle(200, 120, Color.rgb(30, 30, 60, 0.6));
        panel.setArcWidth(20);
        panel.setArcHeight(20);
        panel.setStroke(Color.CYAN);
        panel.setStrokeWidth(1);

        Text text = new Text("MEMORIA RAM");
        text.setFill(Color.CYAN);
        text.setFont(Font.font("Verdana", 11));
        text.setTranslateX(10);
        text.setTranslateY(20);

        UIAdapter.getInstance().textoMemoria = text;

        return entityBuilder(data)
                .type(EntityType.MEMORIA)
                .view(panel)
                .view(text)
                .zIndex(-5)
                .build();
    }

    // --- NAVE PROCESO ---
    @Spawns("NAVE_PROCESO")
    public Entity newNaveProceso(SpawnData data) {
        BCP bcp = data.get("pcb");

        // Usamos nave_espacial.png
        var view = texture("nave_espacial.png", 40, 40);

        Text pidText = new Text("P" + bcp.pid);
        pidText.setFill(Color.YELLOW);
        pidText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        pidText.setTranslateX(10);
        pidText.setTranslateY(-5);

        return entityBuilder(data)
                .type(EntityType.NAVE_PROCESO)
                .view(view)
                .view(pidText)
                .with("pcb", bcp)
                // Caja de colisión cuadrada para la nave
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .build();
    }
}