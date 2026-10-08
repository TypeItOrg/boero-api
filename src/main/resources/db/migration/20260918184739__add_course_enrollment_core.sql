-- Clean-slate academic offering migration.
-- Existing courses and enrollment applications are intentionally not converted. Reset the
-- development database before applying this migration; institutional identity and catalog data
-- remain outside this reset.

ALTER TABLE academic_spaces
    ADD COLUMN instrumental boolean NOT NULL DEFAULT false;

ALTER TABLE courses
    ADD COLUMN study_plan_space_id uuid NOT NULL,
    ADD COLUMN training_path_id uuid NOT NULL,
    ADD COLUMN academic_level_id uuid,
    ADD COLUMN instrument_id uuid;

ALTER TABLE courses
    ALTER COLUMN study_plan_id DROP NOT NULL;

ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS courses_study_plan_fk,
    DROP CONSTRAINT IF EXISTS courses_academic_space_fk;

DROP INDEX IF EXISTS courses_institution_space_year_unique;

ALTER TABLE courses
    ADD CONSTRAINT courses_study_plan_space_fk
        FOREIGN KEY (institution_id, study_plan_space_id)
        REFERENCES study_plan_spaces (institution_id, study_plan_space_id),
    ADD CONSTRAINT courses_academic_space_fk
        FOREIGN KEY (institution_id, academic_space_id)
        REFERENCES academic_spaces (institution_id, academic_space_id),
    ADD CONSTRAINT courses_training_path_fk
        FOREIGN KEY (institution_id, training_path_id)
        REFERENCES training_paths (institution_id, training_path_id),
    ADD CONSTRAINT courses_instrument_fk
        FOREIGN KEY (institution_id, instrument_id)
        REFERENCES instruments (institution_id, instrument_id);

CREATE UNIQUE INDEX courses_curriculum_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_level_id, academic_year_id)
    WHERE instrument_id IS NULL
      AND academic_level_id IS NOT NULL;

CREATE UNIQUE INDEX courses_curriculum_without_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_year_id)
    WHERE instrument_id IS NULL
      AND academic_level_id IS NULL;

CREATE UNIQUE INDEX courses_instrumental_curriculum_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_level_id, academic_year_id, instrument_id)
    WHERE instrument_id IS NOT NULL
      AND academic_level_id IS NOT NULL;

CREATE UNIQUE INDEX courses_instrumental_curriculum_without_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_year_id, instrument_id)
    WHERE instrument_id IS NOT NULL
      AND academic_level_id IS NULL;

ALTER TABLE enrollment_applications
    ALTER COLUMN study_plan_id DROP NOT NULL;

DROP INDEX IF EXISTS enrollment_apps_applicant_active_path_unique;

CREATE UNIQUE INDEX enrollment_apps_applicant_open_path_year_unique
    ON enrollment_applications (institution_id, applicant_person_id, training_path_id, academic_year_id)
    WHERE deleted_at IS NULL
      AND status IN ('DRAFT', 'SUBMITTED');

