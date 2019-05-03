package org.folio.finc.select;

import static org.junit.Assert.*;

import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.folio.rest.jaxrs.model.MetadataCollection;
import org.folio.rest.jaxrs.model.MetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.MetadataCollectionSelect;
import org.junit.Test;

public class MetadataCollectionsHelperTest {

  private static final String COLLECTION_1 = "Collection 1";
  private static final String COLLECTION_2 = "Collection 2";
  private static final String DE_14 = "DE-14";
  private static final String DE_15 = "DE-15";

  @Test
  public void testTransform() {
    MetadataCollection collection1 = new MetadataCollection();
    collection1.setId("uuid-1234");
    collection1.setLabel(COLLECTION_1);
    collection1.setUsageRestricted(UsageRestricted.NO);
    collection1.setSolrMegaCollections(Arrays.asList());
    List<String> permittedFor = new ArrayList<>();
    permittedFor.add(DE_15);
    permittedFor.add(DE_14);
    collection1.setPermittedFor(permittedFor);
    collection1.setSelectedBy(permittedFor);

    MetadataCollection collection2 = new MetadataCollection();
    collection2.setId("uuid-6789");
    collection2.setLabel(COLLECTION_2);
    collection2.setUsageRestricted(UsageRestricted.YES);
    collection2.setSolrMegaCollections(Arrays.asList());
    List<String> permittedFor2 = new ArrayList<>();
    permittedFor2.add(DE_14);
    collection2.setPermittedFor(permittedFor2);
    collection2.setSelectedBy(permittedFor2);

    List<MetadataCollection> collections = new ArrayList<>();
    collections.add(collection1);
    collections.add(collection2);

    MetadataCollectionsHelper cut = new MetadataCollectionsHelper(Vertx.vertx(), "diku");

    List<MetadataCollectionSelect> transformed = cut.filterForIsil(collections, DE_15);
    transformed.stream().forEach(mdCollection -> {
      String label = mdCollection.getLabel();
      switch (label) {
        case COLLECTION_1:
          assertTrue(mdCollection.getSelected());
          break;
        case COLLECTION_2:
          assertFalse(mdCollection.getSelected());
          break;
      }
    });


  }
}