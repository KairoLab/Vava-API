package com.helper.vavahelper.models.User.body;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationDTO(
        @NotBlank @Size(max = 254) String login,
        @NotBlank @Size(max = 128) String password){}
