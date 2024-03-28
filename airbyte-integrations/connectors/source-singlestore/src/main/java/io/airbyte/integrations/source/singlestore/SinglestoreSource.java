/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.singlestore;

import static io.airbyte.cdk.db.jdbc.JdbcUtils.EQUALS;
import static io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils.CLIENT_KEY_STORE_PASS;
import static io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils.CLIENT_KEY_STORE_TYPE;
import static io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils.CLIENT_KEY_STORE_URL;
import static io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils.TRUST_KEY_STORE_PASS;
import static io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils.TRUST_KEY_STORE_TYPE;
import static io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils.TRUST_KEY_STORE_URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.airbyte.cdk.db.factory.DatabaseDriver;
import io.airbyte.cdk.db.jdbc.JdbcUtils;
import io.airbyte.cdk.db.jdbc.streaming.AdaptiveStreamingQueryConfig;
import io.airbyte.cdk.integrations.base.IntegrationRunner;
import io.airbyte.cdk.integrations.base.Source;
import io.airbyte.cdk.integrations.source.jdbc.AbstractJdbcSource;
import io.airbyte.cdk.integrations.source.jdbc.JdbcSSLConnectionUtils;
import io.airbyte.commons.json.Jsons;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.JDBCType;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinglestoreSource extends AbstractJdbcSource<JDBCType> implements Source {

  private static final Logger LOGGER = LoggerFactory.getLogger(SinglestoreSource.class);
  public static final String DRIVER_CLASS = DatabaseDriver.SINGLESTORE.driverClassName;

  public SinglestoreSource() {
    super(DRIVER_CLASS, AdaptiveStreamingQueryConfig::new, JdbcUtils.defaultSourceOperations);
  }

  @Override
  public JsonNode toDatabaseConfig(final JsonNode config) {
    final String encodedDatabaseName = URLEncoder.encode(
        config.get(JdbcUtils.DATABASE_KEY).asText(), StandardCharsets.UTF_8);
    final StringBuilder jdbcUrl = new StringBuilder(
        String.format("jdbc:singlestore://%s:%s/%s", config.get(JdbcUtils.HOST_KEY).asText(),
            config.get(JdbcUtils.PORT_KEY).asText(), encodedDatabaseName));
    // ensure the return year value is a Date; see the rationale
    jdbcUrl.append("?yearIsDateType=false");
    if (config.get(JdbcUtils.JDBC_URL_PARAMS_KEY) != null && !config.get(
        JdbcUtils.JDBC_URL_PARAMS_KEY).asText().isEmpty()) {
      jdbcUrl.append(JdbcUtils.AMPERSAND)
          .append(config.get(JdbcUtils.JDBC_URL_PARAMS_KEY).asText());
    }
    final Map<String, String> sslParameters = JdbcSSLConnectionUtils.parseSSLConfig(config);
    jdbcUrl.append(JdbcUtils.AMPERSAND).append(toJDBCQueryParams(sslParameters));
    final ImmutableMap.Builder<Object, Object> configBuilder = ImmutableMap.builder()
        .put(JdbcUtils.USERNAME_KEY, config.get(JdbcUtils.USERNAME_KEY).asText())
        .put(JdbcUtils.JDBC_URL_KEY, jdbcUrl.toString());
    if (config.has(JdbcUtils.PASSWORD_KEY)) {
      configBuilder.put(JdbcUtils.PASSWORD_KEY, config.get(JdbcUtils.PASSWORD_KEY).asText());
    }
    return Jsons.jsonNode(configBuilder.build());
  }

  /**
   * Generates SSL related query parameters from map of parsed values.
   *
   * @param sslParams ssl parameters
   * @return SSL portion of JDBC question params or and empty string
   */
  public String toJDBCQueryParams(final Map<String, String> sslParams) {
    return Objects.isNull(sslParams) ? ""
        : sslParams.entrySet().stream().map((entry) -> switch (entry.getKey()) {
              case JdbcSSLConnectionUtils.SSL_MODE ->
                  JdbcSSLConnectionUtils.SSL_MODE + EQUALS + com.singlestore.jdbc.export.SslMode.from(
                      entry.getValue()).name();
              case TRUST_KEY_STORE_URL -> "trustStore" + EQUALS + entry.getValue();
              case TRUST_KEY_STORE_PASS -> "trustStorePassword" + EQUALS + entry.getValue();
              case TRUST_KEY_STORE_TYPE -> "trustStoreType" + EQUALS + entry.getValue();
              case CLIENT_KEY_STORE_URL -> "keyStore" + EQUALS + entry.getValue();
              case CLIENT_KEY_STORE_PASS -> "keyStorePassword" + EQUALS + entry.getValue();
              case CLIENT_KEY_STORE_TYPE -> "keyStoreType" + EQUALS + entry.getValue();
              default -> "";
            }).filter(s -> Objects.nonNull(s) && !s.isEmpty())
            .collect(Collectors.joining(JdbcUtils.AMPERSAND));
  }

  @Override
  public Set<String> getExcludedInternalNameSpaces() {
    return Set.of("information_schema", "memsql", "cluster");
  }

  public static void main(final String[] args) throws Exception {
    final Source source = new SinglestoreSource();
    LOGGER.info("starting source: {}", SinglestoreSource.class);
    new IntegrationRunner(source).run(args);
    LOGGER.info("completed source: {}", SinglestoreSource.class);
  }

}
