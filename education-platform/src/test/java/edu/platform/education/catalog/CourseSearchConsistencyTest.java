package edu.platform.education.catalog;

import edu.platform.education.users.Role;
import edu.platform.education.users.User;
import edu.platform.education.users.UserRegistrationRequest;
import edu.platform.education.users.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional   // каждый тест откатывается
class CourseSearchConsistencyTest {

    @Autowired private CourseSearchService searchService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private UserService userService;
    @Autowired private EntityManager em;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = userService.register(new UserRegistrationRequest(
                "search-" + UUID.randomUUID() + "@example.com",
                "password123",              // минимум 6 символов — валидация @Size(min = 6)
                "Search Tester",
                Role.TEACHER
        ));

        // Набор из 5 курсов с разными ценами и разным регистром
        createCourse("Java Basics",    1000);
        createCourse("Advanced Java",  1500);
        createCourse("Java Pro",       1500);   // та же цена, что и выше — проверим сортировку по id
        createCourse("Python 101",     2000);
        createCourse("JAVASCRIPT",     5000);

        em.flush();
        em.clear();
    }

    private void createCourse(String title, long price) {
        courseRepository.save(Course.builder()
                .title(title)
                .summary("d")
                .price(BigDecimal.valueOf(price))
                .version(0L)
                .teacher(teacher)
                .build());
    }

    // ---------- Тесты совпадения ----------

    @Test
    void noFilters_sameResult() {
        assertSame(null, null, null, 0, 10);
    }

    @Test
    void byTitle_sameResult() {
        assertSame("java", null, null, 0, 10);
    }

    @Test
    void byTitle_caseInsensitive_sameResult() {
        assertSame("JAVA", null, null, 0, 10);
        assertSame("Java", null, null, 0, 10);
    }

    @Test
    void byMinPrice_sameResult() {
        assertSame(null, new BigDecimal("1500"), null, 0, 10);
    }

    @Test
    void byMaxPrice_sameResult() {
        assertSame(null, null, new BigDecimal("1500"), 0, 10);
    }

    @Test
    void byTitleAndPriceRange_sameResult() {
        assertSame("java", new BigDecimal("1400"), new BigDecimal("1600"), 0, 10);
    }

    // ---------- Пагинация ----------

    @Test
    void pagination_pagesDoNotOverlap() {
        Page<CourseResponse> p0 = searchService.searchByCriteria(null, null, null, 0, 2);
        Page<CourseResponse> p1 = searchService.searchByCriteria(null, null, null, 1, 2);
        Page<CourseResponse> p2 = searchService.searchByCriteria(null, null, null, 2, 2);

        // все id уникальны
        List<Long> allIds = List.of(
                p0.getContent().get(0).id(),
                p0.getContent().get(1).id(),
                p1.getContent().get(0).id(),
                p1.getContent().get(1).id(),
                p2.getContent().get(0).id());

        assertEquals(5, allIds.stream().distinct().count(),
                "Страницы не должны пересекаться");

        // и Criteria, и Spec на каждой странице совпадают
        assertSame(null, null, null, 0, 2);
        assertSame(null, null, null, 1, 2);
        assertSame(null, null, null, 2, 2);
    }

    // ---------- Экранирование % и _ ----------

    @Test
    void percentInTitle_isLiteral() {
        createCourse("100% Java", 999);
        em.flush();
        em.clear();

        Page<CourseResponse> criteria = searchService.searchByCriteria("100%", null, null, 0, 10);
        Page<CourseResponse> spec     = searchService.searchBySpecification("100%", null, null, 0, 10);

        assertEquals(1, criteria.getTotalElements(), "Символ % должен быть литералом");
        assertEquals(1, spec.getTotalElements(), "Символ % должен быть литералом");
        assertEquals(criteria.getContent().get(0).id(), spec.getContent().get(0).id());
    }

    @Test
    void underscoreInTitle_isLiteral() {
        createCourse("java_advanced", 888);
        em.flush();
        em.clear();

        Page<CourseResponse> criteria = searchService.searchByCriteria("java_", null, null, 0, 10);
        Page<CourseResponse> spec     = searchService.searchBySpecification("java_", null, null, 0, 10);

        assertEquals(1, criteria.getTotalElements(), "Символ _ должен быть литералом");
        assertEquals(1, spec.getTotalElements(), "Символ _ должен быть литералом");
    }

    // ---------- вспомогательный ----------

    private void assertSame(String title, BigDecimal min, BigDecimal max, int page, int size) {
        Page<CourseResponse> criteria = searchService.searchByCriteria(title, min, max, page, size);
        Page<CourseResponse> spec     = searchService.searchBySpecification(title, min, max, page, size);

        assertEquals(criteria.getTotalElements(), spec.getTotalElements(),
                "Разное общее количество для фильтра title=" + title);

        List<Long> idsC = criteria.getContent().stream().map(CourseResponse::id).toList();
        List<Long> idsS = spec.getContent().stream().map(CourseResponse::id).toList();

        assertEquals(idsC, idsS,
                "Разный результат для title=" + title + ", min=" + min + ", max=" + max);
    }
}