CREATE TABLE institution_enrollment_locks (
    institution_id uuid NOT NULL,
    created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT institution_enrollment_locks_pkey PRIMARY KEY (institution_id),
    CONSTRAINT institution_enrollment_locks_institution_fk
        FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

INSERT INTO institution_enrollment_locks (institution_id)
SELECT institution_id
FROM institutions
ON CONFLICT DO NOTHING;

CREATE TABLE enrollment_application_courses (
    enrollment_application_course_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    course_id uuid NOT NULL,
    preferred_teacher_id uuid,
    status varchar(20) NOT NULL,
    requested_at timestamptz(6),
    submitted_with_capacity boolean,
    waitlist_number integer,
    waitlisted_at timestamptz(6),
    waitlist_reason varchar(80),
    resolved_at timestamptz(6),
    resolved_by_person_id uuid,
    resolution_reason_code varchar(80),
    resolution_reason_text text,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT enrollment_application_courses_pkey PRIMARY KEY (enrollment_application_course_id),
    CONSTRAINT enrollment_application_courses_status_check
        CHECK (status IN ('PENDING', 'WAITLISTED', 'ENROLLED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT enrollment_application_courses_app_fk
        FOREIGN KEY (enrollment_application_id) REFERENCES enrollment_applications (enrollment_application_id),
    CONSTRAINT enrollment_application_courses_course_fk
        FOREIGN KEY (institution_id, course_id) REFERENCES courses (institution_id, course_id),
    CONSTRAINT enrollment_application_courses_teacher_fk
        FOREIGN KEY (preferred_teacher_id) REFERENCES people (person_id),
    CONSTRAINT enrollment_application_courses_resolver_fk
        FOREIGN KEY (resolved_by_person_id) REFERENCES people (person_id),
    CONSTRAINT enrollment_application_courses_app_course_unique
        UNIQUE (enrollment_application_id, course_id),
    CONSTRAINT enrollment_application_courses_waitlist_number_positive
        CHECK (waitlist_number IS NULL OR waitlist_number > 0)
);

CREATE INDEX enrollment_application_courses_course_status_idx
    ON enrollment_application_courses (institution_id, course_id, status);

CREATE INDEX enrollment_application_courses_application_idx
    ON enrollment_application_courses (enrollment_application_id);

CREATE TABLE course_individual_slots (
    course_individual_slot_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_class_schedule_id uuid NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT course_individual_slots_pkey PRIMARY KEY (course_individual_slot_id),
    CONSTRAINT course_individual_slots_schedule_fk
        FOREIGN KEY (course_class_schedule_id) REFERENCES course_class_schedules (course_class_schedule_id),
    CONSTRAINT course_individual_slots_time_range_check CHECK (start_time < end_time),
    CONSTRAINT course_individual_slots_schedule_time_unique
        UNIQUE (course_class_schedule_id, start_time, end_time)
);

CREATE TABLE course_enrollments (
    course_enrollment_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    student_id uuid NOT NULL,
    course_id uuid NOT NULL,
    course_class_id uuid NOT NULL,
    source varchar(20) NOT NULL,
    enrollment_application_id uuid,
    status varchar(40) NOT NULL,
    academic_status varchar(30) NOT NULL,
    enrolled_at timestamptz(6) NOT NULL,
    completed_at timestamptz(6),
    withdrawn_at timestamptz(6),
    action_authority_person_id uuid,
    action_reason text,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT course_enrollments_pkey PRIMARY KEY (course_enrollment_id),
    CONSTRAINT course_enrollments_source_check CHECK (source IN ('APPLICATION', 'MANUAL')),
    CONSTRAINT course_enrollments_status_check
        CHECK (status IN ('ENROLLED', 'COMPLETED', 'WITHDRAWN', 'ADMINISTRATIVELY_WITHDRAWN')),
    CONSTRAINT course_enrollments_academic_status_check
        CHECK (academic_status IN ('IN_PROGRESS', 'PENDING_RESULT', 'REGULARIZED', 'PROMOTED', 'PASSED', 'FAILED')),
    CONSTRAINT course_enrollments_source_application_check
        CHECK ((source = 'APPLICATION') = (enrollment_application_id IS NOT NULL)),
    CONSTRAINT course_enrollments_student_fk
        FOREIGN KEY (institution_id, student_id) REFERENCES students (institution_id, student_id),
    CONSTRAINT course_enrollments_course_fk
        FOREIGN KEY (institution_id, course_id) REFERENCES courses (institution_id, course_id),
    CONSTRAINT course_enrollments_class_fk
        FOREIGN KEY (institution_id, course_class_id) REFERENCES course_classes (institution_id, course_class_id),
    CONSTRAINT course_enrollments_application_fk
        FOREIGN KEY (enrollment_application_id) REFERENCES enrollment_applications (enrollment_application_id),
    CONSTRAINT course_enrollments_authority_fk
        FOREIGN KEY (action_authority_person_id) REFERENCES people (person_id)
);

CREATE UNIQUE INDEX course_enrollments_active_student_course_unique
    ON course_enrollments (institution_id, student_id, course_id)
    WHERE status = 'ENROLLED';

CREATE INDEX course_enrollments_institution_student_idx
    ON course_enrollments (institution_id, student_id, enrolled_at DESC);

CREATE INDEX course_enrollments_course_status_idx
    ON course_enrollments (institution_id, course_id, status);

CREATE TABLE course_enrollment_schedules (
    course_enrollment_schedule_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_enrollment_id uuid NOT NULL,
    course_class_schedule_id uuid NOT NULL,
    course_individual_slot_id uuid,
    day_of_week varchar(20) NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    released_at timestamptz(6),
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT course_enrollment_schedules_pkey PRIMARY KEY (course_enrollment_schedule_id),
    CONSTRAINT course_enrollment_schedules_enrollment_fk
        FOREIGN KEY (course_enrollment_id) REFERENCES course_enrollments (course_enrollment_id),
    CONSTRAINT course_enrollment_schedules_schedule_fk
        FOREIGN KEY (course_class_schedule_id) REFERENCES course_class_schedules (course_class_schedule_id),
    CONSTRAINT course_enrollment_schedules_slot_fk
        FOREIGN KEY (course_individual_slot_id) REFERENCES course_individual_slots (course_individual_slot_id),
    CONSTRAINT course_enrollment_schedules_time_range_check CHECK (start_time < end_time),
    CONSTRAINT course_enrollment_schedules_enrollment_schedule_unique
        UNIQUE (course_enrollment_id, course_class_schedule_id, course_individual_slot_id)
);

CREATE UNIQUE INDEX course_enrollment_schedules_active_slot_unique
    ON course_enrollment_schedules (institution_id, course_individual_slot_id)
    WHERE released_at IS NULL AND course_individual_slot_id IS NOT NULL;

CREATE INDEX course_enrollment_schedules_active_day_idx
    ON course_enrollment_schedules (institution_id, day_of_week, start_time, end_time)
    WHERE released_at IS NULL;

CREATE TABLE course_enrollment_histories (
    course_enrollment_history_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    course_enrollment_id uuid NOT NULL,
    previous_status varchar(40),
    new_status varchar(40),
    previous_academic_status varchar(30),
    new_academic_status varchar(30),
    operation varchar(80) NOT NULL,
    reason text,
    authority_person_id uuid,
    changed_at timestamptz(6) NOT NULL,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT course_enrollment_histories_pkey PRIMARY KEY (course_enrollment_history_id),
    CONSTRAINT course_enrollment_histories_enrollment_fk
        FOREIGN KEY (course_enrollment_id) REFERENCES course_enrollments (course_enrollment_id),
    CONSTRAINT course_enrollment_histories_authority_fk
        FOREIGN KEY (authority_person_id) REFERENCES people (person_id)
);

CREATE INDEX course_enrollment_histories_enrollment_changed_idx
    ON course_enrollment_histories (course_enrollment_id, changed_at DESC);

CREATE TABLE enrollment_application_course_assignment_snapshots (
    enrollment_application_course_assignment_snapshot_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    enrollment_application_course_id uuid NOT NULL,
    course_id uuid NOT NULL,
    course_class_id uuid NOT NULL,
    course_class_schedule_id uuid NOT NULL,
    course_individual_slot_id uuid,
    course_name varchar(255) NOT NULL,
    training_path_name varchar(150) NOT NULL,
    study_plan_name varchar(150),
    academic_space_name varchar(150) NOT NULL,
    academic_level_name varchar(150),
    instrument_name varchar(150),
    day_of_week varchar(20) NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    assigned_at timestamptz(6) NOT NULL,
    assigned_by_person_id uuid,
    operation varchar(80) NOT NULL,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT enrollment_application_course_assignment_snapshots_pkey PRIMARY KEY (enrollment_application_course_assignment_snapshot_id),
    CONSTRAINT enrollment_application_course_assignment_snapshots_course_fk
        FOREIGN KEY (institution_id, course_id) REFERENCES courses (institution_id, course_id),
    CONSTRAINT eac_snapshot_application_course_fk
        FOREIGN KEY (enrollment_application_course_id)
        REFERENCES enrollment_application_courses (enrollment_application_course_id),
    CONSTRAINT eac_snapshot_assignment_unique
        UNIQUE (enrollment_application_course_id, course_class_schedule_id, course_individual_slot_id),
    CONSTRAINT enrollment_application_course_assignment_snapshots_authority_fk
        FOREIGN KEY (assigned_by_person_id) REFERENCES people (person_id)
);

CREATE TABLE course_waitlist_sequences (
    course_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    last_number integer NOT NULL DEFAULT 0,
    created_at timestamptz(6) NOT NULL,
    updated_at timestamptz(6) NOT NULL,
    CONSTRAINT course_waitlist_sequences_pkey PRIMARY KEY (course_id),
    CONSTRAINT course_waitlist_sequences_course_fk
        FOREIGN KEY (institution_id, course_id) REFERENCES courses (institution_id, course_id),
    CONSTRAINT course_waitlist_sequences_number_check CHECK (last_number >= 0)
);

INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course-enrollment:read')::uuid, 'INSTITUTION', 'institution:course-enrollment:read', 'Ver cursadas institucionales'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course-enrollment:create')::uuid, 'INSTITUTION', 'institution:course-enrollment:create', 'Crear cursadas manuales'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course-enrollment:withdraw')::uuid, 'INSTITUTION', 'institution:course-enrollment:withdraw', 'Registrar bajas de cursadas'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course-enrollment:academic-status-update')::uuid, 'INSTITUTION', 'institution:course-enrollment:academic-status-update', 'Modificar resultados académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application-course:read')::uuid, 'INSTITUTION', 'institution:enrollment-application-course:read', 'Ver solicitudes de cursada'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application-course:enroll')::uuid, 'INSTITUTION', 'institution:enrollment-application-course:enroll', 'Inscribir solicitudes de cursada'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application-course:reject')::uuid, 'INSTITUTION', 'institution:enrollment-application-course:reject', 'Rechazar solicitudes de cursada'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:course-waitlist:read')::uuid, 'INSTITUTION', 'institution:course-waitlist:read', 'Ver listas de espera')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;
