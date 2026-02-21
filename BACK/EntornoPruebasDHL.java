import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.stream.Collectors;

//1. Domicilio del cliente (con su zona postal para asociar rutas futuras)
class Domicilio {
    String idDomicilio;
    String zonaPostal;

    public Domicilio(String id, String zonaPostal) {
        this.idDomicilio = id;
        this.zonaPostal = zonaPostal;
    }
}

//2. Representa una ubicación alternativa de entrega (Locker o Punto de Conveniencia)
class PuntoRecogida {
    String idPunto;
    String nombre;
    String zonaPostal;
    double distanciaMediaZonaKm; // Distancia media desde las casas de esa zona al punto
    boolean esLocker24h;

    public PuntoRecogida(String id, String nombre, String zona, double distancia, boolean locker) {
        this.idPunto = id;
        this.nombre = nombre;
        this.zonaPostal = zona;
        this.distanciaMediaZonaKm = distancia;
        this.esLocker24h = locker;
    }

    @Override
    public String toString() {
        return nombre + " (" + distanciaMediaZonaKm + " km de tu casa)";
    }
}

//3. Definición consistente de la Ruta
class RutaFutura {
    String idRuta;
    LocalDate fecha;
    String zonaPostal;
    PuntoRecogida puntoRecogidaAsociado; // Null si es ruta a domicilio, lleno si es ruta a un locker
    
    int paquetesActuales;
    int capacidadMaxima;
    
    double costoBaseRutaEuros; // Lo que cuesta sacar la furgoneta (combustible, sueldo)
    double emisionesBaseKgCO2; // Emisiones de la ruta completa

    public RutaFutura(String id, LocalDate fecha, String zona, PuntoRecogida punto, int paquetes, int capacidad, double costo, double emisiones) {
        this.idRuta = id;
        this.fecha = fecha;
        this.zonaPostal = zona;
        this.puntoRecogidaAsociado = punto;
        this.paquetesActuales = paquetes;
        this.capacidadMaxima = capacidad;
        this.costoBaseRutaEuros = costo;
        this.emisionesBaseKgCO2 = emisiones;
    }

    // Calcula cuánto nos cuesta AÑADIR un paquete más a esta ruta
    public double costoPorPaquete() {
        if (paquetesActuales == 0) return costoBaseRutaEuros;
        return costoBaseRutaEuros / paquetesActuales; 
    }

    // Calcula el impacto ambiental por paquete
    public double emisionesPorPaquete() {
        if (paquetesActuales == 0) return emisionesBaseKgCO2;
        return emisionesBaseKgCO2 / paquetesActuales;
    }
    //

}

//6. La recomendación adaptada a ambas opciones (Tiempo y Lugar)
// 6. La recomendación adaptada a la nueva lógica basada 100% en agrupación
class RecomendacionUI {
    PuntoRecogida ubicacion; // Si es null, es "Tu casa"
    LocalDate fechaEntrega;
    int totalEcoPuntos; // Puntos únicos basados en el ahorro económico real
    int paquetesAgrupados; // Útil para mostrar al usuario por qué gana esos puntos

    public RecomendacionUI(PuntoRecogida ubicacion, LocalDate fecha, int totalEcoPuntos, int paquetesAgrupados) {
        this.ubicacion = ubicacion;
        this.fechaEntrega = fecha;
        this.totalEcoPuntos = totalEcoPuntos;
        this.paquetesAgrupados = paquetesAgrupados;
    }
}


//4. Configurador de GeneradorDatos
class ConfiguracionGeneradorDatos {
    int diasSimulacion;
    int numPuntosRecogida;
    int numDomicilios;
    
    // Costos y Emisiones Domicilio (Suele ser más caro y contaminante)
    double minCostoDomicilio;
    double maxCostoDomicilio;
    double minCO2Domicilio;
    double maxCO2Domicilio;
    
    // Costos y Emisiones Punto de Recogida / Locker (Suele ser más barato y ecológico)
    double minCostoPunto;
    double maxCostoPunto;
    double minCO2Punto;
    double maxCO2Punto;

    public ConfiguracionGeneradorDatos(int dias, int numPuntos, int numDomicilios, 
                                   double minCostoDom, double maxCostoDom, double minCO2Dom, double maxCO2Dom,
                                   double minCostoPunto, double maxCostoPunto, double minCO2Punto, double maxCO2Punto) {
        this.diasSimulacion = dias;
        this.numPuntosRecogida = numPuntos;
        this.numDomicilios = numDomicilios;
        
        this.minCostoDomicilio = minCostoDom;
        this.maxCostoDomicilio = maxCostoDom;
        this.minCO2Domicilio = minCO2Dom;
        this.maxCO2Domicilio = maxCO2Dom;
        
        this.minCostoPunto = minCostoPunto;
        this.maxCostoPunto = maxCostoPunto;
        this.minCO2Punto = minCO2Punto;
        this.maxCO2Punto = maxCO2Punto;
    }
}

//5. Generador de datos de ejemplo para pruebas
class GeneradorDatos {
    
    private static Random rand = new Random();

