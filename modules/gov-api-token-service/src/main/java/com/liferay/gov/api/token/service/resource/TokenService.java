package com.liferay.gov.api.token.service.resource;
import com.liferay.gov.api.token.service.config.TokenConfiguration;
import com.liferay.gov.api.token.service.dto.TokenResponseDTO;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import org.osgi.service.component.annotations.Modified;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;


@Component(
        configurationPid = "com.liferay.gov.api.token.service.config.TokenConfiguration",
        immediate = true,
        property = {
                "osgi.jaxrs.resource=true"
        },
        service = TokenService.class
)
@Path("/")
public class TokenService {

    public static final Log _log = LogFactoryUtil.getLog(TokenService.class);

    private TokenConfiguration _tokenConfiguration;

    @Activate
    @Modified
    protected void activate(Map<String, Object> properties) {
        _tokenConfiguration = ConfigurableUtil.createConfigurable(
                TokenConfiguration.class, properties);
    }

    @GET
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToken(){
        try{
            String[] tokenInfo = generateToken();
            TokenResponseDTO responseDTO = new TokenResponseDTO(tokenInfo[0], tokenInfo[1]);
            return Response.ok(responseDTO).build();
        }catch (Exception e){
            _log.error("Error while getting token", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        }

    }

    private String[] generateToken() throws Exception {
        String clientId = _tokenConfiguration.clientId();
        String clientSecret = _tokenConfiguration.clientSecret();
        if(clientId.isEmpty() || clientSecret.isEmpty()){
            throw new Exception("Client credentials not configured");
        }

        URL url = new URL("https://apis.government.ae/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        String requestBody = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s",
                URLEncoder.encode(clientId, "UTF-8"),
                URLEncoder.encode(clientSecret, "UTF-8"));

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to get token. Response code: " + conn.getResponseCode());
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(response.toString());
        return new String[]{
                jsonResponse.getString("access_token"),
                jsonResponse.getString(".expires")
        };


    }
}
