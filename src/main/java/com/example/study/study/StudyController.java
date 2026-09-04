package com.example.study.study;

import com.example.study.common.PageResponse;
import com.example.study.study.dto.StudyDetailResponse;
import com.example.study.study.dto.StudyListResponse;
import com.example.study.study.dto.StudyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.function.Function;

/**
 * 모집글 표현 계층.
 *
 * 예외를 잡지 않음. 전역 처리기가 받아 같은 형태로 변환함.
 * 모집자는 요청 본문이 아니라 토큰에서 확인함.
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /*
     * TODO 12 · 모집글 목록 주소
     *
     * 기능        GET /api/studies 를 받음
     *             page · size · keyword · status 를 질의 값으로 받으며
     *             기본값은 page 0 · size 10 · 정렬은 식별자 내림차순
     *             status 는 문자로 오므로 StudyStatus 로 바꿔 넘김
     * 활용메소드  StudyService.findAll()   TODO 11 · 같은 담당
     *             PageResponse.of()        공통 · 제공됨
     *             PageRequest.of()         쪽 요청을 만듦
     *             Sort.by()                정렬을 지정
     * 반환형태    PageResponse<StudyListResponse> · TODO.md 응답 형태 참고
     * 동작결과    EP-01 · GET /api/studies?page=0&size=10 이 쪽 형태로 응답
     */
    @GetMapping
    public PageResponse<StudyListResponse> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) StudyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<StudyListResponse> result =studyService.findAll(keyword, status, pageable);
        return PageResponse.of(result, Function.identity());
    }


    /*
     * TODO 26 · 모집글 주소 다섯
     *
     * 기능        상세 · 등록 · 수정 · 삭제 · 마감 주소를 만듦
     *             작성자를 본문으로 받지 않고 @AuthenticationPrincipal 로 받음
     *             등록은 201 과 Location 머리 · 삭제는 204
     * 활용메소드  StudyService.findById()   TODO 22 · 같은 담당
     *             StudyService.create()     TODO 21 · 같은 담당
     *             StudyService.update()     TODO 23 · 같은 담당
     *             StudyService.delete()     TODO 24 · 같은 담당
     *             StudyService.close()      TODO 25 · 같은 담당
     *             ResponseEntity.created()  Location 머리를 붙임
     *             URI.create()              주소 문자열을 만듦
     * 반환형태    StudyDetailResponse · 삭제만 없음
     * 동작결과    EP-02 ~ EP-06 · 마감은 PATCH /api/studies/{id}/close
     */
    @GetMapping("/{id}")
    public StudyDetailResponse findById(@PathVariable Long id) {
        return studyService.findById(id);
    }

    @PostMapping
    public ResponseEntity<StudyDetailResponse> create(
            @Valid @RequestBody StudyRequest request,
            @AuthenticationPrincipal Long memberId) {
        StudyDetailResponse response = studyService.create(
                request.title(), request.content(), request.capacity(), request.deadline(), memberId);

        return ResponseEntity.created(URI.create("/api/studies/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public StudyDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody StudyRequest request,
            @AuthenticationPrincipal Long memberId) {
        return studyService.update(
                id, request.title(), request.content(), request.capacity(), request.deadline(), memberId
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {
        studyService.delete(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/close")
    public StudyDetailResponse close(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {
        return studyService.close(id, memberId);
    }
}
