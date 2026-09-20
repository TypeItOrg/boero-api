-- Renames existing academic levels to their derived names ("Nivel <display_order>").
-- Level names are now derived from display order at the application layer;
-- this backfills rows created before that rule existed. Data-only, dev scope.
UPDATE academic_levels
SET name = 'Nivel ' || display_order
WHERE name IS DISTINCT FROM ('Nivel ' || display_order);
