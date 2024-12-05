package com.liferay.guest.token.resources;

import com.liferay.oauth2.provider.model.OAuth2Application;
import com.liferay.oauth2.provider.service.OAuth2ApplicationLocalService;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Portal;
;import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component(
        immediate = true,
        property = {
                "osgi.jaxrs.resource=true"
        },
        service = GuestTokenResource.class
)
@Path("/token")
public class GuestTokenResource {
    private static final Log _log = LogFactoryUtil.getLog(GuestTokenResource.class);

    @Reference
    private OAuth2ApplicationLocalService _oAuth2ApplicationLocalService;

    @Reference
    private Portal _portal;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getGuestToken() {
        try {
            long companyId = _portal.getDefaultCompanyId();

            OAuth2Application app = _oAuth2ApplicationLocalService
                    .getOAuth2Applications(companyId)
                    .stream()
                    .filter(a -> "Guest Rating Service".equals(a.getName()))
                    .findFirst()
                    .orElseThrow(() -> new Exception("OAuth2 application not found"));

            String token = getOAuthToken(app.getClientId(), app.getClientSecret());
            return "{\"token\":\"" + token + "\"}";

        }
        catch (Exception e) {
            _log.error("Error generating guest token", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private String getOAuthToken(String clientId, String clientSecret) throws Exception {

        String tokenUrl =   "http://localhost:8080/o/oauth2/token";
        String params = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));

        HttpURLConnection conn = null;
        try {
            URL url = new URL(tokenUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(params.getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject jsonObject = JSONFactoryUtil.createJSONObject(response.toString());
            return jsonObject.getString("access_token");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }



}