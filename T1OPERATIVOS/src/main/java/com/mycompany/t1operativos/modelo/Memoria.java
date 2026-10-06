package com.mycompany.t1operativos.modelo;

/**
 * Almacena instrucciones y atributos del BCP en sus respectivas zonas.
 *
 * @author deislher sánchez funez
 */
public class Memoria {
    private String[][] memoria;
    private int inicioUsuario;

    /**
     * Construye la memoria con la distribución indicada.
     *
     * @param tamañoTotal cantidad total de posiciones de memoria.
     * @param inicioUsuario primera posición del espacio de usuario.
     */
    public Memoria(int tamañoTotal, int inicioUsuario) {
        if (inicioUsuario <= 0 || inicioUsuario >= tamañoTotal) {
            throw new IllegalArgumentException("La memoria debe tener zonas de kernel y usuario válidas.");
        }
        this.memoria = new String[tamañoTotal][];
        this.inicioUsuario = inicioUsuario;
    }

    /**
     * Obtiene el inicio del espacio de usuario.
     *
     * @return primera posición de usuario.
     */
    public int getInicioUsuario() {
        return inicioUsuario;
    }

    /**
     * Vacía un bloque del kernel.
     *
     * @param inicio primera posición del bloque.
     * @param cantidad cantidad de posiciones.
     */
    public void liberarKernel(int inicio, int cantidad) {
        validarBloque(inicio, cantidad, 0, inicioUsuario);
        for (int i = 0; i < cantidad; i++) {
            memoria[inicio + i] = null;
        }
    }

    /**
     * Vacía un bloque de usuario.
     *
     * @param inicio primera posición del bloque.
     * @param cantidad cantidad de posiciones.
     */
    public void liberarUsuario(int inicio, int cantidad) {
        validarBloque(inicio, cantidad, inicioUsuario, memoria.length);
        for (int i = 0; i < cantidad; i++) {
            memoria[inicio + i] = null;
        }
    }

    /**
     * Valida el rango de un bloque.
     *
     * @param inicio primera posición.
     * @param cantidad cantidad de posiciones.
     * @param limiteInferior límite inclusivo.
     * @param limiteSuperior límite exclusivo.
     */
    private void validarBloque(int inicio, int cantidad, int limiteInferior, int limiteSuperior) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de posiciones debe ser positiva.");
        }
        if (inicio < limiteInferior || cantidad > limiteSuperior - inicio) {
            throw new IllegalArgumentException("El bloque no pertenece completo a la zona indicada.");
        }
    }

    /**
     * Lee el contenido de una posición de memoria.
     *
     * @param posicion dirección que se desea leer.
     * @return el contenido, o {@code null} si la posición está vacía.
     */
    public String[] leer(int posicion) {
        validarDireccion(posicion);
        return memoria[posicion];
    }

    /**
     * Escribe una instrucción en el espacio de usuario.
     *
     * @param posicion dirección donde se desea escribir.
     * @param instruccion instrucción que se desea almacenar, o {@code null} para vaciar la posición.
     */
    public void escribirUsuario(int posicion, String[] instruccion) {
        validarDireccion(posicion);
        if (posicion < inicioUsuario) {
            throw new IllegalArgumentException("La posición no pertenece al espacio de usuario.");
        }
        memoria[posicion] = instruccion;
    }

    /**
     * Escribe un atributo del BCP en el espacio del kernel.
     *
     * @param posicion dirección donde se desea escribir.
     * @param atributo nombre del atributo del BCP.
     * @param valor valor del atributo.
     */
    public void escribirKernel(int posicion, String atributo, String valor) {
        validarDireccion(posicion);
        if (posicion >= inicioUsuario) {
            throw new IllegalArgumentException("La posición no pertenece al kernel.");
        }
        memoria[posicion] = new String[]{atributo, valor};
    }

    /**
     * Comprueba que una dirección pertenezca a la memoria.
     *
     * @param posicion dirección que se desea comprobar.
     */
    private void validarDireccion(int posicion) {
        if (posicion < 0 || posicion >= memoria.length) {
            throw new IndexOutOfBoundsException("Posición de memoria inválida: " + posicion);
        }
    }

    /**
     * Comprueba si una dirección pertenece al kernel.
     *
     * @param posicion dirección que se desea comprobar.
     * @return {@code true} si la dirección pertenece al kernel.
     */
    public boolean esDireccionKernel(int posicion) {
        validarDireccion(posicion);
        return posicion < inicioUsuario;
    }

    /**
     * Obtiene el tamaño total de la memoria.
     *
     * @return la cantidad de posiciones de memoria.
     */
    public int getTamañoTotal() {
        return memoria.length;
    }
}
