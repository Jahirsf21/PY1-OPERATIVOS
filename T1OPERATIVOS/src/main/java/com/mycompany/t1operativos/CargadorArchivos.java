package com.mycompany.t1operativos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga archivos ensamblador con extensión {@code .asm} y convierte cada línea
 * en una instrucción validada mediante el {@link Parser}.
 *
 * @author deislher sánchez funez
 */
public class CargadorArchivos {
    private Parser parser;
    private List<String> errores;

    /**
     * Construye el cargador de archivos ensamblador.
     */
    public CargadorArchivos() {
        this.parser = new Parser();
        this.errores = new ArrayList<>();
    }

    /**
     * Lee y procesa las instrucciones de un archivo ensamblador. Las líneas
     * vacías se omiten. Las líneas inválidas no se incluyen en el resultado y
     * sus errores quedan disponibles mediante {@link #getErrores()}.
     *
     * @param rutaArchivo ruta del archivo {@code .asm} que se desea cargar.
     * @return una lista con las instrucciones válidas procesadas.
     * @throws IOException si ocurre un error al abrir o leer el archivo.
     * @throws IllegalArgumentException si la ruta es inválida, el archivo no tiene extensión {@code .asm} o no contiene instrucciones válidas.
     */
    public List<String[]> cargarArchivo(String rutaArchivo) throws IOException {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar un archivo.");
        }
        if (!rutaArchivo.toLowerCase().endsWith(".asm")) {
            throw new IllegalArgumentException("El archivo debe tener extensión .asm");
        }
        errores.clear();
        List<String[]> instrucciones = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.trim().isEmpty()) {
                    continue;
                }
                ResultadoParser resultado = parser.procesarInstruccion(linea);
                if (!resultado.esValido()) {
                    errores.add("Línea " + numeroLinea + ": " + resultado.getMensajeError());
                    continue;
                }
                instrucciones.add(resultado.getInstruccion());
            }
        }
        if (instrucciones.isEmpty()) {
            throw new IllegalArgumentException("El archivo no contiene instrucciones.");
        }
        return instrucciones;
    }

    /**
     * Obtiene los errores encontrados durante la última carga del archivo.
     *
     * @return una copia de la lista de errores, con su correspondiente número
     *     de línea.
     */
    public List<String> getErrores() {
        return new ArrayList<>(errores);
    }
}
