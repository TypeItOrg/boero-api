-- Preserve existing history; reject inconsistent data rather than silently deleting it.
UPDATE enrollment_application_courses child
SET status = 'CANCELLED', resolved_at = COALESCE(child.resolved_at, CURRENT_TIMESTAMP)
FROM enrollment_applications parent
WHERE child.enrollment_application_id = parent.enrollment_application_id
  AND parent.status = 'CANCELLED' AND child.status IN ('PENDING', 'WAITLISTED');

-- Repair enrollments left active by the old academic-year closure path.
INSERT INTO course_enrollment_histories
    (course_enrollment_history_id, institution_id, course_enrollment_id, previous_status, new_status,
     previous_academic_status, new_academic_status, operation, reason, changed_at, created_at, updated_at)
SELECT gen_random_uuid(), e.institution_id, e.course_enrollment_id, e.status, 'COMPLETED',
       e.academic_status, CASE WHEN e.academic_status = 'IN_PROGRESS' THEN 'PENDING_RESULT' ELSE e.academic_status END,
       'COURSE_FINISHED', 'Regularización de cursada de curso finalizado.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM course_enrollments e JOIN courses c USING (course_id)
WHERE c.status = 'CLOSED' AND e.status = 'ENROLLED';
UPDATE course_enrollments e SET status = 'COMPLETED', completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP),
    academic_status = CASE WHEN academic_status = 'IN_PROGRESS' THEN 'PENDING_RESULT' ELSE academic_status END,
    version = version + 1, updated_at = CURRENT_TIMESTAMP
FROM courses c WHERE c.course_id = e.course_id AND c.status = 'CLOSED' AND e.status = 'ENROLLED';
UPDATE course_enrollment_schedules assignment SET released_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
FROM course_enrollments enrollment WHERE enrollment.course_enrollment_id = assignment.course_enrollment_id
    AND enrollment.status <> 'ENROLLED' AND assignment.released_at IS NULL;

ALTER TABLE enrollment_applications ADD CONSTRAINT enrollment_applications_tenant_id_unique
    UNIQUE (institution_id, enrollment_application_id);
ALTER TABLE enrollment_application_courses
    ADD CONSTRAINT enrollment_application_courses_tenant_id_unique UNIQUE (institution_id, enrollment_application_course_id),
    ADD CONSTRAINT enrollment_application_courses_tenant_parent_fk FOREIGN KEY (institution_id, enrollment_application_id)
        REFERENCES enrollment_applications (institution_id, enrollment_application_id),
    ADD CONSTRAINT enrollment_application_courses_tenant_teacher_fk FOREIGN KEY (institution_id, preferred_teacher_id)
        REFERENCES people (institution_id, person_id),
    ADD CONSTRAINT enrollment_application_courses_waitlist_complete CHECK
        (status <> 'WAITLISTED' OR (waitlist_number IS NOT NULL AND waitlisted_at IS NOT NULL AND waitlist_reason IS NOT NULL));
CREATE UNIQUE INDEX enrollment_application_courses_waitlist_number_unique
    ON enrollment_application_courses (institution_id, course_id, waitlist_number) WHERE waitlist_number IS NOT NULL;

ALTER TABLE course_classes ADD CONSTRAINT course_classes_course_tenant_unique
    UNIQUE (institution_id, course_id, course_class_id);
ALTER TABLE course_class_schedules ADD CONSTRAINT course_class_schedules_tenant_id_unique
    UNIQUE (institution_id, course_class_schedule_id);
ALTER TABLE course_individual_slots
    ADD CONSTRAINT course_individual_slots_tenant_schedule_fk FOREIGN KEY (institution_id, course_class_schedule_id)
        REFERENCES course_class_schedules (institution_id, course_class_schedule_id),
    ADD CONSTRAINT course_individual_slots_schedule_id_unique UNIQUE (institution_id, course_class_schedule_id, course_individual_slot_id);

ALTER TABLE course_enrollments ADD COLUMN enrollment_application_course_id uuid;
UPDATE course_enrollments enrollment
SET enrollment_application_course_id = child.enrollment_application_course_id
FROM enrollment_application_courses child
WHERE enrollment.institution_id = child.institution_id
  AND enrollment.enrollment_application_id = child.enrollment_application_id
  AND enrollment.course_id = child.course_id;
