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
        this.colaListos = cL; this.colaBloq = cB; this.memoria = mem; this.cpu = cpu;
        this.recursos = recursos; this.semaforos = semaforos; this.quantumMs = quantumMs;
    }

    @Override
    public void run() {
        CentroControl.registrar("Planificador: Iniciando Round-Robin (Quantum=" + quantumMs + "ms)");
        while (ejecutando) {
            BCP bcp = null;

            // Sección crítica para sacar proceso
            synchronized (colaListos) {
                bcp = colaListos.desenlistar();
            }

            if (bcp == null) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                continue;
            }

            // --- ANIMACIÓN Y CARGA ---
            // Esto moverá la nave visualmente y pausará este hilo hasta que llegue
            cpu.cargar(bcp);

            boolean bloqueado = false;
            try {
                int tiempo = Math.min(quantumMs, bcp.tiempoCpuRestanteMs);
                int paso = 50; // Ejecutar en pasos pequeños
                int ejecutado = 0;

                while (ejecutado < tiempo) {
                    // Simular tiempo de CPU
                    int s = Math.min(paso, tiempo - ejecutado);
                    Thread.sleep(s); // Tiempo real de simulación
                    ejecutado += s;
                    bcp.tiempoCpuRestanteMs -= s;
                    bcp.contadorPrograma++;

                    // --- LÓGICA DE RECURSOS (Probabilidad de pedir recurso) ---
                    if (!bcp.recursosNecesarios.isEmpty() && Math.random() < 0.08) { // 8% de probabilidad por paso
                        String nombreRecurso = bcp.recursosNecesarios.get(0);

                        // Intentar obtener recurso
                        Recurso r = recursos.get(nombreRecurso);
                        boolean adquirido = false;

                        if (r != null) {
                            synchronized (r) {
                                // Pasamos 'bcp' para que Recurso pueda animar la nave yendo hacia él
                                if (r.solicitar(bcp)) {
                                    bcp.recursosNecesarios.remove(0);
                                    adquirido = true;
                                }
                            }
                        }

                        // Si no se pudo adquirir (o es un semáforo lo que se necesita)
                        if (!adquirido) {
                            SemaphoroGalactico sys = semaforos.get(nombreRecurso);
                            // Intentamos buscar semáforo con el nombre "Portal-" + nombreRecurso si no existe directo
                            if (sys == null) sys = semaforos.get("Portal-" + nombreRecurso);

                            if (sys != null) {
                                // waitSem moverá la nave y bloqueará si es necesario
                                sys.waitSem(bcp, colaListos, colaBloq);
                                bloqueado = true;
                                break; // Salir del quantum, se bloqueó
                            } else {
                                // Si no hay recurso ni semáforo, bloqueo genérico
                                bcp.estado = BCP.EstadoProceso.BLOQUEADO;
                                colaBloq.enlistar(bcp);
                                CentroControl.registrar(String.format("%s bloqueado por recurso no disponible: %s", bcp.nombre, nombreRecurso));
                                bloqueado = true;
                                break;
                            }
                        }
                    }

                    // --- SIMULACIÓN DE E/S (Bloqueo aleatorio) ---
                    if (Math.random() < 0.02) { // 2% probabilidad
                        bcp.estado = BCP.EstadoProceso.BLOQUEADO;
                        colaBloq.enlistar(bcp); // Esto anima la nave yendo al planeta Blocked
                        CentroControl.registrar(String.format("%s realiza E/S y se bloquea.", bcp.nombre));
                        bloqueado = true;
                        break;
                    }

                    if (bcp.tiempoCpuRestanteMs <= 0) break;
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // --- FIN DEL QUANTUM ---

            if (bcp.tiempoCpuRestanteMs <= 0) {
                bcp.estado = BCP.EstadoProceso.TERMINADO;
                cpu.descargar();
                memoria.liberar(bcp);

                // Animación: Destruir la nave visualmente al terminar
                ui.UIAdapter.getInstance().destruirNaveVisual(bcp);

                CentroControl.registrar(String.format("Planificador: %s TERMINADO.", bcp.nombre));
                continue;
            }

            if (bloqueado) {
                cpu.descargar();
                // No reencolamos en Listos porque ya se fue a Bloqueados o Semáforo
                continue;
            } else {
                // Quantum agotado, vuelve a la cola de listos
                cpu.descargar();
                colaListos.enlistar(bcp); // Anima la nave de vuelta a Ready
            }
        }
        CentroControl.registrar("Planificador: Detenido.");
    }

    public void detener() { ejecutando = false; }
}