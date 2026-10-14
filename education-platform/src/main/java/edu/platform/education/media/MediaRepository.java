package edu.platform.education.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface MediaRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findAllByUploadedByOrderByIdDesc(Long uploadedBy);
}