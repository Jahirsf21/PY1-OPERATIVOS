# [Proyecto 1 - Gestor de Procesos]
## Integrantes:
### [2023032794] [Deislher Sánchez Funez]

### Estado del proyecto: 1,5
### [Enlace del video](https://youtu.be/zWprPsxDTME)

## Objetivos alcanzados y no alcanzados

| Objetivo | Estado | Descripción |
| --- | --- | --- |
| Interfaz gráfica | Alcanzado | Interfaz en Java para observar RAM, disco, BCP, procesos, consola y estadísticas. |
| Carga de programas | Alcanzado | Carga de uno o varios archivos `.asm`, validando cada archivo y conservando los aceptados. |
| Validación de archivos | Alcanzado | Revisión de extensión, sintaxis, instrucciones, operandos y nombres duplicados, con mensajes de error claros. |
| Ejecución en ambos modos | Alcanzado | Ejecución paso a paso y automática hasta finalizar o quedar esperando una entrada. |
| Instrucciones y pesos | Alcanzado | Ejecución de LOAD, STORE, MOV, ADD, SUB, INC, DEC y SWAP. |
| Comparaciones y saltos | Alcanzado | CMP, JMP, JE y JNE, con desplazamientos relativos y validación de los límites del programa. |
| Pila por proceso | Alcanzado | PARAM, PUSH y POP sobre una pila individual de cinco posiciones, con control de desbordamiento y pila vacía. |
| Procesador y registros | Alcanzado | Una CPU compartida con PC, IR, AC, AX, BX, CX, DX, AH, AL y flag, hasta cinco procesos sin terminar. |
| Interrupciones y llamadas al sistema | Alcanzado | Atención de INT 09H, INT 10H, INT 20H e INT 21H para teclado, pantalla, finalización y archivos. |
| Teclado | Alcanzado | Entrada de enteros de 0 a 255, confirmación con Enter y almacenamiento en DX. La espera se mide hasta una entrada válida. |
| Pantalla | Alcanzado | Consola para mostrar salidas, solicitudes de teclado y errores, protegiendo el historial frente a modificaciones. |
| Gestión de memoria principal | Alcanzado | Zonas de kernel y usuario para BCP e instrucciones, asignación de espacio y liberación de RAM al terminar. |
| Espera por memoria disponible | Alcanzado | Los trabajos que no caben temporalmente esperan admisión, la memoria liberada se reutiliza para admitirlos. |
| Almacenamiento secundario e índice | Alcanzado | Programas conservados en disco simulado e índice en sus primeras posiciones con nombre, dirección y tamaño. |
| Manejo de archivos | Alcanzado | Crear, abrir, leer, escribir y eliminar archivos numéricos mediante INT 21H, con nombre en DX y contenido en AL. |
| BCP individual | Alcanzado | Conservación de estado, registros, pila, CPU, tiempos, prioridad, base, alcance, enlace al siguiente BCP y archivos abiertos. |
| Visualización del BCP y registros | Alcanzado | Presentación del BCP del proceso actual, sus registros y la ubicación de los BCP e instrucciones en RAM. |
| Listas de trabajos y procesos | Alcanzado | Listas separadas para trabajos pendientes y procesos admitidos, con actualización de sus estados durante la ejecución. |
| Planificación FCFS | Alcanzado | Selección por orden de entrada a la cola de listos, el actual conserva la CPU hasta terminar o pedir teclado. |
| Despachador y cambios de contexto | Alcanzado | Guardado y restauración del contexto al retirar o asignar un proceso, sin mezclar registros ni pilas. |
| Configuración del simulador | Alcanzado | Modal para elegir RAM y disco antes de cargar programas, con valores iniciales de 256 y 512 y tamaños enteros editables. |
| Tiempos y estadísticas | Alcanzado | Registro y visualización del inicio, finalización y duración en segundos de cada proceso aceptado. |
| Protección y seguridad | Alcanzado | Validación de direcciones, capacidad, pila, entrada y archivos. |
| Memoria virtual | No alcanzado | No se implementaron paginación, intercambio ni suspensión y restauración de procesos desde disco. La reserva configurable permanece sin uso. |
