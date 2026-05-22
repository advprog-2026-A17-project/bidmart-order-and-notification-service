package id.ac.ui.cs.advprog.bidmartordernotificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!test")
@EnableWebSecurity
public class ActuatorMetricsSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain prometheusSecurity(
            HttpSecurity http,
            @Value("${bidmart.metrics.basic-user:}") String username,
            @Value("${bidmart.metrics.basic-password:}") String password) throws Exception {
        http.securityMatcher("/actuator/prometheus");
        if (username.isBlank() || password.isBlank()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            UserDetails metricsUser = User.builder()
                    .username(username)
                    .password("{noop}" + password)
                    .roles("METRICS")
                    .build();
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .userDetailsService(new InMemoryUserDetailsManager(metricsUser));
        }
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain permitAllSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
