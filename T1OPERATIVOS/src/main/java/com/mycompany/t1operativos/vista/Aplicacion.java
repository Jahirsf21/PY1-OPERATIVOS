package com.mycompany.t1operativos.vista;

import com.mycompany.t1operativos.controlador.Controlador;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;

/**
 * Ventana principal de Mini PC.
 *
 * @author deislher sánchez funez
 */
public class Aplicacion extends JFrame {

    private JButton btnEjecutar;
    private JButton btnPasoAPaso;
    private JButton btnLimpiar;
    private JButton btnCargarArchivo;
    /** Botón de estadísticas. */
    private JButton btnEstadisticas;
    /** Modal de estadísticas. */
    private JDialog ventanaEstadisticas;
    /** Botón de configuración. */
    private JButton btnConfigurarSimulador;
    /** Modal de configuración. */
    private JDialog ventanaConfiguracion;

    private JTable tablaMemoria;
    private JTable tablaDisco;
    /** Tabla de trabajos pendientes. */
    private JTable tablaTrabajos;
    /** Tabla de procesos cargados en RAM. */
    private JTable tablaProcesos;
    /** Tabla de tiempos por proceso. */
    private JTable tablaEstadisticas;
    private JTextArea areaBCP;
    private JSpinner selectorMemoria;
    private JLabel lblDistribucionMemoria;
    private JSpinner selectorDisco;
    private JLabel lblDistribucionDisco;
    /** Tamaño elegido de memoria virtual. */
    private JSpinner selectorMemoriaVirtual;
    private JTextArea pantalla;
    private int inicioEntradaConsola = -1;
    private boolean actualizandoConsola;
    private DefaultTableModel modeloMemoria;
    private DefaultTableModel modeloDisco;
    /** Filas de trabajos pendientes. */
    private DefaultTableModel modeloTrabajos;
    /** Filas de procesos cargados en RAM. */
    private DefaultTableModel modeloProcesos;
    /** Filas de estadísticas. */
    private DefaultTableModel modeloEstadisticas;
    /**
     * Construye e inicializa la ventana principal.
     */
    public Aplicacion() {
        initComponents();
    }

