package com.spotify.widget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class BackendApplication {

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://127.0.0.1:5500",
                                "http://127.0.0.1:8888",
                                "http://localhost:5500",
                                "http://localhost:8888"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String baseUrl = "http://127.0.0.1:" + port;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("🎵 SPOTIFY WIDGET APPLICATION STARTED SUCCESSFULLY! 🎵");
        System.out.println("=".repeat(60));
        System.out.println("📱 Widget URL: " + baseUrl + "/");
        System.out.println("🔐 Login URL:  " + baseUrl + "/login");
        System.out.println("🎧 Current Song API: " + baseUrl + "/current-song");
        System.out.println("=".repeat(60));
        System.out.println("👆 Click the Login URL to authenticate with Spotify first!");
        System.out.println("=".repeat(60) + "\n");
    }
}