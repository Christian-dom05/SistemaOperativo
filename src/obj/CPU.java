public class CPU {
    private PCB actual = null;

    public synchronized void cargar(PCB pcb) {
        actual = pcb;
        pcb.estado = PCB.EstadoProceso.EJECUTANDO;
        CentroControl.registrar(String.format("CPU: cargado %s (pid=%d) en el SOL.", pcb.nombre, pcb.pid));
    }

    public synchronized void descargar() {
        if (actual != null) {
            CentroControl.registrar(String.format("CPU: descargado %s (pid=%d) desde el SOL.", actual.nombre, actual.pid));
            actual = null;
        }
    }

    public synchronized PCB getActual() { return actual; }
}