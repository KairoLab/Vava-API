package com.helper.vavahelper.models.User.body;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(max = 128) String newPassword) {}
