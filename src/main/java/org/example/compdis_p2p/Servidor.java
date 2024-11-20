package org.example.compdis_p2p;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Clase Servidor
 */
public class Servidor {

    /**
     * iniciar
     * Código asociado al servidor
     */
    public void iniciar() {
        int puertoRMI;
        String registryURL;
        try{
            // Solicita un puerto donde iniciar el servidor
            System.out.println("Introduzca el puerto RMI a ser usado:\n");
            puertoRMI = Integer.parseInt(System.console().readLine().trim());
            // Inicia el registro RMI en el puerto dado
            startRegistry(puertoRMI);
            //Crea una instancia de npuntos_impl
            //npuntos_impl objeto_exportado = new npuntos_impl();
            // Guarda en el resgistro una referencia al objeto a tráves de una URL usando como puerto el dado y como máquina localhost
            // Para ello usa rebind, que sobreescribe en el registro RMI toda referencia asociada al nombre dado
            registryURL = "rmi://localhost:"+ puertoRMI +"/npuntos";
            //Naming.rebind(registryURL, objeto_exportado);
            System.out.println("Servidor resgitrado\n");
            System.out.println("Servidor listo\n");
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * startRegistry
     * Devuelve la referencia al registro RMI que se este ejecutando en el puerto indicado
     * En el caso de no haberlo, crea el registro RMI
     * @param puerto puerto donde se desea inciar el registro RMI
     */
    private void startRegistry(int puerto) throws RemoteException{
        try {
            // Obtiene el registro RMI en el puerto indicado
            Registry registro = LocateRegistry.getRegistry(puerto);
            // Devuelve los objetos registrados en ese puerto
            registro.list();
        } catch (RemoteException e) { // Si no hay un registro en el puerto se lanza una excepción
            System.out.println("No se ha podido encontrar un registro RMI en el puerto " + puerto+"\n");
            // Entonces crea un registro RMI en el puerto
            Registry registry = LocateRegistry.createRegistry(puerto);
            System.out.println("Servidor iniciado en el puerto " + puerto+"\n");
        }
    }


}