ALTER TABLE course_enrollments
    ADD CONSTRAINT course_enrollments_tenant_id_unique UNIQUE (institution_id, course_enrollment_id),
    ADD CONSTRAINT course_enrollments_class_course_fk FOREIGN KEY (institution_id, course_id, course_class_id)
        REFERENCES course_classes (institution_id, course_id, course_class_id),
    ADD CONSTRAINT course_enrollments_tenant_application_fk FOREIGN KEY (institution_id, enrollment_application_id)
        REFERENCES enrollment_applications (institution_id, enrollment_application_id),
    ADD CONSTRAINT course_enrollments_application_course_fk FOREIGN KEY (institution_id, enrollment_application_course_id)
        REFERENCES enrollment_application_courses (institution_id, enrollment_application_course_id),
    ADD CONSTRAINT course_enrollments_application_course_unique UNIQUE (enrollment_application_course_id),
    ADD CONSTRAINT course_enrollments_source_child_check CHECK ((source = 'APPLICATION') = (enrollment_application_course_id IS NOT NULL));

ALTER TABLE course_enrollment_schedules
    ADD CONSTRAINT course_enrollment_schedules_tenant_enrollment_fk FOREIGN KEY (institution_id, course_enrollment_id)
        REFERENCES course_enrollments (institution_id, course_enrollment_id),
    ADD CONSTRAINT course_enrollment_schedules_tenant_schedule_fk FOREIGN KEY (institution_id, course_class_schedule_id)
        REFERENCES course_class_schedules (institution_id, course_class_schedule_id),
    ADD CONSTRAINT course_enrollment_schedules_matching_slot_fk FOREIGN KEY (institution_id, course_class_schedule_id, course_individual_slot_id)
        REFERENCES course_individual_slots (institution_id, course_class_schedule_id, course_individual_slot_id);
CREATE UNIQUE INDEX course_enrollment_schedules_group_assignment_unique
    ON course_enrollment_schedules (course_enrollment_id, course_class_schedule_id)
    WHERE course_individual_slot_id IS NULL;
CREATE UNIQUE INDEX eac_snapshot_group_assignment_unique
    ON enrollment_application_course_assignment_snapshots (enrollment_application_course_id, course_class_schedule_id)
    WHERE course_individual_slot_id IS NULL;
ALTER TABLE course_enrollment_histories ADD CONSTRAINT course_enrollment_histories_tenant_enrollment_fk
    FOREIGN KEY (institution_id, course_enrollment_id) REFERENCES course_enrollments (institution_id, course_enrollment_id);
ALTER TABLE enrollment_application_course_assignment_snapshots
    ADD CONSTRAINT eac_snapshot_tenant_child_fk FOREIGN KEY (institution_id, enrollment_application_course_id)
        REFERENCES enrollment_application_courses (institution_id, enrollment_application_course_id);

-- Backfill stable slots for schedules created before slot creation moved out of GET.
INSERT INTO course_individual_slots
    (course_individual_slot_id, institution_id, course_class_schedule_id, start_time, end_time, created_at, updated_at)
SELECT gen_random_uuid(), schedule.institution_id, schedule.course_class_schedule_id,
       (schedule.start_time + n * make_interval(mins => day.period_duration_minutes))::time,
       (schedule.start_time + (n + 1) * make_interval(mins => day.period_duration_minutes))::time,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM course_class_schedules schedule
JOIN course_class_days day USING (course_class_day_id)
CROSS JOIN LATERAL generate_series(0,
    (extract(epoch FROM (schedule.end_time - schedule.start_time)) / 60 / NULLIF(day.period_duration_minutes, 0))::integer - 1) n
WHERE day.period_duration_minutes > 0
ON CONFLICT (course_class_schedule_id, start_time, end_time) DO NOTHING;

