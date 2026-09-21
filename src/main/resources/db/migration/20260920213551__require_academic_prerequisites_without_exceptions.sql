-- Preserve historical authorizations, but stop accepting or applying exceptions.
CREATE OR REPLACE FUNCTION validate_academic_enrollment_exception() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Academic authorizations are no longer supported'
        USING ERRCODE = '23514', CONSTRAINT = 'academic_exception_immutable';
END $$;

DELETE FROM role_permissions WHERE permission_id IN (
    SELECT permission_id FROM permissions WHERE code = 'institution:academic-enrollment-exception:authorize'
);
DELETE FROM permissions WHERE code = 'institution:academic-enrollment-exception:authorize';

CREATE FUNCTION enrollment_prerequisites_satisfied(tenant_id uuid, applicant_id uuid, target_course_id uuid)
RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT NOT EXISTS (
        SELECT 1 FROM prerequisites requirement
        JOIN courses target ON target.study_plan_space_id = requirement.target_study_plan_space_id
        WHERE target.course_id = target_course_id AND target.institution_id = tenant_id
          AND requirement.requirement_stage = 'TO_ENROLL'
          AND NOT EXISTS (
              SELECT 1 FROM course_enrollments evidence
              JOIN students student ON student.student_id = evidence.student_id AND student.institution_id = tenant_id
              JOIN courses prior_course ON prior_course.course_id = evidence.course_id AND prior_course.institution_id = tenant_id
              WHERE evidence.institution_id = tenant_id AND student.person_id = applicant_id
                AND prior_course.study_plan_space_id = requirement.required_study_plan_space_id
                AND evidence.status IN ('ENROLLED', 'COMPLETED')
                AND (evidence.academic_status IN ('PROMOTED', 'PASSED')
                    OR requirement.required_condition = 'REGULAR' AND evidence.academic_status = 'REGULARIZED')
          )
    );
$$;

CREATE OR REPLACE FUNCTION validate_course_enrollment_academic_requirements() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE applicant_id uuid;
BEGIN
    PERFORM 1 FROM institutions WHERE institution_id = NEW.institution_id FOR UPDATE;
    IF NEW.status <> 'ENROLLED' THEN
        RETURN NEW;
    END IF;
    -- Updating an existing result must not invalidate historical enrollments.
    IF TG_OP = 'UPDATE' AND OLD.status = 'ENROLLED' THEN
        RETURN NEW;
    END IF;
    SELECT person_id INTO applicant_id FROM students WHERE student_id = NEW.student_id AND institution_id = NEW.institution_id;
    IF NOT enrollment_prerequisites_satisfied(NEW.institution_id, applicant_id, NEW.course_id) THEN
        RAISE EXCEPTION 'Academic requirements are pending' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollment_academic_requirements_check';
    END IF;
    RETURN NEW;
END $$;

CREATE FUNCTION validate_application_academic_requirements() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status <> 'SUBMITTED' THEN
        RETURN NEW;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status = 'SUBMITTED' THEN
        RETURN NEW;
    END IF;
    PERFORM 1 FROM institutions WHERE institution_id = NEW.institution_id FOR UPDATE;
    IF EXISTS (
        SELECT 1 FROM enrollment_application_courses selection
        WHERE selection.enrollment_application_id = NEW.enrollment_application_id
          AND NOT enrollment_prerequisites_satisfied(NEW.institution_id, NEW.applicant_person_id, selection.course_id)
    ) THEN
        RAISE EXCEPTION 'Academic requirements are pending' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_application_academic_requirements_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_application_academic_requirements BEFORE INSERT OR UPDATE OF status
    ON enrollment_applications FOR EACH ROW EXECUTE FUNCTION validate_application_academic_requirements();
