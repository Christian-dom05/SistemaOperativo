package ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import obj.BCP;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Text;

public class SpaceOsFactory implements EntityFactory {

    @Spawns("SOL_CPU")
    public Entity newSolCpu(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.SOL_CPU)
                // El Sol es amarillo y grande en el centro
                .viewWithBBox(new Circle(60, Color.GOLD))
                .view(new Text("CPU (Sol)"))
                .build();
    }

    @Spawns("PLANETA_READY")
    public Entity newPlanetaReady(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PLANETA_READY)
                // Planeta azul para Ready
                .viewWithBBox(new Circle(40, Color.DODGERBLUE))
                .view(new Text("Ready"))
                .build();
    }

    @Spawns("PLANETA_BLOCKED")
    public Entity newPlanetaBlocked(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PLANETA_BLOCKED)
                // Planeta rojo para Blocked / Agujero Negro
                .viewWithBBox(new Circle(40, Color.ORANGERED))
                .view(new Text("Blocked"))
                .build();
    }

    @Spawns("NAVE_PROCESO")
    public Entity newNaveProceso(SpawnData data) {
        PCB pcb = data.get("pcb");

        // Una nave triangular simple
        Polygon naveShape = new Polygon(0, 0, 20, 10, 0, 20);
        naveShape.setFill(Color.LIGHTGRAY);
        naveShape.setStroke(Color.WHITE);

        Text pidText = new Text("P" + pcb.pid);
        pidText.setFill(Color.WHITE);
        pidText.setTranslateY(-5);

        return entityBuilder(data)
                .type(EntityType.NAVE_PROCESO)
                .viewWithBBox(naveShape)
                .view(pidText)
                // Guardamos el PCB dentro de la entidad visual para referencia
                .with("pcb", pcb)
                .build();
    }
}