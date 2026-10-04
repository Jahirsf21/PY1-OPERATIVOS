package com.mycompany.t1operativos;

import com.mycompany.t1operativos.gui.Aplicacion;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import javax.swing.Timer;
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
    private Timer ejecucionAutomatica;
    private String[] instruccionPendiente;
    private int segundosPendientes;
    private boolean modoAutomatico;
    private boolean esperandoTeclado;
    private long inicioEsperaTeclado;

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
        this.ejecucionAutomatica = new Timer(1000, e -> ejecutarPaso());
        registrarEventos();
    }

    /**
     * Registra los eventos de la interfaz.
     */
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
        vista.addPropertyChangeListener("Entrada de teclado", evento -> recibirEntradaTeclado());
    }

    /**
     * Carga un archivo, distribuye la memoria y crea el contexto del proceso.
     *
     * @param archivo archivo ensamblador seleccionado.
     */
    private void cargarPrograma(File archivo) {
        if (modoAutomatico || esperandoTeclado) {
            return;
        }
        try {
            int tamañoTotal = vista.getMemoriaSeleccionada();
            if (tamañoTotal % 4 != 0) {
                throw new IllegalArgumentException("La memoria total debe ser múltiplo de 4 para dividirla en 25% y 75%");
            }
            List<String[]> instrucciones = cargador.cargarArchivo(archivo.getAbsolutePath());
            List<String> errores = cargador.getErrores();
            if (!errores.isEmpty()) {
                throw new IllegalArgumentException("No se cargó el archivo porque contiene errores:\n\n" + String.join("\n", errores));
            }
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
            reiniciarEjecucion();
            llenarTablaInstrucciones(instrucciones);
            actualizarTablaMemoria();
            actualizarBCP();
            actualizarSeleccionProximaInstruccion();
            vista.setSelectorMemoriaHabilitado(false);
            vista.setControlesProgramaHabilitados(true);
            vista.setTitle("Mini PC - " + archivo.getName());
            actualizarEstadoEjecucion();
        } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Avanza un segundo de CPU y completa la instrucción al alcanzar su peso.
     */
    private void ejecutarPaso() {
        if (cpu == null || bcp == null) {
            mostrarError("Primero debe cargar un programa.");
            return;
        }
        if (esperandoTeclado || "TERMINADO".equals(bcp.getEstado())) {
            return;
        }
        try {
            if (!hayInstruccionPendiente()) {
                finalizarProceso();
                actualizarContextoVista();
                return;
            }
            bcp.setEstadoEjecutando();
            bcp.setCpuActual(1);
            if (instruccionPendiente == null) {
                instruccionPendiente = memoria.leer(cpu.getPc());
                if (instruccionPendiente == null) {
                    throw new IllegalStateException("No existe una instrucción en la posición " + cpu.getPc() + ".");
                }
                cpu.cargarInstruccion(instruccionPendiente);
                if ("INT".equals(instruccionPendiente[0]) && "09H".equals(instruccionPendiente[1])) {
                    ejecutarSiguiente();
                    actualizarContextoVista();
                    return;
                }
                segundosPendientes = obtenerPeso(instruccionPendiente);
            }

            bcp.aumentarTiempoEmpleado();
            segundosPendientes--;
            if (segundosPendientes == 0) {
                ejecutarSiguiente();
                instruccionPendiente = null;
                if ("TERMINADO".equals(bcp.getEstado()) || !hayInstruccionPendiente()) {
                    finalizarProceso();
                }
            }
            actualizarContextoVista();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            finalizarProceso();
            actualizarContextoVista();
            vista.mostrarEstadoEjecucion("Ejecución finalizada con error.");
            vista.imprimirPantalla("Error: " + ex.getMessage());
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Inicia la ejecución automática del programa.
     */
    private void ejecutarTodo() {
        if (cpu == null || bcp == null) {
            mostrarError("Primero debe cargar un programa.");
            return;
        }
        if (esperandoTeclado || "TERMINADO".equals(bcp.getEstado())) {
            return;
        }
        modoAutomatico = true;
        bcp.setEstadoEjecutando();
        bcp.setCpuActual(1);
        deshabilitarControlesDuranteEjecucion();
        actualizarContextoVista();
        ejecucionAutomatica.start();
    }

    /**
     * Deshabilita los controles durante la ejecución.
     */
    private void deshabilitarControlesDuranteEjecucion() {
        vista.getBtnEjecutar().setEnabled(false);
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnCargarArchivo().setEnabled(false);
        vista.getBtnLimpiar().setEnabled(false);
    }

    /**
     * Obtiene el peso fijo de una instrucción.
     *
     * @param instruccion arreglo con el operador y sus operandos.
     * @return el peso fijo de la instrucción en segundos.
     */
    private int obtenerPeso(String[] instruccion) {
        switch (instruccion[0]) {
            case "MOV":
            case "INC":
            case "DEC":
            case "SWAP":
            case "PUSH":
            case "POP":
                return 1;
            case "LOAD":
            case "STORE":
            case "CMP":
            case "JMP":
            case "JE":
            case "JNE":
                return 2;
            case "ADD":
            case "SUB":
            case "PARAM":
                return 3;
            case "INT":
                if ("10H".equals(instruccion[1]) || "20H".equals(instruccion[1])) {
                    return 2;
                }
                if ("21H".equals(instruccion[1])) {
                    return 5;
                }
                throw new IllegalStateException("INT 09H no tiene un peso fijo; depende de la entrada del usuario.");
            default:
                throw new IllegalArgumentException("No se conoce el peso de " + instruccion[0] + ".");
        }
    }

    /**
     * Ejecuta la instrucción ubicada en el PC actual.
     *
     * @return {@code true} si la instrucción se completó; {@code false} si no se ejecutó o espera una entrada.
     */
    private boolean ejecutarSiguiente() {
        if (esperandoTeclado || "TERMINADO".equals(bcp.getEstado()) || !hayInstruccionPendiente()) {
            return false;
        }
        String[] instruccion = memoria.leer(cpu.getPc());
        if (instruccion == null) {
            throw new IllegalStateException("No existe una instrucción en la posición " + cpu.getPc() + ".");
        }
        cpu.cargarInstruccion(instruccion);
        int siguientePc = cpu.getPc() + 1;
        switch (instruccion[0]) {
            case "JMP":
                siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                break;
            case "JE":
                if (cpu.esIgual()) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                }
                break;
            case "JNE":
                if (!cpu.esIgual()) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                }
                break;
            case "PARAM":
                int cantidad = instruccion.length - 1;
                if (bcp.getCantidadEnPila() + cantidad > bcp.getCapacidadPila()) {
                    throw new IllegalStateException("Desbordamiento de pila: no hay espacio para los " + cantidad + " parámetros. Capacidad máxima de 5 valores.");
                }
                for (int i = 1; i < instruccion.length; i++) {
                    bcp.apilar(Integer.parseInt(instruccion[i]));
                }
                break;
            case "PUSH":
                bcp.apilar(cpu.leerRegistro(instruccion[1]));
                break;
            case "POP":
                cpu.escribirRegistro(instruccion[1], bcp.desapilar());
                break;
            case "INT":
                switch (instruccion[1]) {
                    case "20H":
                        bcp.setEstadoTerminado();
                        break;
                    case "10H":
                        vista.imprimirPantalla(cpu.getDx());
                        break;
                    case "09H":
                        solicitarEntradaTeclado();
                        return false;
                    default:
                        throw new IllegalArgumentException("La interrupción INT " + instruccion[1]
                                + " todavía no está implementada.");
                }
                break;
            default:
                cpu.ejecutarInstruccion(instruccion);
                break;
        }
        cpu.setPc(siguientePc);
        return true;
    }

    /**
     * Solicita una entrada de teclado y bloquea el proceso.
     */
    private void solicitarEntradaTeclado() {
        ejecucionAutomatica.stop();
        esperandoTeclado = true;
        inicioEsperaTeclado = System.nanoTime();
        bcp.setEstadoBloqueado();
        deshabilitarControlesDuranteEjecucion();
        vista.imprimirPantalla(">> Ingresar valor: ");
        vista.setEntradaTecladoHabilitada(true);
    }

    /**
     * Valida la entrada de teclado y completa la interrupción INT 09H.
     */
    private void recibirEntradaTeclado() {
        if (!esperandoTeclado) {
            return;
        }
        String texto = vista.getTextoEntradaTeclado().trim();
        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            vista.imprimirPantalla("Entrada inválida: se requiere un entero de 0 a 255.");
            vista.seleccionarEntradaTeclado();
            return;
        }
        if (valor < 0 || valor > 255) {
            vista.imprimirPantalla("Entrada fuera de rango: el valor debe estar entre 0 y 255.");
            vista.seleccionarEntradaTeclado();
            return;
        }

        registrarEsperaTeclado();
        cpu.setDx(Integer.toString(valor));
        cpu.avanzarPc();
        instruccionPendiente = null;
        segundosPendientes = 0;
        vista.setEntradaTecladoHabilitada(false);
        if (!hayInstruccionPendiente()) {
            finalizarProceso();
        } else {
            bcp.setEstadoListo();
            if (modoAutomatico) {
                ejecucionAutomatica.restart();
            } else {
                vista.setControlesProgramaHabilitados(true);
                vista.getBtnCargarArchivo().setEnabled(true);
            }
        }
        actualizarContextoVista();
    }

    /**
     * Suma la espera de teclado al tiempo del proceso.
     */
    private void registrarEsperaTeclado() {
        if (esperandoTeclado) {
            long segundos = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - inicioEsperaTeclado);
            bcp.aumentarTiempoEmpleado(segundos);
            esperandoTeclado = false;
            inicioEsperaTeclado = 0;
        }
    }

    /**
     * Finaliza el proceso y detiene la ejecución.
     */
    private void finalizarProceso() {
        registrarEsperaTeclado();
        bcp.setEstadoTerminado();
        instruccionPendiente = null;
        segundosPendientes = 0;
        deshabilitarEjecucion();
    }

    /**
     * Guarda el contexto actual y actualiza la vista.
     */
    private void actualizarContextoVista() {
        bcp.guardarContexto(cpu);
        actualizarBCP();
        actualizarTablaMemoria();
        actualizarSeleccionProximaInstruccion();
        actualizarEstadoEjecucion();
    }

    /**
     * Actualiza el estado de ejecución mostrado en la vista.
     */
    private void actualizarEstadoEjecucion() {
        if (bcp == null) {
            vista.mostrarEstadoEjecucion("Sin programa cargado.");
        } else if (esperandoTeclado) {
            vista.mostrarEstadoEjecucion("INT 09H: esperando una entrada del teclado.");
        } else if (instruccionPendiente != null) {
            vista.mostrarEstadoEjecucion("Ejecutando: " + parser.traducirInstruccion(instruccionPendiente));
        } else {
            vista.mostrarEstadoEjecucion("Proceso " + bcp.getEstado());
        }
    }

    /**
     * Restablece el modo de ejecución y limpia la consola.
     */
    private void reiniciarEjecucion() {
        ejecucionAutomatica.stop();
        modoAutomatico = false;
        esperandoTeclado = false;
        inicioEsperaTeclado = 0;
        instruccionPendiente = null;
        segundosPendientes = 0;
        vista.limpiarConsola();
        actualizarEstadoEjecucion();
    }

    /**
     * Calcula el destino de un salto dentro de los límites del proceso.
     *
     * @param desplazamiento desplazamiento relativo al PC actual.
     * @return la dirección de destino del salto.
     */
    private int calcularDestinoSalto(int desplazamiento) {
        long destino = (long) cpu.getPc() + desplazamiento;
        if (destino < bcp.getInicioMemoria() || destino > bcp.getFinMemoria()) {
            throw new IllegalStateException("Salto fuera de los límites del programa: dirección " + destino + ". Rango permitido: " + bcp.getInicioMemoria() + " a " + bcp.getFinMemoria() + ".");
        }
        return (int) destino;
    }

    /**
     * Comprueba si el PC se encuentra dentro del programa cargado.
     *
     * @return {@code true} si el PC está dentro de los límites del programa.
     */
    private boolean hayInstruccionPendiente() {
        return cpu.getPc() >= bcp.getInicioMemoria() && cpu.getPc() <= bcp.getFinMemoria();
    }

    /**
     * Llena la tabla con las instrucciones en ensamblador.
     *
     * @param instrucciones lista de instrucciones procesadas.
     */
    private void llenarTablaInstrucciones(List<String[]> instrucciones) {
        DefaultTableModel modelo = vista.getModeloInstrucciones();
        modelo.setRowCount(0);
        for (String[] instruccion : instrucciones) {
            modelo.addRow(new Object[]{parser.traducirInstruccion(instruccion)});
        }
    }

    /**
     * Escribe los atributos del BCP en el kernel.
     */
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
            "flag",
            "pila1",
            "pila2",
            "pila3",
            "pila4",
            "pila5",
            "punteroPila",
            "cpuActual",
            "tiempoInicio",
            "tiempoFinal",
            "tiempoTotalSegundos",
            "direccionSiguienteBCP"
        };
        String[] valores = {String.valueOf(bcp.getIdProceso()), bcp.getEstado(),
            String.valueOf(bcp.getPrioridad()), String.valueOf(bcp.getPc()),
            String.valueOf(bcp.getInicioMemoria()), String.valueOf(bcp.getFinMemoria()),
            bcp.getIrToString(), String.valueOf(bcp.getAc()), String.valueOf(bcp.getAx()),
            String.valueOf(bcp.getBx()), String.valueOf(bcp.getCx()), bcp.getDx(),
            String.valueOf(bcp.esIgual()), String.valueOf(bcp.getValorPila(0)),
            String.valueOf(bcp.getValorPila(1)), String.valueOf(bcp.getValorPila(2)),
            String.valueOf(bcp.getValorPila(3)), String.valueOf(bcp.getValorPila(4)),
            String.valueOf(bcp.getPunteroPila()), String.valueOf(bcp.getCpuActual()),
            String.valueOf(bcp.getTiempoInicio()), String.valueOf(bcp.getTiempoFinal()),
            String.valueOf(bcp.getTiempoTotalSegundos()), String.valueOf(bcp.getDireccionSiguienteBCP())
        };
        for (int i = 0; i < nombres.length; i++) {
            memoria.escribirKernel(posicionBCP + i, nombres[i], valores[i]);
        }
    }

    /**
     * Actualiza la tabla con el contenido de la memoria.
     */
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

    /**
     * Actualiza la información del BCP en la vista.
     */
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
                + "\nDX: " + bcp.getDx()
                + "\nFlag: " + bcp.esIgual()
                + "\nPila 1: " + bcp.getValorPila(0)
                + "\nPila 2: " + bcp.getValorPila(1)
                + "\nPila 3: " + bcp.getValorPila(2)
                + "\nPila 4: " + bcp.getValorPila(3)
                + "\nPila 5: " + bcp.getValorPila(4)
                + "\nPuntero pila: " + bcp.getPunteroPila()
                + "\nCPU actual: " + bcp.getCpuActual()
                + "\nInicio ejecución: " + (bcp.getTiempoInicio() == null ? "-" : bcp.getTiempoInicio().toString())
                + "\nFin ejecución: " + (bcp.getTiempoFinal() == null ? "-" : bcp.getTiempoFinal().toString())
                + "\nDuración: " + bcp.getTiempoTotalSegundos() + " s";
        vista.mostrarBCP(texto);
    }

    /**
     * Resalta en ambas tablas la instrucción señalada actualmente por el PC.
     */
    private void actualizarSeleccionProximaInstruccion() {
        if (cpu == null || memoria == null || bcp == null || "TERMINADO".equals(bcp.getEstado()) || !hayInstruccionPendiente()) {
            vista.limpiarSeleccionTablas();
            return;
        }
        int posicionMemoria = cpu.getPc();
        int filaInstruccion = posicionMemoria - bcp.getInicioMemoria();
        vista.seleccionarProximaInstruccion(filaInstruccion, posicionMemoria);
    }

    /**
     * Elimina el programa cargado y restablece la interfaz.
     */
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
        vista.getBtnCargarArchivo().setEnabled(true);
        reiniciarEjecucion();
    }

    /**
     * Detiene la ejecución y deshabilita sus controles.
     */
    private void deshabilitarEjecucion() {
        ejecucionAutomatica.stop();
        modoAutomatico = false;
        esperandoTeclado = false;
        vista.setEntradaTecladoHabilitada(false);
        vista.getBtnCargarArchivo().setEnabled(true);
        vista.getBtnEjecutar().setEnabled(false);
        vista.getBtnPasoAPaso().setEnabled(false);
        vista.getBtnLimpiar().setEnabled(true);
    }

    /**
     * Muestra un mensaje de error en la interfaz.
     *
     * @param mensaje descripción del error que se desea mostrar.
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

}
