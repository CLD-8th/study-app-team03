package com.example.study.review;

import com.example.study.application.ApplicationRepository;
import com.example.study.application.ApplicationStatus;
import com.example.study.common.BusinessException;
import com.example.study.common.ErrorCode;
import com.example.study.member.Member;
import com.example.study.member.MemberService;
import com.example.study.review.dto.ReviewResponse;
import com.example.study.study.StudyPost;
import com.example.study.study.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 후기 업무 계층.
 *
 * 참여자는 모집자와 수락된 신청자를 가리킴.
 * 모집자는 신청 절차를 거치지 않으므로 수락된 신청이 존재하지 않음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final StudyService studyService;
    private final MemberService memberService;


    public List<ReviewResponse> findByStudy(Long studyPostId) {
    /*
     * TODO 52 · 후기 목록 조회
     *
     * 기능        모집글 식별자로 오래된 순으로 조회함 · 손님도 볼 수 있음
     * 활용메소드  ReviewRepository 의 후기 규약   TODO 51 · 같은 담당
     *             ReviewResponse.from()         제공됨
     * 반환형태    List<ReviewResponse>
     * 동작결과    EP-12 · 토큰 없이도 200
     */

        List<Review> reviews = reviewRepository.findByStudyPostIdOrderByCreatedAtAsc(studyPostId);
        return reviews.stream().map(ReviewResponse::from).toList();
//        throw new UnsupportedOperationException("TODO 52");
    }

    /**
     * 후기 등록.
     * 순서는 대상 확인 · 마감 여부 · 참여 여부 · 중복임.
     */
    @Transactional
    public ReviewResponse create(Long studyPostId, String content, int rating, Long memberId) {
    /*
     * TODO 53 · 후기 등록
     *
     * 기능        대상 확인 → 마감 여부 → 참여 여부 → 중복 순서로 판단
     * 활용메소드  StudyService.getWithWriter()      제공됨
     *             StudyPost.isRecruiting()         엔티티 · 제공됨
     *             ReviewService.isParticipant()    같은 클래스 · TODO 55
     *             ReviewRepository 의 후기 규약       TODO 51 · 같은 담당
     *             MemberService.getMember()        제공됨
     * 반환형태    ReviewResponse
     * 동작결과    EP-13 · 201 · 모집 중이면 400 STUDY_NOT_CLOSED
     *             참여자가 아니면 403 · 두 번째는 400 DUPLICATE_REVIEW
     */
        //대상 확인
        StudyPost post = studyService.getWithWriter(studyPostId);
        Member member = memberService.getMember(memberId);

        // 마감 여부
        if(post.isRecruiting()){
            throw new BusinessException(ErrorCode.STUDY_NOT_CLOSED);
        }
        //참여 여부
        if(isParticipant(post, memberId)){
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 중복
        if(reviewRepository.existsByStudyPostIdAndWriterId(studyPostId, memberId)){
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        Review review = new Review(content, rating, post, member);
        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.from(savedReview);

//        throw new UnsupportedOperationException("TODO 53");
    }

    /**
     * 후기 삭제
     * 모집자에게 삭제 권한을 주지 않음.
     * 낮은 평점을 지울 수 있게 되어 후기의 의미가 사라짐.
     */
    @Transactional
    public void delete(Long reviewId, Long memberId) {
    /*
     * TODO 54 · 후기 삭제
     *
     * 기능        작성자 본인인지 확인한 뒤 지움
     *             모집자에게 삭제 권한을 주지 않음 · 낮은 평점을 지울 수 있게 됨
     * 활용메소드  ReviewRepository.findById()   제공됨
     *             Review.isWrittenBy()          엔티티 · 제공됨
     *             ReviewRepository.delete()     제공됨
     * 반환형태    없음
     * 동작결과    EP-14 · 204 · 남의 후기는 403 FORBIDDEN
     */
//        //review가 존재하는지 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        //작성자인지 확인, 모집자에게 삭제권헌 주지 않음
        if (!reviewRepository.findById(reviewId).get().isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        reviewRepository.deleteById(reviewId);

//        throw new UnsupportedOperationException("TODO 54");
    }

    private boolean isParticipant(StudyPost post, Long memberId) {
    /*
     * TODO 55 · 참여자 확인 공통
     *
     * 기능        모집자이거나 수락된 신청이 있으면 참여자임
     *             모집자는 신청 절차를 거치지 않으므로 수락된 신청이 존재하지 않음
     * 활용메소드  StudyPost.isWrittenBy()                     엔티티 · 제공됨
     *             ApplicationRepository.exists...StatusIn()   제공됨
     * 반환형태    boolean
     * 동작결과    모집자와 수락된 신청자만 후기 입력란이 보임
     */
        //작성자인지
        boolean isWriter = post.isWrittenBy(memberId);
        //수학된 신청이 있는지
        boolean isAccepted = applicationRepository.existsByStudyPostIdAndApplicantIdAndStatusIn(post.getId(), memberId, List.of(ApplicationStatus.ACCEPTED));
//        throw new UnsupportedOperationException("TODO 55");

        return isWriter || isAccepted;
    }
}
