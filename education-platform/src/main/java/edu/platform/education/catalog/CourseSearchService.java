package edu.platform.education.catalog;

import edu.platform.education.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final CourseRepository courseRepository;
    private final CourseCriteriaSearch courseCriteriaSearch;
    private final CourseMapper courseMapper;

    @Transactional(readOnly = true)
    public Page<CourseResponse> searchByCriteria(String title, BigDecimal minPrice, BigDecimal maxPrice,
                                                 int page, int size) {
        validate(minPrice, maxPrice);
        Pageable pageable = buildPageable(page, size);
        return courseCriteriaSearch
                .search(blankToNull(title), minPrice, maxPrice, pageable)
                .map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> searchBySpecification(String title, BigDecimal minPrice, BigDecimal maxPrice,
                                                      int page, int size) {
        validate(minPrice, maxPrice);
        Pageable pageable = buildPageable(page, size);
        Specification<Course> spec = CourseSpecifications.byFilter(blankToNull(title), minPrice, maxPrice);
        return courseRepository.findAll(spec, pageable).map(courseMapper::toResponse);
    }

    private Pageable buildPageable(int page, int size) {
        Sort sort = Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
        return PageRequest.of(page, size, sort);
    }

    private void validate(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.signum() < 0) {
            throw new BadRequestException("minPrice не может быть отрицательным");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new BadRequestException("maxPrice не может быть отрицательным");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice не может быть больше maxPrice");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}