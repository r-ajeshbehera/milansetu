package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.Size;

public record NoteRequest(@Size(max = 500) String note) { }
