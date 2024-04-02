/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.singlestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.airbyte.cdk.db.jdbc.JdbcUtils;
import io.airbyte.cdk.integrations.source.jdbc.test.JdbcSourceAcceptanceTest;
import io.airbyte.integrations.source.singlestore.SingleStoreTestDatabase.BaseImage;
import io.airbyte.protocol.models.v0.AirbyteConnectionStatus;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SinglestoreJdbcSourceAcceptanceTest extends
    JdbcSourceAcceptanceTest<SinglestoreSource, SingleStoreTestDatabase> {

  protected static final String USERNAME_WITHOUT_PERMISSION = "new_user";
  protected static final String PASSWORD_WITHOUT_PERMISSION = "new_password";
  private static final Logger LOGGER = LoggerFactory.getLogger(
      SinglestoreJdbcSourceAcceptanceTest.class);

  @AfterEach
  public void tearDown() {
    try {
      testdb.close();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  @Override
  protected void maybeSetShorterConnectionTimeout(final JsonNode config) {
    ((ObjectNode) config).put(JdbcUtils.JDBC_URL_PARAMS_KEY, "connectTimeout=1000");
  }

  @Override
  protected JsonNode config() {
    return testdb.testConfigBuilder().build();
  }

  @Override
  protected SinglestoreSource source() {
    return new SinglestoreSource();
  }

  @Override
  protected SingleStoreTestDatabase createTestDatabase() {
    return SingleStoreTestDatabase.in(BaseImage.SINGLESTORE_DEV);
  }

  @Override
  public boolean supportsSchemas() {
    return false;
  }

  @Test
  void testCheckIncorrectPasswordFailure() throws Exception {
    final var config = config();
    maybeSetShorterConnectionTimeout(config);
    ((ObjectNode) config).put(JdbcUtils.PASSWORD_KEY, "fake");
    final AirbyteConnectionStatus status = source().check(config);
    assertEquals(AirbyteConnectionStatus.Status.FAILED, status.getStatus());
    assertTrue(status.getMessage().contains("State code: 28000; Error code: 1045;"),
        status.getMessage());
  }

  @Test
  public void testCheckIncorrectUsernameFailure() throws Exception {
    final var config = config();
    maybeSetShorterConnectionTimeout(config);
    ((ObjectNode) config).put(JdbcUtils.USERNAME_KEY, "fake");
    final AirbyteConnectionStatus status = source().check(config);
    assertEquals(AirbyteConnectionStatus.Status.FAILED, status.getStatus());
    assertTrue(status.getMessage().contains("State code: 28000; Error code: 1045;"),
        status.getMessage());
  }

  @Test
  public void testCheckIncorrectHostFailure() throws Exception {
    final var config = config();
    maybeSetShorterConnectionTimeout(config);
    ((ObjectNode) config).put(JdbcUtils.HOST_KEY, "localhost2");
    final AirbyteConnectionStatus status = source().check(config);
    assertEquals(AirbyteConnectionStatus.Status.FAILED, status.getStatus());
    assertTrue(status.getMessage().contains("State code: 08000;"), status.getMessage());
  }

  @Test
  public void testCheckIncorrectPortFailure() throws Exception {
    final var config = config();
    maybeSetShorterConnectionTimeout(config);
    ((ObjectNode) config).put(JdbcUtils.PORT_KEY, "0000");
    final AirbyteConnectionStatus status = source().check(config);
    assertEquals(AirbyteConnectionStatus.Status.FAILED, status.getStatus());
    assertTrue(status.getMessage().contains("State code: 08000;"), status.getMessage());
  }

  @Test
  public void testCheckIncorrectDataBaseFailure() throws Exception {
    final var config = config();
    maybeSetShorterConnectionTimeout(config);
    ((ObjectNode) config).put(JdbcUtils.DATABASE_KEY, "wrongdatabase");
    final AirbyteConnectionStatus status = source().check(config);
    assertEquals(AirbyteConnectionStatus.Status.FAILED, status.getStatus());
    assertTrue(status.getMessage().contains("State code: 42000; Error code: 1044;"),
        status.getMessage());
  }

  @Test
  public void testUserHasNoPermissionToDataBase() throws Exception {
    final var config = config();
    maybeSetShorterConnectionTimeout(config);
    final String usernameWithoutPermission = testdb.withNamespace(USERNAME_WITHOUT_PERMISSION);
    testdb.singlestoreCmd(Stream.of(
        String.format("CREATE USER '%s'@'%%' IDENTIFIED BY '%s';", usernameWithoutPermission,
            PASSWORD_WITHOUT_PERMISSION))).forEach(c -> {
      try {
        testdb.container.execInContainer(c);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    ((ObjectNode) config).put(JdbcUtils.USERNAME_KEY, usernameWithoutPermission);
    ((ObjectNode) config).put(JdbcUtils.PASSWORD_KEY, PASSWORD_WITHOUT_PERMISSION);
    final AirbyteConnectionStatus status = source().check(config);
    assertEquals(AirbyteConnectionStatus.Status.FAILED, status.getStatus());
    assertTrue(status.getMessage().contains("State code: 28000; Error code: 1045;"),
        status.getMessage());
  }
}
