package com.marketplace.annotations;

import com.marketplace.config.TestcontainersConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest(properties = {"scheduler.enabled=false"})
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
public @interface SpringIntegrationTest {
}
