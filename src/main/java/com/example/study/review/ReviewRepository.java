package com.example.study.review;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 후기 목록.
     *
     * 작성자를 함께 가져와 목록 건수만큼 조회가 늘어나지 않게 함.
     */
    /*
     * TODO 51 · 후기 규약
     *
     * 기능        모집글 식별자로 조회하며 오래된 순 · 작성자를 함께 가져옴
     *             이미 작성했는지 확인하는 것도 함께 선언
     * 활용메소드  없음 · 이름 규약으로 직접 선언 · 둘 다 필요함
     * 반환형태    List<Review> · boolean
     * 동작결과    후기 목록이 조회 하나로 나옴 · 두 번째 작성이 400 으로 막힘
     */
    @EntityGraph(attributePaths = {"writer"})
    List<Review> findByStudyPostIdOrderByCreatedAtAsc(Long studyPostId);

    // 중복 검사
    boolean existsByStudyPostIdAndWriterId(Long studyPostId, Long writerId);
}
