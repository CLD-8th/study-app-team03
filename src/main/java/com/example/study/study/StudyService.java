package com.example.study.study;

import com.example.study.application.ApplicationRepository;
import com.example.study.application.ApplicationStatus;
import com.example.study.application.dto.AcceptedCount;
import com.example.study.common.BusinessException;
import com.example.study.common.ErrorCode;
import com.example.study.member.Member;
import com.example.study.member.MemberService;
import com.example.study.study.dto.StudyDetailResponse;
import com.example.study.study.dto.StudyListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 모집글 업무 계층.
 *
 * 소유 관계와 상태 전이를 여기서 판단함.
 * 값의 형식과 범위는 요청 형태에서 이미 걸러짐.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final StudyPostRepository studyPostRepository;
    private final ApplicationRepository applicationRepository;
    private final MemberService memberService;

    @Transactional
    public StudyDetailResponse create(String title, String content, int capacity,
                                      LocalDate deadline, Long memberId) {
    /*
     * TODO 21 · 모집글 등록
     *
     * 기능        토큰에서 온 식별자로 모집자를 찾아 새 모집글을 저장함
     *             갓 만든 글이므로 수락 인원은 0
     * 활용메소드  MemberService.getMember()   제공됨
     *             StudyPostRepository.save()  제공됨
     *             StudyDetailResponse.of()    제공됨
     * 반환형태    StudyDetailResponse · TODO.md 응답 형태 참고
     * 동작결과    EP-03 · 201 과 Location 머리 · 상태는 RECRUITING
     */
        Member writer = memberService.getMember(memberId);
        StudyPost saved = studyPostRepository.save(
                new StudyPost(title, content, capacity, deadline, writer));

        return StudyDetailResponse.of(saved, 0);
    }

    /**
     * 목록 조회.
     *
     * 수락 인원을 건마다 세면 조회 구문이 건수에 비례함.
     * 식별자 묶음을 한 번에 세어 붙임.
     */
    public Page<StudyListResponse> findAll(String keyword, StudyStatus status, Pageable pageable) {
    /*
     * TODO 11 · 모집글 목록 조회
     *
     * 기능        검색어가 비어 있으면 조건에서 빼고 조회함
     *             수락 인원을 건마다 세지 않고 식별자 묶음으로 한 번에 세어 붙임
     * 활용메소드  StudyPostRepository.search()   제공됨
     *             StudyService.acceptedCounts()  같은 클래스 · 제공됨
     *             StudyListResponse.of()         제공됨
     *             Page.map()                     쪽 객체의 내용만 변환
     * 반환형태    Page<StudyListResponse>
     * 동작결과    EP-01 · 목록이 열 건이어도 조회 구문은 둘
     */
        Page<StudyPost> posts = studyPostRepository.search(keyword, status, pageable);
        List<Long> ids = posts.getContent().stream()
                .map(StudyPost::getId)
                .toList();
        Map<Long, Long> counts = acceptedCounts(ids);
        return posts.map(post -> StudyListResponse.of(post, counts.getOrDefault(post.getId(),0L)));
    }

    public StudyDetailResponse findById(Long id) {
    /*
     * TODO 22 · 모집글 상세 조회
     *
     * 기능        대상을 찾고 수락 인원을 세어 함께 담음
     * 활용메소드  StudyService.getWithWriter()   제공됨
     *             StudyService.countAccepted()   같은 클래스 · 제공됨
     *             StudyDetailResponse.of()       제공됨
     * 반환형태    StudyDetailResponse
     * 동작결과    EP-02 · 200 과 상세 · 없는 번호는 404 NOT_FOUND
     */
        StudyPost studyPost = getWithWriter(id);
        long acceptedCount = countAccepted(studyPost.getId());

        return StudyDetailResponse.of(studyPost, acceptedCount);
    }

    /**
     * 수정.
     *
     * 마감된 모집글은 수정하지 않음. 정원을 늘리면 자리가 있는데 신청이 막히고
     * 마감일을 바꿔도 상태가 그대로라 의미가 없음.
     *
     * 정원은 현재 수락 인원보다 작게 바꿀 수 없음.
     * 인원이 정원을 넘는 상태가 되며 되돌릴 방법이 없음.
     */
    @Transactional
    public StudyDetailResponse update(Long id, String title, String content, int capacity,
                                      LocalDate deadline, Long memberId) {
    /*
     * TODO 23 · 모집글 수정
     *
     * 기능        모집자 본인인지 → 모집 중인지 → 정원이 수락 인원 이상인지 순서로 판단
     *             마감된 글을 수정하면 정원과 상태가 어긋남
     *             정원을 수락 인원보다 줄이면 인원이 정원을 넘는 상태가 됨
     * 활용메소드  StudyService.getWithWriter()   제공됨
     *             StudyService.countAccepted()   같은 클래스 · 제공됨
     *             StudyPost.isWrittenBy()        엔티티 · 제공됨
     *             StudyPost.isRecruiting()       엔티티 · 제공됨
     *             StudyPost.update()             엔티티 · 제공됨
     *             BusinessException              공통 · 제공됨
     * 반환형태    StudyDetailResponse
     * 동작결과    EP-04 · 남의 글 403 FORBIDDEN · 마감된 글 400 STUDY_CLOSED
     *             정원 축소 400 CAPACITY_BELOW_ACCEPTED
     */
        StudyPost studyPost = getWithWriter(id);

        if (!studyPost.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "모집자만 수정 가능");
        }
        if (!studyPost.isRecruiting()) {
            throw new BusinessException(ErrorCode.STUDY_CLOSED, "마감된 모집글은 수정 불가");
        }

        long acceptedCount = countAccepted(studyPost.getId());
        if (capacity < acceptedCount) {
            throw new BusinessException(
                    ErrorCode.CAPACITY_BELOW_ACCEPTED,
                    "정원은 현재 수락 인원 " + acceptedCount + "명보다 작을 수 없음");
        }

        studyPost.update(title, content, capacity, deadline);
        return StudyDetailResponse.of(studyPost, acceptedCount);
    }

    @Transactional
    public void delete(Long id, Long memberId) {
    /*
     * TODO 24 · 모집글 삭제
     *
     * 기능        모집자 본인인지 확인한 뒤 지움
     * 활용메소드  StudyService.getWithWriter()   제공됨
     *             StudyPost.isWrittenBy()        엔티티 · 제공됨
     *             StudyPostRepository.delete()   제공됨
     * 반환형태    없음
     * 동작결과    EP-05 · 204 · 남의 글은 403 FORBIDDEN
     */
        StudyPost studyPost = getWithWriter(id);

        if (!studyPost.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "모집자만 삭제 가능");
        }

        studyPostRepository.delete(studyPost);
    }

    /**
     * 마감.
     *
     * 대기 상태의 신청은 그대로 둠. 모집자가 개별로 처리함.
     */
    @Transactional
    public StudyDetailResponse close(Long id, Long memberId) {
    /*
     * TODO 25 · 모집 마감
     *
     * 기능        모집자 본인인지 → 모집 중인지 확인한 뒤 상태를 마감으로 바꿈
     *             대기 상태의 신청은 그대로 둠 · 모집자가 개별로 처리함
     * 활용메소드  StudyService.getWithWriter()   제공됨
     *             StudyPost.isWrittenBy()        엔티티 · 제공됨
     *             StudyPost.isRecruiting()       엔티티 · 제공됨
     *             StudyPost.close()              엔티티 · 제공됨
     * 반환형태    StudyDetailResponse
     * 동작결과    EP-06 · 상태가 CLOSED · 이미 마감이면 400 STUDY_CLOSED
     */
        StudyPost studyPost = getWithWriter(id);

        if (!studyPost.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "모집자만 마감 가능");
        }
        if (!studyPost.isRecruiting()) {
            throw new BusinessException(ErrorCode.STUDY_CLOSED, "이미 마감된 모집글");
        }

        studyPost.close();
        return StudyDetailResponse.of(studyPost, countAccepted(studyPost.getId()));
    }

    public List<StudyListResponse> findMine(Long memberId) {
    /*
     * TODO 63 · 내 모집글 조회
     *
     * 기능        토큰에서 온 식별자로 내 모집글을 최신순으로 조회함
     *             목록과 마찬가지로 수락 인원을 한 번에 세어 붙임
     * 활용메소드  StudyPostRepository 의 내 모집글 규약   TODO 61 · 같은 담당
     *             StudyService.acceptedCounts()          같은 클래스 · 제공됨
     *             StudyListResponse.of()                 제공됨
     * 반환형태    List<StudyListResponse>
     * 동작결과    EP-16 · 내가 등록한 것만 최신순으로 나옴
     */
        throw new UnsupportedOperationException("TODO 63");
    }

    public StudyPost getWithWriter(Long id) {
        // 제공 · 담당 3 · 4 · 5 도 이 메서드를 씀.
        return studyPostRepository.findWithWriterById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "모집글 부재"));
    }

    private long countAccepted(Long studyPostId) {
        return applicationRepository.countByStudyPostIdAndStatus(studyPostId, ApplicationStatus.ACCEPTED);
    }

    private Map<Long, Long> acceptedCounts(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return applicationRepository.countAcceptedByStudyPostIds(ids, ApplicationStatus.ACCEPTED)
                .stream()
                .collect(Collectors.toMap(AcceptedCount::studyPostId, AcceptedCount::count));
    }
}
