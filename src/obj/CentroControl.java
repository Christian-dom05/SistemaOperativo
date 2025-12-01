package obj;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;

public class CentroControl
{
    private static final String ARCHIVO = "centro_control.log";
    private static PrintWriter writer;
    static
    {
        try
        {
            writer = new PrintWriter(new FileWriter(ARCHIVO, false), true);
        } catch (IOException e)
        {
            writer = null;
            System.err.println("CentroControl: no se pudo abrir archivo de log");
        }
    }

    public static synchronized void registrar(String msg)
    {
        String ts = LocalTime.now().withNano(0).toString();
        String linea = String.format("[%s] %s", ts, msg);
        System.out.println(linea);
        if (writer != null) writer.println(linea);
    }

    public static synchronized void cerrar()
    {
        if (writer != null) writer.close();
    }
}