CREATE TABLE public.addresses (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    address_id uuid NOT NULL,
    city_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    apartment character varying(50),
    floor character varying(50),
    number character varying(50),
    additional_info character varying(255),
    neighborhood character varying(255),
    street character varying(255) NOT NULL
);

CREATE TABLE public.cities (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    city_id uuid NOT NULL,
    province_id uuid NOT NULL,
    department_georef_id character varying(20),
    georef_id character varying(20),
    municipality_georef_id character varying(20),
    department_name character varying(255),
    municipality_name character varying(255),
    name character varying(255) NOT NULL
);

CREATE TABLE public.countries (
    iso_code character varying(3),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    country_id uuid NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.guardian_profiles (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    guardian_profile_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    person_id uuid NOT NULL,
    education_level character varying(40),
    occupation character varying(100),
    CONSTRAINT guardian_profiles_education_level_check CHECK (((education_level)::text = ANY ((ARRAY['PRIMARY_INCOMPLETE'::character varying, 'PRIMARY_COMPLETE'::character varying, 'SECONDARY_INCOMPLETE'::character varying, 'SECONDARY_COMPLETE'::character varying, 'TERTIARY_INCOMPLETE'::character varying, 'TERTIARY_COMPLETE'::character varying, 'UNIVERSITY_INCOMPLETE'::character varying, 'UNIVERSITY_COMPLETE'::character varying])::text[])))
);

CREATE TABLE public.institutions (
    active boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    city_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    phone_number character varying(30),
    number character varying(50),
    slug character varying(100) NOT NULL,
    email character varying(150),
    additional_info character varying(255),
    name character varying(255) NOT NULL,
    neighborhood character varying(255),
    street character varying(255)
);

CREATE TABLE public.people (
    birth_date date,
    deleted boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    document_number character varying(8) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    address_id uuid,
    birth_city_id uuid,
    institution_id uuid NOT NULL,
    nationality_country_id uuid,
    person_id uuid NOT NULL,
    phone_number character varying(30),
    email character varying(150),
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    CONSTRAINT people_document_number_format CHECK (((document_number)::text ~ '^[0-9]{8}$'::text)),
    CONSTRAINT people_first_name_length CHECK (((char_length((first_name)::text) >= 3) AND (char_length((first_name)::text) <= 255))),
    CONSTRAINT people_last_name_length CHECK (((char_length((last_name)::text) >= 3) AND (char_length((last_name)::text) <= 255)))
);

CREATE TABLE public.permissions (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    permission_id uuid NOT NULL,
    scope character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(255) NOT NULL,
    CONSTRAINT permissions_scope_check CHECK (((scope)::text = ANY ((ARRAY['PLATFORM'::character varying, 'INSTITUTION'::character varying])::text[])))
);

CREATE TABLE public.person_role_assignments (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    institution_id uuid NOT NULL,
    person_id uuid NOT NULL,
    person_role_assignment_id uuid NOT NULL,
    role_id uuid NOT NULL
);

CREATE TABLE public.platform_account_roles (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    platform_account_id uuid NOT NULL,
    platform_account_role_id uuid NOT NULL,
    role_id uuid NOT NULL
);

CREATE TABLE public.platform_accounts (
    enabled boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    platform_account_id uuid NOT NULL,
    password character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL
);

CREATE TABLE public.platform_refresh_tokens (
    revoked boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    platform_account_id uuid NOT NULL,
    platform_refresh_token_id uuid NOT NULL,
    platform_session_id uuid NOT NULL,
    family_id character varying(36) NOT NULL,
    token_hash character varying(255) NOT NULL
);

CREATE TABLE public.platform_sessions (
    active boolean NOT NULL,
    remember_me boolean NOT NULL,
    ended_at timestamp(6) without time zone,
    started_at timestamp(6) without time zone NOT NULL,
    platform_account_id uuid NOT NULL,
    platform_session_id uuid NOT NULL,
    ip_address character varying(45),
    user_agent character varying(255)
);

CREATE TABLE public.provinces (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    country_id uuid NOT NULL,
    province_id uuid NOT NULL,
    georef_id character varying(20),
    name character varying(255) NOT NULL
);

CREATE TABLE public.refresh_tokens (
    revoked boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    refresh_token_id uuid NOT NULL,
    session_id uuid NOT NULL,
    family_id character varying(36) NOT NULL,
    token_hash character varying(255) NOT NULL
);

CREATE TABLE public.role_permissions (
    permission_id uuid NOT NULL,
    role_id uuid NOT NULL
);

CREATE TABLE public.roles (
    is_system boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    institution_id uuid,
    role_id uuid NOT NULL,
    scope character varying(20) NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    CONSTRAINT roles_scope_check CHECK (((scope)::text = ANY ((ARRAY['PLATFORM'::character varying, 'INSTITUTION'::character varying])::text[])))
);

CREATE TABLE public.student_guardians (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    guardian_profile_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    student_guardian_id uuid NOT NULL,
    student_id uuid NOT NULL,
    relationship character varying(30) NOT NULL,
    CONSTRAINT student_guardians_relationship_check CHECK (((relationship)::text = ANY ((ARRAY['MOTHER'::character varying, 'FATHER'::character varying, 'LEGAL_GUARDIAN'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.students (
    enrollment_date date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    institution_id uuid NOT NULL,
    person_id uuid NOT NULL,
    student_id uuid NOT NULL,
    status character varying(30) NOT NULL,
    file_number character varying(50),
    CONSTRAINT students_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'GRADUATED'::character varying, 'WITHDRAWN'::character varying, 'SUSPENDED'::character varying])::text[])))
);

CREATE TABLE public.user_sessions (
    active boolean NOT NULL,
    remember_me boolean NOT NULL,
    ended_at timestamp(6) without time zone,
    started_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL,
    user_session_id uuid NOT NULL,
    ip_address character varying(45),
    user_agent character varying(255)
);

CREATE TABLE public.users (
    enabled boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    institution_id uuid NOT NULL,
    person_id uuid NOT NULL,
    user_id uuid NOT NULL,
    password character varying(255) NOT NULL
);

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT addresses_institution_id_id_unique UNIQUE (institution_id, address_id);

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (address_id);

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT cities_pkey PRIMARY KEY (city_id);

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT cities_province_georef_unique UNIQUE (province_id, georef_id);

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_iso_code_unique UNIQUE (iso_code);

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_name_unique UNIQUE (name);

ALTER TABLE ONLY public.countries
    ADD CONSTRAINT countries_pkey PRIMARY KEY (country_id);

ALTER TABLE ONLY public.guardian_profiles
    ADD CONSTRAINT guardian_profiles_institution_id_id_unique UNIQUE (institution_id, guardian_profile_id);

ALTER TABLE ONLY public.guardian_profiles
    ADD CONSTRAINT guardian_profiles_person_id_unique UNIQUE (institution_id, person_id);

ALTER TABLE ONLY public.guardian_profiles
    ADD CONSTRAINT guardian_profiles_pkey PRIMARY KEY (guardian_profile_id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_pkey PRIMARY KEY (institution_id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_slug_unique UNIQUE (slug);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_document_number_unique UNIQUE (institution_id, document_number);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_institution_id_id_unique UNIQUE (institution_id, person_id);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_pkey PRIMARY KEY (person_id);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_code_unique UNIQUE (code);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (permission_id);

ALTER TABLE ONLY public.person_role_assignments
    ADD CONSTRAINT person_role_assignments_pkey PRIMARY KEY (person_role_assignment_id);

ALTER TABLE ONLY public.person_role_assignments
    ADD CONSTRAINT person_role_assignments_unique UNIQUE (person_id, role_id, institution_id);

ALTER TABLE ONLY public.platform_account_roles
    ADD CONSTRAINT platform_account_roles_pkey PRIMARY KEY (platform_account_role_id);

ALTER TABLE ONLY public.platform_account_roles
    ADD CONSTRAINT platform_account_roles_unique UNIQUE (platform_account_id, role_id);

ALTER TABLE ONLY public.platform_accounts
    ADD CONSTRAINT platform_accounts_email_unique UNIQUE (email);

ALTER TABLE ONLY public.platform_accounts
    ADD CONSTRAINT platform_accounts_pkey PRIMARY KEY (platform_account_id);

ALTER TABLE ONLY public.platform_refresh_tokens
    ADD CONSTRAINT platform_refresh_tokens_pkey PRIMARY KEY (platform_refresh_token_id);

ALTER TABLE ONLY public.platform_refresh_tokens
    ADD CONSTRAINT platform_refresh_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.platform_sessions
    ADD CONSTRAINT platform_sessions_pkey PRIMARY KEY (platform_session_id);

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_country_georef_unique UNIQUE (country_id, georef_id);

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_country_name_unique UNIQUE (country_id, name);

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_pkey PRIMARY KEY (province_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (refresh_token_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (permission_id, role_id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_scope_code_institution_unique UNIQUE (scope, code, institution_id);

ALTER TABLE ONLY public.student_guardians
    ADD CONSTRAINT student_guardians_pkey PRIMARY KEY (student_guardian_id);

ALTER TABLE ONLY public.student_guardians
    ADD CONSTRAINT student_guardians_unique UNIQUE (institution_id, student_id, guardian_profile_id);

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_file_number_unique UNIQUE (institution_id, file_number);

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_institution_id_id_unique UNIQUE (institution_id, student_id);

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_person_id_unique UNIQUE (institution_id, person_id);

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (student_id);

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_pkey PRIMARY KEY (user_session_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_institution_person_unique UNIQUE (institution_id, person_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);

CREATE INDEX people_institution_deleted_idx ON public.people USING btree (institution_id, deleted);

CREATE INDEX person_role_assignments_institution_role_idx ON public.person_role_assignments USING btree (institution_id, role_id);

CREATE INDEX person_role_assignments_person_institution_idx ON public.person_role_assignments USING btree (person_id, institution_id);

CREATE INDEX platform_refresh_tokens_family_idx ON public.platform_refresh_tokens USING btree (family_id);

CREATE INDEX platform_refresh_tokens_session_idx ON public.platform_refresh_tokens USING btree (platform_session_id);

CREATE INDEX platform_sessions_account_active_idx ON public.platform_sessions USING btree (platform_account_id, active);

CREATE INDEX refresh_tokens_family_idx ON public.refresh_tokens USING btree (family_id);

CREATE INDEX refresh_tokens_session_idx ON public.refresh_tokens USING btree (session_id);

CREATE INDEX user_sessions_user_active_idx ON public.user_sessions USING btree (user_id, active);

ALTER TABLE ONLY public.student_guardians
    ADD CONSTRAINT fk2o91119oldcr0qjk9wmo50ubd FOREIGN KEY (guardian_profile_id) REFERENCES public.guardian_profiles(guardian_profile_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk2qqjpih9isqcs22710v8lef9w FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk3oc3y1jbad69c3h7y2fv88hsu FOREIGN KEY (session_id) REFERENCES public.user_sessions(user_session_id);

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT fk48p9qkti5auert2gquvn76338 FOREIGN KEY (country_id) REFERENCES public.countries(country_id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT fk4taatjk5unh4y8ep3tx5ivk60 FOREIGN KEY (city_id) REFERENCES public.cities(city_id);

ALTER TABLE ONLY public.platform_refresh_tokens
    ADD CONSTRAINT fk5xgv6h6r8yxl00k576xk80gwe FOREIGN KEY (platform_account_id) REFERENCES public.platform_accounts(platform_account_id);

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT fk8klxsgb8dcjjklmqebqp1twd5 FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT fk8woik45b2i7oya1sapndmkvsl FOREIGN KEY (birth_city_id) REFERENCES public.cities(city_id);

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT fk9fkb8qaj71tiyr9htkmn7r8y5 FOREIGN KEY (city_id) REFERENCES public.cities(city_id);

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT fk9matrr71ro8s4ujmw1qjn40h4 FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk9x7brwurtag62dg4e4eugfaf0 FOREIGN KEY (person_id) REFERENCES public.people(person_id);

ALTER TABLE ONLY public.person_role_assignments
    ADD CONSTRAINT fkboh25xpjxx116ql4n9bje6r6n FOREIGN KEY (person_id) REFERENCES public.people(person_id);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT fkbueucqaikoa3yawsdv8pnkn8p FOREIGN KEY (address_id) REFERENCES public.addresses(address_id);

ALTER TABLE ONLY public.platform_account_roles
    ADD CONSTRAINT fkbxc242eybs403wkw6tkk6acbe FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.person_role_assignments
    ADD CONSTRAINT fkc990551153i07vg1j5ksdho1t FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.cities
    ADD CONSTRAINT fkcf2ndxcsekl26rrkb9egbhq20 FOREIGN KEY (province_id) REFERENCES public.provinces(province_id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(permission_id);

ALTER TABLE ONLY public.student_guardians
    ADD CONSTRAINT fkf3bj5ksuok1k0lbenj0wch3uf FOREIGN KEY (student_id) REFERENCES public.students(student_id);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT fkfgb64ovhsn54jwubn8qrnkcje FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.platform_account_roles
    ADD CONSTRAINT fkgtyk6jl11gg345lawlp7ym5h9 FOREIGN KEY (platform_account_id) REFERENCES public.platform_accounts(platform_account_id);

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fkhu5lxwgdn4ab5bf49h5s10iu5 FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT fkih889rqx3pwxjw7q6x4c08rws FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.platform_refresh_tokens
    ADD CONSTRAINT fkj420c163sgb2ibvr9bgx3kssa FOREIGN KEY (platform_session_id) REFERENCES public.platform_sessions(platform_session_id);

ALTER TABLE ONLY public.people
    ADD CONSTRAINT fkmot2mhshfpnt7ph5j31n4kfr3 FOREIGN KEY (nationality_country_id) REFERENCES public.countries(country_id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.student_guardians
    ADD CONSTRAINT fkoboq83weq0dva8xy1l6g9n0tr FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.guardian_profiles
    ADD CONSTRAINT fks5ejwbpq8bd0gnptumi6y9ru FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.person_role_assignments
    ADD CONSTRAINT fksjud3l0j0dgr5sxn2b2oivca8 FOREIGN KEY (institution_id) REFERENCES public.institutions(institution_id);

ALTER TABLE ONLY public.platform_sessions
    ADD CONSTRAINT fkstonfkigy75f40c8tdy53g3a6 FOREIGN KEY (platform_account_id) REFERENCES public.platform_accounts(platform_account_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fksv7wp99d6g5x8iisfpjf6sbpg FOREIGN KEY (person_id) REFERENCES public.people(person_id);

ALTER TABLE ONLY public.guardian_profiles
    ADD CONSTRAINT fkt8j9nfdtfwbwwu0164eam8aqc FOREIGN KEY (person_id) REFERENCES public.people(person_id);

