/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.singlestore;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singleton;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public final class AirbyteSingleStoreTestContainer extends JdbcDatabaseContainer<AirbyteSingleStoreTestContainer> {
  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
      "ghcr.io/singlestore-labs/singlestoredb-dev");
  static final String DEFAULT_TAG = "latest";
  private static final int PORT = 3306;
  private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 300;
  private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 120;

  // Container defaults
  static final String DEFAULT_DATABASE_NAME = "s2db";
  static final String ROOT_USER = "root";
  static final String DEFAULT_ROOT_USER_PASSWORD = "root";

  // Test container defaults

  // Restricted user and database names
  private static final List<String> SINGLESTORE_USERS = Arrays.asList(ROOT_USER);

  private String databaseName = DEFAULT_DATABASE_NAME;
  private String username = ROOT_USER;
  private String password = DEFAULT_ROOT_USER_PASSWORD;
  private static final String SINGLESTORE_LICENSE = System.getenv("SINGLESTORE_LICENSE");

  public AirbyteSingleStoreTestContainer() {
    this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
  }

  public AirbyteSingleStoreTestContainer(final String dockerImageName) {
    this(DockerImageName.parse(dockerImageName));
  }

  public AirbyteSingleStoreTestContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    preconfigure();
  }

  public AirbyteSingleStoreTestContainer(final Future<String> dockerImageName) {
    super(dockerImageName);
    preconfigure();
  }

  private void preconfigure() {
    this.waitStrategy = new LogMessageWaitStrategy().withRegEx(
            ".*Log Opened*\\s").withTimes(1)
        .withStartupTimeout(Duration.of(DEFAULT_STARTUP_TIMEOUT_SECONDS, SECONDS));
    this.withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    this.addExposedPorts(PORT);
  }

  @Override
  protected void waitUntilContainerStarted() {
    getWaitStrategy().waitUntilReady(this);
  }

  @Override
  public Set<Integer> getLivenessCheckPortNumbers() {
    return singleton(getMappedPort(PORT));
  }

  @Override
  public String getDriverClassName() {
    return "com.singlestore.jdbc.Driver";
  }

  @Override
  public String getJdbcUrl() {
    return String.format("jdbc:singlestore://%s:%d/%s", getHost(), getPort(), getDatabaseName());
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public AirbyteSingleStoreTestContainer withUsername(final String username) {
    if (StringUtils.isEmpty(username)) {
      throw new IllegalArgumentException("Username cannot be null or empty");
    }
    if (SINGLESTORE_USERS.contains(username.toLowerCase())) {
      throw new IllegalArgumentException("Username cannot be one of " + SINGLESTORE_USERS);
    }
    this.username = username;
    return self();
  }

  @Override
  public AirbyteSingleStoreTestContainer withPassword(final String password) {
    if (StringUtils.isEmpty(password)) {
      throw new IllegalArgumentException("Password cannot be null or empty");
    }
    this.password = password;
    return self();
  }

  @Override
  public AirbyteSingleStoreTestContainer withDatabaseName(final String databaseName) {
    if (StringUtils.isEmpty(databaseName)) {
      throw new IllegalArgumentException("Database name cannot be null or empty");
    }

    if (DEFAULT_DATABASE_NAME.equals(databaseName.toLowerCase())) {
      throw new IllegalArgumentException("Database name cannot be set to " + DEFAULT_DATABASE_NAME);
    }
    this.databaseName = databaseName;
    return self();
  }

  @Override
  public AirbyteSingleStoreTestContainer withUrlParam(final String paramName,
      final String paramValue) {
    throw new UnsupportedOperationException(
        "The SingleStore Database driver does not support this");
  }

  public Integer getPort() {
    return getMappedPort(PORT);
  }

  @Override
  public String getTestQueryString() {
    return "SELECT 1";
  }

  @Override
  protected void configure() {
    withEnv("ROOT_PASSWORD", password);
    withEnv("SINGLESTORE_LICENSE", SINGLESTORE_LICENSE);
  }

}
