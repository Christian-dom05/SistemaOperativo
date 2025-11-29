import java.util.Map;

public class Planificador implements Runnable {
    private final ColaListos colaListos;
    private final ColaBloqueados colaBloq;
    private final Memoria memoria;
    private final CPU cpu;
    private final Map<String, Recurso> recursos;
    private final Map<String, SemaphoroGalactico> semaforos;
    private final int quantumMs;
    private volatile boolean ejecutando = true;

    public Planificador(ColaListos cL, ColaBloqueados cB, Memoria mem, CPU cpu,
                        Map<String, Recurso> recursos, Map<String, SemaphoroGalactico> semaforos, int quantumMs) {
        this.colaListos = cL; this.colaBloq = cB; this.memoria = mem; this.cpu = cpu;
        this.recursos = recursos; this.semaforos = semaforos; this.quantumMs = quantumMs;
    }

    @Override
    public void run() {
        CentroControl.registrar("Planificador: iniciando Round-Robin con quantum=" + quantumMs + "ms");
        while (ejecutando) {
            PCB pcb = null;
            synchronized (colaListos) { pcb = colaListos.desencolar(); }
            if (pcb == null) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                continue;
            }
            cpu.cargar(pcb);
            boolean bloqueado = false;
            try {
                int tiempo = Math.min(quantumMs, pcb.tiempoCpuRestanteMs);
                int paso = 50;
                int ejecutado = 0;
                while (ejecutado < tiempo) {
                    int s = Math.min(paso, tiempo - ejecutado);
                    Thread.sleep(s);
                    ejecutado += s;
                    pcb.tiempoCpuRestanteMs -= s;
                    pcb.contadorPrograma++;

                    // Simular intento de adquirir recurso
                    if (!pcb.recursosNecesarios.isEmpty() && Math.random() < 0.12) {
                        String nombreRecurso = pcb.recursosNecesarios.get(0);
                        Recurso r = recursos.get(nombreRecurso);
                        if (r != null) {
                            synchronized (r) {
                                if (r.solicitar()) {
                                    CentroControl.registrar(String.format("%s (pid=%d) adquirio recurso %s.", pcb.nombre, pcb.pid, nombreRecurso));
                                    pcb.recursosNecesarios.remove(0);
                                } else {
                                    SemaphoroGalactico sys = semaforos.get(nombreRecurso);
                                    if (sys != null) {
                                        sys.waitSem(pcb, colaListos, colaBloq);
                                        bloqueado = true; break;
                                    } else {
                                        pcb.estado = PCB.EstadoProceso.BLOQUEADO;
                                        colaBloq.encolar(pcb);
                                        CentroControl.registrar(String.format("%s (pid=%d) bloqueado por recurso %s.", pcb.nombre, pcb.pid, nombreRecurso));
                                        bloqueado = true; break;
                                    }
                                }
                            }
                        }
                    }
                    if (Math.random() < 0.04) {
                        pcb.estado = PCB.EstadoProceso.BLOQUEADO;
                        colaBloq.encolar(pcb);
                        CentroControl.registrar(String.format("%s (pid=%d) realiza E/S y se bloquea.", pcb.nombre, pcb.pid));
                        bloqueado = true; break;
                    }
                    if (pcb.tiempoCpuRestanteMs <= 0) break;
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            if (pcb.tiempoCpuRestanteMs <= 0) {
                pcb.estado = PCB.EstadoProceso.TERMINADO;
                cpu.descargar();
                memoria.liberar(pcb);
                CentroControl.registrar(String.format("Planificador: %s (pid=%d) TERMINADO.", pcb.nombre, pcb.pid));
                continue;
            }

            if (bloqueado) {
                cpu.descargar();
                CentroControl.registrar(String.format("Planificador: %s (pid=%d) fue bloqueado durante su quantum.", pcb.nombre, pcb.pid));
                continue; // no reencolar
            } else {
                cpu.descargar();
                colaListos.encolar(pcb);
            }
        }
        CentroControl.registrar("Planificador: detenido.");
    }

    public void detener() { ejecutando = false; }
}