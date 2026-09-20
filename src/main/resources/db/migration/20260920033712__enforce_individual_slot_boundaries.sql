-- Reject inconsistent historical slots without deleting enrollment history.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM course_individual_slots slot
        JOIN course_class_schedules schedule USING (course_class_schedule_id)
        JOIN course_class_days day USING (course_class_day_id)
        WHERE day.period_duration_minutes IS NULL OR day.period_duration_minutes <= 0
           OR slot.start_time < schedule.start_time OR slot.end_time > schedule.end_time
           OR EXTRACT(EPOCH FROM (slot.end_time - slot.start_time)) <> day.period_duration_minutes * 60
           OR MOD(EXTRACT(EPOCH FROM (slot.start_time - schedule.start_time)),
                  NULLIF(day.period_duration_minutes * 60, 0)) <> 0
    ) THEN
        RAISE EXCEPTION 'Existing individual slots do not match their schedule; review them before migrating'
            USING ERRCODE = '23514', CONSTRAINT = 'course_individual_slots_schedule_bounds_check';
    END IF;
END $$;

CREATE FUNCTION validate_individual_slot_boundaries() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE schedule_start time; schedule_end time; period_minutes integer;
BEGIN
    SELECT schedule.start_time, schedule.end_time, day.period_duration_minutes
    INTO schedule_start, schedule_end, period_minutes
    FROM course_class_schedules schedule
    JOIN course_class_days day USING (course_class_day_id)
    WHERE schedule.course_class_schedule_id = NEW.course_class_schedule_id;

    IF NOT FOUND OR period_minutes IS NULL OR period_minutes <= 0
       OR NEW.start_time < schedule_start OR NEW.end_time > schedule_end
       OR EXTRACT(EPOCH FROM (NEW.end_time - NEW.start_time)) <> period_minutes * 60
       OR MOD(EXTRACT(EPOCH FROM (NEW.start_time - schedule_start)), NULLIF(period_minutes * 60, 0)) <> 0 THEN
        RAISE EXCEPTION 'Individual slot does not match its schedule'
            USING ERRCODE = '23514', CONSTRAINT = 'course_individual_slots_schedule_bounds_check';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER course_individual_slots_schedule_bounds
    BEFORE INSERT OR UPDATE ON course_individual_slots
    FOR EACH ROW EXECUTE FUNCTION validate_individual_slot_boundaries();
