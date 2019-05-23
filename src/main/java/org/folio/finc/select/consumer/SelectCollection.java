package org.folio.finc.select.consumer;

import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;

public class SelectCollection extends SelectCollectionConsumer {

  private void selectSingleCollection(
      FincConfigMetadataCollection metadataCollection, String isil, Vertx vertx) {
    if (!metadataCollection.getSelectedBy().contains(isil)) {
      metadataCollection.getSelectedBy().add(isil);
    }
    saveMetadataCollection(metadataCollection, vertx);
  }

  private void selectSingleMetadataCollectionIfPermitted(
      FincConfigMetadataCollection metadataCollection, String isil, Vertx vertx) {
    if (metadataCollection.getPermittedFor().contains(isil)) {
      selectSingleCollection(metadataCollection, isil, vertx);
    }
  }

  @Override
  public void apply(FincConfigMetadataCollection metadataCollection, String isil, Vertx vertx) {
    selectSingleMetadataCollectionIfPermitted(metadataCollection, isil, vertx);
  }
}
