package obj;

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
        this.colaListos = cL;
        this.colaBloq = cB;
        this.memoria = mem;
        this.cpu = cpu;
        this.recursos = recursos;
        this.semaforos = semaforos;
        this.quantumMs = quantumMs;
    }

    @Override
    public void run() {
        CentroControl.registrar("obj.Planificador: iniciando Round-Robin con quantum=" + quantumMs + "ms");
        while (ejecutando) {
            BCP bcp = null;
            synchronized (colaListos) { bcp = colaListos.desenlistar(); }
            if (bcp == null) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                continue;
            }
            cpu.cargar(bcp);
            boolean bloqueado = false;
            try {
                int tiempo = Math.min(quantumMs, bcp.tiempoCpuRestanteMs);
                int paso = 50;
                int ejecutado = 0;
                while (ejecutado < tiempo) {
                    int s = Math.min(paso, tiempo - ejecutado);
                    Thread.sleep(s);
                    ejecutado += s;
                    bcp.tiempoCpuRestanteMs -= s;
                    bcp.contadorPrograma++;

                    // Simula el intento de adquirir recurso
                    if (!bcp.recursosNecesarios.isEmpty() && Math.random() < 0.12) {
                        String nombreRecurso = bcp.recursosNecesarios.get(0);
                        Recurso r = recursos.get(nombreRecurso);
                        if (r != null) {
                            synchronized (r) {
                                if (r.solicitar()) {
                                    CentroControl.registrar(String.format("%s (pid=%d) adquirio recurso %s.", bcp.nombre, bcp.pid, nombreRecurso));
                                    bcp.recursosNecesarios.remove(0);
                                } else {
                                    SemaphoroGalactico sys = semaforos.get(nombreRecurso);
                                    if (sys != null) {
                                        sys.waitSem(bcp, colaListos, colaBloq);
                                        bloqueado = true; break;
                                    } else {
                                        bcp.estado = BCP.EstadoProceso.BLOQUEADO;
                                        colaBloq.enlistar(bcp);
                                        CentroControl.registrar(String.format("%s (pid=%d) bloqueado por recurso %s.", bcp.nombre, bcp.pid, nombreRecurso));
                                        bloqueado = true; break;
                                    }
                                }
                            }
                        }
                    }
                    if (Math.random() < 0.04) {
                        bcp.estado = BCP.EstadoProceso.BLOQUEADO;
                        colaBloq.enlistar(bcp);
                        CentroControl.registrar(String.format("%s (pid=%d) realiza E/S y se bloquea.", bcp.nombre, bcp.pid));
                        bloqueado = true; break;
                    }
                    if (bcp.tiempoCpuRestanteMs <= 0) break;
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            if (bcp.tiempoCpuRestanteMs <= 0) {
                bcp.estado = BCP.EstadoProceso.TERMINADO;
                cpu.descargar();
                memoria.liberar(bcp);
                CentroControl.registrar(String.format("obj.Planificador: %s (pid=%d) TERMINADO.", bcp.nombre, bcp.pid));
                continue;
            }

            if (bloqueado) {
                cpu.descargar();
                CentroControl.registrar(String.format("obj.Planificador: %s (pid=%d) fue bloqueado durante su quantum.", bcp.nombre, bcp.pid));
                continue; // no reencolar
            } else {
                cpu.descargar();
                colaListos.enlistar(bcp);
            }
        }
        CentroControl.registrar("obj.Planificador: detenido.");
    }

    public void detener() { ejecutando = false; }
}