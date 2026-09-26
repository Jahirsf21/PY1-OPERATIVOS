package com.mycompany.t1operativos;

import com.mycompany.t1operativos.gui.Aplicacion;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Conecta la interfaz gráfica con los componentes de la Mini PC.
 *
 * Se encarga de cargar programas, crear la memoria y su BCP, ejecutar la CPU y
 * mantener actualizadas las tablas y el panel de contexto del proceso.
 *
 * @author deislher sánchez funez
 */
public class Controlador {
    private Aplicacion vista;
    private CargadorArchivos cargador;
    private Parser parser;
    private Memoria memoria;
    private CPU cpu;
    private BCP bcp;
    private int posicionBCP;

    /**
     * Construye el controlador y registra los eventos de la interfaz.
     *
     * @param vista ventana principal que se desea controlar.
     */
    public Controlador(Aplicacion vista) {
        if (vista == null) {
            throw new IllegalArgumentException("La vista no puede ser nula.");
        }
        this.vista = vista;
        this.cargador = new CargadorArchivos();
        this.parser = new Parser();
        this.posicionBCP = -1;
        registrarEventos();
    }

    /** Registra las acciones de los botones y del selector de archivos. */
    private void registrarEventos() {
        vista.addPropertyChangeListener("Archivo cargado", evento -> {
            Object archivoSeleccionado = evento.getNewValue();
            if (archivoSeleccionado instanceof File) {
                cargarPrograma((File) archivoSeleccionado);
            }
        });
        vista.getBtnPasoAPaso().addActionListener(e -> ejecutarPaso());
        vista.getBtnEjecutar().addActionListener(e -> ejecutarTodo());
        vista.getBtnLimpiar().addActionListener(e -> limpiar());
    }

    /**
     * Carga un archivo, distribuye la memoria y crea el contexto del proceso.
     *
     * @param archivo archivo ensamblador seleccionado.
     */
    private void cargarPrograma(File archivo) {
        try {
            int tamañoTotal = vista.getMemoriaSeleccionada();
            if (tamañoTotal % 4 != 0) {
                throw new IllegalArgumentException("La memoria total debe ser múltiplo de 4 para dividirla en 25% y 75%.");
            }

            List<String[]> instrucciones = cargador.cargarArchivo(archivo.getAbsolutePath());
            int tamañoKernel = tamañoTotal / 4;
            memoria = new Memoria(tamañoTotal, tamañoKernel);
            memoria.cargarPrograma(instrucciones);
            cpu = new CPU(memoria);

            bcp = new BCP(1, 1, memoria.getInicioUsuario(), memoria.getFinPrograma());
            bcp.setEstadoListo();
            bcp.guardarContexto(cpu);
            posicionBCP = memoria.guardarBCP(bcp);

            llenarTablaInstrucciones(instrucciones);
            actualizarTablaMemoria();
            actualizarBCP();
            actualizarSeleccionProximaInstruccion();
            vista.setSelectorMemoriaHabilitado(false);
            vista.setControlesProgramaHabilitados(true);
            vista.setTitle("Mini PC - " + archivo.getName());
            mostrarErroresCarga(cargador.getErrores());
        } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /** Ejecuta una instrucción y refleja el nuevo contexto del proceso. */
    private void ejecutarPaso() {
        if (cpu == null || bcp == null) {
            mostrarError("Primero debe cargar un programa.");
            return;
        }

        try {
            bcp.setEstadoEjecutando();
            if (cpu.ejecutarSiguiente()) {
                bcp.guardarContexto(cpu);
            }
            if (!cpu.hayInstruccionPendiente()) {
                bcp.setEstadoTerminado();
                deshabilitarEjecucion();
            }
            actualizarBCP();
            actualizarTablaMemoria();
            actualizarSeleccionProximaInstruccion();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bcp.setEstadoBloqueado();
            bcp.guardarContexto(cpu);
            actualizarBCP();
            mostrarError(ex.getMessage());
        }
    }

    /** Ejecuta todas las instrucciones pendientes y muestra el contexto final. */
    private void ejecutarTodo() {
        if (cpu == null || bcp == null) {
            mostrarError("Primero debe cargar un programa.");
            return;
        }

        try {
            bcp.setEstadoEjecutando();
            cpu.ejecutarTodo();
            bcp.guardarContexto(cpu);
            bcp.setEstadoTerminado();
            actualizarBCP();
            actualizarTablaMemoria();
            actualizarSeleccionProximaInstruccion();
            deshabilitarEjecucion();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bcp.setEstadoBloqueado();
            bcp.guardarContexto(cpu);
            actualizarBCP();
            mostrarError(ex.getMessage());
        }
    }

    /** Llena la tabla con las instrucciones en formato ensamblador. */
    private void llenarTablaInstrucciones(List<String[]> instrucciones) {
        DefaultTableModel modelo = vista.getModeloInstrucciones();
        modelo.setRowCount(0);
        for (String[] instruccion : instrucciones) {
            modelo.addRow(new Object[]{parser.traducirInstruccion(instruccion)});
        }
    }

    /** Actualiza la tabla que representa todas las posiciones de memoria. */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);
        if (memoria == null) {
            return;
        }

