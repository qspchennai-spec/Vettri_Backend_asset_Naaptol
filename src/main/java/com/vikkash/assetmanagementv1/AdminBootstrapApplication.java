package com.vikkash.assetmanagementv1;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the profile-gated admin bootstrap and exits after the command completes. */
public final class AdminBootstrapApplication {

    private AdminBootstrapApplication() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(AssetManagementV1Application.class)
                .web(WebApplicationType.NONE)
                .profiles("admin-bootstrap")
                .run(args);

        int exitCode = org.springframework.boot.SpringApplication.exit(context);
        System.exit(exitCode);
    }
}