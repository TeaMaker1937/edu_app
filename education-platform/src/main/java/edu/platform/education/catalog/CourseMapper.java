package edu.platform.education.catalog;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface CourseMapper {

    @Mapping(target = "teacherId", source = "teacher.id")
    CourseResponse toResponse(Course course);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "version", ignore = true)
    Course toEntity(CourseRequest request);
}