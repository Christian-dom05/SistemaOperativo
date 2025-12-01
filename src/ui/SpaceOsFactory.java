package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle; // Usamos Rectangle para el "cuadro"
import javafx.scene.text.Text;
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

        // --- CAMBIO: Ahora es un Cuadro (Rectangle) ---
        // Si quieres poner una imagen luego, usarías: FXGL.texture("nave.jpg", 40, 40)
        Rectangle naveShape = new Rectangle(40, 40, Color.SILVER);
        naveShape.setStroke(Color.WHITE);
        naveShape.setStrokeWidth(2);

        // Centrar el texto del PID sobre el cuadro
        Text pidText = new Text("P" + bcp.pid);
        pidText.setFill(Color.BLACK);
        pidText.setTranslateX(10); // Ajuste visual
        pidText.setTranslateY(25);

        return entityBuilder(data)
                .type(EntityType.NAVE_PROCESO)
                .viewWithBBox(naveShape) // La caja de colisión es el cuadro
                .view(pidText)
                .with("pcb", bcp)
                .build();
    }
}