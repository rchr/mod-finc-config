-- DESELECT ALL
WITH    i AS
        (SELECT jsonb->>'isil' AS isil FROM finc_mod_finc_config.isils WHERE jsonb->>'tenant' = '?')
UPDATE  finc_mod_finc_config.metadata_collections AS mc
SET     jsonb = jsonb_set(
            jsonb::jsonb,
            array['selectedBy'],
            (jsonb->'selectedBy')::jsonb - i.isil)
FROM    i
WHERE   (jsonb->'permittedFor')::jsonb ? i.isil
AND     (jsonb->'selectedBy')::jsonb ? i.isil
AND     (jsonb->'mdSource'->>'id' = '?');
