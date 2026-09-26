package com.mycompany.t1operativos;

/**
 * Almacena instrucciones y atributos del BCP en sus respectivas zonas.
 *
 * @author deislher sánchez funez
 */
public class Memoria {
    private String[][] memoria;
    private int inicioUsuario;

    /** Construye la memoria con la distribución indicada por el controlador. */
    public Memoria(int tamañoTotal, int inicioUsuario) {
        this.memoria = new String[tamañoTotal][];
        this.inicioUsuario = inicioUsuario;
    }

    /** Lee el contenido de una posición de memoria. */
    public String[] leer(int posicion) {
        validarDireccion(posicion);
        return memoria[posicion] == null ? null : memoria[posicion].clone();
    }

    /** Escribe una instrucción en el espacio de usuario. */
    public void escribirUsuario(int posicion, String[] instruccion) {
        validarDireccion(posicion);
        if (posicion < inicioUsuario) {
            throw new IllegalArgumentException("La posición no pertenece al espacio de usuario.");
        }
        memoria[posicion] = instruccion == null ? null : instruccion.clone();
    }

    /** Escribe un atributo del BCP en el espacio del kernel. */
    public void escribirKernel(int posicion, String atributo, String valor) {
        validarDireccion(posicion);
        if (posicion >= inicioUsuario) {
            throw new IllegalArgumentException("La posición no pertenece al kernel.");
        }
        memoria[posicion] = new String[]{atributo, valor};
    }

    /** Comprueba que una dirección pertenezca a la memoria. */
    private void validarDireccion(int posicion) {
        if (posicion < 0 || posicion >= memoria.length) {
            throw new IndexOutOfBoundsException("Posición de memoria inválida: " + posicion);
        }
    }

    /** Indica si una dirección pertenece al kernel. */
    public boolean esDireccionKernel(int posicion) {
        validarDireccion(posicion);
        return posicion < inicioUsuario;
    }

    /** @return la cantidad de posiciones de memoria. */
    public int getTamañoTotal() {
        return memoria.length;
    }
}
