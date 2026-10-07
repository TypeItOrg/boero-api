ALTER TABLE course_classes
    ADD COLUMN class_number integer;

WITH numbered_classes AS (
    SELECT course_class_id,
           row_number() OVER (
               PARTITION BY course_id
               ORDER BY created_at, course_class_id
           ) AS class_number
    FROM course_classes
)
UPDATE course_classes
SET class_number = numbered_classes.class_number
FROM numbered_classes
WHERE course_classes.course_class_id = numbered_classes.course_class_id;

ALTER TABLE course_classes
    ALTER COLUMN class_number SET NOT NULL,
    ADD CONSTRAINT course_classes_class_number_positive CHECK (class_number > 0),
    ADD CONSTRAINT course_classes_course_class_number_unique UNIQUE (course_id, class_number);
