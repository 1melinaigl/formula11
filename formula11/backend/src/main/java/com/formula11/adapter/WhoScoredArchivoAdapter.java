package com.formula11.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.config.ClockConfig;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.Liga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Respaldo sin red: lee páginas de partido guardadas en app.whoscored.carpeta-paginas.
 * La carpeta tiene una subcarpeta por liga con el nombre del enum Liga (por ejemplo, LA_LIGA).
 * Cada archivo es un .json con las claves matchId, matchCentreData y matchCentreEventTypeJson
 * (como el fixture de los tests) o un .html guardado desde el navegador.
 * La lista de partidos sale de los archivos: la liga, de la subcarpeta, y la fecha, del startTime.
 */
public class WhoScoredArchivoAdapter implements WhoScoredPort {
    private static final Logger log = LoggerFactory.getLogger(WhoScoredArchivoAdapter.class);
    private static final String MATCH_CENTRE_DATA = "matchCentreData";
    private static final String EVENT_TYPES = "matchCentreEventTypeJson";

    private final WhoScoredProperties properties;
    private final ObjectMapper objectMapper;
    private List<ArchivoPartido> indice;

    public WhoScoredArchivoAdapter(WhoScoredProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized List<PartidoWhoScoredResumen> listarPartidos(Liga liga, YearMonth mes) {
        return indice().stream()
                .filter(archivo -> archivo.liga() == liga)
                .filter(archivo -> YearMonth.from(archivo.inicio()).equals(mes))
                .map(ArchivoPartido::resumen)
                .toList();
    }

    @Override
    public synchronized DatosPartidoWhoScored obtenerDatos(long partidoId) {
        ArchivoPartido archivo = indice().stream()
                .filter(item -> item.matchId() == partidoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el partido " + partidoId));
        return new DatosPartidoWhoScored(partidoId, archivo.liga(), archivo.inicio(),
                archivo.matchCentreData(), archivo.eventTypes());
    }

    /** Descarta el índice para que la próxima carga vea los archivos nuevos. */
    @Override
    public synchronized void finalizarCarga() {
        indice = null;
    }

    private List<ArchivoPartido> indice() {
        if (indice == null) {
            indice = construirIndice();
        }
        return indice;
    }

    private List<ArchivoPartido> construirIndice() {
        Path raiz = Path.of(properties.getCarpetaPaginas());
        List<ArchivoPartido> resultado = new ArrayList<>();
        for (Liga liga : Liga.values()) {
            Path carpetaLiga = raiz.resolve(liga.name());
            if (Files.isDirectory(carpetaLiga)) {
                archivosDe(carpetaLiga).forEach(path -> leer(path, liga).ifPresent(resultado::add));
            }
        }
        return List.copyOf(resultado);
    }

    private static List<Path> archivosDe(Path carpeta) {
        try (Stream<Path> files = Files.list(carpeta)) {
            return files.filter(Files::isRegularFile).filter(WhoScoredArchivoAdapter::esPagina).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo leer " + carpeta, exception);
        }
    }

    private static boolean esPagina(Path path) {
        String nombre = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return nombre.endsWith(".json") || nombre.endsWith(".html") || nombre.endsWith(".htm");
    }

    private Optional<ArchivoPartido> leer(Path path, Liga liga) {
        try {
            String contenido = Files.readString(path);
            CrudoPartido crudo = esHtml(path) ? desdeHtml(contenido) : desdeJson(contenido);
            if (crudo.matchId() <= 0) {
                log.warn("Se ignora {}: no tiene matchId", path);
                return Optional.empty();
            }
            JsonNode data = objectMapper.readTree(crudo.matchCentreData());
            ZonedDateTime inicio = LocalDateTime.parse(data.path("startTime").asText())
                    .atZone(ZoneId.of(properties.getZonaFuente()))
                    .withZoneSameInstant(ClockConfig.ZONA_NEGOCIO);
            return Optional.of(new ArchivoPartido(crudo.matchId(), liga, inicio,
                    data.path("home").path("name").asText(""), data.path("away").path("name").asText(""),
                    data.path("elapsed").asText(""), crudo.matchCentreData(), crudo.eventTypes()));
        } catch (IOException | RuntimeException exception) {
            log.warn("Se ignora {}: no se pudo leer como página de partido", path, exception);
            return Optional.empty();
        }
    }

    private static boolean esHtml(Path path) {
        String nombre = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return nombre.endsWith(".html") || nombre.endsWith(".htm");
    }

    private static CrudoPartido desdeHtml(String contenido) {
        WhoScoredHtmlParser.DatosHtml html = WhoScoredHtmlParser.extraer(contenido);
        return new CrudoPartido(html.matchId(), html.matchCentreData(), html.matchCentreEventTypeJson());
    }

    private CrudoPartido desdeJson(String contenido) throws IOException {
        JsonNode root = objectMapper.readTree(contenido);
        if (root.path(MATCH_CENTRE_DATA).isMissingNode() || root.path(EVENT_TYPES).isMissingNode()) {
            throw new IllegalArgumentException("El JSON no tiene " + MATCH_CENTRE_DATA + " y " + EVENT_TYPES);
        }
        return new CrudoPartido(root.path("matchId").asLong(-1),
                root.path(MATCH_CENTRE_DATA).toString(), root.path(EVENT_TYPES).toString());
    }

    private record CrudoPartido(long matchId, String matchCentreData, String eventTypes) {}

    private record ArchivoPartido(long matchId, Liga liga, ZonedDateTime inicio, String local,
                                  String visitante, String elapsed, String matchCentreData,
                                  String eventTypes) {
        PartidoWhoScoredResumen resumen() {
            return new PartidoWhoScoredResumen(matchId, inicio, local, visitante, elapsed, liga);
        }
    }
}
