package com.example.study.member;

import com.example.study.application.ApplicationService;
import com.example.study.application.dto.ApplicationResponse;
import com.example.study.member.dto.MemberRequest;
import com.example.study.member.dto.MemberResponse;
import com.example.study.study.StudyService;
import com.example.study.study.dto.StudyListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 회원 표현 계층.
 *
 * 내 자료 조회는 식별자를 받지 않고 토큰에서 확인함.
 * 받으면 남의 자료를 조회할 수 있음.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final StudyService studyService;
    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<MemberResponse> join(@Valid @RequestBody MemberRequest request) {
        MemberResponse created = memberService.join(
                request.email(), request.password(), request.nickname());

        return ResponseEntity.created(URI.create("/api/members/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public MemberResponse findOne(@PathVariable Long id) {
        return memberService.findById(id);
    }

    /*
     * TODO 65 · 내 자료 주소 셋
     *
     * 기능        GET /api/members/me · /me/studies · /me/applications 를 만듦
     *             식별자를 받지 않고 토큰에서 확인함 · 받으면 남의 자료를 볼 수 있음
     * 활용메소드  MemberService.findById()             제공됨
     *             StudyService.findMine()             TODO 63 · 같은 담당
     *             ApplicationService.findMine()       TODO 64 · 같은 담당
     * 반환형태    MemberResponse · List<StudyListResponse> · List<ApplicationResponse>
     * 동작결과    EP-15 · EP-16 · EP-17 · 토큰이 없으면 401 UNAUTHORIZED
     */

    @GetMapping("/me")
    public MemberResponse findMe(Long memberId) {
        return memberService.findById(memberId);
    }

    @GetMapping("/me/studies")
    public List<StudyListResponse> findMyStudies(Long memberId) {
        return studyService.findMine(memberId);
    }

    @GetMapping("/me/applications")
    public List<ApplicationResponse> findMyApplications(Long memberId) {
        return applicationService.findMine(memberId);
    }

}
