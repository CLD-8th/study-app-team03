package com.example.study.review;

import com.example.study.member.Member;
import com.example.study.review.dto.ReviewRequest;
import com.example.study.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;


    /*
     * TODO 56 · 후기 주소 셋
     *
     * 기능        GET /api/studies/{studyId}/reviews 는 손님도 볼 수 있음
     *             POST 는 201 · DELETE 는 204
     * 활용메소드  ReviewService.findByStudy()   TODO 52 · 같은 담당
     *             ReviewService.create()        TODO 53 · 같은 담당
     *             ReviewService.delete()        TODO 54 · 같은 담당
     * 반환형태    List<ReviewResponse> · ReviewResponse
     * 동작결과    EP-12 · EP-13 · EP-14 · 목록은 토큰 없이 200
     */
    @GetMapping("/studies/{studyId}/reviews")
    //인증 필요 없음, 손님도 조회 가능
    public ResponseEntity <List<ReviewResponse>> getReviews(@PathVariable Long studyId){
        List<ReviewResponse> reviews = reviewService.findByStudy(studyId);
        return ResponseEntity.ok(reviews);
    }
    @PostMapping("/studies/{studyId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(@PathVariable Long studyId,
                                                             @Valid @RequestBody ReviewRequest request,
                                                             @AuthenticationPrincipal Long memberId){
        ReviewResponse response = reviewService.create(studyId, request.content(), request.rating(), memberId);
        return ResponseEntity.created(URI.create("/api/studies/" + studyId + "/reviews/" + response.id())).body(response);
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        reviewService.delete(id, memberId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

}
