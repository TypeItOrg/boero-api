UPDATE permissions
SET description = CASE code
    WHEN 'institution:training-path:update-status' THEN 'Activar o desactivar trayectos formativos'
    WHEN 'institution:study-plan:update-status' THEN 'Activar o desactivar planes de estudio'
    WHEN 'institution:academic-space:update-status' THEN 'Activar o desactivar espacios académicos'
    WHEN 'institution:instrument:update-status' THEN 'Activar o desactivar instrumentos'
END,
updated_at = CURRENT_TIMESTAMP
WHERE code IN (
    'institution:training-path:update-status',
    'institution:study-plan:update-status',
    'institution:academic-space:update-status',
    'institution:instrument:update-status'
);
