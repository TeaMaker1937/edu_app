package edu.platform.education.mapper;

import edu.platform.education.dto.CourseRequest;
import edu.platform.education.dto.CourseResponse;
import edu.platform.education.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "teacherId", source = "teacher.id")
    CourseResponse toResponse(Course course);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", ignore = true)  // учителя установим в сервисе из контекста
    Course toEntity(CourseRequest request);
}