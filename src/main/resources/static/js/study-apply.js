/*
 * 신청 구획 · 담당 3
 *
 * StudyPage.study 와 StudyPage.myApplication 을 읽어 표시함.
 * 자료를 직접 조회하지 않음. 이미 읽어 둔 것을 씀.
 */

StudyPage.register(async function renderApply() {
    /*
     * TODO 34 · 구획 표시 조건과 신청 전 화면
     *
     * 기능        손님 · 모집자 본인 · 마감된 모집글에는 구획을 두지 않음
     *             내 신청이 없으면 메시지 입력란과 신청하기 단추를 그림
     *             신청에 성공하면 다시 그려 상태가 바뀐 화면이 나옴
     * 활용메소드  auth.loggedIn                       api.js · 제공됨
     *             StudyPage.isOwner() · StudyPage.study   제공됨
     *             StudyPage.reload()                  제공됨 · 네 구획을 다시 그림
     *             api.post() · showError()            제공됨
     *             POST /api/studies/{id}/applications   TODO 33 · 같은 담당
     * 받는자료    ApplicationResponse · TODO.md 응답 형태 참고
     * 그릴위치    SC-02 · #apply-panel
     *             조각은 parts.html 의 "신청 전"
     * 동작결과    로그아웃 상태에서 구획이 보이지 않음
     *             자기 글이면 400 SELF_APPLICATION 이 아니라 구획 자체가 없음
     */
    //
    const panel = document.querySelector("#apply-panel");

    const application = StudyPage.myApplication;
    const hasApplication = application != null && application.status !== 'REJECTED';

    if (!hasApplication) {
            if (!auth.loggedIn || StudyPage.isOwner() || StudyPage.study.status !== 'RECRUITING') {
                panel.innerHTML = '';
                return;
        }

        panel.innerHTML = '<div class="card-head"><div class="card-title">신청</div></div>\n' +
            '<div class="field"><textarea placeholder="신청 메시지"></textarea></div>\n' +
            '<div class="error hidden"></div>\n' +
            '<div class="actions"><button class="primary">신청하기</button></div>';
        panel.classList.remove('hidden');
        const messageInput = panel.querySelector('textarea');
        const errorBox = panel.querySelector('.error');
        const applyButton = panel.querySelector('.actions .primary');

        applyButton.addEventListener('click', async () => {
            try {
                await api.post(`/api/studies/${StudyPage.study.id}/applications`, {
                    message: messageInput.value
                });
                StudyPage.reload();
            } catch (error) {
                showError(errorBox, error);
            }
        });
        return;
    }


    /*
     * TODO 35 · 신청 후 화면
     *
     * 기능        내 신청이 있으면 상태와 신청일을 보임
     *             대기 상태일 때만 취소 단추를 둠
     *             취소에 성공하면 다시 그려 신청 전 화면으로 돌아감
     * 활용메소드  StudyPage.myApplication   제공됨 · 없으면 null
     *             badge() · shortDate()     common.js · 제공됨
     *             api.del()                 api.js · 제공됨
     *             DELETE /api/applications/{id}   TODO 33 · 같은 담당
     * 받는자료    ApplicationResponse · status 는 PENDING · ACCEPTED · REJECTED
     * 그릴위치    SC-02 · #apply-panel
     *             조각은 parts.html 의 "신청 후 · 대기" 와 "신청 후 · 수락됨"
     * 동작결과    대기 건은 취소 단추가 보이고 수락된 건은 보이지 않음
     */

    if (application.status === 'PENDING') {
        panel.innerHTML = '<div class="card-head"><div class="card-title">신청</div></div>\n' +
            '<div class="item">\n' +
            '<div class="item-meta">\n' +
            '<span class="badge badge-PENDING">대기</span>\n' +
            '<span>09-03에 신청함</span>\n' +
            '</div>\n' + '<button>신청 취소</button>\n' + '</div>'
        panel.classList.remove('hidden')

    } else if (application.status === 'ACCEPTED') {
        panel.innerHTML = '<div class="card-head"><div class="card-title">신청</div></div>\n' +
            '<div class="item">\n' +
            '<div class="item-meta">\n' +
            '<span class="badge badge-ACCEPTED">수락됨</span>\n' +
            '<span>09-03에 신청함</span>\n' +
            '</div>\n' +
            '</div>'
        panel.classList.remove('hidden')
    }
});
