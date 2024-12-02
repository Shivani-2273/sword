package com.mocd.uaepass.constants;

public class UaePassConstants {

    public static final class Endpoints {
            public static final String LOGIN = "/account/login";
            public static final String TOKEN = "/api/identity/getUaePassTokenCustom";
            public static final String LOGOUT = "/api/identity/uaepasslogout";
        }

        public static final class Defaults {
            public static final String BASE_URL = "https://useridentitypre.mocd.gov.ae";
            public static final String CLIENT_ID = "website_dev";
            public static final String ENVIRONMENT = "dev";
        }

        public static final class Config {
            public static final String COMPONENT_PID = "com.mocd.uaepass.configuration.UaePassConfiguration";
            public static final String SESSION_EMAIL_KEY = "UAE_PASS_USER_EMAIL";
        }

}
