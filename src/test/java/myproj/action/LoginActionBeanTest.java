/*
 * This file is part of Stripes-Spring-Testing.
 * 
 * Stripes-Spring-Testing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Stripes-Spring-Testing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Stripes-Spring-Testing. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2011 Marcus Krassmann, Email: marcus.krassmann@gmail.com
 */
package myproj.action;

import base.StripesTestFixture;
import myproj.exception.WrongPasswordException;
import myproj.model.User;
import myproj.service.LoginService;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.ValidationErrors;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginActionBeanTest extends StripesTestFixture {
    public static final User USER = new User();

    private static final Class<LoginActionBean> CLAZZ = LoginActionBean.class;
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpass";
    @Autowired
    private LoginService loginService;
    private MockHttpSession session;
    private MockRoundtrip trip;

    @Before
    public void initFixture() throws Exception {
        initMockService();
        resetStripesMocks();
    }

    private void initMockService() throws Exception {
    	reset(loginService);
        when(loginService.login(USERNAME, PASSWORD)).thenReturn(USER);
    }

    private void resetStripesMocks() {
        session = new MockHttpSession(CTX);
        trip = new MockRoundtrip(CTX, CLAZZ, session);
    }

    @Test
    public void validCredentials() throws Exception {
        trip.setParameter("username", USERNAME);
        trip.setParameter("password", PASSWORD);

        trip.execute("login");

        /* The call of verify is usually not needed:
         * The event handler relies on a valid User returned by the loginService. */
        verify(loginService).login(USERNAME, PASSWORD); 
        
        // From here on the real unit tests
        LoginActionBean bean = trip.getActionBean(CLAZZ);
        assertThat(bean.getUser(), notNullValue());
        assertThat(trip.getValidationErrors().size(), is(0));
        assertThat(trip.getRedirectUrl(), is("/mock_ctx" + LoginActionBean.SUCCESS_JSP));
        assertThat((User) session.getAttribute("user"), is(USER));
    }

    @Test
    public void wrongCredentials() throws Exception {
        String wrongpass = "wrongpass";
        when(loginService.login(USERNAME, wrongpass)).thenThrow(new WrongPasswordException());
        trip.setParameter("username", USERNAME);
        trip.setParameter("password", wrongpass);

        trip.execute("login");

        /* The call of verify is usually not needed:
         * The event handler relies on a valid User returned by the loginService. */
        verify(loginService).login(USERNAME, wrongpass);
        
        // From here on the real unit tests
        assertThat(trip.getForwardUrl(), equalTo(MockRoundtrip.DEFAULT_SOURCE_PAGE));
        assertThat(trip.getValidationErrors().size(), is(1));
        assertThat(session.getAttribute("user"), is(nullValue()));
    }

    @Test
    public void noCredentials() throws Exception {
        trip.execute("login");

        // From here on the real unit tests
        ValidationErrors errors = trip.getValidationErrors();
        assertThat(errors.size(), is(2));
        LocalizableError error = (LocalizableError) errors.get("username").get(0);
        assertThat(error.getMessageKey(), equalTo("validation.required.valueNotPresent"));
        error = (LocalizableError) errors.get("password").get(0);
        assertThat(error.getMessageKey(), equalTo("validation.required.valueNotPresent"));
        assertThat(session.getAttribute("user"), is(nullValue()));
    }
}
