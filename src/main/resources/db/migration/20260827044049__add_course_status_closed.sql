ALTER TABLE courses ADD COLUMN status varchar(20);

UPDATE courses SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END;

ALTER TABLE courses ALTER COLUMN status SET NOT NULL;

ALTER TABLE courses ADD CONSTRAINT courses_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'));

ALTER TABLE courses DROP CONSTRAINT courses_deleted_state_check;

ALTER TABLE courses ADD CONSTRAINT courses_deleted_state_check CHECK (deleted_at IS NULL OR status = 'INACTIVE');

DROP INDEX courses_institution_deleted_at_idx;

CREATE INDEX courses_institution_deleted_at_idx ON courses (institution_id, deleted_at);

ALTER TABLE courses DROP COLUMN active;