        for (int posicion = 0; posicion < memoria.getTamañoTotal(); posicion++) {
            String contenido = "";
            if (!memoria.esDireccionKernel(posicion)) {
                String[] instruccion = memoria.leer(posicion);
                if (instruccion != null) {
                    contenido = parser.traducirInstruccion(instruccion);
                }
            }
            modelo.addRow(new Object[]{posicion, contenido});
        }
    }

    /** Muestra en el área lateral todos los valores actuales del BCP. */
    private void actualizarBCP() {
        if (bcp == null) {
            vista.mostrarBCP("");
            return;
        }

        String texto = "ID: " + bcp.getIdProceso()
                + "\nEstado: " + bcp.getEstado()
                + "\nPrioridad: " + bcp.getPrioridad()
                + "\nPosición BCP: " + posicionBCP
                + "\nInicio memoria: " + bcp.getInicioMemoria()
                + "\nFin memoria: " + bcp.getFinMemoria()
                + "\nPC: " + bcp.getPc()
                + "\nIR: " + bcp.getIrToString()
                + "\nAC: " + bcp.getAc()
                + "\nAX: " + bcp.getAx()
                + "\nBX: " + bcp.getBx()
                + "\nCX: " + bcp.getCx()
                + "\nDX: " + bcp.getDx();
        vista.mostrarBCP(texto);
    }

    /**
     * Resalta en ambas tablas la instrucción señalada actualmente por el PC.
     */
    private void actualizarSeleccionProximaInstruccion() {
        if (cpu == null || memoria == null || !cpu.hayInstruccionPendiente()) {
            vista.limpiarSeleccionTablas();
            return;
        }
        int posicionMemoria = cpu.getPc();
        int filaInstruccion = posicionMemoria - memoria.getInicioUsuario();
        vista.seleccionarProximaInstruccion(filaInstruccion, posicionMemoria);
    }

    /** Elimina el programa y permite seleccionar una nueva memoria. */
    private void limpiar() {
        memoria = null;
        cpu = null;
        bcp = null;
        posicionBCP = -1;
        vista.getModeloInstrucciones().setRowCount(0);
        vista.getModeloMemoria().setRowCount(0);
        vista.limpiarSeleccionTablas();
        vista.mostrarBCP("");
        vista.setControlesProgramaHabilitados(false);
        vista.setSelectorMemoriaHabilitado(true);
        vista.setTitle("Mini PC");
    }

    /** Deshabilita los botones cuando el proceso ya no puede continuar. */
    private void deshabilitarEjecucion() {
        vista.getBtnEjecutar().setEnabled(false);
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnLimpiar().setEnabled(true);
    }

    /** Muestra un mensaje de error asociado a la ventana principal. */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Informa las líneas omitidas cuando un archivo se carga parcialmente.
     *
     * @param errores errores encontrados durante la carga.
     */
    private void mostrarErroresCarga(List<String> errores) {
        if (errores.isEmpty()) {
            return;
        }
        String mensaje = "El archivo se cargó, pero se omitieron estas líneas:\n\n"
                + String.join("\n", errores);
        JOptionPane.showMessageDialog(vista, mensaje, "Carga parcial", JOptionPane.WARNING_MESSAGE);
    }
}