    public static List<PuntoRecogida> generarPuntosDeRecogida(ConfiguracionGeneradorDatos config, String zonaPostal) {
        List<PuntoRecogida> puntos = new ArrayList<>();
        for (int i = 1; i <= config.numPuntosRecogida; i++) {
            double distanciaAleatoria = 0.5 + (4.5 * rand.nextDouble()); 
            boolean esLocker = rand.nextBoolean(); 
            puntos.add(new PuntoRecogida("PUNTO-" + i, "Punto Alternativo " + i, zonaPostal, 
                                         Math.round(distanciaAleatoria * 10.0) / 10.0, esLocker));
        }
        return puntos;
    }

    public static List<RutaFutura> generarRutas(ConfiguracionGeneradorDatos config, LocalDate fechaInicio, 
                                                String zonaPostal, List<PuntoRecogida> puntosDisponibles) {
        List<RutaFutura> rutas = new ArrayList<>();

        for (int d = 0; d < config.diasSimulacion; d++) {
            LocalDate fechaRuta = fechaInicio.plusDays(d);
            
            // 1. Ruta a Domicilios
            double costoDom = config.minCostoDomicilio + (rand.nextDouble() * (config.maxCostoDomicilio - config.minCostoDomicilio));
            double emisionesDom = config.minCO2Domicilio + (rand.nextDouble() * (config.maxCO2Domicilio - config.minCO2Domicilio));
            int paquetesDom = rand.nextInt(30) + 5;
            
            rutas.add(new RutaFutura("R-DOM-DIA" + d, fechaRuta, zonaPostal, null, 
                                     paquetesDom, 100, costoDom, emisionesDom));

            // 2. Rutas a cada Punto de Recogida
            for (PuntoRecogida punto : puntosDisponibles) {
                double costoPunto = config.minCostoPunto + (rand.nextDouble() * (config.maxCostoPunto - config.minCostoPunto));
                double emisionesPunto = config.minCO2Punto + (rand.nextDouble() * (config.maxCO2Punto - config.minCO2Punto));
                int paquetesPunto = rand.nextInt(60) + 20; 
                
                rutas.add(new RutaFutura("R-" + punto.idPunto + "-DIA" + d, fechaRuta, zonaPostal, punto, 
                                         paquetesPunto, 150, costoPunto, emisionesPunto));
            }
        }
        return rutas;
    }
}


// 7. El optimizador que calcula la mejor opción para el cliente y la empresa
//CAMBIO
class OptimizadorEcoPuntosFinal {

    // --- VARIABLES DE NEGOCIO ---
    private static final double VALOR_PUNTO_EUROS = 0.10; 
    private static final double FACTOR_RECOMPENSA = 0.50; // DHL da el 50% del ahorro neto al cliente
    
    // --- LÍMITES OPERATIVOS ---
    private static final double COSTO_ALMACENAJE_DIARIO = 0.15; // € por día que el paquete ocupa espacio
    private static final int MAX_DIAS_RETRASO = 7; // Ventana máxima operativa (Cross-docking)

    public List<RecomendacionUI> calcularOpciones(LocalDate fechaBase, RutaFutura rutaBaseDomicilio, List<RutaFutura> todasLasRutas) {
        List<RecomendacionUI> opciones = new ArrayList<>();
        double costoBase = rutaBaseDomicilio.costoPorPaquete(); 

        for (RutaFutura ruta : todasLasRutas) {
            // 1. Descartar si la furgoneta / locker ya está lleno
            if (ruta.paquetesActuales >= ruta.capacidadMaxima) continue;

            // 2. Calcular los días de retraso y aplicar el límite operativo (Máx 7 días)
            int diasRetraso = (int) ChronoUnit.DAYS.between(fechaBase, ruta.fecha);
            if (diasRetraso < 0 || diasRetraso > MAX_DIAS_RETRASO) continue;

            // 3. Calcular el ahorro económico real (Combustible vs. Almacenaje)
            double costoFuturo = ruta.costoPorPaquete(); 
            double ahorroBrutoGasolina = costoBase - costoFuturo;
            double costoAlmacenaje = diasRetraso * COSTO_ALMACENAJE_DIARIO;
            
            double ahorroNeto = ahorroBrutoGasolina - costoAlmacenaje;

            // 4. Solo generamos la opción si DHL realmente gana dinero/eficiencia con el cambio
            if (ahorroNeto > 0) {
                int ecoPuntos = (int) Math.round((ahorroNeto * FACTOR_RECOMPENSA) / VALOR_PUNTO_EUROS);

                // Evitamos dar 0 puntos si el ahorro es minúsculo, estableciendo un mínimo de cortesía si es viable
                ecoPuntos = Math.max(ecoPuntos, 5); 

                opciones.add(new RecomendacionUI(
                    ruta.puntoRecogidaAsociado, 
                    ruta.fecha, 
                    ecoPuntos, 
                    ruta.paquetesActuales
                ));
            }
        }
        return opciones;
    }

    // --- MÉTODOS PARA ALIMENTAR LA INTERFAZ WEB ---

