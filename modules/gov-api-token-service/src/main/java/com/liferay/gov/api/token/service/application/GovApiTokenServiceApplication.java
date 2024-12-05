package com.liferay.gov.api.token.service.application;

import java.util.Collections;
import java.util.Set;
import javax.ws.rs.core.Application;
import com.liferay.gov.api.token.service.resource.TokenService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

@Component(
	property = {
		JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/api/gov",
		JaxrsWhiteboardConstants.JAX_RS_NAME + "=External.Token",
		"auth.verifier.guest.allowed=true",
		"liferay.auth.verifier=false"
	},
	service = Application.class
)
public class GovApiTokenServiceApplication extends Application {

	@Reference
	private TokenService tokenService;

	@Override
	public Set<Object> getSingletons() {
		return Collections.<Object>singleton(tokenService);
	}



}