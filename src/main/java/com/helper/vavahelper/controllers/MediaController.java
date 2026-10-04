package com.helper.vavahelper.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helper.vavahelper.infra.web.HttpCachePolicy;
import com.helper.vavahelper.models.Lineups.body.LineupResponseDTO;
import com.helper.vavahelper.models.Lineups.body.LineupSummaryDTO;
import com.helper.vavahelper.service.CatalogService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Media", description = "API for managing media files")
@RestController
@RequestMapping("/media")
public class MediaController {

    private final CatalogService catalog;
    private final HttpCachePolicy httpCache;

    public MediaController(CatalogService catalog, HttpCachePolicy httpCache) {
        this.catalog = catalog;
        this.httpCache = httpCache;
    }

    @GetMapping
    public ResponseEntity<List<LineupResponseDTO>> getAllLineups() {
        return ResponseEntity.ok().cacheControl(httpCache.get()).body(catalog.lineups());
    }

    @GetMapping("/agents/{name}")
    public ResponseEntity<List<LineupSummaryDTO>> getLineupsByAgent(@PathVariable String name) {
        List<LineupSummaryDTO> result = catalog.lineups().stream()
                .filter(lineup -> lineup.agentName().equalsIgnoreCase(name.trim()))
                .map(lineup -> new LineupSummaryDTO(lineup.description(), lineup.videoUrl()))
                .toList();
        return ResponseEntity.ok().cacheControl(httpCache.get()).body(result);
    }

    @GetMapping("/maps/{mapName}")
    public ResponseEntity<List<LineupSummaryDTO>> getLineupsByMap(@PathVariable String mapName) {
        List<LineupSummaryDTO> result = catalog.lineups().stream()
                .filter(lineup -> lineup.mapName().equalsIgnoreCase(mapName.trim()))
                .map(lineup -> new LineupSummaryDTO(lineup.description(), lineup.videoUrl()))
                .toList();
        return ResponseEntity.ok().cacheControl(httpCache.get()).body(result);
    }
}
