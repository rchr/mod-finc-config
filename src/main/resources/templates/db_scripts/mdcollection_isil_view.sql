CREATE OR REPLACE VIEW  isil_mdc_view
AS
  SELECT  metadata_collections._id, metadata_collections.jsonb AS jsonb, isils.jsonb AS isil_jsonb
  FROM    finc_mod_finc_config.metadata_collections metadata_collections JOIN finc_mod_finc_config.isils isils
  ON      (metadata_collections.jsonb->'permittedFor')::jsonb ? (isils.jsonb->>'isil');
