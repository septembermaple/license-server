package com.license.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 * 配置管理员认证和授权规则
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 管理员用户名
     */
    @Value("${license.admin.username:admin}")
    private String adminUsername;

    /**
     * 管理员密码
     */
    @Value("${license.admin.password:admin123}")
    private String adminPassword;

    /**
     * 配置安全过滤器链
     *
     * @param http HttpSecurity对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（客户端API不需要）
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 客户端API允许匿名访问（通过加密验证）
                .requestMatchers("/api/license/**").permitAll()
                
                // H2控制台允许访问（开发环境）
                .requestMatchers("/h2-console/**").permitAll()
                
                // 静态资源允许访问
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                
                // 登录页面允许访问
                .requestMatchers("/login").permitAll()
                
                // 管理后台需要认证
                .requestMatchers("/admin/**").authenticated()
                
                // 其他请求允许访问
                .anyRequest().permitAll()
            )
            
            // 配置表单登录
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            
            // 配置登出
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            
            // 配置HTTP Basic认证（用于API调试）
            .httpBasic(AbstractHttpConfigurer::disable);

        // 允许H2控制台的frame（开发环境）
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    /**
     * 配置用户详情服务
     * 使用内存中的管理员账号
     *
     * @return UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * 配置密码编码器
     * 使用BCrypt强哈希算法
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
