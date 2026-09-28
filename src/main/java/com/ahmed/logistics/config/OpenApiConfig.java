package com.ahmed.logistics.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Logistics Management System API")
                        .version("1.0.0")
                        .description("Production-grade Logistics Management System API documentation. " +
                                "Provides comprehensive endpoints for shipment lifecycle management, " +
                                "chronological tracking timelines, geocoding and distance routing, driver/vehicle assignments, " +
                                "warehouse movements, proof of delivery, cash on delivery (COD), electronic payments, " +
                                "and asynchronous email/in-app notifications.")
                        .contact(new Contact()
                                .name("Logistics Support Team")
                                .email("support@logistics.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("/").description("Current Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token obtained from `/api/auth/login`.")))
                .tags(List.of(
                        new Tag().name("Authentication").description("User registration, authentication, and JWT token refresh"),
                        new Tag().name("Shipments").description("Shipment creation, lifecycle state transitions, pricing, and assignments"),
                        new Tag().name("Shipment Tracking").description("Append-only chronological tracking timeline and status audits"),
                        new Tag().name("Deliveries").description("Delivery dispatch, execution, and completion"),
                        new Tag().name("Proof of Delivery").description("Proof of delivery records, recipient signatures, and notes"),
                        new Tag().name("Delivery Failures").description("Failed delivery attempt records and failure reasons"),
                        new Tag().name("Delivery Rescheduling").description("Delivery rescheduling requests and attempt tracking"),
                        new Tag().name("Payments").description("Electronic payment creation, verification, and refunds"),
                        new Tag().name("Cash on Delivery").description("Cash on delivery management and driver collection"),
                        new Tag().name("Notifications").description("In-app notifications and unread count queries"),
                        new Tag().name("Customers").description("Customer profile and account management"),
                        new Tag().name("Drivers").description("Driver registration, availability, and vehicle link"),
                        new Tag().name("Vehicles").description("Fleet vehicle inventory and maintenance status"),
                        new Tag().name("Warehouses").description("Warehouse facilities and storage management"),
                        new Tag().name("Warehouse Movements").description("Shipment movement between warehouse facilities"),
                        new Tag().name("Branches").description("Branch office locations and dispatch centers")
                ));
    }
}
