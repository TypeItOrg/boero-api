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
    CONSTRAINT enrollment_periods_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT enrollment_periods_academic_year_fk FOREIGN KEY (academic_year_id) REFERENCES academic_years (academic_year_id),
    CONSTRAINT enrollment_periods_dates_check CHECK (start_date <= end_date),
    CONSTRAINT enrollment_periods_status_check CHECK (status IN ('PLANNED', 'OPEN', 'CLOSED'))
);

CREATE TABLE enrollment_applications (
    enrollment_application_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    applicant_person_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    enrollment_period_id uuid NOT NULL,
    status varchar(20) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT enrollment_applications_pkey PRIMARY KEY (enrollment_application_id),
    CONSTRAINT enrollment_applications_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT enrollment_applications_person_fk FOREIGN KEY (applicant_person_id) REFERENCES people (person_id),
    CONSTRAINT enrollment_applications_study_plan_fk FOREIGN KEY (study_plan_id) REFERENCES study_plans (study_plan_id),
    CONSTRAINT enrollment_applications_academic_year_fk FOREIGN KEY (academic_year_id) REFERENCES academic_years (academic_year_id),
    CONSTRAINT enrollment_applications_period_fk FOREIGN KEY (enrollment_period_id) REFERENCES enrollment_periods (enrollment_period_id),
    CONSTRAINT enrollment_applications_status_check CHECK (status IN ('DRAFT', 'SUBMITTED', 'CANCELLED', 'REJECTED'))
);

CREATE TABLE applicant_education_backgrounds (
    applicant_education_background_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    secondary_school varchar(255),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT applicant_education_bg_pkey PRIMARY KEY (applicant_education_background_id),
    CONSTRAINT applicant_education_bg_app_fk FOREIGN KEY (enrollment_application_id) REFERENCES enrollment_applications (enrollment_application_id)
);

CREATE UNIQUE INDEX enrollment_apps_applicant_active_draft_unique
    ON enrollment_applications (applicant_person_id, study_plan_id, academic_year_id)
    WHERE status = 'DRAFT' AND deleted_at IS NULL;
