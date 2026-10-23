ALTER TABLE courses
    ADD COLUMN reserved_seats INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN courses.reserved_seats IS
    'Сколько мест зарезервировано сагой платной записи';