package util;


import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import javax.ws.rs.core.Response;

public class ResponseUtil {
    public static Response createErrorResponse(Response.Status status, String message) {
        JSONObject error = JSONFactoryUtil.createJSONObject();
        error.put("error", message);
        return Response.status(status).entity(error.toString()).build();
    }

    public static Response createSuccessResponse(String message) {
        JSONObject success = JSONFactoryUtil.createJSONObject();
        success.put("message", message);
        return Response.ok(success.toString()).build();
    }
}