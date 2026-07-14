package com.civic.civicbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.civic.civicbackend.repository.UserRepository;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(UserRepository userRepository,JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // Specific rules first
                .requestMatchers(HttpMethod.POST, "/api/complaints").hasRole("USER")
                .requestMatchers("/api/department/**").hasRole("DEPARTMENT_ADMIN")
                .requestMatchers("/api/superadmin/**").hasRole("SUPER_ADMIN")

                // General complaint access (GET etc.)
                .requestMatchers("/api/complaints/**").authenticated()
                .requestMatchers("/api/notifications/**")
                .hasAnyRole("USER", "DEPARTMENT_ADMIN")
               



                .anyRequest().authenticated()
)

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