-- Database-side validation uses the same institution mutex as application mutations.
CREATE FUNCTION validate_course_selection_integrity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent enrollment_applications%ROWTYPE; selected courses%ROWTYPE;
BEGIN
    PERFORM 1 FROM institutions WHERE institution_id = NEW.institution_id FOR UPDATE;
    SELECT * INTO parent FROM enrollment_applications WHERE enrollment_application_id = NEW.enrollment_application_id;
    SELECT * INTO selected FROM courses WHERE course_id = NEW.course_id;
    IF parent.institution_id IS DISTINCT FROM NEW.institution_id
       OR selected.institution_id IS DISTINCT FROM NEW.institution_id
       OR selected.academic_year_id IS DISTINCT FROM parent.academic_year_id
       OR selected.training_path_id IS DISTINCT FROM parent.training_path_id THEN
        RAISE EXCEPTION 'Invalid course context' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_application_courses_context_check';
    END IF;
    IF NEW.status IN ('PENDING', 'WAITLISTED') AND parent.status NOT IN ('CANCELLED', 'REJECTED')
       AND EXISTS (SELECT 1 FROM enrollment_application_courses other
           JOIN enrollment_applications app ON app.enrollment_application_id = other.enrollment_application_id
           WHERE other.institution_id = NEW.institution_id AND other.course_id = NEW.course_id
             AND other.enrollment_application_course_id <> NEW.enrollment_application_course_id
             AND app.applicant_person_id = parent.applicant_person_id AND app.deleted_at IS NULL
             AND app.status NOT IN ('CANCELLED', 'REJECTED') AND other.status IN ('PENDING', 'WAITLISTED')) THEN
        RAISE EXCEPTION 'Course already requested' USING ERRCODE = '23505', CONSTRAINT = 'enrollment_application_courses_active_applicant_course_unique';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_application_courses_integrity BEFORE INSERT OR UPDATE ON enrollment_application_courses
    FOR EACH ROW EXECUTE FUNCTION validate_course_selection_integrity();

