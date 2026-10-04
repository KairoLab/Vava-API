package com.helper.vavahelper.models.Agents.body;

/** Mesmo formato JSON que a entidade Agents tinha, mas imutavel e segura para ficar em cache. */
public record AgentResponseDTO(
        Integer id,
        String name,
        int ultPoints,
        String function,
        String iconAgent,
        String imgAgent,
        String description) {}
