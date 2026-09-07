package com.example.study.application;

import com.example.study.application.dto.ApplicationResponse;
import com.example.study.common.BusinessException;
import com.example.study.common.ErrorCode;
import com.example.study.member.Member;
import com.example.study.member.MemberService;
import com.example.study.study.StudyPost;
import com.example.study.study.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.study.application.ApplicationStatus.PENDING;
import static com.example.study.common.ErrorCode.SELF_APPLICATION;

/**
 * 신청 업무 계층.
 *
 * 판단 순서가 중요함. 대상 확인을 먼저 하지 않으면
 * 없는 모집글에 대해 다른 판단을 시도하게 됨.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudyService studyService;
    private final MemberService memberService;

    /**
     * 신청.
     *
     * 순서는 대상 확인 · 자기 모집글 · 상태 · 마감일 · 중복임.
     * 상태가 마감인 경우와 마감일이 지난 경우는 사유가 다르므로 나누어 판단함.
     */
    @Transactional
    public ApplicationResponse apply(Long studyPostId, String message, Long memberId) {
    /*
     * TODO 31 · 신청
     *
     * 기능        대상 확인 → 자기 모집글 → 상태 → 마감일 → 중복 순서로 판단
     *             순서가 바뀌면 없는 모집글에 다른 판단을 시도하게 됨
     *             상태가 마감인 것과 마감일이 지난 것은 사유가 다름
     *             거절된 신청도 중복으로 봄 · 재신청을 허용하지 않기로 정함
     * 활용메소드  StudyService.getWithWriter()      제공됨
     *             StudyPost.isWrittenBy()          엔티티 · 제공됨
     *             StudyPost.isRecruiting()         엔티티 · 제공됨
     *             StudyPost.isDeadlinePassed()     엔티티 · 제공됨
     *             ApplicationRepository 의 조회 규약  제공됨
     *             MemberService.getMember()        제공됨
     *             ApplicationResponse.from()       제공됨
     * 반환형태    ApplicationResponse · TODO.md 응답 형태 참고
     * 동작결과    EP-07 · 201 · 자기 글 400 SELF_APPLICATION
     *             마감 400 STUDY_CLOSED · 마감일 경과 400 DEADLINE_PASSED
     *             중복 400 DUPLICATE_APPLICATION
     */
        //대상 확인
        StudyPost post = studyService.getWithWriter(studyPostId);

        //자기 모집글
        if(post.isWrittenBy(memberId)){
            throw new BusinessException(ErrorCode.SELF_APPLICATION, "자기 모집글에는 신청 불가");
        }

        //상태
        if(!post.isRecruiting()){
            throw new BusinessException(ErrorCode.STUDY_CLOSED, "마감된 모집글");
        }

        //마감일
        if(post.isDeadlinePassed()){
            throw new BusinessException(ErrorCode.DEADLINE_PASSED, "마감일이 지남");
        }

        //중복
        if(applicationRepository.existsByStudyPostIdAndApplicantIdAndStatusIn(post.getId(), memberId, List.of(ApplicationStatus.PENDING))){
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION, "이미 신청한 모집글");
        }

        //신청자
        Member applicant = memberService.getMember(memberId);
        Application application = new Application(post, applicant, message);

        Application saved = applicationRepository.save(application);
        return ApplicationResponse.from(saved);

    }

    /**
     * 신청 취소.
     *
     * 대기 상태만 취소 가능함. 수락된 신청을 취소하면
     * 마감된 모집글에 빈자리가 생기며 되돌릴 방법이 없음.
     */
    @Transactional
    public void cancel(Long applicationId, Long memberId) {
    /*
     * TODO 32 · 신청 취소
     *
     * 기능        신청자 본인인지 → 대기 상태인지 확인한 뒤 행을 지움
     *             수락된 신청을 취소하면 마감된 글에 빈자리가 생기며 되돌릴 수 없음
     * 활용메소드  ApplicationService.getWithStudyPost()   같은 클래스 · 제공됨
     *             Application.isAppliedBy()              엔티티 · 제공됨
     *             Application.isPending()                엔티티 · 제공됨
     *             ApplicationRepository.delete()         제공됨
     * 반환형태    없음
     * 동작결과    EP-08 · 204 · 남의 신청 403 · 처리된 건 400 ALREADY_PROCESSED
     */
        Application application = getWithStudyPost(applicationId);

        if(!application.isAppliedBy(memberId)){
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if(!application.isPending()){
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED);
        }

        applicationRepository.delete(application);
    }

    public List<ApplicationResponse> findByStudy(Long studyPostId, Long memberId) {
    /*
     * TODO 42 · 신청 목록 조회
     *
     * 기능        모집자 본인인지 확인한 뒤 오래된 순으로 조회함
     * 활용메소드  StudyService.getWithWriter()        제공됨
     *             StudyPost.isWrittenBy()             엔티티 · 제공됨
     *             ApplicationRepository 의 목록 규약     TODO 41 · 같은 담당
     *             ApplicationResponse.from()          제공됨
     * 반환형태    List<ApplicationResponse>
     * 동작결과    EP-09 · 모집자는 200 · 남이면 403 FORBIDDEN
     */
        StudyPost studyPost = studyService.getWithWriter(studyPostId);

        if (!studyPost.isWrittenBy(memberId) ){
            throw new BusinessException(ErrorCode.FORBIDDEN, "권한 부재");
        }

        return applicationRepository.findWithApplicantByStudyPostIdOrderByCreatedAtAsc(studyPostId)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    public List<ApplicationResponse> findMine(Long memberId) {
    /*
     * TODO 64 · 내 신청 조회
     *
     * 기능        토큰에서 온 식별자로 내 신청을 최신순으로 조회함
     * 활용메소드  ApplicationRepository 의 내 신청 규약   TODO 62 · 같은 담당
     *             ApplicationResponse.from()           제공됨
     * 반환형태    List<ApplicationResponse>
     * 동작결과    EP-17 · 상세 화면의 신청 구획도 이 값을 씀
     */
        List<Application> applications = applicationRepository.findApplicationByApplicant(memberId);

        if(applications.isEmpty()){
            throw new BusinessException(ErrorCode.NOT_FOUND, "신청한 모집 글이 존재하지 않습니다.");
        }

        return applications.stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    /**
     * 수락.
     *
     * 마지막 자리를 채우면 모집글도 함께 마감함.
     * 별도 처리를 두지 않고 수락 시점에 판단함.
     */
    @Transactional
    public ApplicationResponse accept(Long applicationId, Long memberId) {
    /*
     * TODO 43 · 신청 수락
     *
     * 기능        처리 가능한지 확인 → 정원 여유 확인 → 수락으로 바꿈
     *             마지막 자리를 채우면 모집글도 함께 마감함
     *             별도 처리를 두지 않고 수락 시점에 판단함
     * 활용메소드  ApplicationService.processable()        같은 클래스 · TODO 45
     *             ApplicationRepository.count...Status()  제공됨
     *             Application.accept()                   엔티티 · 제공됨
     *             StudyPost.close()                      엔티티 · 제공됨
     * 반환형태    ApplicationResponse
     * 동작결과    EP-10 · 상태가 ACCEPTED · 정원이 차면 400 CAPACITY_EXCEEDED
     *             마지막 자리를 채우면 모집글 상태가 CLOSED
     */
        Application application = processable(applicationId, memberId);
        StudyPost studyPost = application.getStudyPost();
        long acceptedCount = applicationRepository.countByStudyPostIdAndStatus(
                studyPost.getId(), ApplicationStatus.ACCEPTED);

        if (acceptedCount >= studyPost.getCapacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED, "정원 초과");
        }

        application.accept();

        if (acceptedCount + 1 == studyPost.getCapacity()) {
            studyPost.close();
        }

        return ApplicationResponse.from(application);

    }

    /**
     * 거절.
     *
     * 정원을 확인하지 않음. 거절은 인원에 영향을 주지 않음.
     */
    @Transactional
    public ApplicationResponse reject(Long applicationId, Long memberId) {
    /*
     * TODO 44 · 신청 거절
     *
     * 기능        처리 가능한지 확인한 뒤 거절로 바꿈
     *             정원을 확인하지 않음 · 거절은 인원에 영향을 주지 않음
     * 활용메소드  ApplicationService.processable()   같은 클래스 · TODO 45
     *             Application.reject()              엔티티 · 제공됨
     * 반환형태    ApplicationResponse
     * 동작결과    EP-11 · 상태가 REJECTED · 처리된 건은 400 ALREADY_PROCESSED
     */
        Application application = processable(applicationId, memberId);
        application.reject();
        return ApplicationResponse.from(application);
    }

    private Application processable(Long applicationId, Long memberId) {
    /*
     * TODO 45 · 처리 가능 확인 공통
     *
     * 기능        모집자 본인인지 → 대기 상태인지 확인하고 대상을 돌려줌
     *             수락과 거절이 같은 확인을 하므로 하나로 묶음
     * 활용메소드  ApplicationService.getWithStudyPost()   같은 클래스 · 제공됨
     *             StudyPost.isWrittenBy()                엔티티 · 제공됨
     *             Application.isPending()                엔티티 · 제공됨
     * 반환형태    Application
     * 동작결과    남의 글 403 · 처리된 건 400 ALREADY_PROCESSED
     */
        Application application = getWithStudyPost(applicationId);

        if (!application.getStudyPost().isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "권한 부재");
        }

        if (!application.isPending()) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED, "이미 처리된 신청");
        }

        return application;
    }

    private Application getWithStudyPost(Long id) {
        // 제공 · 담당 4 도 이 메서드를 씀.
        return applicationRepository.findWithStudyPostById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신청 부재"));
    }
}