CREATE FUNCTION validate_course_instrument_integrity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE placement study_plan_spaces%ROWTYPE; space academic_spaces%ROWTYPE; path uuid;
BEGIN
    SELECT * INTO placement FROM study_plan_spaces WHERE study_plan_space_id = NEW.study_plan_space_id;
    SELECT * INTO space FROM academic_spaces WHERE academic_space_id = placement.academic_space_id FOR SHARE;
    SELECT training_path_id INTO path FROM study_plans WHERE study_plan_id = placement.study_plan_id;
    IF placement.institution_id IS DISTINCT FROM NEW.institution_id
       OR placement.academic_space_id IS DISTINCT FROM NEW.academic_space_id
       OR placement.academic_level_id IS DISTINCT FROM NEW.academic_level_id
       OR path IS DISTINCT FROM NEW.training_path_id
       OR space.instrumental IS DISTINCT FROM (NEW.instrument_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Invalid curriculum or instrument' USING ERRCODE = '23514', CONSTRAINT = 'courses_curriculum_instrument_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER courses_curriculum_integrity BEFORE INSERT OR UPDATE OF study_plan_space_id, academic_space_id, academic_level_id, training_path_id, instrument_id
    ON courses FOR EACH ROW EXECUTE FUNCTION validate_course_instrument_integrity();

CREATE FUNCTION validate_course_enrollment_parent() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.source = 'APPLICATION' AND NOT EXISTS (
        SELECT 1 FROM enrollment_application_courses child
        JOIN enrollment_applications parent USING (enrollment_application_id)
        JOIN students student ON student.person_id = parent.applicant_person_id AND student.institution_id = parent.institution_id
        WHERE child.enrollment_application_course_id = NEW.enrollment_application_course_id
          AND child.enrollment_application_id = NEW.enrollment_application_id AND child.course_id = NEW.course_id
          AND parent.status = 'APPROVED' AND student.student_id = NEW.student_id) THEN
        RAISE EXCEPTION 'Invalid enrollment origin' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollments_origin_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER course_enrollments_origin BEFORE INSERT OR UPDATE ON course_enrollments
    FOR EACH ROW EXECUTE FUNCTION validate_course_enrollment_parent();

CREATE FUNCTION validate_course_enrollment_assignment() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE enrollment course_enrollments%ROWTYPE; schedule course_class_schedules%ROWTYPE;
        day course_class_days%ROWTYPE; slot course_individual_slots%ROWTYPE; occupied integer;
BEGIN
    PERFORM 1 FROM institutions WHERE institution_id = NEW.institution_id FOR UPDATE;
    SELECT * INTO enrollment FROM course_enrollments WHERE course_enrollment_id = NEW.course_enrollment_id;
    SELECT * INTO schedule FROM course_class_schedules WHERE course_class_schedule_id = NEW.course_class_schedule_id;
    SELECT * INTO day FROM course_class_days WHERE course_class_day_id = schedule.course_class_day_id;
    IF enrollment.course_class_id IS DISTINCT FROM day.course_class_id OR NEW.day_of_week IS DISTINCT FROM day.day_of_week
       OR (day.period_duration_minutes IS NOT NULL) IS DISTINCT FROM (NEW.course_individual_slot_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Invalid assignment' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollment_schedules_context_check';
    END IF;
    IF NEW.course_individual_slot_id IS NOT NULL THEN
        SELECT * INTO slot FROM course_individual_slots WHERE course_individual_slot_id = NEW.course_individual_slot_id;
        IF NEW.start_time IS DISTINCT FROM slot.start_time OR NEW.end_time IS DISTINCT FROM slot.end_time THEN
            RAISE EXCEPTION 'Invalid slot time' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollment_schedules_context_check';
        END IF;
    ELSIF NEW.start_time IS DISTINCT FROM schedule.start_time OR NEW.end_time IS DISTINCT FROM schedule.end_time THEN
        RAISE EXCEPTION 'Invalid group time' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollment_schedules_context_check';
    END IF;
    IF NEW.released_at IS NULL THEN
        IF EXISTS (SELECT 1 FROM course_enrollment_schedules other
            JOIN course_class_schedules other_schedule USING (course_class_schedule_id)
            WHERE other.course_enrollment_id = NEW.course_enrollment_id
              AND other_schedule.course_class_day_id = day.course_class_day_id
              AND other.course_enrollment_schedule_id <> NEW.course_enrollment_schedule_id AND other.released_at IS NULL) THEN
            RAISE EXCEPTION 'Duplicate day' USING ERRCODE = '23505', CONSTRAINT = 'course_enrollment_schedules_active_day_unique';
        END IF;
        SELECT count(*) INTO occupied FROM course_enrollment_schedules other
            JOIN course_class_schedules other_schedule USING (course_class_schedule_id)
            WHERE other.institution_id = NEW.institution_id AND other.released_at IS NULL
              AND other.course_enrollment_schedule_id <> NEW.course_enrollment_schedule_id
              AND other_schedule.course_class_day_id = day.course_class_day_id;
        IF day.capacity IS NOT NULL AND occupied >= day.capacity THEN
            RAISE EXCEPTION 'No capacity' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollment_schedules_capacity_check';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER course_enrollment_schedules_integrity BEFORE INSERT OR UPDATE ON course_enrollment_schedules
    FOR EACH ROW EXECUTE FUNCTION validate_course_enrollment_assignment();

-- Roles able to resolve a child must also be able to read its documentary parent.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, parent.permission_id FROM role_permissions rp
JOIN permissions child ON child.permission_id = rp.permission_id
CROSS JOIN permissions parent
WHERE child.code IN ('institution:enrollment-application-course:enroll', 'institution:enrollment-application-course:reject')
  AND parent.code = 'institution:enrollment-application:read'
ON CONFLICT DO NOTHING;

CREATE FUNCTION protect_instantiated_academic_space() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.instrumental IS DISTINCT FROM OLD.instrumental OR NEW.format IS DISTINCT FROM OLD.format)
       AND EXISTS (SELECT 1 FROM courses WHERE academic_space_id = NEW.academic_space_id) THEN
        RAISE EXCEPTION 'Academic space already instantiated' USING ERRCODE = '23514', CONSTRAINT = 'academic_spaces_instantiated_shape_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER academic_spaces_instantiated_shape BEFORE UPDATE OF instrumental, format ON academic_spaces
    FOR EACH ROW EXECUTE FUNCTION protect_instantiated_academic_space();

ALTER TABLE course_enrollments ADD CONSTRAINT course_enrollments_completed_result_check
    CHECK (status <> 'COMPLETED' OR academic_status <> 'IN_PROGRESS');
ALTER TABLE enrollment_application_course_assignment_snapshots
    ADD CONSTRAINT eac_snapshot_class_course_fk FOREIGN KEY (institution_id, course_id, course_class_id)
        REFERENCES course_classes (institution_id, course_id, course_class_id),
    ADD CONSTRAINT eac_snapshot_tenant_schedule_fk FOREIGN KEY (institution_id, course_class_schedule_id)
        REFERENCES course_class_schedules (institution_id, course_class_schedule_id),
    ADD CONSTRAINT eac_snapshot_matching_slot_fk FOREIGN KEY (institution_id, course_class_schedule_id, course_individual_slot_id)
        REFERENCES course_individual_slots (institution_id, course_class_schedule_id, course_individual_slot_id);
