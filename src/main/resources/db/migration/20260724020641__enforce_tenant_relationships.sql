ALTER TABLE people
    ADD CONSTRAINT people_address_institution_fk
    FOREIGN KEY (institution_id, address_id)
    REFERENCES addresses (institution_id, address_id);

ALTER TABLE users
    ADD CONSTRAINT users_person_institution_fk
    FOREIGN KEY (institution_id, person_id)
    REFERENCES people (institution_id, person_id);

ALTER TABLE students
    ADD CONSTRAINT students_person_institution_fk
    FOREIGN KEY (institution_id, person_id)
    REFERENCES people (institution_id, person_id);

ALTER TABLE guardian_profiles
    ADD CONSTRAINT guardian_profiles_person_institution_fk
    FOREIGN KEY (institution_id, person_id)
    REFERENCES people (institution_id, person_id);

ALTER TABLE student_guardians
    ADD CONSTRAINT student_guardians_student_institution_fk
    FOREIGN KEY (institution_id, student_id)
    REFERENCES students (institution_id, student_id);

ALTER TABLE student_guardians
    ADD CONSTRAINT student_guardians_guardian_institution_fk
    FOREIGN KEY (institution_id, guardian_profile_id)
    REFERENCES guardian_profiles (institution_id, guardian_profile_id);

ALTER TABLE person_role_assignments
    ADD CONSTRAINT person_role_assignments_person_institution_fk
    FOREIGN KEY (institution_id, person_id)
    REFERENCES people (institution_id, person_id);