    /**
     * Configura la ventana y sus componentes.
     */
    private void initComponents() {
        setTitle("Mini PC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelTablas(), BorderLayout.CENTER);
        add(crearPanelBCP(), BorderLayout.EAST);
        add(crearPanelConsola(), BorderLayout.SOUTH);
        ventanaEstadisticas = new JDialog(this, "Estadísticas", true);
        ventanaEstadisticas.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        ventanaEstadisticas.setContentPane(crearPanelEstadisticas());
        ventanaEstadisticas.setSize(650, 350);
        ventanaConfiguracion = new JDialog(this, "Configurar Simulador", true);
        ventanaConfiguracion.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        ventanaConfiguracion.setContentPane(crearPanelConfiguracion());
        ventanaConfiguracion.setSize(620, 260);
        ventanaConfiguracion.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                confirmarConfiguracion();
            }
        });
    }

    /**
     * Crea el panel de controles del programa.
     *
     * @return el panel superior.
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        btnEjecutar = new JButton("Ejecutar");
        btnPasoAPaso = new JButton("Paso a paso");
        btnLimpiar = new JButton("Limpiar");
        btnEjecutar.setEnabled(false);
        btnPasoAPaso.setEnabled(false);
        btnLimpiar.setEnabled(false);
        panelBotonesAccion.add(btnEjecutar);
        panelBotonesAccion.add(btnPasoAPaso);
        panelBotonesAccion.add(btnLimpiar);
        btnEstadisticas = new JButton("Estadísticas");
        btnEstadisticas.addActionListener(e -> mostrarEstadisticas());
        panelBotonesAccion.add(btnEstadisticas);

        JPanel panelCargarArchivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnCargarArchivo = new JButton("Cargar archivos");
        btnCargarArchivo.addActionListener(e -> abrirSelectorArchivos());
        panelCargarArchivo.add(btnCargarArchivo);
        btnConfigurarSimulador = new JButton("Configurar Simulador");
        btnConfigurarSimulador.addActionListener(e -> mostrarConfiguracion());
        panelCargarArchivo.add(btnConfigurarSimulador);

        panel.add(panelBotonesAccion);
        panel.add(panelCargarArchivo);

        return panel;
    }

    /**
     * Crea los controles de configuración.
     *
     * @return panel de tamaños y distribución.
     */
    private JPanel crearPanelConfiguracion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel campos = new JPanel(new GridLayout(3, 2, 10, 10));
        selectorMemoria = new JSpinner(new SpinnerNumberModel(256, 128, null, 4));
        selectorDisco = new JSpinner(new SpinnerNumberModel(512, 256, null, 4));
        int tamañoDisco = (Integer) selectorDisco.getValue();
        int maximoVirtual = tamañoDisco - tamañoDisco / 20;
        selectorMemoriaVirtual = new JSpinner(new SpinnerNumberModel(64, 0, maximoVirtual, 4));
        configurarSelector(selectorMemoria);
        configurarSelector(selectorDisco);
        configurarSelector(selectorMemoriaVirtual);
        campos.add(new JLabel("Memoria principal (RAM):"));
        campos.add(selectorMemoria);
        campos.add(new JLabel("Disco:"));
        campos.add(selectorDisco);
        campos.add(new JLabel("Memoria virtual:"));
        campos.add(selectorMemoriaVirtual);
        panel.add(campos, BorderLayout.NORTH);

        JPanel distribucion = new JPanel(new GridLayout(2, 1, 0, 5));
        lblDistribucionMemoria = new JLabel();
        lblDistribucionDisco = new JLabel();
        distribucion.add(lblDistribucionMemoria);
        distribucion.add(lblDistribucionDisco);
        panel.add(distribucion, BorderLayout.CENTER);
        selectorMemoria.addChangeListener(e -> actualizarDistribucionMemoria());
        selectorDisco.addChangeListener(e -> {
            actualizarLimiteMemoriaVirtual();
            actualizarDistribucionDisco();
        });
        selectorMemoriaVirtual.addChangeListener(e -> actualizarDistribucionDisco());
        actualizarDistribucionMemoria();
        actualizarDistribucionDisco();

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton aceptar = new JButton("Aceptar");
        aceptar.addActionListener(e -> confirmarConfiguracion());
        botones.add(aceptar);
        panel.add(botones, BorderLayout.SOUTH);
        ventanaConfiguracion.getRootPane().setDefaultButton(aceptar);
        return panel;
    }

    /**
     * Muestra la configuración.
     */
    private void mostrarConfiguracion() {
        ventanaConfiguracion.setLocationRelativeTo(this);
        ventanaConfiguracion.setVisible(true);
    }

    /**
     * Confirma los tamaños y cierra el modal.
     */
    private void confirmarConfiguracion() {
        try {
            getMemoriaSeleccionada();
            getDiscoSeleccionado();
            getMemoriaVirtualSeleccionada();
            ventanaConfiguracion.setVisible(false);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(ventanaConfiguracion, ex.getMessage(), "Configuración inválida", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea las tablas de recursos, trabajos y procesos.
     *
     * @return el panel con las tablas.
     */
    private JPanel crearPanelTablas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel recursos = new JPanel(new BorderLayout(10, 0));

        modeloMemoria = new DefaultTableModel(new Object[]{"Posición", "Valor en memoria"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaMemoria = new JTable(modeloMemoria);
        tablaMemoria.getTableHeader().setReorderingAllowed(false);
        tablaMemoria.setCellSelectionEnabled(false);
        tablaMemoria.setRowSelectionAllowed(true);
        tablaMemoria.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMemoria.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchosMemoria = {60, 260};
        for (int i = 0; i < anchosMemoria.length; i++) {
            TableColumn columna = tablaMemoria.getColumnModel().getColumn(i);
            columna.setMinWidth(anchosMemoria[i]);
            columna.setMaxWidth(anchosMemoria[i]);
            columna.setPreferredWidth(anchosMemoria[i]);
            columna.setWidth(anchosMemoria[i]);
            columna.setResizable(false);
        }
        tablaMemoria.setPreferredScrollableViewportSize(new Dimension(tablaMemoria.getColumnModel().getTotalColumnWidth(), tablaMemoria.getPreferredScrollableViewportSize().height));
        JPanel panelMemoria = new JPanel(new BorderLayout());
        panelMemoria.setBorder(BorderFactory.createTitledBorder("RAM"));
        panelMemoria.add(new JScrollPane(tablaMemoria), BorderLayout.CENTER);
        recursos.add(panelMemoria, BorderLayout.WEST);

        modeloDisco = new DefaultTableModel(new Object[]{"Posición", "Valor en disco"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaDisco = new JTable(modeloDisco);
        tablaDisco.getTableHeader().setReorderingAllowed(false);
        tablaDisco.setCellSelectionEnabled(false);
        tablaDisco.setRowSelectionAllowed(true);
        tablaDisco.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaDisco.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn columnaPosicion = tablaDisco.getColumnModel().getColumn(0);
        columnaPosicion.setMinWidth(60);
        columnaPosicion.setMaxWidth(60);
        columnaPosicion.setPreferredWidth(60);
        columnaPosicion.setResizable(false);
        tablaDisco.getColumnModel().getColumn(1).setPreferredWidth(260);
        tablaDisco.setPreferredScrollableViewportSize(new Dimension(320, tablaDisco.getPreferredScrollableViewportSize().height));
        JPanel panelDisco = new JPanel(new BorderLayout());
        panelDisco.setBorder(BorderFactory.createTitledBorder("Disco"));
        panelDisco.add(new JScrollPane(tablaDisco), BorderLayout.CENTER);
        recursos.add(panelDisco, BorderLayout.CENTER);
        panel.add(recursos, BorderLayout.CENTER);
        JPanel listas = new JPanel(new GridLayout(1, 2, 10, 0));
        listas.add(crearPanelTrabajos());
        listas.add(crearPanelProcesos());
        panel.add(listas, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Permite escribir tamaños enteros dentro de sus límites.
     *
     * @param selector control del recurso.
     */
    private void configurarSelector(JSpinner selector) {
        JFormattedTextField campo = ((JSpinner.DefaultEditor) selector.getEditor()).getTextField();
        NumberFormatter formato = new NumberFormatter(new DecimalFormat("0")) {
            @Override
            public Object stringToValue(String texto) throws ParseException {
                int tamaño;
                try {
                    tamaño = Integer.parseInt(texto.trim());
                } catch (NumberFormatException ex) {
                    throw new ParseException("Debe ingresar un tamaño entero.", 0);
                }
                int minimo = (Integer) ((SpinnerNumberModel) selector.getModel()).getMinimum();
                if (tamaño < minimo) {
                    throw new ParseException("El tamaño debe ser al menos " + minimo + " posiciones.", 0);
                }
                Integer maximo = (Integer) ((SpinnerNumberModel) selector.getModel()).getMaximum();
                if (maximo != null && tamaño > maximo) {
                    throw new ParseException("El tamaño no puede superar " + maximo + " posiciones.", 0);
                }
                return tamaño;
            }
        };
        formato.setOverwriteMode(false);
        campo.setFormatterFactory(new DefaultFormatterFactory(formato));
        campo.setFocusLostBehavior(JFormattedTextField.COMMIT);
        campo.setEditable(true);
    }

    /**
     * Crea la tabla de trabajos pendientes.
     *
     * @return panel de trabajos y estados.
     */
    private JPanel crearPanelTrabajos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Lista de Trabajos"));
        modeloTrabajos = new DefaultTableModel(new Object[]{"Trabajos", "Estados"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaTrabajos = new JTable(modeloTrabajos);
        tablaTrabajos.getTableHeader().setReorderingAllowed(false);
        tablaTrabajos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configurarTablaLista(tablaTrabajos);
        panel.add(new JScrollPane(tablaTrabajos), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Crea la tabla de procesos cargados en RAM.
     *
     * @return panel de procesos y estados.
     */
    private JPanel crearPanelProcesos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Lista de Procesos"));
        modeloProcesos = new DefaultTableModel(new Object[]{"Procesos", "Estados"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.getTableHeader().setReorderingAllowed(false);
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configurarTablaLista(tablaProcesos);
        panel.add(new JScrollPane(tablaProcesos), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Ajusta una lista al panel y bloquea el ajuste manual.
     *
     * @param tabla tabla de trabajos o procesos.
     */
    private void configurarTablaLista(JTable tabla) {
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setResizingAllowed(false);
        int ancho = 150;
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            TableColumn columna = tabla.getColumnModel().getColumn(i);
            columna.setPreferredWidth(ancho);
            columna.setWidth(ancho);
            columna.setResizable(false);
        }
        tabla.setPreferredScrollableViewportSize(new Dimension(tabla.getColumnModel().getTotalColumnWidth(), 100));
    }

    /**
     * Crea la tabla de estadísticas.
     *
     * @return panel de tiempos por proceso.
     */
    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        modeloEstadisticas = new DefaultTableModel(new Object[]{"Proceso", "Inicio", "Finalización", "Duración (s)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaEstadisticas = new JTable(modeloEstadisticas);
        tablaEstadisticas.getTableHeader().setReorderingAllowed(false);
        tablaEstadisticas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(tablaEstadisticas), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Muestra las estadísticas.
     */
    private void mostrarEstadisticas() {
        ventanaEstadisticas.setLocationRelativeTo(this);
        ventanaEstadisticas.setVisible(true);
    }

    /**
     * Crea el panel del BCP actual.
     *
     * @return el panel del BCP.
     */
    private JPanel crearPanelBCP() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,0,10,10));
        panel.setPreferredSize(new Dimension(360, 0));
        JLabel titulo = new JLabel("BCP ACTUAL");
        panel.add(titulo,BorderLayout.NORTH);
        areaBCP = new JTextArea();
        areaBCP.setEditable(false);
        areaBCP.setFont(new Font(Font.MONOSPACED, Font.PLAIN,12));
        areaBCP.setLineWrap(true);
        areaBCP.setWrapStyleWord(true);
        panel.add(new JScrollPane(areaBCP, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Obtiene el área de texto del BCP.
     *
     * @return el área del BCP actual.
     */
    public JTextArea getAreaBCP() {
        return areaBCP;
    }

    /**
     * Actualiza la información del BCP.
     *
     * @param textoBCP texto que se desea mostrar.
     */
    public void mostrarBCP(String textoBCP) {
        areaBCP.setText(textoBCP);
    }

    /**
     * Crea la consola de la aplicación.
     *
     * @return el panel de la consola.
     */
    private JPanel crearPanelConsola() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Consola"));
        pantalla = new JTextArea(6, 40);
        pantalla.setEditable(false);
        pantalla.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        configurarEntradaConsola();
        panel.add(new JScrollPane(pantalla), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Protege el historial de la consola y configura Enter para confirmar la entrada.
     */
    private void configurarEntradaConsola() {
        ((AbstractDocument) pantalla.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass filtro, int posicion, String texto, AttributeSet atributos) throws BadLocationException {
                replace(filtro, posicion, 0, texto, atributos);
            }
            @Override
            public void remove(FilterBypass filtro, int posicion, int longitud) throws BadLocationException {
                replace(filtro, posicion, longitud, null, null);
            }
            @Override
            public void replace(FilterBypass filtro, int posicion, int longitud, String texto, AttributeSet atributos) throws BadLocationException {
                if (actualizandoConsola) {
                    filtro.replace(posicion, longitud, texto, atributos);
                    return;
                }
                if (inicioEntradaConsola < 0 || posicion < inicioEntradaConsola) {
                    return;
                }
                if (texto != null) {
                    if (texto.contains("\n") || texto.contains("\r")) {
                        return;
                    }
                }
                filtro.replace(posicion, longitud, texto, atributos);
            }
        });

        pantalla.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirmarEntradaConsola");
        pantalla.getActionMap().put("confirmarEntradaConsola", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                if (inicioEntradaConsola >= 0) {
                    Aplicacion.this.firePropertyChange("Entrada de teclado", null, getTextoEntradaTeclado());
                }
            }
        });
        pantalla.getActionMap().put("select-all", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                if (inicioEntradaConsola >= 0) {
                    seleccionarEntradaTeclado();
                } else {
                    pantalla.selectAll();
                }
            }
        });
    }

    /**
     * Obtiene la consola de la aplicación.
     *
     * @return el área de texto de la consola.
     */
    public JTextArea getPantalla() {
        return pantalla;
    }

    /**
     * Obtiene el texto de la entrada activa.
     *
     * @return el texto ingresado, o una cadena vacía si no hay una entrada activa.
     */
    public String getTextoEntradaTeclado() {
        if (inicioEntradaConsola < 0) {
            return "";
        }
        return pantalla.getText().substring(inicioEntradaConsola);
    }

    /**
     * Selecciona el texto de la entrada activa.
     */
    public void seleccionarEntradaTeclado() {
        if (inicioEntradaConsola >= 0) {
            pantalla.select(inicioEntradaConsola, pantalla.getDocument().getLength());
        }
    }

    /**
     * Agrega una línea a la consola.
     *
     * @param texto texto que se desea mostrar.
     */
    public void imprimirPantalla(String texto) {
        actualizandoConsola = true;
        try {
            if (inicioEntradaConsola >= 0) {
                int inicioLinea = pantalla.getText().lastIndexOf('\n', inicioEntradaConsola - 1) + 1;
                int posicion = pantalla.getCaret().getDot();
                int marca = pantalla.getCaret().getMark();
                String mensaje = texto + "\n";
                pantalla.insert(mensaje, inicioLinea);
                inicioEntradaConsola += mensaje.length();
                if (marca >= inicioLinea) {
                    marca += mensaje.length();
                }
                if (posicion >= inicioLinea) {
                    posicion += mensaje.length();
                }
                pantalla.setCaretPosition(marca);
                pantalla.moveCaretPosition(posicion);
            } else {
                pantalla.append(texto + "\n");
                pantalla.setCaretPosition(pantalla.getDocument().getLength());
            }
        } finally {
            actualizandoConsola = false;
        }
    }

    /**
     * Muestra una solicitud de teclado.
     *
     * @param idProceso identificador del solicitante.
     */
    public void mostrarSolicitudTeclado(int idProceso) {
        if (idProceso <= 0) {
            throw new IllegalArgumentException("El identificador del solicitante debe ser positivo.");
        }
        if (inicioEntradaConsola >= 0) {
            throw new IllegalStateException("Ya existe una entrada de teclado activa.");
        }
        actualizandoConsola = true;
        try {
            pantalla.append("Proceso " + idProceso + ": >> Ingresar valor: ");
        } finally {
            actualizandoConsola = false;
        }
        setEntradaTecladoHabilitada(true);
    }

    /**
     * Habilita o deshabilita la entrada de teclado durante INT 09H.
     *
     * @param habilitada {@code true} para permitir la entrada.
     */
    public void setEntradaTecladoHabilitada(boolean habilitada) {
        if (habilitada) {
            if (inicioEntradaConsola < 0) {
                inicioEntradaConsola = pantalla.getDocument().getLength();
            }
            pantalla.setEditable(true);
            pantalla.setCaretPosition(pantalla.getDocument().getLength());
            pantalla.requestFocusInWindow();
        } else {
            if (inicioEntradaConsola >= 0) {
                actualizandoConsola = true;
                try {
                    pantalla.append("\n");
                } finally {
                    actualizandoConsola = false;
                }
            }
            inicioEntradaConsola = -1;
            pantalla.setEditable(false);
        }
    }

    /**
     * Limpia la consola y deshabilita la entrada.
     */
    public void limpiarConsola() {
        inicioEntradaConsola = -1;
        pantalla.setEditable(false);
        actualizandoConsola = true;
        try {
            pantalla.setText("");
        } finally {
            actualizandoConsola = false;
        }
    }

    /**
     * Confirma y obtiene el tamaño elegido de RAM.
     *
     * @return la cantidad seleccionada de posiciones de memoria.
     */
    public int getMemoriaSeleccionada() {
        return leerTamañoSeleccionado(selectorMemoria, "RAM");
    }

    /**
     * Confirma y obtiene el tamaño elegido de disco.
     *
     * @return la cantidad seleccionada de posiciones del disco.
     */
    public int getDiscoSeleccionado() {
        return leerTamañoSeleccionado(selectorDisco, "disco");
    }

    /**
     * Confirma y obtiene la memoria virtual elegida.
     *
     * @return cantidad de posiciones reservadas.
     */
    public int getMemoriaVirtualSeleccionada() {
        return leerTamañoSeleccionado(selectorMemoriaVirtual, "memoria virtual");
    }

    /**
     * Confirma el tamaño escrito y lo devuelve.
     *
     * @param selector control del recurso.
     * @param nombre nombre del recurso.
     * @return tamaño confirmado.
     */
    private int leerTamañoSeleccionado(JSpinner selector, String nombre) {
        try {
            selector.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Tamaño de " + nombre + " inválido: " + ex.getMessage());
        }
        return (Integer) selector.getValue();
    }

    /**
     * Habilita o deshabilita los controles del programa.
     *
     * @param habilitados {@code true} para habilitar los controles.
     */
    public void setControlesProgramaHabilitados(boolean habilitados) {
        btnEjecutar.setEnabled(habilitados);
        btnPasoAPaso.setEnabled(habilitados);
        btnLimpiar.setEnabled(habilitados);
    }

    /**
     * Habilita o deshabilita la configuración.
     *
     * @param habilitado {@code true} para permitir cambiar el tamaño.
     */
    public void setConfiguracionHabilitada(boolean habilitado) {
        btnConfigurarSimulador.setEnabled(habilitado);
        selectorMemoria.setEnabled(habilitado);
        selectorDisco.setEnabled(habilitado);
        selectorMemoriaVirtual.setEnabled(habilitado);
    }

    /**
     * Resalta la próxima instrucción en RAM.
     *
     * @param posicionMemoria posición de la instrucción en memoria.
     */
    public void seleccionarProximaInstruccion(int posicionMemoria) {
        seleccionarFila(tablaMemoria, posicionMemoria);
    }

    /**
     * Elimina la selección de ambas tablas.
     */
    public void limpiarSeleccionTablas() {
        tablaMemoria.clearSelection();
        tablaDisco.clearSelection();
    }

    /**
     * Selecciona una fila y la desplaza al área visible.
     *
     * @param tabla tabla que se desea actualizar.
     * @param fila índice de la fila que se desea seleccionar.
     */
    private void seleccionarFila(JTable tabla, int fila) {
        if (fila < 0 || fila >= tabla.getRowCount()) {
            tabla.clearSelection();
            return;
        }
        tabla.setRowSelectionInterval(fila, fila);
        Rectangle areaFila = tabla.getCellRect(fila, 0, true);
        tabla.scrollRectToVisible(areaFila);
    }

    /**
     * Actualiza el texto de la distribución de memoria.
     */
    private void actualizarDistribucionMemoria() {
        int total = (Integer) selectorMemoria.getValue();
        int kernel = total / 4;
        int usuario = total - kernel;
        lblDistribucionMemoria.setText("SO: " + kernel + "  | Usuario: " + usuario);
    }

    /**
     * Ajusta el máximo de la memoria virtual.
     */
    private void actualizarLimiteMemoriaVirtual() {
        int tamañoDisco = (Integer) selectorDisco.getValue();
        int maximo = tamañoDisco - tamañoDisco / 20;
        SpinnerNumberModel modelo = (SpinnerNumberModel) selectorMemoriaVirtual.getModel();
        modelo.setMaximum(maximo);
        if ((Integer) selectorMemoriaVirtual.getValue() > maximo) {
            selectorMemoriaVirtual.setValue(maximo);
        }
    }

    /**
     * Actualiza la distribución elegida del disco.
     */
    private void actualizarDistribucionDisco() {
        int tamañoDisco = (Integer) selectorDisco.getValue();
        int tamañoIndices = tamañoDisco / 20;
        int tamañoMemoriaVirtual = (Integer) selectorMemoriaVirtual.getValue();
        int tamañoDatos = tamañoDisco - tamañoIndices - tamañoMemoriaVirtual;
        int maximoVirtual = tamañoDisco - tamañoIndices;
        lblDistribucionDisco.setText("Índices: " + tamañoIndices + "  | Programas y datos: " + tamañoDatos + "  | Máximo virtual: " + maximoVirtual);
    }

    /**
     * Selecciona archivos ensamblador.
     */
    private void abrirSelectorArchivos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM (*.asm)", "asm"));
        fileChooser.setMultiSelectionEnabled(true);
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File[] archivos = fileChooser.getSelectedFiles();
            firePropertyChange("Archivos cargados", null, Arrays.asList(archivos));
        }
    }

    /**
     * Obtiene el botón de ejecución automática.
     *
     * @return el botón Ejecutar.
     */
    public JButton getBtnEjecutar() {
        return btnEjecutar;
    }

    /**
     * Obtiene el botón de ejecución manual.
     *
     * @return el botón Paso a paso.
     */
    public JButton getBtnPasoAPaso() {
        return btnPasoAPaso;
    }

    /**
     * Obtiene el botón de limpieza.
     *
     * @return el botón Limpiar.
     */
    public JButton getBtnLimpiar() {
        return btnLimpiar;
    }

    /**
     * Obtiene el botón de carga de archivos.
     *
     * @return el botón Cargar archivos.
     */
    public JButton getBtnCargarArchivo() {
        return btnCargarArchivo;
    }

    /**
     * Obtiene el modelo de la tabla de disco.
     *
     * @return el modelo del disco.
     */
    public DefaultTableModel getModeloDisco() {
        return modeloDisco;
    }

    /**
     * Obtiene el modelo de trabajos pendientes.
     *
     * @return modelo de trabajos y estados.
     */
    public DefaultTableModel getModeloTrabajos() {
        return modeloTrabajos;
    }

    /**
     * Obtiene el modelo de procesos cargados en RAM.
     *
     * @return modelo de procesos y estados.
     */
    public DefaultTableModel getModeloProcesos() {
        return modeloProcesos;
    }

    /**
     * Obtiene el modelo de estadísticas.
     *
     * @return modelo de tiempos por proceso.
     */
    public DefaultTableModel getModeloEstadisticas() {
        return modeloEstadisticas;
    }

    /**
     * Obtiene el modelo de la tabla de memoria.
     *
     * @return el modelo de memoria.
     */
    public DefaultTableModel getModeloMemoria() { return modeloMemoria; }

    /**
     * Inicia la aplicación.
     *
     * @param args argumentos de inicio.
     */
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            Aplicacion aplicacion = new Aplicacion();
            new Controlador(aplicacion);
            aplicacion.setVisible(true);
        });
    }
}
