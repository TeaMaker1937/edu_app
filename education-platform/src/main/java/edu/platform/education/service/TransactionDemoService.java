package edu.platform.education.service;

import edu.platform.education.dto.CourseResponse;
import edu.platform.education.entity.Course;
import edu.platform.education.exception.ResourceNotFoundException;
import edu.platform.education.mapper.CourseMapper;
import edu.platform.education.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDemoService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    /**
     * Пессимистическая блокировка:
     * SELECT ... FOR UPDATE. Строка блокируется до конца транзакции —
     * другие транзакции не смогут её изменить, пока мы не закоммитимся.
     *
     * Используется, когда важна консистентность чтения и записи в одной транзакции
     * (например, при списании баланса или бронировании места).
     */
    @Transactional
    public void renameCoursePessimistic(Long courseId, String newTitle) {
        log.info("[PESSIMISTIC] Начало транзакции для курса {}", courseId);
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс с id " + courseId + " не найден"));
        // ... имитация работы
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        course.setTitle(newTitle);
        log.info("[PESSIMISTIC] Курс переименован в '{}'", newTitle);
        // COMMIT здесь освобождает блокировку
    }

    /**
     * Оптимистическая блокировка через @Version.
     * Hibernate при UPDATE добавит условие AND version = <значение_при_чтении>.
     * Если параллельная транзакция успела изменить строку — будет OptimisticLockException.
     *
     * Это дешевле пессимистической блокировки, но требует обработки конфликта на уровне приложения
     * (повтор или сообщение пользователю).
     */
    @Transactional
    public void renameCourseOptimistic(Long courseId, String newTitle) {
        log.info("[OPTIMISTIC] Начало транзакции для курса {}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс с id " + courseId + " не найден"));
        // ... имитация работы
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        course.setTitle(newTitle);
        log.info("[OPTIMISTIC] Курс переименован в '{}', version будет инкрементирован", newTitle);
    }

    /**
     * Демонстрация разных уровней изоляции.
     * В реальности для Postgres чаще всего используется READ_COMMITTED.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseResponse readCommitted(Long id) {
        log.info("[READ_COMMITTED] Чтение курса {}", id);
        return courseMapper.toResponse(findCourse(id));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CourseResponse repeatableRead(Long id) {
        log.info("[REPEATABLE_READ] Чтение курса {}", id);
        return courseMapper.toResponse(findCourse(id));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseResponse serializable(Long id) {
        log.info("[SERIALIZABLE] Чтение курса {}", id);
        return courseMapper.toResponse(findCourse(id));
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Курс с id " + id + " не найден"));
    }

    /**
     * Демонстрация Propagation.
     * REQUIRES_NEW — создаёт независимую транзакцию; её ошибки/откат
     * не повлияют на внешнюю транзакцию.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActionInNewTransaction(String action) {
        log.info("[REQUIRES_NEW] Аудит-лог ({}): {}", LocalDateTime.now(), action);
    }


}