package io.apiml.security.service.configuration;

import io.apiml.security.service.general.UnauthorizedHandler;
import io.apiml.security.service.login.provider.ServiceLoginAuthenticationProvider;
import io.apiml.security.service.login.filter.ServiceLoginFilter;
import io.apiml.security.service.query.ServiceCookieTokenFilter;
import io.apiml.security.service.query.ServiceHeaderTokenFilter;
import io.apiml.security.service.query.TokenAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ServiceSpringSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final ServiceLoginFilter serviceLoginFilter;
    private final ServiceLoginAuthenticationProvider serviceLoginAuthenticationProvider;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final ServiceCookieTokenFilter serviceCookieFilter;
    private final ServiceHeaderTokenFilter serviceTokenFilter;
    private final UnauthorizedHandler unAuthorizedHandler;

    public ServiceSpringSecurityConfiguration(
        ServiceLoginFilter serviceLoginFilter,
        ServiceLoginAuthenticationProvider serviceLoginAuthenticationProvider,
        TokenAuthenticationProvider tokenAuthenticationProvider,
        ServiceCookieTokenFilter serviceCookieFilter,
        ServiceHeaderTokenFilter serviceTokenFilter,
        UnauthorizedHandler unAuthorizedHandler
    ) {
        this.serviceLoginFilter = serviceLoginFilter;
        this.serviceLoginAuthenticationProvider = serviceLoginAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.serviceCookieFilter = serviceCookieFilter;
        this.serviceTokenFilter = serviceTokenFilter;
        this.unAuthorizedHandler = unAuthorizedHandler;
    }

    @Override
    public void configure(WebSecurity web) {
        //web.ignoring().antMatchers("");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .headers().disable()
            .exceptionHandling().authenticationEntryPoint(unAuthorizedHandler)

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .addFilterBefore(serviceLoginFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, serviceLoginFilter.getLoginPath()).permitAll()

            .and()
            .addFilterBefore(serviceCookieFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers("/**").authenticated();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(serviceLoginAuthenticationProvider);
        auth.authenticationProvider(tokenAuthenticationProvider);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
