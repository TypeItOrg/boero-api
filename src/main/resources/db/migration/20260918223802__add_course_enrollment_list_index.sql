CREATE INDEX course_enrollments_institution_enrolled_idx
    ON course_enrollments (institution_id, enrolled_at DESC, course_enrollment_id);
