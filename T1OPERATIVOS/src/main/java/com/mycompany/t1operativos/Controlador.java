package com.mycompany.t1operativos;

import com.mycompany.t1operativos.gui.Aplicacion;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * Conecta la vista con el gestor de procesos.
 *
 * @author deislher sánchez funez
 */
public class Controlador {
    private Aplicacion vista;
    private GestorProcesos gestor;
    private Parser parser;
    private Timer ejecucionAutomatica;
    private boolean modoAutomatico;
    private Proceso solicitudTecladoVisible;

    /**
     * Construye el controlador y registra los eventos.
     *
     * @param vista ventana principal.
     */
    public Controlador(Aplicacion vista) {
        if (vista == null) {
            throw new IllegalArgumentException("La vista no puede ser nula.");
        }
        this.vista = vista;
        this.gestor = null;
        this.parser = new Parser();
        this.modoAutomatico = false;
        this.solicitudTecladoVisible = null;
        this.ejecucionAutomatica = new Timer(1000, e -> {
            if (modoAutomatico) {
                ejecutarPaso();
            }
        });
        registrarEventos();
    }

    /**
     * Registra los eventos de la vista.
     */
    private void registrarEventos() {
        vista.addPropertyChangeListener("Archivos cargados", evento -> {
            if (!(evento.getNewValue() instanceof List<?> seleccion)) {
                mostrarError("La selección debe contener una lista de archivos.");
                return;
            }
            for (Object elemento : seleccion) {
                if (elemento != null && !(elemento instanceof File)) {
                    mostrarError("La selección contiene un elemento que no es un archivo.");
                    return;
                }
            }
            @SuppressWarnings("unchecked")
            List<File> archivos = (List<File>) seleccion;
            cargarProgramas(archivos);
        });
        vista.getBtnPasoAPaso().addActionListener(e -> {
            if (!modoAutomatico) {
                ejecutarPaso();
            }
        });
        vista.getBtnEjecutar().addActionListener(e -> ejecutarTodo());
        vista.getBtnLimpiar().addActionListener(e -> limpiar());
        vista.addPropertyChangeListener("Entrada de teclado", evento -> recibirEntradaTeclado());
    }

