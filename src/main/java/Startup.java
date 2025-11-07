import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class Startup {

    @Inject
    @RedisClientName("simple")
    RedisDataSource simple;

    @Inject
    @Any
    InjectableInstance<RedisDataSource> dataSources;

    void onStartup(@Observes StartupEvent ev) {
        System.out.println("Application is starting up!");
        Config config = ConfigProvider.getConfig();
        for (var dsHandle : dataSources.handles()) {
            var qualifiers = dsHandle.getBean().getQualifiers();
            var named = qualifiers.stream()
                    .filter(v -> v instanceof RedisClientName)
                    .map(v -> ((RedisClientName) v).value())
                    .findFirst()
                    .orElse("default");

            // Build the effective config key for hosts; this value already reflects env overrides.
            String configKey = named.equals("default") ? "quarkus.redis.hosts" : "quarkus.redis." + named + ".hosts";
            String hosts = config.getOptionalValue(configKey, String.class).orElse("(not configured)");

            System.out.println("===========================================");
            System.out.println("Named Redis Client: " + named);
            System.out.println("Effective hosts value: " + hosts);
        }

        System.out.println("--- Trying to select client2 connection ---");
        RedisDataSource ds2 = Arc.container().instance(RedisDataSource.class, RedisClientName.Literal.of("client2")).get();
        System.out.println("ds2 found: " + (ds2 != null));

        System.out.println("--- Trying to select client3 connection from runtime ---");
        RedisDataSource ds3 = Arc.container().instance(RedisDataSource.class, RedisClientName.Literal.of("client3")).get();
        System.out.println("ds3 found: " + (ds3 != null));

        System.out.println("--- Trying to select client4 connection from runtime ---");
        RedisDataSource ds4 = Arc.container().instance(RedisDataSource.class, RedisClientName.Literal.of("client4")).get();
        System.out.println("ds4 found: " + (ds4 != null));

        // Optionally list all redis host properties discovered (debug aid)
        System.out.println("--- All redis host properties detected ---");
        config.getPropertyNames().forEach(p -> {
            if (p.startsWith("quarkus.redis") && p.endsWith("hosts")) {
                System.out.println(p + " = " + config.getOptionalValue(p, String.class).orElse("(unset)"));
            }
        });
    }
}
