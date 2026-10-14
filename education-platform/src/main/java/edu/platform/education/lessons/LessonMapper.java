package edu.platform.education.lessons;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface LessonMapper {

    LessonResponse toResponse(Lesson lesson);
}