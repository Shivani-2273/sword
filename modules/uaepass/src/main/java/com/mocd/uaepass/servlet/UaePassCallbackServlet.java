package com.mocd.uaepass.servlet;


import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.util.PortalUtil;
import com.mocd.uaepass.constants.UaePassConstants;
import com.mocd.uaepass.service.UaePassService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

@Component(
        immediate = true,
        property = {
                "osgi.http.whiteboard.context.path=/",
                "osgi.http.whiteboard.servlet.pattern=/token/*"
        },
        service = Servlet.class
)
public class UaePassCallbackServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Log _log = LogFactoryUtil.getLog(UaePassCallbackServlet.class);

    @Reference
    private UaePassService uaePassService;

    @Override
    public void init() throws ServletException {
        _log.debug("Initializing UAE Pass Callback Servlet");
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        _log.debug("Received callback request from UAE Pass");

        try {
            uaePassService.validateRequest(request);
            String code = request.getParameter("code");
            String tokenResponse = uaePassService.getAuthorizationToken(code);
            _log.debug("Successfully received token response");

            // Parse token response to get access token
            HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
            HttpSession session = originalRequest.getSession();

            User user= uaePassService.authenticateUser(tokenResponse);
            session.setAttribute(UaePassConstants.Config.SESSION_EMAIL_KEY, user.getEmailAddress());
            _log.debug("User authenticated successfully: " + user.getEmailAddress());
            response.sendRedirect("/");


        } catch (IllegalArgumentException e) {
            _log.error("Validation failed: " + e.getMessage());
            handleError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            _log.error("Authentication failed: " + e.getMessage());
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Authentication failed");
        }
    }



    private void handleError(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.sendError(statusCode, message);
    }



}
