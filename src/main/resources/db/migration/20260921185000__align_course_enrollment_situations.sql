ALTER TABLE course_enrollments
    DROP CONSTRAINT course_enrollments_academic_status_check;

ALTER TABLE course_enrollments
    ADD CONSTRAINT course_enrollments_academic_status_check
        CHECK (academic_status IN ('IN_PROGRESS', 'PENDING_RESULT', 'REGULARIZED', 'PROMOTED', 'PASSED', 'FAILED', 'NOT_APPLICABLE')),
    ADD CONSTRAINT course_enrollments_situation_check
        CHECK (
            status = 'ENROLLED' AND academic_status = 'IN_PROGRESS'
            OR status = 'COMPLETED' AND academic_status IN ('PENDING_RESULT', 'REGULARIZED', 'PROMOTED', 'PASSED', 'FAILED')
            OR status IN ('WITHDRAWN', 'ADMINISTRATIVELY_WITHDRAWN') AND academic_status = 'NOT_APPLICABLE'
        ) NOT VALID;
