package com.liferay.gov.api.token.service.config;

import aQute.bnd.annotation.metatype.Meta;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

@ExtendedObjectClassDefinition(category="external-configuration")
@Meta.OCD(
        id = "com.liferay.gov.api.token.service.config.TokenConfiguration",
        localization = "content/Language",
        name = "API Configuration"
)
public interface TokenConfiguration {

    @Meta.AD(required = false, deflt = "default")
    String clientId();

    @Meta.AD(required = false, deflt = "default")
    String clientSecret();
}
