package com.mocd.uaepass.configuration;


import aQute.bnd.annotation.metatype.Meta;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.mocd.uaepass.constants.UaePassConstants;

@ExtendedObjectClassDefinition(category = "uae-pass")
@Meta.OCD(
        id = UaePassConstants.Config.COMPONENT_PID,
        localization = "content/Language",
        name = "UAE Pass Configuration"
)
public interface UaePassConfiguration {
    @Meta.AD(required = false, deflt = UaePassConstants.Defaults.BASE_URL)
    String baseUrl();

    @Meta.AD(required = false, deflt = UaePassConstants.Defaults.CLIENT_ID)
    String clientId();

}