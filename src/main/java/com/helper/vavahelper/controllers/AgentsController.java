package com.helper.vavahelper.controllers;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helper.vavahelper.infra.web.HttpCachePolicy;
import com.helper.vavahelper.models.Agents.body.AgentResponseDTO;
import com.helper.vavahelper.models.Agents.body.AgentsDTO;
import com.helper.vavahelper.models.Agents.body.AgentsWithSkillsDTO;
import com.helper.vavahelper.models.Skills.body.SkillDTO;
import com.helper.vavahelper.service.CatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Agents", description = "API for managing agents and their skills")
@RestController
@RequestMapping("/agents")
public class AgentsController {

    // Apelidos aceitos na URL, em minusculas, apontando para o nome real salvo no banco.
    private static final Map<String, String> ALIASES = Map.of(
            "kay-o", "kay/0",
            "kayo", "kay/0");

    private final CatalogService catalog;
    private final HttpCachePolicy httpCache;

    public AgentsController(CatalogService catalog, HttpCachePolicy httpCache) {
        this.catalog = catalog;
        this.httpCache = httpCache;
    }

    @Operation(
        summary = "Get all agents",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of agents",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AgentResponseDTO.class)))
        }
    )
    @GetMapping
    public ResponseEntity<List<AgentResponseDTO>> getAllAgents() {
        return ResponseEntity.ok().cacheControl(httpCache.get()).body(catalog.agents());
    }

    @Operation(
        summary = "Get an agent by name",
        parameters = {
            @Parameter(name = "name", description = "Name of the agent to retrieve", required = true)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Agent found",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AgentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found")
        }
    )
    @GetMapping("/{name}")
    public ResponseEntity<AgentResponseDTO> getAgentByName(@PathVariable String name) {
        return findAgent(name)
                .map(agent -> ResponseEntity.ok().cacheControl(httpCache.get()).body(agent))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get an agent along with their skills",
        parameters = {
            @Parameter(name = "name", description = "Name of the agent", required = true)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Agent and skills returned",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AgentsWithSkillsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found")
        }
    )
    @GetMapping("/{name}/with-skills")
    public ResponseEntity<AgentsWithSkillsDTO> getAgentWithSkills(@PathVariable String name) {
        Optional<AgentResponseDTO> found = findAgent(name);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AgentResponseDTO agent = found.get();

        AgentsDTO agentDto = new AgentsDTO(
                agent.name(),
                agent.ultPoints(),
                agent.function(),
                agent.imgAgent(),
                agent.description());

        List<SkillDTO> skills = catalog.skills().stream()
                .filter(skill -> agent.name().equalsIgnoreCase(skill.agentName()))
                .toList();

        return ResponseEntity.ok().cacheControl(httpCache.get())
                .body(new AgentsWithSkillsDTO(agentDto, skills));
    }

    private Optional<AgentResponseDTO> findAgent(String rawName) {
        String trimmed = rawName.trim();
        String wanted = ALIASES.getOrDefault(trimmed.toLowerCase(Locale.ROOT), trimmed);
        return catalog.agents().stream()
                .filter(agent -> agent.name().equalsIgnoreCase(wanted))
                .findFirst();
    }
}
