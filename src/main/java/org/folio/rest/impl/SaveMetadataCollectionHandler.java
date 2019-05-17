package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveMetadataCollectionHandler implements Handler<AsyncResult<UpdateResult>> {

  private static final Logger logger = LoggerFactory.getLogger(SaveMetadataCollectionHandler.class);

  @Override
  public void handle(AsyncResult<UpdateResult> asyncResult) {
    if (asyncResult.succeeded()) {
      logger.info("Successfully saved metadata collection when selected with whole metadata source: " + asyncResult.result());
    } else {
      logger.error("Error while saving metadata collection when selected with whole metadata source: " + asyncResult.result());
    }
  }
}
