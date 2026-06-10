package com.spiritlane.config;

import com.spiritlane.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/", "/home", "/auth/**", "/error",
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/products/browse", "/products/detail/**",
                    "/shops/list", "/api/public/**"
                ).permitAll()

                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Shop owner endpoints
                .requestMatchers("/shop/**").hasRole("SHOP_OWNER")

                // Delivery agent endpoints
                .requestMatchers("/delivery/**").hasRole("DELIVERY_AGENT")

                // Customer endpoints
                .requestMatchers("/customer/**", "/cart/**", "/checkout/**", "/orders/**").hasRole("CUSTOMER")

                // Any other request needs authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("spiritlane-remember-me-key")
                .tokenValiditySeconds(7 * 24 * 60 * 60)  // 7 days
                .userDetailsService(userDetailsService)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )
            .sessionManagement(session -> session
                .maximumSessions(2)
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            switch (role) {
                case "ROLE_ADMIN"          -> response.sendRedirect("/admin/dashboard");
                case "ROLE_SHOP_OWNER"     -> response.sendRedirect("/shop/dashboard");
                case "ROLE_DELIVERY_AGENT" -> response.sendRedirect("/delivery/dashboard");
                default                    -> response.sendRedirect("/customer/home");
            }
        };
    }
}
