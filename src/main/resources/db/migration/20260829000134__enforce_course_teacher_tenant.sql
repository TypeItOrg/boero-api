ALTER TABLE course_class_teachers
    DROP CONSTRAINT course_class_teachers_person_fk;

ALTER TABLE course_class_teachers
    ADD CONSTRAINT course_class_teachers_person_institution_fk
        FOREIGN KEY (institution_id, person_id)
            REFERENCES people (institution_id, person_id);
