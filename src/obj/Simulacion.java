import java.util.*;

public class Simulacion {
    public static void main(String[] args) {
        CentroControl.registrar("=== Iniciando Simulacion Space OS ===");

        Memoria memoria = new Memoria(50, 4);
        ColaListos colaListos = new ColaListos();
        ColaBloqueados colaBloq = new ColaBloqueados();
        CPU cpu = new CPU();

        Map<String, Recurso> recursos = new HashMap<>();
        recursos.put("Planeta-X", new Recurso("Planeta-X", 1));
        recursos.put("Estacion-Alpha", new Recurso("Estacion-Alpha", 2));
        recursos.put("Planeta-Y", new Recurso("Planeta-Y", 1));

        Map<String, SemaphoroGalactico> semaforos = new HashMap<>();
        semaforos.put("Planeta-X", new SemaphoroGalactico("Portal-PlanetaX", 1));
        semaforos.put("Estacion-Alpha", new SemaphoroGalactico("Portal-EstAlpha", 2));

        List<PCB> naves = new ArrayList<>();
        naves.add(new PCB(1, "Nave-Ariel", 1200, 5, Arrays.asList("Planeta-X")));
        naves.add(new PCB(2, "Nave-Borealis", 800, 3, Arrays.asList("Estacion-Alpha")));
        naves.add(new PCB(3, "Nave-Cassiopeia", 1500, 7, Arrays.asList("Planeta-Y", "Estacion-Alpha")));
        naves.add(new PCB(4, "Nave-Draco", 600, 2, Collections.emptyList()));
        naves.add(new PCB(5, "Nave-Equinox", 400, 1, Arrays.asList("Estacion-Alpha")));

        for (PCB pcb : naves) {
            pcb.estado = PCB.EstadoProceso.NUEVO;
            boolean ok = memoria.asignar(pcb);
            if (ok) {
                colaListos.encolar(pcb);
            } else {
                colaBloq.encolar(pcb);
                CentroControl.registrar(String.format("%s (pid=%d) no puede iniciar por memoria. Enviado al agujero negro.", pcb.nombre, pcb.pid));
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
                    for (Recurso r : recursos.values()) { r.liberar(); CentroControl.registrar(String.format("Evento: Recurso %s liberado por entorno.", r.nombre)); }
                }
                synchronized (colaBloq) {
                    List<PCB> b = colaBloq.snapshot();
                    for (PCB p : b) if (Math.random() < 0.3) { colaBloq.remover(p); colaListos.encolar(p); CentroControl.registrar(String.format("Evento: I/O completado para %s (pid=%d). Mover BLACK HOLE -> PLANET.", p.nombre, p.pid)); }
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

        CentroControl.registrar("=== Simulacion finalizada ===");
        CentroControl.cerrar();
    }
}