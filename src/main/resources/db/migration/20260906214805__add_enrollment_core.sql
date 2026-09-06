CREATE TABLE enrollment_periods (
    enrollment_period_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    start_date timestamp(6) NOT NULL,
    end_date timestamp(6) NOT NULL,
    status varchar(20) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT enrollment_periods_pkey PRIMARY KEY (enrollment_period_id),
    CONSTRAINT enrollment_periods_institution_id_id_unique UNIQUE (institution_id, enrollment_period_id),
    CONSTRAINT enrollment_periods_dates_check CHECK (start_date <= end_date),
    CONSTRAINT enrollment_periods_status_check CHECK (status IN ('PLANNED', 'OPEN', 'CLOSED')),
    CONSTRAINT enrollment_periods_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE INDEX enrollment_periods_institution_deleted_at_idx
    ON enrollment_periods (institution_id, deleted_at);

CREATE TABLE enrollment_applications (
    enrollment_application_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    applicant_person_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    enrollment_period_id uuid NOT NULL,
    status varchar(20) NOT NULL,
    rejection_reason text,
    resolved_at timestamp(6),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT enrollment_applications_pkey PRIMARY KEY (enrollment_application_id),
    CONSTRAINT enrollment_applications_institution_id_id_unique UNIQUE (institution_id, enrollment_application_id),
    CONSTRAINT enrollment_applications_resolution_consistency_check CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
    CONSTRAINT enrollment_applications_status_check CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT enrollment_applications_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT enrollment_applications_person_fk FOREIGN KEY (applicant_person_id) REFERENCES people (person_id),
    CONSTRAINT enrollment_applications_study_plan_institution_fk
        FOREIGN KEY (institution_id, study_plan_id) REFERENCES study_plans (institution_id, study_plan_id),
    CONSTRAINT enrollment_applications_academic_year_institution_fk
        FOREIGN KEY (institution_id, academic_year_id) REFERENCES academic_years (institution_id, academic_year_id),
    CONSTRAINT enrollment_applications_period_institution_fk
        FOREIGN KEY (institution_id, enrollment_period_id) REFERENCES enrollment_periods (institution_id, enrollment_period_id)
);

CREATE UNIQUE INDEX enrollment_applications_applicant_active_draft_unique
    ON enrollment_applications (institution_id, applicant_person_id, study_plan_id, academic_year_id)
    WHERE status = 'DRAFT' AND deleted_at IS NULL;

CREATE INDEX enrollment_applications_institution_deleted_at_idx
    ON enrollment_applications (institution_id, deleted_at);

CREATE INDEX enrollment_applications_institution_status_idx
    ON enrollment_applications (institution_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX enrollment_applications_applicant_person_idx
    ON enrollment_applications (applicant_person_id)
    WHERE deleted_at IS NULL;

CREATE TABLE applicant_education_backgrounds (
    applicant_education_background_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    secondary_school varchar(255),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT applicant_education_bg_pkey PRIMARY KEY (applicant_education_background_id),
    CONSTRAINT applicant_education_bg_application_unique UNIQUE (enrollment_application_id),
    CONSTRAINT applicant_education_bg_app_fk FOREIGN KEY (enrollment_application_id) REFERENCES enrollment_applications (enrollment_application_id)
);

INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application:read')::uuid, 'INSTITUTION', 'institution:enrollment-application:read', 'Ver solicitudes de inscripción'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application:approve')::uuid, 'INSTITUTION', 'institution:enrollment-application:approve', 'Aprobar solicitudes de inscripción'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application:reject')::uuid, 'INSTITUTION', 'institution:enrollment-application:reject', 'Rechazar solicitudes de inscripción')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;