package org.folio.finc.select;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.Arrays;
import java.util.List;
import org.folio.rest.impl.SaveMetadataCollectionHandler;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

public class SelectMetadataSourceVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(SelectMetadataSourceVerticle.class);

  private static final String METADATA_SOURCES_TABLE = "metadata_sources";
  private static final String METADATA_COLLECTIONS_TABLE = "metadata_collections";
  private static final String ISILS_TABLE = "isils";
  private String metadataSourceId;
  private String tenantId;

  public SelectMetadataSourceVerticle(Vertx vertx, Context ctx) {
    super();
    super.init(vertx, ctx);
  }

  private CQLWrapper getCQLMetadataCollections(String metadataSourceId, String isil)
      throws FieldException {
    /*String query =
    "mdSource.id=\"f6f03fb4-3368-4bc0-bc02-3bf6e19604a5\" AND permittedFor any \"DE-14\"";*/
    String query = "mdSource.id=\"" + metadataSourceId + "\" AND permittedFor any \"" + isil + "\"";
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(METADATA_COLLECTIONS_TABLE + ".jsonb"));
    CQLWrapper cqlWrapper = new CQLWrapper(cql2PgJSON, query);
    return cqlWrapper;
  }

  private CQLWrapper getCQLTenant(String tenantId) throws FieldException {
    String query = "tenant=\"" + tenantId + "\"";
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(ISILS_TABLE + ".jsonb"));
    CQLWrapper cqlWrapper = new CQLWrapper(cql2PgJSON, query);
    return cqlWrapper;
  }

  @Override
  public void start() throws Exception {
    metadataSourceId = config().getString("metadataSourceId");
    tenantId = config().getString("tenantId");
    fetchIsilAndSelectCollectionsOfSource(vertx.getOrCreateContext(), tenantId, metadataSourceId);
  }

  @Override
  public void stop() throws Exception {
    super.stop();
  }

  private void saveMetadataCollection(FincConfigMetadataCollection metadataCollection) {
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .update(
            METADATA_COLLECTIONS_TABLE,
            metadataCollection,
            metadataCollection.getId(),
            asyncResult -> {
              SaveMetadataCollectionHandler saveMetadataCollectionHandler =
                  new SaveMetadataCollectionHandler();
              saveMetadataCollectionHandler.handle(asyncResult);
            });
  }

  private void selectSingleCollection(
      FincConfigMetadataCollection metadataCollection, String isil) {
    if (!metadataCollection.getSelectedBy().contains(isil)) {
      metadataCollection.getSelectedBy().add(isil);
    }
    saveMetadataCollection(metadataCollection);
  }

  private void selectSingleMetadataCollectionIfPermitted(
      FincConfigMetadataCollection metadataCollection, String isil) {
    if (metadataCollection.getPermittedFor().contains(isil)) {
      selectSingleCollection(metadataCollection, isil);
    }
  }

  private void selectCollectionsForIsil(
      List<FincConfigMetadataCollection> fincConfigMetadataCollections, String isil) {
    fincConfigMetadataCollections.stream()
        .forEach(
            metadataCollection -> {
              selectSingleMetadataCollectionIfPermitted(metadataCollection, isil);
            });
  }

  private void fetchAndSelectCollectionsOfSource(
      Context ctx, String isil, String metadataSourceId) {

    String field = "*";
    String[] fieldList = {field};
    try {
      CQLWrapper cql = getCQLMetadataCollections(metadataSourceId, isil);
      PostgresClient.getInstance(ctx.owner(), Constants.MODULE_TENANT)
          .get(
              METADATA_COLLECTIONS_TABLE,
              FincConfigMetadataCollection.class,
              fieldList,
              cql,
              true,
              false,
              reply -> {
                if (reply.succeeded()) {
                  List<FincConfigMetadataCollection> fincConfigMetadataCollections =
                      reply.result().getResults();
                  selectCollectionsForIsil(fincConfigMetadataCollections, isil);
                } else {
                  logger.error("Error while fetching metadatacollections.", reply.cause());
                }
              });
    } catch (Exception e) {
      logger.error("Error while fetching metadatacollections.");
    }
  }

  private void fetchIsilAndSelectCollectionsOfSource(
      Context ctx, String tenantId, String metadataSourceId) {
    try {
      String field = "*";
      String[] fieldList = {field};
      CQLWrapper cql = getCQLTenant(tenantId);
      PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
          .get(
              ISILS_TABLE,
              Isil.class,
              fieldList,
              cql,
              true,
              false,
              resultsAsyncResult -> {
                if (resultsAsyncResult.succeeded()) {
                  List<Isil> isils = resultsAsyncResult.result().getResults();
                  if (isils.size() != 1) {
                    logger.error("Isils.size()!=1 for tenant " + tenantId);
                    return;
                  } else {
                    String isil = isils.get(0).getIsil();
                    fetchAndSelectCollectionsOfSource(ctx, isil, metadataSourceId);
                  }
                } else {
                  logger.error(
                      "Error while fetching isils from db for tenant "
                          + tenantId
                          + ". "
                          + resultsAsyncResult.cause().getMessage());
                }
              });
    } catch (Exception e) {
      logger.error("Error while fetching isils: " + tenantId);
    }
  }
}
