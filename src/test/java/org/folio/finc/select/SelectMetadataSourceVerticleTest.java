package org.folio.finc.select;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourceVerticleTest {

  private static final Logger logger =
      LoggerFactory.getLogger(SelectMetadataSourceVerticleTest.class);

  private static final String TENANT = "diku";
  private static final String TENANT_UBL = "ubl";
  private static final String APPLICATION_JSON = "application/json";
  private static final String BASE_URI = "/finc-config/metadata-sources";
  private static Vertx vertx;
  private static SelectMetadataSourceVerticle selectMetadataSourceVerticle;
  private static FincConfigMetadataSource metadataSource1;
  private static FincConfigMetadataSource metadataSource2;
  private static FincConfigMetadataCollection metadataCollection1;
  private static FincConfigMetadataCollection metadataCollection2;
  private static FincConfigMetadataCollection metadataCollection3;
  private static Isil isil1;
  private static Isil isil2;
  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context) {
    try {
      String metadataSourceStr1 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataSource.sample")));
      metadataSource1 = Json.decodeValue(metadataSourceStr1, FincConfigMetadataSource.class);
      String metadataSourceStr2 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataSource2.sample")));
      metadataSource2 = Json.decodeValue(metadataSourceStr2, FincConfigMetadataSource.class);

      String metadataCollectionStr1 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataCollection.sample")));
      metadataCollection1 =
          Json.decodeValue(metadataCollectionStr1, FincConfigMetadataCollection.class);
      String metadataCollectionStr2 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataCollection2.sample")));
      metadataCollection2 =
          Json.decodeValue(metadataCollectionStr2, FincConfigMetadataCollection.class);
      String metadataCollectionStr3 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataCollection3.sample")));
      metadataCollection3 =
          Json.decodeValue(metadataCollectionStr3, FincConfigMetadataCollection.class);

      String isilStr1 = new String(Files.readAllBytes(Paths.get("ramls/examples/isil1.sample")));
      isil1 = Json.decodeValue(isilStr1, Isil.class);

      String isilStr2 = new String(Files.readAllBytes(Paths.get("ramls/examples/isil2.sample")));
      isil2 = Json.decodeValue(isilStr2, Isil.class);
    } catch (Exception e) {
      context.fail(e);
    }

    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient instance = PostgresClient.getInstance(vertx);
      instance.startEmbeddedPostgres();
    } catch (Exception e) {
      context.fail(e);
      return;
    }

    Async async1 = context.async(1);
    Async async2 = context.async(2);
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    String url = "http://localhost:" + port;
    TenantClient tenantClientFinc =
        new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
    TenantClient tenantClientUBL = new TenantClient(url, TENANT_UBL, TENANT_UBL);
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          try {
            tenantClientFinc.postTenant(null, postTenantRes -> async1.complete());
            tenantClientUBL.postTenant(null, postTenantRes -> async2.complete());
          } catch (Exception e) {
            context.fail(e);
          }
        });
    selectMetadataSourceVerticle =
        new SelectMetadataSourceVerticle(vertx, vertx.getOrCreateContext());
  }

  @AfterClass
  public static void teardown(TestContext context) {
    RestAssured.reset();
    Async async = context.async();
    vertx.close(
        context.asyncAssertSuccess(
            res -> {
              PostgresClient.stopEmbeddedPostgres();
              async.complete();
            }));
  }

  @Test
  public void test(TestContext context) throws Exception {
    loadData(context);

    Future<String> deploy = Future.future();
    JsonObject cfg = vertx.getOrCreateContext().config();
    cfg.put("tenantId", "ubl");
    cfg.put("metadataSourceId", "f6f03fb4-3368-4bc0-bc02-3bf6e19604a5");
    vertx.deployVerticle(
        selectMetadataSourceVerticle,
        new DeploymentOptions().setConfig(cfg), // .setWorker(true),
        deploy.completer());
    deploy.setHandler(
        asyncResult -> {
          if (asyncResult.succeeded()) {
            try {
              PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                  .getById(
                      "metadata_collections",
                      metadataCollection3.getId(),
                      FincConfigMetadataCollection.class,
                      collectionAsyncResult -> {
                        FincConfigMetadataCollection result = collectionAsyncResult.result();
                        context.assertTrue(result.getSelectedBy().contains("DE-15"));
                      });
            } catch (Exception e) {
              System.out.println("FAIL");
            }
          } else {
            System.out.println("FAIL");
          }
        });
  }

  private void loadData(TestContext context) {
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "isils",
            isil1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded isil");
              } else {
                context.fail("Could not load isil");
              }
            });

    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "isils",
            isil2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded isil");
              } else {
                context.fail("Could not load isil");
              }
            });

    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_sources",
            metadataSource1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata source");
              } else {
                context.fail("Could not load metadata source");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_sources",
            metadataSource1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata source");
              } else {
                context.fail("Could not load metadata source");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection");
              } else {
                context.fail("Could not load metadata colletion");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection");
              } else {
                context.fail("Could not load metadata colletion");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection3,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection");
              } else {
                context.fail("Could not load metadata colletion");
              }
            });
  }
}
