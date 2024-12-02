package com.mocd.uaepass.service;

import com.liferay.portal.kernel.model.User;
import javax.servlet.http.HttpServletRequest;
import com.mocd.uaepass.exception.UaePassException;


public interface UaePassService {
    /**
     * Gets the authorization token from UAE Pass using the provided code
     *
     * @param code The authorization code received from UAE Pass
     * @return String The token response from UAE Pass
     * @throws UaePassException if token retrieval fails
     */
    String getAuthorizationToken(String code) throws UaePassException;


    /**
     * Validates the UAE Pass callback request parameters.
     * Checks for required parameters and validates the state parameter against the configured client ID.
     *
     * @param request The HTTP servlet request containing callback parameters
     * @throws UaePassException if validation fails due to:
     *         - Missing or invalid state parameter
     *         - Missing authorization code
     *         - Presence of error parameter from UAE Pass
     */
    void validateRequest(HttpServletRequest request) throws UaePassException;

    /**
     * Authenticates or creates a user based on UAE Pass token response.
     * If the user exists in the system, returns the existing user.
     * If the user doesn't exist, creates a new user with UAE Pass information and returns it.
     *
     * @param tokenResponse The JSON response containing user information from UAE Pass
     * @return User The authenticated or newly created Liferay user
     * @throws UaePassException if authentication fails or user creation fails
     */
    User authenticateUser(String tokenResponse) throws UaePassException;


}