package org.folio.finc.select.consumer;

import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;

public class UnselectCollection extends SelectCollectionConsumer {

  private void unselectSingleCollection(
      FincConfigMetadataCollection metadataCollection, String isil, Vertx vertx) {
    boolean removed = metadataCollection.getSelectedBy().remove(isil);
    if (removed) {
      saveMetadataCollection(metadataCollection, vertx);
    }
  }

  @Override
  public void apply(FincConfigMetadataCollection metadataCollection, String isil, Vertx vertx) {
    unselectSingleCollection(metadataCollection, isil, vertx);
  }
}
