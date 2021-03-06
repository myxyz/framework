/*
 *
 *  * Copyright 2013 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package leap.oauth2.as.sso;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import leap.core.annotation.Inject;
import leap.core.security.Authentication;
import leap.core.security.UserPrincipal;
import leap.lang.Arrays2;
import leap.lang.Strings;
import leap.oauth2.as.OAuth2AuthzServerConfig;
import leap.oauth2.as.authc.AuthzAuthentication;
import leap.oauth2.as.client.AuthzClient;
import leap.web.Request;
import leap.web.Response;
import leap.web.security.logout.LogoutContext;

public class DefaultAuthzSSOManager implements AuthzSSOManager {

    protected @Inject OAuth2AuthzServerConfig config;

    @Override
    public void onOAuth2LoginSuccess(Request request, Response response, AuthzAuthentication authc) throws Throwable {
        if(!config.isSingleLoginEnabled()) {
            return;
        }

        Authentication secAuthc = authc.getAuthentication();
        String token = secAuthc.getToken();
        if(null == token) {
            throw new IllegalStateException("The authentication token must be exists");
        }

        AuthzSSOStore ss = config.getSSOStore();

        AuthzSSOSession session = ss.loadSessionByToken(authc.getUserDetails().getLoginName(), token);
        if(null == session) {
            //Creates a new sso session and save it.
            session = newSession(request, response, authc);
            AuthzSSOLogin login = newLogin(request, response, authc, session, true);

            ss.saveSession(session, login);
        }else{
            //Creates a new login and save it in session.
            AuthzSSOLogin login = newLogin(request, response, authc, session, false);
            ss.saveLogin(session, login);
        }
    }

    @Override
    public String[] resolveLogoutUrls(Request request, Response response, LogoutContext context) throws Throwable {
        Authentication authc = context.getAuthentication();
        if(null == authc) {
            return Arrays2.EMPTY_STRING_ARRAY;
        }

        String token = context.getAuthenticationToken();
        if(Strings.isEmpty(token)) {
            throw new IllegalStateException("The authentication token must be exists.");
        }

        AuthzSSOStore ss = config.getSSOStore();
        AuthzSSOSession session = ss.loadSessionByToken(authc.getUser().getLoginName(), token);
        if(null == session) {
            return Arrays2.EMPTY_STRING_ARRAY;
        }

        List<AuthzSSOLogin> logins = ss.loadLoginsInSession(session);

        Set<String> urls = new HashSet<>();
        for(AuthzSSOLogin login : logins){
            if(!Strings.isEmpty(login.getLogoutUri())) {
                urls.add(login.getLogoutUri());
            }
        }
        return urls.toArray(new String[urls.size()]);
    }

    protected AuthzSSOSession newSession(Request request, Response response, AuthzAuthentication authc) {
        SimpleAuthzSSOSession session = new SimpleAuthzSSOSession();

        UserPrincipal user = authc.getAuthentication().getUser();

        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getIdAsString());
        session.setUsername(user.getLoginName());
        session.setToken(authc.getAuthentication().getToken());
        session.setExpiresIn(config.getDefaultLoginSessionExpires());
        session.setCreated(System.currentTimeMillis());

        return session;
    }

    protected AuthzSSOLogin newLogin(Request request, Response response, AuthzAuthentication authc, AuthzSSOSession session, boolean initial) {

        SimpleAuthzSSOLogin login = new SimpleAuthzSSOLogin();

        login.setInitial(initial);
        login.setLoginTime(System.currentTimeMillis());
        login.setLogoutUri(authc.getParams().getLogoutUri());

        AuthzClient client = authc.getClientDetails();
        if(null != client) {
            login.setClientId(client.getId());

            if(Strings.isEmpty(login.getLogoutUri())) {
                login.setLogoutUri(client.getLogoutUri());
            }
        }

        return login;
    }
}