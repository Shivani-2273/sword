package com.mocd.uaepass.util;
import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.PortalUtil;
import java.util.Arrays;
import java.util.List;

public class CustomFieldsUtil {

    private static final Log _log = LogFactoryUtil.getLog(CustomFieldsUtil.class);

    // Define all required custom fields
    private static final List<String> CUSTOM_FIELDS = Arrays.asList(
            "accessToken",
            "crmUserId",
            "fullNameAR",
            "fullNameEN",
            "uuid",
            "firstNameAR",
            "lastNameAR",
            "idn",
            "nationalityEN",
            "nationalityAR",
            "userType",
            "userIdentifier",
            "mobileNumber"

    );


    public static void initializeCustomFields() {
        try {
            long companyId = PortalUtil.getDefaultCompanyId();
            ExpandoTable expandoTable = getOrCreateExpandoTable(companyId);
            createRequiredCustomFields(expandoTable);
        } catch (Exception e) {
            _log.error("Failed to initialize custom fields", e);
        }
    }

    private static ExpandoTable getOrCreateExpandoTable(long companyId) throws PortalException {
        try {
            return ExpandoTableLocalServiceUtil.getDefaultTable(
                    companyId, User.class.getName());
        } catch (PortalException e) {
            _log.debug("Creating new expando table for User class");
            return ExpandoTableLocalServiceUtil.addDefaultTable(
                    companyId, User.class.getName());
        }
    }

    private static void createRequiredCustomFields(ExpandoTable expandoTable) throws PortalException {
        for (String fieldName : CUSTOM_FIELDS) {
            createCustomFieldIfNotExists(expandoTable, fieldName);
        }
    }

    private static void createCustomFieldIfNotExists(
            ExpandoTable expandoTable,
            String fieldName) throws PortalException {

        try {
            ExpandoColumnLocalServiceUtil.getColumn(
                    expandoTable.getTableId(), fieldName);
            _log.debug("Custom field already exists: " + fieldName);
        } catch (PortalException e) {
            _log.debug("Creating custom field: " + fieldName);

            ExpandoColumn column = ExpandoColumnLocalServiceUtil.addColumn(
                    expandoTable.getTableId(),
                    fieldName,
                    ExpandoColumnConstants.STRING
            );

            ExpandoColumnLocalServiceUtil.updateExpandoColumn(column);
        }
    }

}