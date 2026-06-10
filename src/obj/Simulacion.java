package obj;

import java.util.*;

public class Simulacion {
    public static void main(String[] args) {
        CentroControl.registrar("=== Iniciando oSimulacion Space OS ===");

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

        for (BCP bcp : naves) {
            bcp.estado = BCP.EstadoProceso.NUEVO;
            boolean ok = memoria.asignar(bcp);
            if (ok) {
                colaListos.enlistar(bcp);
            } else {
                colaBloq.enlistar(bcp);
                CentroControl.registrar(String.format("%s (pid=%d) no puede iniciar por memoria. Enviado al agujero negro.", bcp.nombre, bcp.pid));
            }
        }

        Planificador planificador = new Planificador(colaListos, colaBloq, memoria, cpu, recursos, semaforos, 200);
        Thread hiloPlanificador = new Thread(planificador, "HiloPlanificador");
        hiloPlanificador.start();

        Thread hiloEventos = new Thread(() -> {
            Random rnd = new Random();
            int ciclos = 0;
            while (ciclos < 120) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                if (rnd.nextDouble() < 0.4) {
                    for (SemaphoroGalactico s : semaforos.values()) s.signalSem(colaListos, colaBloq);
                }
                if (rnd.nextDouble() < 0.2) {
                    for (Recurso r : recursos.values()) { r.liberar(); CentroControl.registrar(String.format("Evento: obj.Recurso %s liberado por entorno.", r.nombre)); }
                }
                synchronized (colaBloq) {
                    List<BCP> b = colaBloq.snapshot();
                    for (BCP p : b) if (Math.random() < 0.3) { colaBloq.remover(p); colaListos.enlistar(p); CentroControl.registrar(String.format("Evento: I/O completado para %s (pid=%d). Mover BLACK HOLE -> PLANET.", p.nombre, p.pid)); }
                }
                ciclos++;
            }
            CentroControl.registrar("HiloEventos: finalizo simulacion de eventos.");
        }, "HiloEventos");
        hiloEventos.start();

        try { hiloEventos.join(); } catch (InterruptedException ignored) {}
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        planificador.detener();
        try { hiloPlanificador.join(); } catch (InterruptedException ignored) {}

        CentroControl.registrar("=== " +
                "Simulacion finalizada ===");
        CentroControl.cerrar();
    }
}