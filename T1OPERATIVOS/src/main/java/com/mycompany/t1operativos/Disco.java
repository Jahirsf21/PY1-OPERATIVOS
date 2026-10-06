package com.mycompany.t1operativos;

/**
 * Almacena archivos en el disco.
 *
 * @author deislher sánchez funez
 */
public class Disco {
    private String[][] disco;
    private int inicioDatos;
    private int inicioMemoriaVirtual;

    /**
     * Construye el disco con la distribución indicada.
     *
     * @param tamañoTotal cantidad total de posiciones del disco.
     * @param inicioDatos primera posición del espacio de archivos.
     * @param inicioMemoriaVirtual primera posición del respaldo de memoria virtual.
     */
    public Disco(int tamañoTotal, int inicioDatos, int inicioMemoriaVirtual) {
        if (tamañoTotal < 256) {
            throw new IllegalArgumentException("El disco debe tener al menos 256 posiciones.");
        }
        if (inicioDatos <= 0 || inicioDatos >= inicioMemoriaVirtual) {
            throw new IllegalArgumentException("La distribución del disco debe separar índice, datos y memoria virtual.");
        }
        if (tamañoTotal - inicioMemoriaVirtual != tamañoTotal / 8) {
            throw new IllegalArgumentException("La memoria virtual debe ocupar el 12,5% del disco");
        }
        this.disco = new String[tamañoTotal][];
        this.inicioDatos = inicioDatos;
        this.inicioMemoriaVirtual = inicioMemoriaVirtual;
    }

    /**
     * Lee el contenido de una posición del disco.
     *
     * @param posicion dirección que se desea leer.
     * @return el contenido, o {@code null} si la posición está vacía.
     */
    public String[] leer(int posicion) {
        validarDireccion(posicion);
        return disco[posicion];
    }

    /**
     * Escribe el nombre, dirección inicial y tamaño de un archivo en el índice.
     *
     * @param posicion posición de la entrada en el índice.
     * @param nombre nombre del archivo.
     * @param direccion primera posición del contenido en el disco.
     * @param tamaño cantidad de posiciones ocupadas por el archivo.
     */
    public void escribirIndice(int posicion, String nombre, int direccion, int tamaño) {
        validarDireccion(posicion);
        if (posicion >= inicioDatos) {
            throw new IllegalArgumentException("La posición no pertenece al índice del disco.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre.");
        }
        if (tamaño <= 0) {
            throw new IllegalArgumentException("El tamaño del archivo debe ser positivo.");
        }
        if (direccion < inicioDatos || tamaño > inicioMemoriaVirtual - direccion) {
            throw new IllegalArgumentException("El archivo debe almacenarse completo en la zona de datos del disco.");
        }
        disco[posicion] = new String[]{nombre, String.valueOf(direccion), String.valueOf(tamaño)};
    }

    /**
     * Escribe contenido en la zona de archivos del disco.
     *
     * @param posicion dirección donde se desea escribir.
     * @param contenido contenido que se desea almacenar, o {@code null} para vaciar la posición.
     */
    public void escribirDatos(int posicion, String[] contenido) {
        validarDireccion(posicion);
        if (posicion < inicioDatos || posicion >= inicioMemoriaVirtual) {
            throw new IllegalArgumentException("La posición no pertenece a la zona de datos del disco.");
        }
        disco[posicion] = contenido;
    }

    /**
     * Vacía una entrada del índice.
     *
     * @param posicion posición de la entrada.
     */
    public void liberarIndice(int posicion) {
        validarDireccion(posicion);
        if (posicion >= inicioDatos) {
            throw new IllegalArgumentException("La posición no pertenece al índice del disco.");
        }
        disco[posicion] = null;
    }

    /**
     * Vacía un bloque de datos.
     *
     * @param inicio primera posición del bloque.
     * @param cantidad posiciones que se liberarán.
     */
    public void liberarDatos(int inicio, int cantidad) {
        if (cantidad <= 0 || inicio < inicioDatos || cantidad > inicioMemoriaVirtual - inicio) {
            throw new IllegalArgumentException("El bloque debe pertenecer completo a la zona de datos del disco.");
        }
        for (int posicion = inicio; posicion < inicio + cantidad; posicion++) {
            disco[posicion] = null;
        }
    }

    /**
     * Comprueba que una dirección pertenezca al disco.
     *
     * @param posicion dirección que se desea comprobar.
     */
    private void validarDireccion(int posicion) {
        if (posicion < 0 || posicion >= disco.length) {
            throw new IndexOutOfBoundsException("Posición de disco inválida: " + posicion);
        }
    }

    /**
     * Comprueba si una dirección pertenece al índice.
     *
     * @param posicion dirección que se desea comprobar.
     * @return {@code true} si la dirección pertenece al índice.
     */
    public boolean esDireccionIndice(int posicion) {
        validarDireccion(posicion);
        return posicion < inicioDatos;
    }

    /**
     * Comprueba si una dirección pertenece al respaldo de memoria virtual.
     *
     * @param posicion dirección que se desea comprobar.
     * @return {@code true} si la dirección pertenece a la zona reservada.
     */
    public boolean esDireccionMemoriaVirtual(int posicion) {
        validarDireccion(posicion);
        return posicion >= inicioMemoriaVirtual;
    }

    /**
     * Obtiene el tamaño total del disco.
     *
     * @return la cantidad de posiciones del disco.
     */
    public int getTamañoTotal() {
        return disco.length;
    }

    /**
     * Obtiene la primera posición de archivos.
     *
     * @return el inicio de datos y límite exclusivo del índice.
     */
    public int getInicioDatos() {
        return inicioDatos;
    }

    /**
     * Obtiene la primera posición del respaldo de memoria virtual.
     *
     * @return el inicio de la zona reservada.
     */
    public int getInicioMemoriaVirtual() {
        return inicioMemoriaVirtual;
    }
}
