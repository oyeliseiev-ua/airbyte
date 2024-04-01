/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.singlestore;

import io.airbyte.cdk.testutils.ContainerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class SingleStoreContainerFactory extends ContainerFactory<AirbyteSingleStoreTestContainer> {

  @Override
  protected AirbyteSingleStoreTestContainer createNewContainer(DockerImageName imageName) {
    return new AirbyteSingleStoreTestContainer(
        imageName.asCompatibleSubstituteFor("ghcr.io/singlestore-labs/singlestoredb-dev"));
  }

  /**
   * Create a new network and bind it to the container.
   */
  public void withNetwork(AirbyteSingleStoreTestContainer container) {
    container.withNetwork(Network.newNetwork());
  }

  static private void execInContainer(AirbyteSingleStoreTestContainer container,
      String... commands) {
    container.start();
    try {
      for (String command : commands) {
        container.execInContainer("/bin/bash", "-c", command);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
