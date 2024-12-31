package com.mocd.uaepass.service.impl;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.service.*;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.mocd.uaepass.configuration.UaePassConfiguration;
import com.mocd.uaepass.service.UaePassService;
import com.mocd.uaepass.constants.UaePassConstants;
import com.mocd.uaepass.exception.UaePassException;
import com.mocd.uaepass.util.CustomFieldsUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component(
        immediate = true,
        service = UaePassService.class,
        configurationPid = UaePassConstants.Config.COMPONENT_PID
)
public class UaePassServiceImpl implements UaePassService {

    private static final Log _log = LogFactoryUtil.getLog(UaePassServiceImpl.class);

    private UaePassConfiguration configuration;

    @Reference
    private UserLocalService userLocalService;


    @Activate
    @Modified
    protected void activate(Map<String, Object> properties) {
        try{
            _log.debug("in activate");
            configuration = ConfigurableUtil.createConfigurable(
                    UaePassConfiguration.class, properties);
            CustomFieldsUtil.initializeCustomFields();
            _log.debug("custom fields initialized");
        }catch (Exception e) {
            _log.error("Failed to activate uae pass service ",e);
        }

    }

    @Override
    public String getAuthorizationToken(String code) throws UaePassException {
        _log.debug("Starting token exchange for code: " + code);
        HttpURLConnection conn = null;

        try {
            String tokenUrl = configuration.baseUrl() + UaePassConstants.Endpoints.TOKEN + "?code=" + code;

            URL url = new URL(tokenUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            _log.debug("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONObject response = readResponse(conn.getInputStream());
                return response.toString();
            } else {
                String errorMessage = readResponse(conn.getErrorStream()).toString();
                throw new UaePassException("Token exchange failed with HTTP " + responseCode +
                        ". Error: " + errorMessage);
            }
        } catch (Exception e) {
            throw new UaePassException("Failed to get authorization token", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public void validateRequest(HttpServletRequest request) {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        StringBuffer fullUrl = request.getRequestURL();
        if (request.getQueryString() != null) {
            fullUrl.append("?").append(request.getQueryString());
        }
        _log.debug("callback URL: " + fullUrl.toString());

        _log.debug("state param: " + state);
        String error = request.getParameter("error");
        if (!configuration.clientId().equals(state)) {
            throw new IllegalArgumentException("Invalid state parameter: " + state);
        }
        if (Validator.isNotNull(error)) {
            throw new IllegalArgumentException("UAE Pass error: ");
        }
        if (Validator.isNull(code)) {
            throw new IllegalArgumentException("Authorization code is required");
        }
    }

    @Override
    public User authenticateUser(String tokenResponse) throws UaePassException {
        try {
            JSONObject tokenJson = JSONFactoryUtil.createJSONObject(tokenResponse);
            JSONObject userInfo = tokenJson.getJSONObject("userInfo");
            _log.debug("Token json "+tokenJson);
            String email = tokenJson.getString("email");

            User user = userLocalService.fetchUserByEmailAddress(
                    PortalUtil.getDefaultCompanyId(), email);


            if (user == null) {
                user = createLiferayUser(tokenJson);
                setUserCustomFields(user, userInfo,
                        tokenJson.getString("crmUserId"),
                        tokenJson.getString("access_token"),
                        tokenJson.getString("userIdentifier"));
            }else {
                _log.debug("Found existing user: " + email);
                setUserCustomFields(user, userInfo,
                        tokenJson.getString("crmUserId"),
                        tokenJson.getString("access_token"),
                        tokenJson.getString("userIdentifier"));
                _log.debug("Updated custom fields for user: " + email);

            }
            return user;

        } catch (Exception e) {
            throw new UaePassException("Failed to authenticate user");
        }
    }


    private JSONObject readResponse(java.io.InputStream stream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        try {
            return JSONFactoryUtil.createJSONObject(response.toString());
        } catch (Exception e) {
            throw new IOException("Invalid response format", e);
        }
    }

    private User createLiferayUser(JSONObject tokenResponse) throws Exception {
        JSONObject userInfo = tokenResponse.getJSONObject("userInfo");
        String email = tokenResponse.getString("email");
        String firstNameEN = userInfo.getString("firstnameEN");
        String lastNameEN = userInfo.getString("lastnameEN");
        String gender = userInfo.getString("gender");
        String mobile = userInfo.getString("mobile");
        boolean isMale = "MALE".equalsIgnoreCase(gender);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);
        try {
            User user = UserLocalServiceUtil.addUser(
                    0L, PortalUtil.getDefaultCompanyId(), true, StringPool.BLANK, StringPool.BLANK,
                    true, StringPool.BLANK, email, LocaleUtil.getDefault(), firstNameEN, StringPool.BLANK,
                    lastNameEN, 0L, 0L, isMale,
                    Calendar.JANUARY, 1, 1970, StringPool.BLANK, 1,
                    null, null, null, null,
                    true, serviceContext);

            setUserContact(user, mobile);
            return user;

        } catch (Exception e) {
            _log.error("Error creating user from UAE Pass data", e);
            throw new PortalException("Failed to create user", e);
        }
    }

    private void setUserContact(User user, String mobile) throws Exception {
        if (Validator.isNotNull(mobile)) {
            Contact contact = user.getContact();

            ListType phoneType = ListTypeLocalServiceUtil.getListType(
                    user.getCompanyId(),
                    "personal",
                    ListTypeConstants.CONTACT_PHONE
            );

            Phone phone = PhoneLocalServiceUtil.createPhone(
                    CounterLocalServiceUtil.increment(Phone.class.getName()));

            phone.setCompanyId(contact.getCompanyId());
            phone.setUserId(contact.getUserId());
            phone.setUserName(contact.getFullName());
            phone.setClassName(Contact.class.getName());
            phone.setClassPK(contact.getContactId());
            phone.setNumber(mobile);
            phone.setListTypeId(phoneType.getListTypeId());
            phone.setPrimary(true);

            PhoneLocalServiceUtil.addPhone(phone);
        }
    }

    private void setUserCustomFields(User user, JSONObject userInfo,
                                     String crmUserId, String accessToken,String userIdentifier) throws Exception {

        Map<String, String> customFields = new HashMap<>();
        customFields.put("accessToken", accessToken);
        customFields.put("crmUserId", crmUserId);
        customFields.put("fullNameAR", userInfo.getString("fullnameAR"));
        customFields.put("fullNameEN", userInfo.getString("fullnameEN"));
        customFields.put("uuid", userInfo.getString("uuid"));
        customFields.put("lastNameAR", userInfo.getString("lastnameAR"));
        customFields.put("idn", userInfo.getString("idn"));
        customFields.put("nationalityEN", userInfo.getString("nationalityEN"));
        customFields.put("userType", userInfo.getString("userType"));
        customFields.put("nationalityAR", userInfo.getString("nationalityAR"));
        customFields.put("firstNameAR", userInfo.getString("firstnameAR"));
        customFields.put("userIdentifier", userIdentifier);
        customFields.put("mobileNumber", userInfo.getString("mobile"));

        ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getDefaultTable(
                user.getCompanyId(), User.class.getName());

        for (Map.Entry<String, String> entry : customFields.entrySet()) {
            try {
                ExpandoColumn expandoColumn = ExpandoColumnLocalServiceUtil
                        .getColumn(expandoTable.getTableId(), entry.getKey());

                if (expandoColumn != null && entry.getValue() != null) {
                    ExpandoValueLocalServiceUtil.addValue(
                            user.getCompanyId(),
                            User.class.getName(),
                            expandoTable.getName(),
                            expandoColumn.getName(),
                            user.getUserId(),
                            entry.getValue()
                    );
                }
            } catch (Exception e) {
                _log.error("Error setting custom field: " + entry.getKey(), e);
            }
        }
    }


}