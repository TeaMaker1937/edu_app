-- Копируем данные из description в summary, пока summary ещё NULL
UPDATE courses SET summary = description WHERE summary IS NULL;