    /**
     * Entrega los archivos al gestor.
     *
     * @param archivos selección de la vista.
     */
    private void cargarProgramas(List<File> archivos) {
        GestorProcesos destino = gestor;
        try {
            if (destino == null || destino.getProcesosRegistrados().isEmpty()) {
                destino = new GestorProcesos(vista.getMemoriaSeleccionada(), vista.getDiscoSeleccionado());
            }
            int cantidadAnterior = destino.getProcesosRegistrados().size();
            List<String> mensajes = destino.cargarProgramas(archivos);
            if (destino.getProcesosRegistrados().size() > cantidadAnterior) {
                gestor = destino;
                actualizarVista();
            }
            if (!mensajes.isEmpty()) {
                String texto = "";
                for (int i = 0; i < mensajes.size(); i++) {
                    if (i > 0) {
                        texto = texto + "\n";
                    }
                    texto = texto + mensajes.get(i);
                }
                mostrarError(texto);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (destino != null && !destino.getProcesosRegistrados().isEmpty()) {
                gestor = destino;
            }
            ejecucionAutomatica.stop();
            modoAutomatico = false;
            actualizarVista();
            vista.imprimirPantalla("Error de carga: " + ex.getMessage());
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Solicita un paso y muestra su resultado.
     */
    private void ejecutarPaso() {
        if (gestor == null) {
            mostrarError("Primero debe cargar programas.");
            return;
        }
        if (!gestor.puedeEjecutar()) {
            actualizarVista();
            return;
        }
        try {
            String salida = gestor.ejecutarPaso();
            if (salida != null) {
                vista.imprimirPantalla(salida);
            }
            actualizarVista();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ejecucionAutomatica.stop();
            modoAutomatico = false;
            actualizarVista();
            vista.mostrarEstadoEjecucion("Ejecución detenida con error.");
            vista.imprimirPantalla("Error: " + ex.getMessage());
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Inicia el modo automático.
     */
    private void ejecutarTodo() {
        if (gestor == null) {
            mostrarError("Primero debe cargar programas.");
            return;
        }
        if (!gestor.puedeEjecutar()) {
            actualizarVista();
            return;
        }
        modoAutomatico = true;
        actualizarControles();
    }

    /**
     * Entrega la entrada activa al gestor.
     */
    private void recibirEntradaTeclado() {
        if (gestor == null || solicitudTecladoVisible == null) {
            return;
        }
        if (gestor.getSolicitudTeclado() != solicitudTecladoVisible) {
            vista.imprimirPantalla("Error de teclado: el aviso no corresponde al solicitante actual.");
            actualizarSolicitudTeclado();
            return;
        }
        try {
            gestor.recibirEntradaTeclado(vista.getTextoEntradaTeclado());
            vista.setEntradaTecladoHabilitada(false);
            solicitudTecladoVisible = null;
            actualizarVista();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            vista.imprimirPantalla(ex.getMessage());
            vista.seleccionarEntradaTeclado();
        }
    }

    /**
     * Limpia el dominio y la presentación.
     */
    private void limpiar() {
        ejecucionAutomatica.stop();
        modoAutomatico = false;
        if (gestor != null) {
            gestor.limpiar();
        }
        solicitudTecladoVisible = null;
        vista.limpiarConsola();
        vista.setTitle("Mini PC");
        actualizarVista();
    }

    /**
     * Actualiza los datos de la vista.
     */
    private void actualizarVista() {
        actualizarTablaMemoria();
        actualizarTablaDisco();
        actualizarTablaTrabajos();
        actualizarTablaProcesos();
        actualizarEstadisticas();
        actualizarBCP();
        actualizarSeleccionProximaInstruccion();
        actualizarEstadoEjecucion();
        actualizarSolicitudTeclado();
        actualizarControles();
    }

    /**
     * Muestra el contenido de RAM.
     */
    private void actualizarTablaMemoria() {
        DefaultTableModel modelo = vista.getModeloMemoria();
        modelo.setRowCount(0);
        if (gestor == null || gestor.getProcesosRegistrados().isEmpty()) {
            return;
        }
        Memoria memoria = gestor.getMemoria();
        for (int posicion = 0; posicion < memoria.getTamañoTotal(); posicion++) {
            String contenido = "";
            String[] valor = memoria.leer(posicion);
            if (valor != null) {
                if (memoria.esDireccionKernel(posicion)) {
                    contenido = valor[0] + ": " + valor[1];
                } else {
                    contenido = parser.traducirInstruccion(valor);
                }
            }
            modelo.addRow(new Object[]{posicion, contenido});
        }
    }

    /**
     * Muestra el contenido del disco.
     */
    private void actualizarTablaDisco() {
        DefaultTableModel modelo = vista.getModeloDisco();
        modelo.setRowCount(0);
        if (gestor == null || gestor.getProcesosRegistrados().isEmpty()) {
            return;
        }
        Disco disco = gestor.getDisco();
        for (int posicion = 0; posicion < disco.getTamañoTotal(); posicion++) {
            String contenido = "";
            String[] valor = disco.leer(posicion);
            if (disco.esDireccionIndice(posicion)) {
                if (valor == null) {
                    contenido = "Índice (libre)";
                } else {
                    contenido = "[" + valor[0] + ", " + valor[1] + ", " + valor[2] + "]";
                }
            } else if (disco.esDireccionMemoriaVirtual(posicion)) {
                contenido = "Memoria virtual (sin uso)";
            } else if (valor != null) {
                if (valor.length == 1) {
                    if (valor[0].isEmpty()) {
                        contenido = "Archivo vacío";
                    } else {
                        contenido = "Dato numérico: " + valor[0];
                    }
                } else {
                    contenido = parser.traducirInstruccion(valor);
                }
            }
            modelo.addRow(new Object[]{posicion, contenido});
        }
    }

    /**
     * Muestra los trabajos pendientes de RAM.
     */
    private void actualizarTablaTrabajos() {
        DefaultTableModel modelo = vista.getModeloTrabajos();
        modelo.setRowCount(0);
        if (gestor != null) {
            for (Proceso proceso : gestor.getProcesosRegistrados()) {
                BCP bcp = proceso.getBCP();
                if ("NUEVO".equals(bcp.getEstado())) {
                    modelo.addRow(new Object[]{bcp.getIdProceso(), bcp.getEstado()});
                }
            }
        }
    }

    /**
     * Muestra los procesos cargados en RAM.
     */
    private void actualizarTablaProcesos() {
        DefaultTableModel modelo = vista.getModeloProcesos();
        modelo.setRowCount(0);
        if (gestor != null) {
            for (Proceso proceso : gestor.getProcesosRegistrados()) {
                if (proceso.getPosicionBCP() >= 0) {
                    modelo.addRow(new Object[]{proceso.getBCP().getIdProceso(), proceso.getBCP().getEstado()});
                }
            }
        }
    }

    /**
     * Muestra los tiempos de todos los procesos registrados.
     */
    private void actualizarEstadisticas() {
        DefaultTableModel modelo = vista.getModeloEstadisticas();
        modelo.setRowCount(0);
        if (gestor != null) {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Proceso proceso : gestor.getProcesosRegistrados()) {
                BCP bcp = proceso.getBCP();
                String inicio = "-";
                String fin = "-";
                if (bcp.getTiempoInicio() != null) {
                    inicio = bcp.getTiempoInicio().format(formato);
                }
                if (bcp.getTiempoFinal() != null) {
                    fin = bcp.getTiempoFinal().format(formato);
                }
                modelo.addRow(new Object[]{bcp.getIdProceso(), inicio, fin, bcp.getTiempoTotalSegundos()});
            }
        }
    }

    /**
     * Muestra únicamente el BCP con CPU.
     */
    private void actualizarBCP() {
        Proceso proceso = null;
        if (gestor != null) {
            proceso = gestor.getActual();
        }
        if (proceso == null) {
            vista.mostrarBCP("");
            return;
        }
        BCP bcp = proceso.getBCP();
        String inicio = "-";
        String fin = "-";
        if (bcp.getTiempoInicio() != null) {
            inicio = bcp.getTiempoInicio().toString();
        }
        if (bcp.getTiempoFinal() != null) {
            fin = bcp.getTiempoFinal().toString();
        }
        String texto = "ID: " + bcp.getIdProceso()
                + "\nEstado: " + bcp.getEstado()
                + "\nPrioridad: " + bcp.getPrioridad()
                + "\nPosición BCP: " + proceso.getPosicionBCP()
                + "\nInicio memoria: " + bcp.getInicioMemoria()
                + "\nFin memoria: " + bcp.getFinMemoria()
                + "\nPC: " + bcp.getPc()
                + "\nIR: " + bcp.getIrToString()
                + "\nAC: " + bcp.getAc()
                + "\nAX: " + bcp.getAx()
                + "\nBX: " + bcp.getBx()
                + "\nCX: " + bcp.getCx()
                + "\nDX: " + bcp.getDx()
                + "\nAH: " + bcp.getAh()
                + "\nAL: " + bcp.getAl()
                + "\nFlag: " + bcp.esIgual()
                + "\nPila 1: " + bcp.getValorPila(0)
                + "\nPila 2: " + bcp.getValorPila(1)
                + "\nPila 3: " + bcp.getValorPila(2)
                + "\nPila 4: " + bcp.getValorPila(3)
                + "\nPila 5: " + bcp.getValorPila(4)
                + "\nPuntero pila: " + bcp.getPunteroPila()
                + "\nCPU actual: " + bcp.getCpuActual()
                + "\nInicio ejecución: " + inicio
                + "\nFin ejecución: " + fin
                + "\nDuración: " + bcp.getTiempoTotalSegundos() + " s"
                + "\nSiguiente BCP: " + bcp.getDireccionSiguienteBCP()
                + "\nArchivos abiertos: " + bcp.getArchivosAbiertos();
        vista.mostrarBCP(texto);
    }

    /**
     * Resalta el PC del proceso actual.
     */
    private void actualizarSeleccionProximaInstruccion() {
        Proceso actual = null;
        if (gestor != null) {
            actual = gestor.getActual();
        }
        if (actual == null) {
            vista.limpiarSeleccionTablas();
        } else {
            vista.seleccionarProximaInstruccion(gestor.getCPU().getPc());
        }
    }

    /**
     * Muestra el estado de ejecución.
     */
    private void actualizarEstadoEjecucion() {
        if (gestor == null || gestor.getProcesosRegistrados().isEmpty()) {
            vista.mostrarEstadoEjecucion("Sin programas cargados.");
            return;
        }
        Proceso actual = gestor.getActual();
        Proceso solicitud = gestor.getSolicitudTeclado();
        String estado;
        if (actual != null) {
            estado = "Proceso " + actual.getBCP().getIdProceso() + " ejecutando";
            String[] instruccion = gestor.getInstruccionPendiente();
            if (instruccion != null) {
                estado += ": " + parser.traducirInstruccion(instruccion);
            }
        } else if (gestor.puedeEjecutar()) {
            estado = "CPU libre: hay procesos listos.";
        } else if (gestor.hayProcesosSinTerminar()) {
            estado = "Esperando teclado o admisión en memoria.";
        } else {
            estado = "Todos los procesos terminaron.";
        }
        if (solicitud != null) {
            estado += " | Proceso " + solicitud.getBCP().getIdProceso() + ": INT 09H, esperando teclado.";
        }
        vista.mostrarEstadoEjecucion(estado);
    }

    /**
     * Abre el aviso del solicitante pendiente.
     */
    private void actualizarSolicitudTeclado() {
        Proceso solicitud = null;
        if (gestor != null) {
            solicitud = gestor.getSolicitudTeclado();
        }
        if (solicitud == solicitudTecladoVisible) {
            return;
        }
        if (solicitudTecladoVisible != null) {
            vista.setEntradaTecladoHabilitada(false);
        }
        solicitudTecladoVisible = null;
        if (solicitud != null) {
            vista.mostrarSolicitudTeclado(solicitud.getBCP().getIdProceso());
            solicitudTecladoVisible = solicitud;
        }
    }

    /**
     * Ajusta los controles y la actividad del Timer.
     */
    private void actualizarControles() {
        boolean hayProcesosRegistrados = gestor != null && !gestor.getProcesosRegistrados().isEmpty();
        boolean ejecutable = hayProcesosRegistrados && gestor.puedeEjecutar();
        if (!hayProcesosRegistrados || !gestor.hayProcesosSinTerminar()) {
            modoAutomatico = false;
        }
        if (modoAutomatico && ejecutable) {
            if (!ejecucionAutomatica.isRunning()) {
                ejecucionAutomatica.start();
            }
        } else {
            ejecucionAutomatica.stop();
        }
        vista.getBtnEjecutar().setEnabled(ejecutable && !modoAutomatico);
        vista.getBtnPasoAPaso().setEnabled(ejecutable && !modoAutomatico);
        vista.getBtnCargarArchivo().setEnabled(true);
        vista.getBtnLimpiar().setEnabled(hayProcesosRegistrados && (!modoAutomatico || !ejecutable));
        vista.setSelectorMemoriaHabilitado(!hayProcesosRegistrados);
        vista.setSelectorDiscoHabilitado(!hayProcesosRegistrados);
    }

    /**
     * Muestra un mensaje de error.
     *
     * @param mensaje descripción del error.
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
