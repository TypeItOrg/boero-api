CREATE TABLE academic_enrollment_exceptions (
    exception_id uuid PRIMARY KEY,
    institution_id uuid NOT NULL,
    person_id uuid NOT NULL,
    course_id uuid NOT NULL,
    prerequisite_id uuid NOT NULL REFERENCES prerequisites(prerequisite_id),
    required_study_plan_space_id uuid NOT NULL REFERENCES study_plan_spaces(study_plan_space_id),
    required_condition varchar(20) NOT NULL CHECK (required_condition IN ('REGULAR', 'PASSED')),
    reason varchar(2000) NOT NULL CHECK (length(trim(reason)) > 0),
    authority_person_id uuid,
    authority_platform_account_id uuid REFERENCES platform_accounts(platform_account_id),
    authority_name varchar(300) NOT NULL,
    authorized_at timestamptz NOT NULL,
    CONSTRAINT academic_exception_person_fk FOREIGN KEY(institution_id, person_id) REFERENCES people(institution_id, person_id),
    CONSTRAINT academic_exception_course_fk FOREIGN KEY(institution_id, course_id) REFERENCES courses(institution_id, course_id),
    CONSTRAINT academic_exception_authority_fk FOREIGN KEY(institution_id, authority_person_id) REFERENCES people(institution_id, person_id),
    CONSTRAINT academic_exception_authority_check CHECK ((authority_person_id IS NULL) <> (authority_platform_account_id IS NULL))
);
CREATE INDEX academic_exceptions_person_course_idx ON academic_enrollment_exceptions(institution_id, person_id, course_id);

CREATE FUNCTION validate_academic_enrollment_exception() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'Academic authorizations are append only' USING ERRCODE = '23514', CONSTRAINT = 'academic_exception_immutable';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM prerequisites prerequisite JOIN courses course ON course.study_plan_space_id = prerequisite.target_study_plan_space_id
        WHERE course.course_id = NEW.course_id AND course.institution_id = NEW.institution_id
          AND prerequisite.prerequisite_id = NEW.prerequisite_id
          AND prerequisite.required_study_plan_space_id = NEW.required_study_plan_space_id
          AND prerequisite.required_condition = NEW.required_condition AND prerequisite.requirement_stage = 'TO_ENROLL'
    ) THEN
        RAISE EXCEPTION 'Invalid academic authorization' USING ERRCODE = '23514', CONSTRAINT = 'academic_exception_requirement_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER academic_enrollment_exception_validate BEFORE INSERT OR UPDATE OR DELETE ON academic_enrollment_exceptions
    FOR EACH ROW EXECUTE FUNCTION validate_academic_enrollment_exception();

INSERT INTO permissions(created_at, updated_at, permission_id, scope, code, description)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-enrollment-exception:authorize')::uuid,
    'INSTITUTION', 'institution:academic-enrollment-exception:authorize', 'Autorizar excepciones académicas de inscripción');

CREATE FUNCTION validate_course_enrollment_academic_requirements() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE applicant_id uuid; target_id uuid;
BEGIN
    -- All enrollment and result mutations use the same tenant root lock.
    PERFORM 1 FROM institutions WHERE institution_id = NEW.institution_id FOR UPDATE;
    IF TG_OP = 'UPDATE' OR NEW.status <> 'ENROLLED' THEN
        RETURN NEW;
    END IF;
    SELECT person_id INTO applicant_id FROM students WHERE student_id = NEW.student_id AND institution_id = NEW.institution_id;
    SELECT study_plan_space_id INTO target_id FROM courses WHERE course_id = NEW.course_id AND institution_id = NEW.institution_id;
    IF EXISTS (
        SELECT 1 FROM prerequisites requirement
        WHERE requirement.target_study_plan_space_id = target_id AND requirement.requirement_stage = 'TO_ENROLL'
          AND NOT EXISTS (
              SELECT 1 FROM course_enrollments evidence
              JOIN students student ON student.student_id = evidence.student_id
              JOIN courses prior_course ON prior_course.course_id = evidence.course_id
              WHERE evidence.institution_id = NEW.institution_id AND student.person_id = applicant_id
                AND prior_course.study_plan_space_id = requirement.required_study_plan_space_id
                AND evidence.status IN ('ENROLLED', 'COMPLETED')
                AND (evidence.academic_status IN ('PROMOTED', 'PASSED')
                    OR requirement.required_condition = 'REGULAR' AND evidence.academic_status = 'REGULARIZED')
          )
          AND NOT EXISTS (
              SELECT 1 FROM academic_enrollment_exceptions exemption
              WHERE exemption.institution_id = NEW.institution_id AND exemption.person_id = applicant_id
                AND exemption.course_id = NEW.course_id AND exemption.prerequisite_id = requirement.prerequisite_id
                AND exemption.required_study_plan_space_id = requirement.required_study_plan_space_id
                AND exemption.required_condition = requirement.required_condition
          )
    ) THEN
        RAISE EXCEPTION 'Academic requirements are pending' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollment_academic_requirements_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER course_enrollment_academic_requirements BEFORE INSERT OR UPDATE OF academic_status, status
    ON course_enrollments FOR EACH ROW EXECUTE FUNCTION validate_course_enrollment_academic_requirements();
