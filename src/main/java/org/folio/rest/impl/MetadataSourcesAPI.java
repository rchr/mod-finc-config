package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.MetadataSource;
import org.folio.rest.jaxrs.model.MetadataSourcesGetOrder;
import org.folio.rest.jaxrs.resource.MetadataSources;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.utils.Constants;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

/**
 * ATTENTION: API works tenant agnostic. Thus, don't use 'x-okapi-tenant' header, but {@value
 * Constants#MODULE_TENANT} as tenant.
 */
public class MetadataSourcesAPI implements MetadataSources {

  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "metadata_sources";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(MetadataSourcesAPI.class);

  public MetadataSourcesAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  @Override
  @Validate
  public void getMetadataSources(
      String query,
      String orderBy,
      MetadataSourcesGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting metadata sources");
    try {
      CQLWrapper cql = getCQL(query, limit, offset);
      vertxContext.runOnContext(
          v -> {
            String tenantId = Constants.MODULE_TENANT;
            String field = "*";
            String[] fieldList = {field};
            try {
              PostgresClient.getInstance(vertxContext.owner(), tenantId)
                  .get(
                      TABLE_NAME,
                      MetadataSource.class,
                      fieldList,
                      cql,
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.MetadataSources sourcesCollection =
                                new org.folio.rest.jaxrs.model.MetadataSources();
                            List<MetadataSource> sources = reply.result().getResults();
                            sourcesCollection.setMetadataSources(sources);
                            sourcesCollection.setTotalRecords(
                                reply.result().getResultInfo().getTotalRecords());

                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataSourcesResponse.respond200WithApplicationJson(
                                        sourcesCollection)));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataSourcesResponse.respond500WithTextPlain(
                                        messages.getMessage(
                                            lang, MessageConsts.InternalServerError))));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetMetadataSourcesResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetMetadataSourcesResponse.respond400WithTextPlain(
                          "CQL Illegal State Error for '" + "" + "': " + e.getLocalizedMessage())));
            } catch (Exception e) {
              Throwable cause = e;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              logger.debug(
                  "Got error " + cause.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
              if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetMetadataSourcesResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetMetadataSourcesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            }
          });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      if (e.getCause() != null
          && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
        logger.debug("BAD CQL");
        asyncResultHandler.handle(
            Future.succeededFuture(
                GetMetadataSourcesResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetMetadataSourcesResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  @Override
  @Validate
  public void postMetadataSources(
      String lang,
      MetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.post(
        TABLE_NAME,
        entity,
        okapiHeaders,
        vertxContext,
        PostMetadataSourcesResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void getMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting single metadata source by id: " + id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById(
        TABLE_NAME,
        MetadataSource.class,
        id,
        okapiHeaders,
        vertxContext,
        GetMetadataSourcesByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Delete metadata source: " + id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.deleteById(
        TABLE_NAME,
        id,
        okapiHeaders,
        vertxContext,
        DeleteMetadataSourcesByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void putMetadataSourcesById(
      String id,
      String lang,
      MetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Update metadata source: " + id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.put(
        TABLE_NAME,
        entity,
        id,
        okapiHeaders,
        vertxContext,
        PutMetadataSourcesByIdResponse.class,
        asyncResultHandler);
  }
}
