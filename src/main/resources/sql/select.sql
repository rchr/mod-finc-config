SELECT jsonb->>'isil' AS isil FROM finc_mod_finc_config.isils WHERE jsonb->>'tenant' = ?;
