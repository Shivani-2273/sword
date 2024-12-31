package com.liferay.sharik.api.token.service.application;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;

import com.liferay.sharik.api.token.service.resource.TokenService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;


@Component(
		property = {
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/api/sharik",
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=External.Token",
				"auth.verifier.guest.allowed=true",
				"liferay.auth.verifier=false"
		},
		service = Application.class
)
public class SharikApiTokenServiceApplication extends Application {

	@Reference
	private TokenService tokenService;

	public Set<Object> getSingletons() {
		return Collections.<Object>singleton(tokenService);
	}



}