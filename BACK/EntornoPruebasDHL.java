import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


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

//6. La recomendación adaptada a ambas opciones (Tiempo y Lugar)
class RecomendacionEco {
    int diasRetraso;
    PuntoRecogida nuevoPunto; // Puede ser null si solo retrasa el envío a casa
    int ecoPuntosOfrecidos;
    
    // Métricas para el dashboard de la empresa
    double ahorroEuros;
    double ahorroCO2;
    LocalDate nuevaFecha;

    public RecomendacionEco(int dias, PuntoRecogida punto, int puntos, double euros, double co2, LocalDate fecha) {
        this.diasRetraso = dias;
        this.nuevoPunto = punto;
        this.ecoPuntosOfrecidos = puntos;
        this.ahorroEuros = euros;
        this.ahorroCO2 = co2;
        this.nuevaFecha = fecha;
    }

    @Override
    public String toString() {
        String lugar = (nuevoPunto == null) ? "Domicilio" : nuevoPunto.toString();
        return String.format("Retraso: %d días | Lugar: %s | Puntos: %d | Ahorro Empresa: %.2f€ | CO2 Evitado: %.2f kg", 
                             diasRetraso, lugar, ecoPuntosOfrecidos, ahorroEuros, ahorroCO2);
    }
}



// 7. El optimizador que calcula la mejor opción para el cliente y la empresa
class OptimizadorEcoPuntos {

    // Constantes del modelo de negocio
    private static final double VALOR_PUNTO_EUROS = 0.10; // 1 punto = 10 céntimos
    private static final double FACTOR_RECOMPENSA_ECONOMICA = 0.40; // Damos el 40% del ahorro económico
    private static final int PUNTOS_POR_KG_CO2_AHORRADO = 2; // Premio puramente ecológico
    
    // Compensaciones por molestia al cliente
    private static final int PUNTOS_POR_DIA_ESPERA = 5; 
    private static final int PUNTOS_POR_KM_CAMINADO = 10;

    public RecomendacionEco evaluarMejoresOpciones(LocalDate fechaBase, RutaFutura rutaBaseDomicilio, List<RutaFutura> todasLasRutas) {
        
        List<RecomendacionEco> opcionesViables = new ArrayList<>();
        
        double costoBase = rutaBaseDomicilio.costoPorPaquete();
        double co2Base = rutaBaseDomicilio.emisionesPorPaquete();

        for (RutaFutura rutaFutura : todasLasRutas) {
            // Ignoramos la ruta base que ya tiene asignada el cliente
            if (rutaFutura.idRuta.equals(rutaBaseDomicilio.idRuta)) continue;

            double ahorroEuros = costoBase - rutaFutura.costoPorPaquete();
            double ahorroCO2 = co2Base - rutaFutura.emisionesPorPaquete();

            // Solo nos interesan opciones que realmente ahorren dinero y emisiones
            if (ahorroEuros > 0 && ahorroCO2 > 0) {
                
                int diasRetraso = (int) ChronoUnit.DAYS.between(fechaBase, rutaFutura.fecha);
                PuntoRecogida punto = rutaFutura.puntoRecogidaAsociado;
                
                // 1. Beneficio Económico Compartido
                int puntosPorAhorro = (int) Math.round((ahorroEuros * FACTOR_RECOMPENSA_ECONOMICA) / VALOR_PUNTO_EUROS);
                
                // 2. Beneficio Ecológico
                int puntosPorCO2 = (int) Math.round(ahorroCO2 * PUNTOS_POR_KG_CO2_AHORRADO);
                
                // 3. Compensación por Molestias
                int puntosPorEspera = diasRetraso * PUNTOS_POR_DIA_ESPERA;
                int puntosPorCaminar = (punto != null) ? (int) Math.round(punto.distanciaMediaZonaKm * PUNTOS_POR_KM_CAMINADO) : 0;

                // Suma total de puntos a ofrecer
                int ecoPuntosTotales = puntosPorAhorro + puntosPorCO2 + puntosPorEspera + puntosPorCaminar;

                opcionesViables.add(new RecomendacionEco(
                    diasRetraso, 
                    punto, 
                    ecoPuntosTotales, 
                    ahorroEuros, 
                    ahorroCO2, 
                    rutaFutura.fecha
                ));
            }
        }

        if (opcionesViables.isEmpty()) {
            return null; // No hay ninguna alternativa mejor
        }

        // Devolvemos la opción que le dé más puntos al cliente (suele ser la más eficiente para la empresa también)
        return opcionesViables.stream()
                .max(Comparator.comparingInt(r -> r.ecoPuntosOfrecidos))
                .orElse(null);
    }
}

// 8. Entorno de pruebas para validar la lógica con datos simulados
public class EntornoPruebasDHL {
    public static void main(String[] args) {
        LocalDate hoy = LocalDate.now();
        String zonaTest = "28931";

        // 1. Configuración del escenario
        ConfiguracionGeneradorDatos config = new ConfiguracionGeneradorDatos(
            5, 3, 100, // 5 días, 3 lockers, 100 casas
            120.0, 180.0, 20.0, 45.0, // Domicilio: Caro y Contaminante
            40.0, 80.0, 5.0, 15.0     // Lockers: Barato y Ecológico
        );

        // 2. Generar Datos
        List<PuntoRecogida> puntos = GeneradorDatos.generarPuntosDeRecogida(config, zonaTest);
        List<RutaFutura> todasLasRutas = GeneradorDatos.generarRutas(config, hoy, zonaTest, puntos);

        // 3. Establecer el "Escenario Base" (Lo que pasaría si el cliente no hace nada)
        // Buscamos la ruta a domicilio de HOY (Día 0)
        RutaFutura rutaBaseHoy = todasLasRutas.stream()
            .filter(r -> r.fecha.equals(hoy) && r.puntoRecogidaAsociado == null)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hay ruta base hoy"));

        System.out.println("--- ESCENARIO BASE (SIN ECO PUNTOS) ---");
        System.out.println("Entrega prevista: Hoy a Domicilio");
        System.out.printf("Costo empresa: %.2f€ | Emisiones: %.2f kg CO2\n\n", 
                          rutaBaseHoy.costoPorPaquete(), rutaBaseHoy.emisionesPorPaquete());

        // 4. Ejecutar el Optimizador
        OptimizadorEcoPuntos optimizador = new OptimizadorEcoPuntos();
        System.out.println("Buscando alternativas más sostenibles...\n");
        RecomendacionEco mejorAlternativa = optimizador.evaluarMejoresOpciones(hoy, rutaBaseHoy, todasLasRutas);

        // 5. Mostrar Resultados
        System.out.println("=== PROPUESTA PARA EL CLIENTE EN LA APP ===");
        if (mejorAlternativa != null) {
            System.out.println("¡Enhorabuena! Puedes ganar " + mejorAlternativa.ecoPuntosOfrecidos + " Eco Puntos.");
            System.out.println("Nueva Fecha: " + mejorAlternativa.nuevaFecha + " (+" + mejorAlternativa.diasRetraso + " días)");
            System.out.println("Lugar de Recogida: " + (mejorAlternativa.nuevoPunto != null ? mejorAlternativa.nuevoPunto.nombre : "Domicilio"));
            System.out.println("-------------------------------------------");
            System.out.printf("IMPACTO LOGRADO -> DHL ahorra: %.2f€ y evita %.2f kg de CO2\n", 
                              mejorAlternativa.ahorroEuros, mejorAlternativa.ahorroCO2);
        } else {
            System.out.println("Tu envío ya está en la ruta más óptima. ¡Gracias por confiar en DHL!");
        }
    }
}