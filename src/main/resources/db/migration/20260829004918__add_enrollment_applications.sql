CREATE TABLE enrollment_applications (
    enrollment_application_id uuid NOT NULL,
    person_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    enrollment_period_id uuid,
    status varchar(20) NOT NULL,
    data jsonb NOT NULL,
    deleted_at timestamp(6),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT enrollment_applications_pkey PRIMARY KEY (enrollment_application_id),
    CONSTRAINT enrollment_applications_institution_id_id_unique
        UNIQUE (institution_id, enrollment_application_id),
    CONSTRAINT enrollment_applications_person_id_id_unique
        UNIQUE (person_id, enrollment_application_id),
    CONSTRAINT enrollment_applications_status_check
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'CANCELLED', 'REJECTED')),
    CONSTRAINT enrollment_applications_data_object_check
        CHECK (jsonb_typeof(data) = 'object'),
    CONSTRAINT enrollment_applications_institution_fk
        FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT enrollment_applications_person_institution_fk
        FOREIGN KEY (institution_id, person_id)
        REFERENCES people (institution_id, person_id),
    CONSTRAINT enrollment_applications_study_plan_institution_fk
        FOREIGN KEY (institution_id, study_plan_id)
        REFERENCES study_plans (institution_id, study_plan_id),
    CONSTRAINT enrollment_applications_academic_year_institution_fk
        FOREIGN KEY (institution_id, academic_year_id)
        REFERENCES academic_years (institution_id, academic_year_id)
);

CREATE INDEX enrollment_applications_person_status_idx
    ON enrollment_applications (institution_id, person_id, status, deleted_at);
