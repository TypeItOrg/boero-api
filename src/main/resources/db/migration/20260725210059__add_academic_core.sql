CREATE TABLE academic_years (
    academic_year_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    year integer NOT NULL,
    start_date date,
    end_date date,
    status varchar(20) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT academic_years_pkey PRIMARY KEY (academic_year_id),
    CONSTRAINT academic_years_institution_id_id_unique UNIQUE (institution_id, academic_year_id),
    CONSTRAINT academic_years_institution_year_unique UNIQUE (institution_id, year),
    CONSTRAINT academic_years_year_check CHECK (year > 0),
    CONSTRAINT academic_years_dates_check CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
    CONSTRAINT academic_years_dates_pair_check CHECK ((start_date IS NULL) = (end_date IS NULL)),
    CONSTRAINT academic_years_status_check CHECK (status IN ('PLANNED', 'ACTIVE', 'CLOSED')),
    CONSTRAINT academic_years_active_dates_check CHECK (status = 'PLANNED' OR (start_date IS NOT NULL AND end_date IS NOT NULL)),
    CONSTRAINT academic_years_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE UNIQUE INDEX academic_years_active_institution_unique
    ON academic_years (institution_id)
    WHERE status = 'ACTIVE';

CREATE TABLE training_paths (
    training_path_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    description varchar(1000),
    active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT training_paths_pkey PRIMARY KEY (training_path_id),
    CONSTRAINT training_paths_institution_id_id_unique UNIQUE (institution_id, training_path_id),
    CONSTRAINT training_paths_name_format_check CHECK (name = regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g')),
    CONSTRAINT training_paths_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE UNIQUE INDEX training_paths_institution_name_unique
    ON training_paths (institution_id, lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')));

CREATE TABLE study_plans (
    study_plan_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    training_path_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    effective_from date,
    effective_to date,
    status varchar(20) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT study_plans_pkey PRIMARY KEY (study_plan_id),
    CONSTRAINT study_plans_institution_id_id_unique UNIQUE (institution_id, study_plan_id),
    CONSTRAINT study_plans_name_format_check CHECK (name = regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g')),
    CONSTRAINT study_plans_dates_check CHECK (effective_from IS NULL OR effective_to IS NULL OR effective_from <= effective_to),
    CONSTRAINT study_plans_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT study_plans_active_dates_check CHECK (status = 'DRAFT' OR effective_from IS NOT NULL),
    CONSTRAINT study_plans_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT study_plans_training_path_institution_fk
        FOREIGN KEY (institution_id, training_path_id)
        REFERENCES training_paths (institution_id, training_path_id)
);

CREATE UNIQUE INDEX study_plans_institution_name_unique
    ON study_plans (training_path_id, lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')));

CREATE TABLE academic_levels (
    academic_level_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    display_order integer NOT NULL,
    description varchar(1000),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT academic_levels_pkey PRIMARY KEY (academic_level_id),
    CONSTRAINT academic_levels_study_plan_id_id_unique UNIQUE (study_plan_id, academic_level_id),
    CONSTRAINT academic_levels_study_plan_order_unique UNIQUE (study_plan_id, display_order),
    CONSTRAINT academic_levels_display_order_check CHECK (display_order > 0),
    CONSTRAINT academic_levels_name_format_check CHECK (name = regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g')),
    CONSTRAINT academic_levels_study_plan_fk FOREIGN KEY (study_plan_id) REFERENCES study_plans (study_plan_id)
);

CREATE UNIQUE INDEX academic_levels_study_plan_name_unique
    ON academic_levels (study_plan_id, lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')));

CREATE TABLE academic_spaces (
    academic_space_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    description varchar(1000),
    type varchar(20) NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT academic_spaces_pkey PRIMARY KEY (academic_space_id),
    CONSTRAINT academic_spaces_institution_id_id_unique UNIQUE (institution_id, academic_space_id),
    CONSTRAINT academic_spaces_type_check CHECK (type IN ('SUBJECT', 'WORKSHOP', 'SEMINAR', 'PRACTICE', 'OTHER')),
    CONSTRAINT academic_spaces_name_format_check CHECK (name = regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g')),
    CONSTRAINT academic_spaces_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE UNIQUE INDEX academic_spaces_institution_name_type_unique
    ON academic_spaces (institution_id, lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')), type);

CREATE TABLE instruments (
    instrument_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    description varchar(1000),
    active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT instruments_pkey PRIMARY KEY (instrument_id),
    CONSTRAINT instruments_institution_id_id_unique UNIQUE (institution_id, instrument_id),
    CONSTRAINT instruments_name_format_check CHECK (name = regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g')),
    CONSTRAINT instruments_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE UNIQUE INDEX instruments_institution_name_unique
    ON instruments (institution_id, lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')));

CREATE TABLE study_plan_spaces (
    study_plan_space_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    academic_space_id uuid NOT NULL,
    academic_level_id uuid,
    requirement_type varchar(20) NOT NULL,
    display_order integer NOT NULL,
    approval_mode varchar(30) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT study_plan_spaces_pkey PRIMARY KEY (study_plan_space_id),
    CONSTRAINT study_plan_spaces_institution_id_id_unique UNIQUE (institution_id, study_plan_space_id),
    CONSTRAINT study_plan_spaces_plan_id_id_unique UNIQUE (study_plan_id, study_plan_space_id),
    CONSTRAINT study_plan_spaces_display_order_check CHECK (display_order > 0),
    CONSTRAINT study_plan_spaces_requirement_type_check CHECK (requirement_type IN ('REQUIRED', 'OPTIONAL')),
    CONSTRAINT study_plan_spaces_approval_mode_check CHECK (approval_mode IN ('PROMOTION', 'FINAL_EXAM', 'PROMOTION_OR_FINAL_EXAM')),
    CONSTRAINT study_plan_spaces_plan_institution_fk
        FOREIGN KEY (institution_id, study_plan_id)
        REFERENCES study_plans (institution_id, study_plan_id),
    CONSTRAINT study_plan_spaces_space_institution_fk
        FOREIGN KEY (institution_id, academic_space_id)
        REFERENCES academic_spaces (institution_id, academic_space_id),
    CONSTRAINT study_plan_spaces_level_plan_fk
        FOREIGN KEY (study_plan_id, academic_level_id)
        REFERENCES academic_levels (study_plan_id, academic_level_id)
);

CREATE UNIQUE INDEX study_plan_spaces_plan_level_space_unique
    ON study_plan_spaces (study_plan_id, academic_level_id, academic_space_id)
    WHERE academic_level_id IS NOT NULL;

CREATE UNIQUE INDEX study_plan_spaces_plan_unassigned_space_unique
    ON study_plan_spaces (study_plan_id, academic_space_id)
    WHERE academic_level_id IS NULL;

CREATE UNIQUE INDEX study_plan_spaces_plan_level_order_unique
    ON study_plan_spaces (study_plan_id, academic_level_id, display_order)
    WHERE academic_level_id IS NOT NULL;

CREATE UNIQUE INDEX study_plan_spaces_plan_unassigned_order_unique
    ON study_plan_spaces (study_plan_id, display_order)
    WHERE academic_level_id IS NULL;

CREATE TABLE prerequisites (
    prerequisite_id uuid NOT NULL,
    study_plan_id uuid NOT NULL,
    target_study_plan_space_id uuid NOT NULL,
    required_study_plan_space_id uuid NOT NULL,
    requirement_stage varchar(20) NOT NULL,
    required_condition varchar(20) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT prerequisites_pkey PRIMARY KEY (prerequisite_id),
    CONSTRAINT prerequisites_study_plan_id_id_unique UNIQUE (study_plan_id, prerequisite_id),
    CONSTRAINT prerequisites_target_required_stage_unique
        UNIQUE (target_study_plan_space_id, required_study_plan_space_id, requirement_stage),
    CONSTRAINT prerequisites_distinct_spaces_check CHECK (target_study_plan_space_id <> required_study_plan_space_id),
    CONSTRAINT prerequisites_requirement_stage_check CHECK (requirement_stage IN ('TO_ENROLL', 'TO_PASS')),
    CONSTRAINT prerequisites_required_condition_check CHECK (required_condition IN ('REGULAR', 'PASSED')),
    CONSTRAINT prerequisites_study_plan_fk FOREIGN KEY (study_plan_id) REFERENCES study_plans (study_plan_id),
    CONSTRAINT prerequisites_target_plan_fk
        FOREIGN KEY (study_plan_id, target_study_plan_space_id)
        REFERENCES study_plan_spaces (study_plan_id, study_plan_space_id),
    CONSTRAINT prerequisites_required_plan_fk
        FOREIGN KEY (study_plan_id, required_study_plan_space_id)
        REFERENCES study_plan_spaces (study_plan_id, study_plan_space_id)
);

CREATE INDEX study_plan_spaces_plan_order_idx
    ON study_plan_spaces (study_plan_id, display_order);

CREATE INDEX prerequisites_plan_target_idx
    ON prerequisites (study_plan_id, target_study_plan_space_id);
