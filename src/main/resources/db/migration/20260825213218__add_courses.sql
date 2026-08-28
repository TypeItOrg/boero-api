CREATE TABLE courses (
    course_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    academic_space_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT courses_pkey PRIMARY KEY (course_id),
    CONSTRAINT courses_institution_id_id_unique UNIQUE (institution_id, course_id),
    CONSTRAINT courses_deleted_state_check CHECK (deleted_at IS NULL OR active = false),
    CONSTRAINT courses_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT courses_study_plan_fk FOREIGN KEY (institution_id, study_plan_id) REFERENCES study_plans (institution_id, study_plan_id),
    CONSTRAINT courses_academic_space_fk FOREIGN KEY (institution_id, academic_space_id) REFERENCES academic_spaces (institution_id, academic_space_id),
    CONSTRAINT courses_academic_year_fk FOREIGN KEY (institution_id, academic_year_id) REFERENCES academic_years (institution_id, academic_year_id)
);

CREATE UNIQUE INDEX courses_institution_space_year_unique
    ON courses (institution_id, academic_space_id, academic_year_id)
    WHERE deleted_at IS NULL;

CREATE INDEX courses_institution_deleted_at_idx
    ON courses (institution_id, deleted_at);

CREATE TABLE course_classes (
    course_class_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT course_classes_pkey PRIMARY KEY (course_class_id),
    CONSTRAINT course_classes_institution_id_id_unique UNIQUE (institution_id, course_class_id),
    CONSTRAINT course_classes_course_fk
        FOREIGN KEY (institution_id, course_id) REFERENCES courses (institution_id, course_id),
    CONSTRAINT course_classes_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE INDEX course_classes_course_idx
    ON course_classes (course_id);

CREATE TABLE course_class_days (
    course_class_day_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_class_id uuid NOT NULL,
    day_of_week varchar(20) NOT NULL,
    capacity integer,
    period_duration_minutes integer,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT course_class_days_pkey PRIMARY KEY (course_class_day_id),
    CONSTRAINT course_class_days_institution_id_id_unique UNIQUE (institution_id, course_class_day_id),
    CONSTRAINT course_class_days_class_fk
        FOREIGN KEY (institution_id, course_class_id) REFERENCES course_classes (institution_id, course_class_id),
    CONSTRAINT course_class_days_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT course_class_days_day_of_week_check CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY')),
    CONSTRAINT course_class_days_capacity_check CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT course_class_days_period_duration_check CHECK (period_duration_minutes IS NULL OR period_duration_minutes > 0),
    CONSTRAINT course_class_days_class_day_unique UNIQUE (course_class_id, day_of_week)
);

CREATE TABLE course_class_schedules (
    course_class_schedule_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_class_day_id uuid NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT course_class_schedules_pkey PRIMARY KEY (course_class_schedule_id),
    CONSTRAINT course_class_schedules_day_fk
        FOREIGN KEY (institution_id, course_class_day_id) REFERENCES course_class_days (institution_id, course_class_day_id),
    CONSTRAINT course_class_schedules_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT course_class_schedules_time_range_check CHECK (start_time < end_time)
);

CREATE INDEX course_class_schedules_day_idx
    ON course_class_schedules (course_class_day_id);

CREATE TABLE course_class_teachers (
    course_class_teacher_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_class_id uuid NOT NULL,
    person_id uuid NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT course_class_teachers_pkey PRIMARY KEY (course_class_teacher_id),
    CONSTRAINT course_class_teachers_unique UNIQUE (course_class_id, person_id),
    CONSTRAINT course_class_teachers_class_fk
        FOREIGN KEY (institution_id, course_class_id) REFERENCES course_classes (institution_id, course_class_id),
    CONSTRAINT course_class_teachers_person_fk FOREIGN KEY (person_id) REFERENCES people (person_id),
    CONSTRAINT course_class_teachers_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

ALTER TABLE academic_lifecycle_events DROP CONSTRAINT academic_lifecycle_events_resource_type_check;

ALTER TABLE academic_lifecycle_events
    ADD CONSTRAINT academic_lifecycle_events_resource_type_check
    CHECK (resource_type IN ('ACADEMIC_YEAR', 'TRAINING_PATH', 'STUDY_PLAN', 'ACADEMIC_SPACE', 'INSTRUMENT', 'COURSE'));

INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course:read')::uuid, 'INSTITUTION', 'institution:course:read', 'Ver cursos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course:create')::uuid, 'INSTITUTION', 'institution:course:create', 'Crear cursos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course:update')::uuid, 'INSTITUTION', 'institution:course:update', 'Editar cursos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course:update-status')::uuid, 'INSTITUTION', 'institution:course:update-status', 'Activar o desactivar cursos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course:delete')::uuid, 'INSTITUTION', 'institution:course:delete', 'Eliminar cursos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course:restore')::uuid, 'INSTITUTION', 'institution:course:restore', 'Restaurar cursos')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;
