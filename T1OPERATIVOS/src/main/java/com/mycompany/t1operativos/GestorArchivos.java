package com.mycompany.t1operativos;

import java.util.List;

/**
 * Gestiona programas y archivos numéricos en disco.
 *
 * @author deislher sánchez funez
 */
public class GestorArchivos {
    private Disco disco;
    private List<Proceso> procesosRegistrados;

    /**
     * Usa el disco y el registro de procesos.
     *
     * @param disco disco de la simulación.
     * @param procesosRegistrados registro de procesos.
     */
    public GestorArchivos(Disco disco, List<Proceso> procesosRegistrados) {
        if (disco == null || procesosRegistrados == null) {
            throw new IllegalArgumentException("Debe indicar el disco y el registro de procesos.");
        }
        this.disco = disco;
        this.procesosRegistrados = procesosRegistrados;
    }

    /**
     * Almacena las instrucciones validadas de un programa.
     *
     * @param nombre nombre del programa.
     * @param instrucciones instrucciones recibidas.
     * @return posición del programa en el índice.
     */
    public int registrarPrograma(String nombre, List<String[]> instrucciones) {
        if (instrucciones == null || instrucciones.isEmpty()) {
            throw new IllegalArgumentException("El programa debe contener instrucciones.");
        }
        int posicion = buscarEntradaLibre(nombre);
        int inicio = buscarEspacioLibre(instrucciones.size(), -1, 0);
        for (int i = 0; i < instrucciones.size(); i++) {
            disco.escribirDatos(inicio + i, instrucciones.get(i));
        }
        disco.escribirIndice(posicion, nombre, inicio, instrucciones.size());
        return posicion;
    }

    /**
     * Crea un archivo vacío sin abrirlo.
     *
     * @param nombre nombre del archivo.
     */
    public void crearArchivo(String nombre) {
        int posicion = buscarEntradaLibre(nombre);
        int inicio = buscarEspacioLibre(1, -1, 0);
        disco.escribirDatos(inicio, new String[]{""});
        disco.escribirIndice(posicion, nombre, inicio, 1);
    }

    /**
     * Abre un archivo sin repetirlo en la lista del proceso.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre del archivo.
     */
    public void abrirArchivo(BCP bcp, String nombre) {
        if (bcp == null) {
            throw new IllegalArgumentException("Debe indicar el BCP solicitante.");
        }
        int posicion = obtenerArchivoDatos(nombre);
        if (!estaAbierto(bcp, nombre)) {
            bcp.getArchivosAbiertos().add(disco.leer(posicion)[0]);
        }
    }

    /**
     * Lee el entero de un archivo abierto.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre del archivo.
     * @return valor almacenado.
     */
    public int leerArchivo(BCP bcp, String nombre) {
        if (bcp == null) {
            throw new IllegalArgumentException("Debe indicar el BCP solicitante.");
        }
        int posicion = obtenerArchivoDatos(nombre);
        validarApertura(bcp, nombre);
        String[] entrada = disco.leer(posicion);
        int inicio = Integer.parseInt(entrada[1]);
        int cantidad = Integer.parseInt(entrada[2]);
        String contenido = "";
        for (int i = 0; i < cantidad; i++) {
            contenido = contenido + disco.leer(inicio + i)[0];
        }
        if (contenido.isEmpty()) {
            throw new IllegalStateException("El archivo " + nombre + " está vacío; todavía no contiene un número.");
        }
        return Integer.parseInt(contenido);
    }

    /**
     * Reemplaza el número de un archivo abierto si hay espacio.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre del archivo.
     * @param contenido número que se guardará.
     */
    public void escribirArchivo(BCP bcp, String nombre, int contenido) {
        if (bcp == null) {
            throw new IllegalArgumentException("Debe indicar el BCP solicitante.");
        }
        int posicion = obtenerArchivoDatos(nombre);
        validarApertura(bcp, nombre);
        String[] entrada = disco.leer(posicion);
        int inicioActual = Integer.parseInt(entrada[1]);
        int tamañoActual = Integer.parseInt(entrada[2]);
        String numero = Integer.toString(contenido);
        int cantidad = numero.length();
        int inicio = inicioActual;
        if (!puedeUsarBloque(inicio, cantidad, inicioActual, tamañoActual)) {
            inicio = buscarEspacioLibre(cantidad, inicioActual, tamañoActual);
        }
        disco.liberarDatos(inicioActual, tamañoActual);
        guardarNumero(inicio, numero);
        disco.escribirIndice(posicion, entrada[0], inicio, cantidad);
    }

    /**
     * Elimina el archivo si ningún otro proceso lo tiene abierto.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre del archivo.
     */
    public void eliminarArchivo(BCP bcp, String nombre) {
        if (bcp == null) {
            throw new IllegalArgumentException("Debe indicar el BCP solicitante.");
        }
        int posicion = obtenerArchivoDatos(nombre);
        if (estaAbiertoPorOtro(bcp, nombre)) {
            throw new IllegalStateException("El archivo " + nombre + " está abierto por otro proceso.");
        }
        String[] entrada = disco.leer(posicion);
        disco.liberarDatos(Integer.parseInt(entrada[1]), Integer.parseInt(entrada[2]));
        disco.liberarIndice(posicion);
        List<String> abiertos = bcp.getArchivosAbiertos();
        for (int i = 0; i < abiertos.size(); i++) {
            if (abiertos.get(i).equalsIgnoreCase(nombre)) {
                abiertos.remove(i);
                break;
            }
        }
    }

