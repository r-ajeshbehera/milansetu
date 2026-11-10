package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloseRequest(@NotBlank @Size(max = 500) String note) { }
