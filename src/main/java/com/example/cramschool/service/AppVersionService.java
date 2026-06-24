package com.example.cramschool.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class AppVersionService {

	private final ObjectProvider<BuildProperties> buildPropertiesProvider;

	public AppVersionService(ObjectProvider<BuildProperties> buildPropertiesProvider) {
		this.buildPropertiesProvider = buildPropertiesProvider;
	}

	public String currentVersion() {
		BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
		return buildProperties == null ? "開發版本" : buildProperties.getVersion();
	}
}
