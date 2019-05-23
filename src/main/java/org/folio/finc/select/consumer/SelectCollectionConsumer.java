package org.folio.finc.select.consumer;

import io.vertx.core.Vertx;
import org.folio.rest.impl.SaveMetadataCollectionHandler;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public abstract class SelectCollectionConsumer {

  private static final String METADATA_COLLECTIONS_TABLE = "metadata_collections";

  public abstract void apply(FincConfigMetadataCollection metadataCollection, String isil, Vertx vertx);

  protected void saveMetadataCollection(
    FincConfigMetadataCollection metadataCollection, Vertx vertx) {
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .update(
            METADATA_COLLECTIONS_TABLE,
            metadataCollection,
            metadataCollection.getId(),
            asyncResult -> {
              if (asyncResult.succeeded()) {
                SaveMetadataCollectionHandler saveMetadataCollectionHandler =
                    new SaveMetadataCollectionHandler();
                saveMetadataCollectionHandler.handle(asyncResult);
              } else {
                System.out.println("FFFF");
              }
            });
  }


}
