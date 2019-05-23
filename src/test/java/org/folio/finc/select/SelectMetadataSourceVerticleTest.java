package org.folio.finc.select;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourceVerticleTest {

  private static final Logger logger =
      LoggerFactory.getLogger(SelectMetadataSourceVerticleTest.class);

  private static final String TENANT_UBL = "ubl";
  private static Vertx vertx;
  private static FincConfigMetadataSource metadataSource1;
  private static FincConfigMetadataSource metadataSource2;
  private static FincConfigMetadataCollection metadataCollection1;
  private static FincConfigMetadataCollection metadataCollection2;
  private static FincConfigMetadataCollection metadataCollection3;
  private static Isil isil1;
  private static Isil isil2;
  private static SelectMetadataSourceVerticle cut;
  @Rule public Timeout timeout = Timeout.seconds(1000);

  @BeforeClass
  public static void setUp(TestContext context) {
    readData(context);
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
            tenantClientUBL.postTenant(
                null,
                postTenantRes -> {
                  writeDataToDB(context);
                  async2.complete();
                });
          } catch (Exception e) {
            context.fail(e);
          }
        });
    cut = new SelectMetadataSourceVerticle(vertx, vertx.getOrCreateContext());
  }

  @AfterClass
  public static void teardown(TestContext context) {
    RestAssured.reset();
    Async async = context.async(3);
    vertx.close(
        context.asyncAssertSuccess(
            res -> {
              PostgresClient.stopEmbeddedPostgres();
              async.complete();
            }));
  }

  private static void readData(TestContext context) {
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
  }

  private static void writeDataToDB(TestContext context) {
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "isils",
            isil1.getId(),
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
            isil2.getId(),
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
            metadataSource1.getId(),
            metadataSource1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata source 1");
              } else {
                context.fail("Could not load metadata source 1");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_sources",
            metadataSource2.getId(),
            metadataSource2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata source 2");
              } else {
                context.fail("Could not load metadata source 2");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection1.getId(),
            metadataCollection1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection 1");
              } else {
                context.fail("Could not load metadata collection 1");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection2.getId(),
            metadataCollection2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection 2");
              } else {
                context.fail("Could not load metadata collection 2");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection3.getId(),
            metadataCollection3,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection 3");
              } else {
                context.fail("Could not load metadata collection 3");
              }
            });
  }

  @Before
  public void before(TestContext context) {
    Async async = context.async();
    vertx = Vertx.vertx();
    JsonObject cfg2 = vertx.getOrCreateContext().config();
    cfg2.put("tenantId", TENANT_UBL);
    cfg2.put("metadataSourceId", metadataSource2.getId());
    cfg2.put("testing", true);
    vertx.deployVerticle(
        cut,
        new DeploymentOptions().setConfig(cfg2).setWorker(true),
        context.asyncAssertSuccess(
            h -> {
              async.complete();
            }));
  }

  @Test
  public void testSuccessfulSelect(TestContext context) {
//    Async async = context.async(5);
    cut.selectAllCollections(metadataSource2.getId(), TENANT_UBL)
        .setHandler(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                      .getById(
                          "metadata_collections",
                          metadataCollection3.getId(),
                          FincConfigMetadataCollection.class,
                          collectionAsyncResult -> {
                            if (collectionAsyncResult.succeeded()) {
                              context.assertTrue(
                                  collectionAsyncResult.result().getSelectedBy().contains("DE-15"));
//                              async.complete();
                            } else {
                              context.fail(collectionAsyncResult.cause().toString());
//                              async.complete();
                            }
                          });
                } catch (Exception e) {
                  context.fail(e);
//                  async.complete();
                }
              } else {
                context.fail();
//                async.complete();
              }
            });
  }

  @Test
  public void testNoSelect(TestContext context) {
    Async async = context.async(6);
    cut.selectAllCollections(metadataSource2.getId(), TENANT_UBL)
        .setHandler(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                      .getById(
                          "metadata_collections",
                          metadataCollection2.getId(),
                          FincConfigMetadataCollection.class,
                          collectionAsyncResult -> {
                            if (collectionAsyncResult.succeeded()) {
                              context.assertFalse(
                                  collectionAsyncResult.result().getSelectedBy().contains("DE-15"));
                              async.complete();
                            } else {
                              context.fail(collectionAsyncResult.cause().toString());
                              async.complete();
                            }
                          });
                } catch (Exception e) {
                  context.fail(e);
                  async.complete();
                }
              } else {
                context.fail();
                async.complete();
              }
            });
  }
}