    /**
     * Cierra los archivos del proceso sin borrarlos.
     *
     * @param bcp BCP del proceso que termina.
     */
    public void cerrarArchivos(BCP bcp) {
        if (bcp == null) {
            throw new IllegalArgumentException("Debe indicar el BCP solicitante.");
        }
        bcp.getArchivosAbiertos().clear();
    }

    /**
     * Comprueba que el nombre no esté vacío.
     *
     * @param nombre nombre recibido.
     */
    private void validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre.");
        }
    }

    /**
     * Busca el archivo sin distinguir mayúsculas.
     *
     * @param nombre nombre recibido.
     * @return posición en el índice, o -1 si no existe.
     */
    private int buscarArchivo(String nombre) {
        validarNombre(nombre);
        for (int posicion = 0; posicion < disco.getInicioDatos(); posicion++) {
            String[] entrada = disco.leer(posicion);
            if (entrada != null && entrada[0].equalsIgnoreCase(nombre)) {
                return posicion;
            }
        }
        return -1;
    }

    /**
     * Busca una entrada libre y rechaza nombres repetidos.
     *
     * @param nombre nombre recibido.
     * @return primera entrada libre.
     */
    private int buscarEntradaLibre(String nombre) {
        if (buscarArchivo(nombre) >= 0) {
            throw new IllegalArgumentException("El archivo " + nombre + " ya existe en el índice del disco.");
        }
        for (int posicion = 0; posicion < disco.getInicioDatos(); posicion++) {
            if (disco.leer(posicion) == null) {
                return posicion;
            }
        }
        throw new IllegalStateException("El índice del disco no tiene entradas disponibles.");
    }

    /**
     * Busca un archivo de datos y rechaza los programas.
     *
     * @param nombre nombre recibido.
     * @return posición del archivo en el índice.
     */
    private int obtenerArchivoDatos(String nombre) {
        int posicion = buscarArchivo(nombre);
        if (posicion < 0) {
            throw new IllegalArgumentException("El archivo " + nombre + " no existe en el disco.");
        }
        for (Proceso proceso : procesosRegistrados) {
            if (proceso.getPosicionIndice() == posicion) {
                throw new IllegalArgumentException("El archivo " + nombre + " es un programa registrado, no un archivo de datos.");
            }
        }
        return posicion;
    }

    /**
     * Comprueba si el proceso tiene abierto el archivo.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre recibido.
     * @return true si el proceso lo tiene abierto.
     */
    private boolean estaAbierto(BCP bcp, String nombre) {
        for (String abierto : bcp.getArchivosAbiertos()) {
            if (abierto.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Comprueba que el proceso tenga abierto el archivo.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre recibido.
     */
    private void validarApertura(BCP bcp, String nombre) {
        if (!estaAbierto(bcp, nombre)) {
            throw new IllegalStateException("El proceso no tiene abierto el archivo " + nombre + ".");
        }
    }

    /**
     * Comprueba si otro proceso tiene abierto el archivo.
     *
     * @param bcp BCP del proceso.
     * @param nombre nombre recibido.
     * @return true si otro proceso lo tiene abierto.
     */
    private boolean estaAbiertoPorOtro(BCP bcp, String nombre) {
        for (Proceso proceso : procesosRegistrados) {
            if (proceso.getBCP() != bcp && estaAbierto(proceso.getBCP(), nombre)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca espacio contiguo usando también el bloque actual.
     *
     * @param cantidad posiciones necesarias.
     * @param inicioActual inicio del bloque actual, o -1 si no existe.
     * @param tamañoActual tamaño del bloque actual.
     * @return primer inicio disponible.
     */
    private int buscarEspacioLibre(int cantidad, int inicioActual, int tamañoActual) {
        int libres = 0;
        for (int posicion = disco.getInicioDatos(); posicion < disco.getInicioMemoriaVirtual(); posicion++) {
            if (disco.leer(posicion) == null || posicion >= inicioActual && posicion < inicioActual + tamañoActual) {
                libres++;
            } else {
                libres = 0;
            }
            if (libres == cantidad) {
                return posicion - cantidad + 1;
            }
        }
        throw new IllegalStateException("El disco no tiene un bloque de datos libre para las " + cantidad + " posiciones del archivo.");
    }

    /**
     * Comprueba un bloque sin modificar el disco.
     *
     * @param inicio inicio propuesto.
     * @param cantidad posiciones necesarias.
     * @param inicioActual inicio del bloque actual, o -1 si no existe.
     * @param tamañoActual tamaño del bloque actual.
     * @return true si el bloque puede usarse.
     */
    private boolean puedeUsarBloque(int inicio, int cantidad, int inicioActual, int tamañoActual) {
        if (inicio < disco.getInicioDatos() || cantidad > disco.getInicioMemoriaVirtual() - inicio) {
            return false;
        }
        for (int posicion = inicio; posicion < inicio + cantidad; posicion++) {
            if (disco.leer(posicion) != null && (posicion < inicioActual || posicion >= inicioActual + tamañoActual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Guarda cada dígito o signo en una posición del disco.
     *
     * @param inicio inicio del bloque disponible.
     * @param numero número en texto decimal.
     */
    private void guardarNumero(int inicio, String numero) {
        for (int i = 0; i < numero.length(); i++) {
            disco.escribirDatos(inicio + i, new String[]{numero.substring(i, i + 1)});
        }
    }
}
