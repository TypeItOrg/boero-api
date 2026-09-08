-- 1. Actualizar check de estado en enrollment_applications para incluir CANCELLED y APPROVED
ALTER TABLE enrollment_applications 
    DROP CONSTRAINT IF EXISTS enrollment_applications_status_check;
ALTER TABLE enrollment_applications 
    ADD CONSTRAINT enrollment_applications_status_check 
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'CANCELLED', 'APPROVED', 'REJECTED'));

-- 2. Extender antecedentes educativos
ALTER TABLE applicant_education_backgrounds
    ADD COLUMN IF NOT EXISTS school_origin varchar(150),
    ADD COLUMN IF NOT EXISTS current_grade_year varchar(50),
    ADD COLUMN IF NOT EXISTS secondary_completed boolean DEFAULT false NOT NULL,
    ADD COLUMN IF NOT EXISTS secondary_degree_title varchar(150);

-- 3. Salud e Inclusión
CREATE TABLE applicant_health_inclusions (
    applicant_health_inclusion_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    receives_reasonable_adjustments boolean DEFAULT false NOT NULL,
    adjustment_details text,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT applicant_health_inc_pkey PRIMARY KEY (applicant_health_inclusion_id),
    CONSTRAINT applicant_health_inc_app_fk FOREIGN KEY (enrollment_application_id) 
        REFERENCES enrollment_applications (enrollment_application_id),
    CONSTRAINT applicant_health_inc_unique_app UNIQUE (enrollment_application_id)
);

-- 4. Tutor o Responsable Legal
CREATE TABLE applicant_responsibles (
    applicant_responsible_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    full_name varchar(200) NOT NULL,
    document_number varchar(20) NOT NULL,
    occupation varchar(100),
    phone_number varchar(50) NOT NULL,
    email varchar(150),
    education_level varchar(50),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT applicant_resp_pkey PRIMARY KEY (applicant_responsible_id),
    CONSTRAINT applicant_resp_app_fk FOREIGN KEY (enrollment_application_id) 
        REFERENCES enrollment_applications (enrollment_application_id),
    CONSTRAINT applicant_resp_unique_app UNIQUE (enrollment_application_id)
);

-- 5. Preferencias y Reingreso
CREATE TABLE applicant_preferences (
    applicant_preference_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    preferred_shift varchar(30) NOT NULL,
    allows_image_use boolean DEFAULT false NOT NULL,
    is_reenrolling boolean DEFAULT false NOT NULL,
    previous_teacher varchar(150),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT applicant_pref_pkey PRIMARY KEY (applicant_preference_id),
    CONSTRAINT applicant_pref_app_fk FOREIGN KEY (enrollment_application_id) 
        REFERENCES enrollment_applications (enrollment_application_id),
    CONSTRAINT applicant_pref_unique_app UNIQUE (enrollment_application_id)
);

-- 6. Adjuntos de Documentación
CREATE TABLE enrollment_attachments (
    enrollment_attachment_id uuid NOT NULL,
    enrollment_application_id uuid NOT NULL,
    attachment_type varchar(50) NOT NULL,
    original_file_name varchar(255) NOT NULL,
    storage_path varchar(500) NOT NULL,
    content_type varchar(100) NOT NULL,
    file_size bigint NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT enrollment_att_pkey PRIMARY KEY (enrollment_attachment_id),
    CONSTRAINT enrollment_att_app_fk FOREIGN KEY (enrollment_application_id) 
        REFERENCES enrollment_applications (enrollment_application_id)
);
