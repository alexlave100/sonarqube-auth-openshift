package org.sonarsource.auth.openshift;

import org.sonar.api.server.ServerSide;
import com.github.scribejava.core.builder.api.DefaultApi20;


@ServerSide
public class OpenShiftApi extends DefaultApi20 {
	
	private final OpenShiftSettings settings;
	private static final String accessTokenEndpointSuffix = "oauth/token";
	private static final String authorizationBaseUrlSuffix = "oauth/authorize";

	public OpenShiftApi(OpenShiftSettings settings) {
		this.settings = settings;		
	}

	@Override
	public String getAccessTokenEndpoint() {
	    return settings.getWebURL() + accessTokenEndpointSuffix;
	}

	@Override
	protected String getAuthorizationBaseUrl() {
	    return settings.getWebURL() + authorizationBaseUrlSuffix;
	}
}
