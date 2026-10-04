package com.mycompany.t1operativos.gui;

import com.mycompany.t1operativos.Controlador;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

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

    private JTable tablaMemoria;
    private JTable tablaDisco;
    private JTextArea areaBCP;
    private JSpinner selectorMemoria;
    private JLabel lblDistribucionMemoria;
    private JSpinner selectorDisco;
    private JLabel lblDistribucionDisco;
    private JLabel lblEstadoEjecucion;
    private JTextArea pantalla;
    private int inicioEntradaConsola = -1;
    private boolean actualizandoConsola;
    private DefaultTableModel modeloMemoria;
    private DefaultTableModel modeloDisco;
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

        JPanel panelCargarArchivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnCargarArchivo = new JButton("Cargar archivo");
        btnCargarArchivo.addActionListener(e -> abrirSelectorArchivo());
        panelCargarArchivo.add(btnCargarArchivo);

        JPanel panelMemoria = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelMemoria.add(new JLabel("Memoria total:"));
        selectorMemoria = new JSpinner(new SpinnerNumberModel(256, 128, null, 4));
        JSpinner.DefaultEditor editorMemoria = (JSpinner.DefaultEditor) selectorMemoria.getEditor();
        editorMemoria.getTextField().setEditable(false);
        panelMemoria.add(selectorMemoria);
        lblDistribucionMemoria = new JLabel();
        panelMemoria.add(lblDistribucionMemoria);
        selectorMemoria.addChangeListener(e -> actualizarDistribucionMemoria());
        actualizarDistribucionMemoria();

        JPanel panelDisco = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelDisco.add(new JLabel("Disco total:"));
        selectorDisco = new JSpinner(new SpinnerNumberModel(512, 256, null, 4));
        JSpinner.DefaultEditor editorDisco = (JSpinner.DefaultEditor) selectorDisco.getEditor();
        editorDisco.getTextField().setEditable(false);
        panelDisco.add(selectorDisco);
        lblDistribucionDisco = new JLabel();
        panelDisco.add(lblDistribucionDisco);
        selectorDisco.addChangeListener(e -> actualizarDistribucionDisco());
        actualizarDistribucionDisco();

        panel.add(panelBotonesAccion);
        panel.add(panelCargarArchivo);
        panel.add(panelMemoria);
        panel.add(panelDisco);

        JPanel panelEjecucion = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        lblEstadoEjecucion = new JLabel("Sin programa cargado.");
        panelEjecucion.add(lblEstadoEjecucion);
        panel.add(panelEjecucion);

        return panel;
    }

    /**
     * Crea las tablas de RAM y disco visibles juntas.
     *
     * @return el panel con las tablas.
     */
    private JPanel crearPanelTablas() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        panel.add(panelMemoria, BorderLayout.WEST);

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
        panel.add(panelDisco, BorderLayout.CENTER);
        return panel;
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
                if (texto != null && (texto.contains("\n") || texto.contains("\r"))) {
                    return;
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
        String entrada = getTextoEntradaTeclado();
        actualizandoConsola = true;
        try {
            if (inicioEntradaConsola >= 0) {
                pantalla.append("\n" + texto + "\n");
                inicioEntradaConsola = pantalla.getDocument().getLength();
                pantalla.append(entrada);
            } else {
                pantalla.append(texto + "\n");
            }
        } finally {
            actualizandoConsola = false;
        }
        pantalla.setCaretPosition(pantalla.getDocument().getLength());
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
     * Actualiza el texto del estado de ejecución.
     *
     * @param estado texto del estado o de la instrucción actual.
     */
    public void mostrarEstadoEjecucion(String estado) {
        lblEstadoEjecucion.setText(estado);
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
     * Obtiene el tamaño de memoria seleccionado.
     *
     * @return la cantidad seleccionada de posiciones de memoria.
     */
    public int getMemoriaSeleccionada() {
        return (Integer) selectorMemoria.getValue();
    }

    /**
     * Obtiene el tamaño de disco seleccionado.
     *
     * @return la cantidad seleccionada de posiciones del disco.
     */
    public int getDiscoSeleccionado() {
        return (Integer) selectorDisco.getValue();
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
     * Habilita o deshabilita el selector de memoria.
     *
     * @param habilitado {@code true} para permitir cambiar el tamaño.
     */
    public void setSelectorMemoriaHabilitado(boolean habilitado) {
        selectorMemoria.setEnabled(habilitado);
    }

    /**
     * Habilita o deshabilita el selector de disco.
     *
     * @param habilitado {@code true} para permitir cambiar el tamaño.
     */
    public void setSelectorDiscoHabilitado(boolean habilitado) {
        selectorDisco.setEnabled(habilitado);
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
        int total = getMemoriaSeleccionada();
        int kernel = total / 4;
        int usuario = total - kernel;
        lblDistribucionMemoria.setText("SO: " + kernel + "  | Usuario: " + usuario);
    }

    /**
     * Actualiza la distribución calculada del disco.
     */
    private void actualizarDistribucionDisco() {
        int tamañoDisco = getDiscoSeleccionado();
        int tamañoIndices = tamañoDisco / 20;
        int tamañoMemoriaVirtual = tamañoDisco / 8;
        int tamañoDatos = tamañoDisco - tamañoIndices - tamañoMemoriaVirtual;
        lblDistribucionDisco.setText("Índices: " + tamañoIndices + "  | Datos: " + tamañoDatos + "  | Memoria virtual: " + tamañoMemoriaVirtual);
    }

    /**
     * Selecciona un archivo ensamblador y notifica su carga.
     */
    private void abrirSelectorArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM (*.asm)", "asm"));
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            firePropertyChange("Archivo cargado", null, archivo);
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
     * @return el botón Cargar archivo.
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
