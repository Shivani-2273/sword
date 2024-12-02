package com.mocd.uaepass.security;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auto.login.AutoLogin;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.mocd.uaepass.constants.UaePassConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component(
        immediate = true,
        property = {
                "key.for.login=UAE_PASS",
                "service.ranking:Integer=100"
        },
        service = AutoLogin.class
)
public class UaePassAutoLogin implements AutoLogin {

    private static final Log _log = LogFactoryUtil.getLog(UaePassAutoLogin.class);

    @Reference
    private Portal portal;

    @Reference
    private UserLocalService userLocalService;

    @Override
    public String[] login(HttpServletRequest request,
                          HttpServletResponse response) throws AutoLoginException {

        HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
        HttpSession session = originalRequest.getSession(false);

        try {
            if (session == null) return null;

            String userEmail = (String) session.getAttribute(
                    UaePassConstants.Config.SESSION_EMAIL_KEY);
            if (userEmail == null) return null;


            long companyId = portal.getCompanyId(request);
            User user = userLocalService.fetchUserByEmailAddress(
                    companyId, userEmail);
            _log.info("login with user: " + userEmail);


            String[] credentials = new String[] {
                    String.valueOf(user.getUserId()),
                    user.getPassword(),
                    Boolean.TRUE.toString()
            };

            return credentials;

        } catch (Exception e) {
            throw new AutoLoginException(e);
        }
    }
}