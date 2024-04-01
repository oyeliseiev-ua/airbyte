/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.singlestore;

import io.airbyte.cdk.db.factory.DatabaseDriver;
import io.airbyte.cdk.testutils.TestDatabase;
import io.airbyte.integrations.source.singlestore.SingleStoreTestDatabase.SingleStoreConfigBuilder;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.SQLDialect;

public class SingleStoreTestDatabase extends
    TestDatabase<AirbyteSingleStoreTestContainer, SingleStoreTestDatabase, SingleStoreConfigBuilder> {

  public enum BaseImage {

    SINGLESTORE_DEV("ghcr.io/singlestore-labs/singlestoredb-dev:latest");

    public final String reference;

    BaseImage(String reference) {
      this.reference = reference;
    }

  }

  static public SingleStoreTestDatabase in(BaseImage baseImage) {
    final var container = new SingleStoreContainerFactory().shared(baseImage.reference);
    return new SingleStoreTestDatabase(container).initialized();
  }

  public SingleStoreTestDatabase(AirbyteSingleStoreTestContainer container) {
    super(container);
  }

  @Override
  protected Stream<Stream<String>> inContainerBootstrapCmd() {
    final var sql = Stream.of(String.format("CREATE DATABASE %s", getDatabaseName()),
        String.format("CREATE USER %s IDENTIFIED BY '%s'", getUserName(), getPassword()),
        String.format("GRANT ALL ON %s.* TO %s", getDatabaseName(), getUserName()));
    container.withUsername(getUserName()).withPassword(getPassword())
        .withDatabaseName(getDatabaseName());
    return Stream.of(singlestoreCmd(sql));
  }

  @Override
  protected Stream<String> inContainerUndoBootstrapCmd() {
    return singlestoreCmd(Stream.of(String.format("DROP USER %s", getUserName()),
        String.format("DROP DATABASE \\`%s\\`", getDatabaseName())));
  }

  @Override
  public DatabaseDriver getDatabaseDriver() {
    return DatabaseDriver.SINGLESTORE;
  }

  @Override
  public SQLDialect getSqlDialect() {
    return SQLDialect.DEFAULT;
  }

  @Override
  public SingleStoreConfigBuilder configBuilder() {
    return new SingleStoreConfigBuilder(this);
  }

  public Stream<String> singlestoreCmd(Stream<String> sql) {
    return Stream.of("/bin/bash", "-c", String.format(
        "set -o errexit -o pipefail; echo \"%s\" | singlestore -v -v -v --user=root --password=root",
        sql.collect(Collectors.joining("; "))));
  }

  static public class SingleStoreConfigBuilder extends
      ConfigBuilder<SingleStoreTestDatabase, SingleStoreConfigBuilder> {

    protected SingleStoreConfigBuilder(SingleStoreTestDatabase testDatabase) {
      super(testDatabase);
    }
  }
}