    // Devuelve el top de puntos por ubicación (Para la Vista "Selecciona un punto de recogida")
    public Map<String, Integer> obtenerMaximosPorUbicacion(List<RecomendacionUI> opciones) {
        return opciones.stream()
            .collect(Collectors.toMap(
                opt -> opt.ubicacion != null ? opt.ubicacion.nombre : "Domicilio",
                opt -> opt.totalEcoPuntos,
                Integer::max 
            ));
    }

    // Devuelve los días y puntos para una ubicación concreta (Para la Vista "¿Cuándo puedes recibirlo?")
    public List<RecomendacionUI> obtenerFechasParaUbicacion(List<RecomendacionUI> opciones, String nombreUbicacion) {
        return opciones.stream()
            .filter(opt -> {
                if (nombreUbicacion.equals("Domicilio")) return opt.ubicacion == null;
                return opt.ubicacion != null && opt.ubicacion.nombre.equals(nombreUbicacion);
            })
            .sorted((o1, o2) -> o1.fechaEntrega.compareTo(o2.fechaEntrega))
            .collect(Collectors.toList());
    }
}
// 8. Entorno de pruebas para validar la lógica con datos simulados

// 8. Entorno de pruebas para validar la lógica con datos simulados
public class EntornoPruebasDHL {
    public static void main(String[] args) {
        LocalDate hoy = LocalDate.now();
        String zonaTest = "28931";

        // 1. Configuración del escenario
        ConfiguracionGeneradorDatos config = new ConfiguracionGeneradorDatos(
            7, 3, 100, // 7 días vista, 3 lockers/tiendas, 100 casas
            120.0, 180.0, 20.0, 45.0, // Domicilio: Caro y Contaminante
            40.0, 80.0, 5.0, 15.0     // Lockers: Barato y Ecológico
        );

        // 2. Generar Datos
        List<PuntoRecogida> puntos = GeneradorDatos.generarPuntosDeRecogida(config, zonaTest);
        List<RutaFutura> todasLasRutas = GeneradorDatos.generarRutas(config, hoy, zonaTest, puntos);

        // 3. Establecer el "Escenario Base" (Lo que pasaría si el cliente no hace nada)
        RutaFutura rutaBaseHoy = todasLasRutas.stream()
            .filter(r -> r.fecha.equals(hoy) && r.puntoRecogidaAsociado == null)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hay ruta base hoy"));

        System.out.println("--- ESCENARIO BASE (SIN ECO PUNTOS) ---");
        System.out.println("Entrega prevista: Hoy a Domicilio");
        System.out.printf("Costo empresa: %.2f€ | Emisiones: %.2f kg CO2\n\n", 
                          rutaBaseHoy.costoPorPaquete(), rutaBaseHoy.emisionesPorPaquete());

        // 4. Ejecutar el Optimizador Final
        OptimizadorEcoPuntosFinal optimizador = new OptimizadorEcoPuntosFinal();
        System.out.println("Buscando alternativas más sostenibles...\n");
        List<RecomendacionUI> opcionesGeneradas = optimizador.calcularOpciones(hoy, rutaBaseHoy, todasLasRutas);

        // 5. Mostrar Resultados Simulando la Interfaz (UI)
        if (opcionesGeneradas.isEmpty()) {
            System.out.println("Tu envío ya está en la ruta más óptima o no hay opciones rentables. ¡Gracias por confiar en DHL!");
            return;
        }

        // --- VISTA 1: SELECCIONA UN PUNTO DE RECOGIDA ---
        System.out.println("=== VISTA 1: SELECCIONA UN PUNTO DE RECOGIDA ===");
        Map<String, Integer> maximosPorUbicacion = optimizador.obtenerMaximosPorUbicacion(opcionesGeneradas);
        
        // Ordenamos para mostrar primero los que dan más puntos (como en tu mockup)
        maximosPorUbicacion.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> System.out.println("- " + entry.getKey() + "\t-> hasta +" + entry.getValue() + " pts"));

        // --- VISTA 2: FECHAS DISPONIBLES PARA UNA UBICACIÓN ---
        // Simulamos que el usuario hace clic en el primer punto de recogida de la lista (si hay)
        String ubicacionElegida = "Domicilio"; 
        if (!puntos.isEmpty()) {
            ubicacionElegida = puntos.get(0).nombre; 
        }

        System.out.println("\n=== VISTA 2: ¿CUÁNDO PUEDES RECIBIRLO EN '" + ubicacionElegida.toUpperCase() + "'? ===");
        List<RecomendacionUI> fechasDisponibles = optimizador.obtenerFechasParaUbicacion(opcionesGeneradas, ubicacionElegida);
        
        if (fechasDisponibles.isEmpty()) {
            System.out.println("No hay fechas rentables para esta ubicación.");
        } else {
            fechasDisponibles.forEach(opt -> {
                long diasDeRetraso = ChronoUnit.DAYS.between(hoy, opt.fechaEntrega);
                System.out.println("- " + opt.fechaEntrega + " (+" + diasDeRetraso + " días)\t| " 
                                   + opt.paquetesAgrupados + " paquetes agrupados -> +" + opt.totalEcoPuntos + " pts");
            });
        }
    }
}
