package com.itineraryledger.kabengosafaris.Health;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Is the database the one this jar expects?
 *
 * A jar that starts against a schema it does not match is the worst kind of live: it serves most
 * requests and fails whichever one touches the column that is not there. With one deployment you
 * would notice; with one per company the odd one out is silent until a customer finds it.
 *
 * Reported as DOWN when migrations are pending, because "the app is running" and "the app is
 * correct" are different questions and the deploy gate asks the second one.
 *
 * The Flyway bean is optional on purpose — this reports honestly on an instance that still leans on
 * Hibernate's `ddl-auto`, and upgrades itself the moment migrations are wired in.
 */
@Component("schema")
@RequiredArgsConstructor
public class SchemaHealthIndicator implements HealthIndicator {

    /** Present once Flyway is on the classpath and enabled; absent while ddl-auto still rules. */
    private final ObjectProvider<org.flywaydb.core.Flyway> flyway;

    @Override
    public Health health() {
        org.flywaydb.core.Flyway migrator = flyway.getIfAvailable();
        Map<String, Object> detail = new LinkedHashMap<>();

        if (migrator == null) {
            /*
             * Not a failure — a statement of fact. Hibernate is deciding the schema, so nothing can
             * assert the schema matches this jar; the deploy gate should not pretend otherwise.
             */
            detail.put("migrations", "not configured");
            detail.put("note", "schema is managed by hibernate ddl-auto; no version to compare");
            return Health.unknown().withDetails(detail).build();
        }

        var info = migrator.info();
        var current = info.current();
        var pending = info.pending();
        detail.put("current", current != null && current.getVersion() != null
            ? current.getVersion().toString() : "none");
        detail.put("pending", pending.length);

        if (pending.length > 0) {
            String next = pending[0].getVersion() != null
                ? pending[0].getVersion().toString() : pending[0].getDescription();
            return Health.down().withDetails(detail)
                .withDetail("reason", "the database is behind this build")
                .withDetail("nextMigration", next)
                .build();
        }
        return Health.up().withDetails(detail).build();
    }
}
