package com.example.study.application;

import com.example.study.application.dto.AcceptedCount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * 신청 목록.
     *
     * 신청자를 함께 가져와 목록 건수만큼 조회가 늘어나지 않게 함.
     */
    /*
     * TODO 41 · 신청 목록 규약
     *
     * 기능        모집글 식별자로 조회하며 오래된 순 · 신청자를 함께 가져옴
     * 활용메소드  없음 · 이름 규약으로 직접 선언
     * 반환형태    List<Application>
     * 동작결과    신청 목록에서 신청자 별명이 조회 하나로 나옴
     */
    @EntityGraph(attributePaths = {"applicant"})
    List<Application> findWithApplicantByStudyPostIdOrderByCreatedAtAsc(Long studyPostId);
    /**
     * 내 신청 목록.
     *
     * 제목을 표시하므로 모집글과 그 모집자를 함께 가져옴.
     */
    /*
     * TODO 62 · 내 신청 규약
     *
     * 기능        신청자 식별자로 조회하며 최신순
     *             제목을 표시하므로 모집글과 그 모집자를 함께 가져옴
     * 활용메소드  없음 · 이름 규약으로 직접 선언
     * 반환형태    List<Application>
     * 동작결과    마이페이지에서 내 신청이 최신순으로 나옴
     */

    // 제공 · 담당 3 과 담당 4 가 함께 씀.
    @EntityGraph(attributePaths = {"studyPost", "applicant"})
    Optional<Application> findWithStudyPostById(Long id);

    Optional<Application> findByStudyPostIdAndApplicantId(Long studyPostId, Long applicantId);


    long countByStudyPostIdAndStatus(Long studyPostId, ApplicationStatus status);

    boolean existsByStudyPostIdAndApplicantIdAndStatusIn(
            Long studyPostId, Long applicantId, List<ApplicationStatus> statuses);

    /**
     * 모집글 묶음의 수락 인원을 한 번에 셈.
     *
     * 목록에서 건마다 세면 조회 구문이 건수에 비례함.
     * 식별자 묶음을 넘겨 한 구문으로 처리함.
     */
    @Query("""
            select new com.example.study.application.dto.AcceptedCount(a.studyPost.id, count(a))
            from Application a
            where a.studyPost.id in :studyPostIds and a.status = :status
            group by a.studyPost.id
            """)
    List<AcceptedCount> countAcceptedByStudyPostIds(List<Long> studyPostIds, ApplicationStatus status);
}
