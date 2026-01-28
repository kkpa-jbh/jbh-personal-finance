package com.jbh.notification.infra.config;

import com.jbh.notification.infra.adapters.in.rest.NotificationRestAdapterV1;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI Configuration for the Notification module.
 *
 * <p>This class ensures proper bean registration for GraalVM native compilation.
 * The actual beans are auto-discovered via their @ApplicationScoped annotations.</p>
 *
 * <p>Beans in this module:</p>
 * <ul>
 *   <li>NotificationService - Orchestrates notification sending</li>
 *   <li>NotificationRepositoryAdapter - Persistence layer</li>
 *   <li>QuarkusMailerAdapter - Email sending via Quarkus Mailer</li>
 *   <li>NotificationJPARepository - Panache JPA repository</li>
 * </ul>
 */
@ApplicationScoped
@RegisterForReflection(targets = {
    com.jbh.notification.infra.service.NotificationService.class,
    com.jbh.notification.infra.adapters.out.persistence.NotificationRepositoryAdapter.class,
    com.jbh.notification.infra.adapters.out.email.QuarkusMailerAdapter.class,
    NotificationRestAdapterV1.class
})
public class NotificationCDIConfig {
}
