package com.example.study.study;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    /**
     * 목록 조회.
     *
     * 검색어와 상태를 함께 받으며 값이 없으면 조건에서 제외됨.
     * 모집자를 함께 가져와 목록 건수만큼 조회가 늘어나지 않게 함.
     */
    @EntityGraph(attributePaths = {"writer"})
    @Query("""
            select p from StudyPost p
            where (:keyword is null or p.title like concat('%', :keyword, '%'))
              and (:status is null or p.status = :status)
            """)
    Page<StudyPost> search(String keyword, StudyStatus status, Pageable pageable);

    // 제공 · 다른 담당도 쓰는 규약이라 미리 만들어 둠.
    //
    // 모집자를 함께 가져옴. 지정하지 않으면 작성자 조회가 따로 나감.
    @EntityGraph(attributePaths = {"writer"})
    Optional<StudyPost> findWithWriterById(Long id);

    /*
     * TODO 61 · 내 모집글 규약
     *
     * 기능        모집자 식별자로 조회하며 최신순 정렬 · 모집자를 함께 가져옴
     * 활용메소드  없음 · 이름 규약으로 직접 선언
     * 반환형태    List<StudyPost>
     * 동작결과    마이페이지에서 내 모집글이 최신순으로 나옴
     */
    List<StudyPost> findByWriterId(Long id);
}
