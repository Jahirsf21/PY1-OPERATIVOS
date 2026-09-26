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
                throw new IllegalArgumentException("La memoria total debe ser múltiplo de 4 para dividirla en 25% y 75%");
            }
            List<String[]> instrucciones = cargador.cargarArchivo(archivo.getAbsolutePath());
            int tamañoKernel = tamañoTotal / 4;
            int tamañoUsuario = tamañoTotal - tamañoKernel;
            if (instrucciones.isEmpty() || instrucciones.size() > tamañoUsuario) {
                throw new IllegalArgumentException("El programa debe contener instrucciones y caber en el espacio de usuario.");
            }
            Memoria nuevaMemoria = new Memoria(tamañoTotal, tamañoKernel);
            for (int i = 0; i < instrucciones.size(); i++) {
                nuevaMemoria.escribirUsuario(tamañoKernel + i, instrucciones.get(i));
            }
            int finPrograma = tamañoKernel + instrucciones.size() - 1;
            memoria = nuevaMemoria;
            cpu = new CPU();
            cpu.setPc(tamañoKernel);
            bcp = new BCP(1, 1, tamañoKernel, finPrograma);
            bcp.setEstadoListo();
            bcp.guardarContexto(cpu);
            posicionBCP = 0;
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
            if (ejecutarSiguiente()) {
                bcp.guardarContexto(cpu);
            }
            if (!hayInstruccionPendiente()) {
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
            actualizarTablaMemoria();
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
            while (ejecutarSiguiente()) {
            }
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
            actualizarTablaMemoria();
            mostrarError(ex.getMessage());
        }
    }

    /** Ejecuta la instrucción ubicada en el PC actual. */
    private boolean ejecutarSiguiente() {
        if (!hayInstruccionPendiente()) {
            return false;
        }
        String[] instruccion = memoria.leer(cpu.getPc());
        if (instruccion == null) {
            throw new IllegalStateException("No existe una instrucción en la posición " + cpu.getPc() + ".");
        }
        cpu.ejecutarInstruccion(instruccion);
        cpu.avanzarPc();
        return true;
    }

    /** Indica si el PC se encuentra dentro del programa cargado. */
    private boolean hayInstruccionPendiente() {
        return cpu.getPc() >= bcp.getInicioMemoria() && cpu.getPc() <= bcp.getFinMemoria();
    }

    /** Llena la tabla con las instrucciones en formato ensamblador. */
    private void llenarTablaInstrucciones(List<String[]> instrucciones) {
        DefaultTableModel modelo = vista.getModeloInstrucciones();
        modelo.setRowCount(0);
        for (String[] instruccion : instrucciones) {
            modelo.addRow(new Object[]{parser.traducirInstruccion(instruccion)});
        }
    }

    /** Escribe los atributos del BCP en el kernel. */
    private void escribirBCPEnMemoria() {
        String[] nombres = {
            "idProceso",
            "estado",
            "prioridad",
            "pc",
            "inicioMemoria",
            "finMemoria",
            "ir",
            "ac",
            "ax",
            "bx",
            "cx",
            "dx",
            "punteroPila",
            "cpuActual",
            "tiempoInicio",
            "tiempoFinal",
            "tiempoEmpleadoSegundos",
            "direccionSiguienteBCP"
        };
        String[] valores = {String.valueOf(bcp.getIdProceso()), bcp.getEstado(),
            String.valueOf(bcp.getPrioridad()), String.valueOf(bcp.getPc()),
            String.valueOf(bcp.getInicioMemoria()), String.valueOf(bcp.getFinMemoria()),
            bcp.getIrToString(), String.valueOf(bcp.getAc()), String.valueOf(bcp.getAx()),
            String.valueOf(bcp.getBx()), String.valueOf(bcp.getCx()), bcp.getDx(),
            String.valueOf(bcp.getPunteroPila()), String.valueOf(bcp.getCpuActual()),
            String.valueOf(bcp.getTiempoInicio()), String.valueOf(bcp.getTiempoFinal()),
            String.valueOf(bcp.getTiempoEmpleadoSegundos()), String.valueOf(bcp.getDireccionSiguienteBCP())
        };
        for (int i = 0; i < 18; i++) {
            memoria.escribirKernel(posicionBCP + i, nombres[i], valores[i]);
        }
    }

    /** Actualiza la tabla que representa todas las posiciones de memoria. */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);
        if (memoria == null) {
            return;
        }
        escribirBCPEnMemoria();
        for (int posicion = 0; posicion < memoria.getTamañoTotal(); posicion++) {
            String contenido = "";
            if (memoria.esDireccionKernel(posicion)) {
                String[] atributo = memoria.leer(posicion);
                if (atributo != null) {
                    contenido = atributo[0] + ": " + atributo[1];
                }
            } else {
                String[] instruccion = memoria.leer(posicion);
                if (instruccion != null) {
                    contenido = parser.traducirInstruccion(instruccion);
                }
            }
            modelo.addRow(new Object[] { posicion, contenido });
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
        if (cpu == null || memoria == null || bcp == null || !hayInstruccionPendiente()) {
            vista.limpiarSeleccionTablas();
            return;
        }
        int posicionMemoria = cpu.getPc();
        int filaInstruccion = posicionMemoria - bcp.getInicioMemoria();
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
