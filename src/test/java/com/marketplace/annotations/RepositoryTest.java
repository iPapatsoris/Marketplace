package com.marketplace.annotations;

import com.marketplace.config.TestcontainersConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@Target(TYPE)
@Retention(RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
public @interface RepositoryTest {
}
