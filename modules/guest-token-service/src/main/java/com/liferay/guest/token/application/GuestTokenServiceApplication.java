package com.liferay.guest.token.application;

import java.util.Collections;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import com.liferay.guest.token.resources.GuestTokenResource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
	property = {
		JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/service",
		JaxrsWhiteboardConstants.JAX_RS_NAME + "=Guest.Token.REST",
		"auth.verifier.guest.allowed=true",
		"liferay.auth.verifier=false"
	},
	service = Application.class
)
public class GuestTokenServiceApplication extends Application {

	@Reference
	private GuestTokenResource guestTokenResource;

	@Override
	public Set<Object> getSingletons() {
		return Collections.<Object>singleton(guestTokenResource);
	}



}