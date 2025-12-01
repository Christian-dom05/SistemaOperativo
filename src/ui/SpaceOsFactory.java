package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import obj.BCP;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

public class SpaceOsFactory implements EntityFactory {

    @Spawns("SOL_CPU")
    public Entity newSolCpu(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.SOL_CPU)
                .viewWithBBox(new Circle(60, Color.GOLD))
                .view(new Text("CPU (Sol)"))
                .build();
    }

    @Spawns("PLANETA_READY")
    public Entity newPlanetaReady(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PLANETA_READY)
                .viewWithBBox(new Circle(40, Color.DODGERBLUE))
                .view(new Text("Ready"))
                .build();
    }

    @Spawns("PLANETA_BLOCKED")
    public Entity newPlanetaBlocked(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PLANETA_BLOCKED)
                .viewWithBBox(new Circle(40, Color.ORANGERED))
                .view(new Text("Blocked"))
                .build();
    }

    @Spawns("NAVE_PROCESO")
    public Entity newNaveProceso(SpawnData data) {
        BCP bcp = data.get("pcb");
        Rectangle naveShape = new Rectangle(40, 40, Color.SILVER);
        naveShape.setStroke(Color.WHITE);
        naveShape.setStrokeWidth(2);

        Text pidText = new Text("P" + bcp.pid);
        pidText.setFill(Color.BLACK);
        pidText.setTranslateX(10);
        pidText.setTranslateY(25);

        return entityBuilder(data)
                .type(EntityType.NAVE_PROCESO)
                .viewWithBBox(naveShape)
                .view(pidText)
                .with("pcb", bcp)
                .build();
    }

    // --- NUEVAS ENTIDADES VISUALES ---

    @Spawns("RECURSO")
    public Entity newRecurso(SpawnData data) {
        String nombre = data.get("nombre");
        int total = data.get("capacidad");

        // Círculo verde (Planeta Recurso)
        // Opción imagen: .view(FXGL.texture("recurso.png"))
        Circle shape = new Circle(35, Color.FORESTGREEN);
        shape.setStroke(Color.LIMEGREEN);
        shape.setStrokeWidth(3);

        Text text = new Text(nombre + "\nUso: 0/" + total);
        text.setFill(Color.WHITE);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-20); // Centrar aprox
        text.setTranslateY(5);

        // Registrar texto en el adaptador para actualizarlo luego
        UIAdapter.getInstance().textosRecursos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.RECURSO)
                .viewWithBBox(shape)
                .view(text)
                .build();
    }

    @Spawns("SEMAFORO")
    public Entity newSemaforo(SpawnData data) {
        String nombre = data.get("nombre");
        int valorInicial = data.get("valor");

        // Cuadrado plateado (Portal Galáctico)
        Rectangle shape = new Rectangle(60, 60, Color.DARKGRAY);
        shape.setStroke(Color.CYAN);
        shape.setStrokeType(StrokeType.INSIDE);
        shape.setStrokeWidth(4);
        shape.setArcWidth(15);
        shape.setArcHeight(15);

        Text text = new Text("Semaforo\n" + nombre + "\nValor: " + valorInicial);
        text.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        text.setFill(Color.CYAN);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(5);
        text.setTranslateY(20);

        // Registrar texto
        UIAdapter.getInstance().textosSemaforos.put(nombre, text);

        return entityBuilder(data)
                .type(EntityType.SEMAFORO)
                .viewWithBBox(shape)
                .view(text)
                .build();
    }

    @Spawns("NEBULOSA_MEMORIA")
    public Entity newNebulosa(SpawnData data) {
        // Nube simulada con un elipse grande translúcido
        Circle shape = new Circle(70, Color.rgb(138, 43, 226, 0.4)); // Violeta translúcido
        shape.setStroke(Color.VIOLET);
        shape.setStrokeWidth(2);
        shape.setEffect(new javafx.scene.effect.GaussianBlur(10)); // Efecto difuso de nebulosa

        Text text = new Text("MEMORIA RAM\nIniciando...");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTranslateX(-40);

        // Registrar referencia única de memoria
        UIAdapter.getInstance().textoMemoria = text;

        return entityBuilder(data)
                .type(EntityType.MEMORIA)
                .view(shape) // Sin BBox física dura para no chocar feo
                .view(text)
                .zIndex(-1) // Que se dibuje detrás de las naves
                .build();
    }
}