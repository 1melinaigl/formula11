package com.formula11.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.config.ClockConfig;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.Liga;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "app.whoscored.modo", havingValue = "archivos")
public class WhoScoredArchivoAdapter implements WhoScoredPort {
    private final WhoScoredProperties properties;
    private final ObjectMapper objectMapper;

    public WhoScoredArchivoAdapter(WhoScoredProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public java.util.List<PartidoWhoScoredResumen> listarPartidos(Liga liga, java.time.YearMonth mes) {
        throw new UnsupportedOperationException("El modo archivos no contiene calendario");
    }

    @Override
    public DatosPartidoWhoScored obtenerDatos(long partidoId) {
        Path root = Path.of(properties.getCarpetaPaginas());
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getParent().getFileName().toString().equals(Liga.LA_LIGA.name())
                            || isLeagueDirectory(path))
                    .map(path -> read(path, partidoId))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el partido " + partidoId));
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer " + root, exception);
        }
    }

    private boolean isLeagueDirectory(Path path) {
        try {
            Liga.valueOf(path.getParent().getFileName().toString());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private DatosPartidoWhoScored read(Path path, long partidoId) {
        try {
            String data;
            String types;
            String content = Files.readString(path);
            if (path.toString().toLowerCase().endsWith(".html")) {
                var html = WhoScoredHtmlParser.extraer(content);
                data = html.matchCentreData();
                types = html.matchCentreEventTypeJson();
            } else {
                JsonNode root = objectMapper.readTree(content);
                if (root.path("matchCentreData").isMissingNode()) return null;
                data = root.path("matchCentreData").toString();
                types = root.path("matchCentreEventTypeJson").toString();
                if (root.path("matchId").asLong(-1) != partidoId) return null;
            }
            JsonNode parsed = objectMapper.readTree(data);
            Path leagueDirectory = path.getParent();
            Liga liga = Liga.valueOf(leagueDirectory.getFileName().toString());
            ZonedDateTime start = LocalDateTime.parse(parsed.path("startTime").asText())
                    .atZone(ZoneId.of(properties.getZonaFuente()))
                    .withZoneSameInstant(ClockConfig.ZONA_NEGOCIO);
            return new DatosPartidoWhoScored(partidoId, liga, start, data, types);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalArgumentException) throw (IllegalArgumentException) exception;
            throw new IllegalStateException("No se pudo leer " + path, exception);
        }
    }
}
