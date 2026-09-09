CREATE TABLE enrollment_application_spaces (
    enrollment_application_space_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    study_plan_space_id uuid NOT NULL,
    instrument_id uuid,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT enrollment_app_spaces_pkey PRIMARY KEY (enrollment_application_space_id),
    CONSTRAINT enrollment_app_spaces_app_fk FOREIGN KEY (enrollment_application_id)
        REFERENCES enrollment_applications (enrollment_application_id),
    CONSTRAINT enrollment_app_spaces_space_fk FOREIGN KEY (study_plan_space_id)
        REFERENCES study_plan_spaces (study_plan_space_id),
    CONSTRAINT enrollment_app_spaces_instrument_fk FOREIGN KEY (instrument_id)
        REFERENCES instruments (instrument_id),
    CONSTRAINT enrollment_app_spaces_app_space_unique UNIQUE (enrollment_application_id, study_plan_space_id)
);

CREATE INDEX idx_enrollment_app_spaces_app_id
    ON enrollment_application_spaces (enrollment_application_id);
