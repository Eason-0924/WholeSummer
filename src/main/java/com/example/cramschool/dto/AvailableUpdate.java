package com.example.cramschool.dto;

import java.net.URI;

public record AvailableUpdate(
		String currentVersion,
		String latestVersion,
		String releaseNotes,
		String assetName,
		URI downloadUri) {
}
