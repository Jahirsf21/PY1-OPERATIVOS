package com.mycompany.t1operativos.gui;

import com.mycompany.t1operativos.Controlador;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.io.File;

/**
 * Ventana principal de la aplicación Mini PC.
 *
 * Contiene los controles para cargar archivos ensamblador (.asm), ejecutarlos,
 * y visualizar el resultado de la simulación en dos tablas: instrucciones ensamblador y el estado de la memoria.
 *
 * @author deislher sánchez funez
 */
public class Aplicacion extends JFrame {

    private JButton btnEjecutar;
    private JButton btnPasoAPaso;
    private JButton btnLimpiar;
    private JButton btnCargarArchivo;

    private JTable tablaInstrucciones;
    private JTable tablaMemoria;
    private JTextArea areaBCP;
    private JSpinner selectorMemoria;
    private JLabel lblDistribucionMemoria;
    private DefaultTableModel modeloInstrucciones;
    private DefaultTableModel modeloMemoria;
    /**
     * Construye la ventana principal e inicializa todos sus componentes.
     *
     */
    public Aplicacion() {
        initComponents();
    }

    /**
     * Configura las propiedades generales de la ventana (título, tamaño, cierre) y
     * construye el layout principal agregando el panel superior y el panel de tablas.
     */
    private void initComponents() {
        setTitle("Mini PC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelTablas(), BorderLayout.CENTER);
        add(crearPanelBCP(), BorderLayout.EAST);
    }

    /**
     * Crea el panel superior de la ventana, compuesto por la fila de botones de
     * acción (Ejecutar, Paso a paso y Limpiar) y la fila del botón
     * para cargar archivos .asm.
     *
     * @return el panel superior ya construido con sus botones.
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

        panel.add(panelBotonesAccion);
        panel.add(panelCargarArchivo);
        panel.add(panelMemoria);

        return panel;
    }

    /**
     * Crea el panel central con las dos tablas de la simulación:
     * instrucciones (columna Instrucción) a la izquierda,
     * memoria (columnas Posición/Valor en memoria) a la derecha.
     * Ambas tablas se configuran como de solo lectura, sin reordenamiento de columnas ni selección de celdas.
     *
     * @return el panel central con las dos tablas dentro de sus respectivos
     *     {@link JScrollPane}.
     */
    private JPanel crearPanelTablas() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modeloInstrucciones = new DefaultTableModel(new Object[]{"Instrucción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tablaInstrucciones = new JTable(modeloInstrucciones);
        tablaInstrucciones.getTableHeader().setReorderingAllowed(false);
        tablaInstrucciones.setCellSelectionEnabled(false);
        tablaInstrucciones.setRowSelectionAllowed(true);
        tablaInstrucciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(tablaInstrucciones));

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
        panel.add(new JScrollPane(tablaMemoria));
        return panel;
    }

    /**
     * Crea el panel lateral que muestra la información del bloque de control
     * del proceso actual en un área de texto de solo lectura.
     *
     * @return el panel que contiene el título y el área de texto del BCP.
     */
    private JPanel crearPanelBCP() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,0,10,10));
        panel.setPreferredSize(new Dimension(220, 0));
        JLabel titulo = new JLabel("BCP ACTUAL");
        panel.add(titulo,BorderLayout.NORTH);
        areaBCP = new JTextArea();
        areaBCP.setEditable(false);
        areaBCP.setFont(new Font(Font.MONOSPACED, Font.PLAIN,12));
        panel.add(new JScrollPane(areaBCP), BorderLayout.CENTER);
        return panel;
    }

    /** @return el área de texto donde se muestra la información del BCP actual. */
    public JTextArea getAreaBCP() {
        return areaBCP;
    }

    /**
     * Actualiza la información visible del bloque de control del proceso actual.
     *
     * @param textoBCP texto con la información del BCP que se desea mostrar.
     */
    public void mostrarBCP(String textoBCP) {
        areaBCP.setText(textoBCP);
    }

    /**
     * Obtiene el tamaño total de memoria seleccionado por el usuario.
     *
     * @return la cantidad seleccionada de posiciones de memoria.
     */
    public int getMemoriaSeleccionada() {
        return (Integer) selectorMemoria.getValue();
    }

    /**
     * Habilita o deshabilita los controles que requieren un programa cargado.
     *
     * @param habilitados {@code true} para habilitar los controles.
     */
    public void setControlesProgramaHabilitados(boolean habilitados) {
        btnEjecutar.setEnabled(habilitados);
        btnPasoAPaso.setEnabled(habilitados);
        btnLimpiar.setEnabled(habilitados);
    }

    /**
     * Habilita o deshabilita el selector del tamaño de memoria.
     *
     * @param habilitado {@code true} para permitir cambiar el tamaño.
     */
    public void setSelectorMemoriaHabilitado(boolean habilitado) {
        selectorMemoria.setEnabled(habilitado);
    }

    /**
     * Resalta la próxima instrucción y la posición donde está almacenada.
     *
     * @param filaInstruccion fila correspondiente en la tabla de instrucciones.
     * @param posicionMemoria fila correspondiente en la tabla de memoria.
     */
    public void seleccionarProximaInstruccion(int filaInstruccion, int posicionMemoria) {
        seleccionarFila(tablaInstrucciones, filaInstruccion);
        seleccionarFila(tablaMemoria, posicionMemoria);
    }

    /** Elimina el resaltado actual de las tablas. */
    public void limpiarSeleccionTablas() {
        tablaInstrucciones.clearSelection();
        tablaMemoria.clearSelection();
    }

    /**
     * Selecciona una fila y la desplaza al área visible de su tabla.
     *
     * @param tabla tabla que contiene la fila.
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

    /** Actualiza el texto con la distribución de memoria seleccionada. */
    private void actualizarDistribucionMemoria() {
        int total = getMemoriaSeleccionada();
        int kernel = total / 4;
        int usuario = total - kernel;
        lblDistribucionMemoria.setText("SO: " + kernel + "  | Usuario: " + usuario);
    }

    /**
     * Abre un {@link JFileChooser} restringido a archivos con extensión .asm y
     * que permite seleccionar un único archivo. Si el usuario confirma
     * la selección, dispara un evento de propiedad {@code "Archivo cargado"}
     * para que el controlador procese el archivo.
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

    /** @return el botón de ejecutar. */
    public JButton getBtnEjecutar() {
        return btnEjecutar;
    }

    /** @return el botón de Paso a paso. */
    public JButton getBtnPasoAPaso() {
        return btnPasoAPaso;
    }

    /** @return el botón de Limpiar. */
    public JButton getBtnLimpiar() {
        return btnLimpiar;
    }

   /** @return el botón de Cargar archivos. */
    public JButton getBtnCargarArchivo() {
        return btnCargarArchivo;
    }

    /** @return el modelo de datos de la tabla de instrucciones. */
    public DefaultTableModel getModeloInstrucciones() {
        return modeloInstrucciones;
    }

    /** @return el modelo de datos de la tabla de memoria. */
    public DefaultTableModel getModeloMemoria() { return modeloMemoria; }

    /**
     * Punto de entrada de la aplicación. Intenta aplicar el Look & Feel Nimbus antes de mostrar la ventana principal.
     *
     * @param args argumentos
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
