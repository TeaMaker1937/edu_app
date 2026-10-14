package edu.platform.education.catalog;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Поиск курсов через Criteria API (ручное построение запроса через EntityManager).
 * Это второй, независимый от Specification, способ.
 */
@Repository
class CourseCriteriaSearch {

    @PersistenceContext
    private EntityManager em;

    public Page<Course> search(String title, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // -------- 1. Основной запрос с контентом страницы --------
        CriteriaQuery<Course> cq = cb.createQuery(Course.class);
        Root<Course> root = cq.from(Course.class);
        cq.where(buildPredicates(cb, root, title, minPrice, maxPrice));
        cq.orderBy(toOrders(cb, root, pageable.getSort()));

        TypedQuery<Course> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Course> content = query.getResultList();

        // -------- 2. Отдельный count-запрос --------
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Course> countRoot = countCq.from(Course.class);
        countCq.select(cb.count(countRoot));
        countCq.where(buildPredicates(cb, countRoot, title, minPrice, maxPrice));
        Long total = em.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Course> root,
                                        String title, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Predicate> ps = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            String pattern = "%" + CourseSpecifications.escapeLike(title.toLowerCase()) + "%";
            ps.add(cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        if (minPrice != null) ps.add(cb.ge(root.get("price"), minPrice));
        if (maxPrice != null) ps.add(cb.le(root.get("price"), maxPrice));
        return ps.toArray(new Predicate[0]);
    }

    private List<Order> toOrders(CriteriaBuilder cb, Root<Course> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            orders.add(o.isAscending()
                    ? cb.asc(root.get(o.getProperty()))
                    : cb.desc(root.get(o.getProperty())));
        }
        return orders;
    }
}