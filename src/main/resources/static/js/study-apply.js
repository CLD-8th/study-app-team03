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
    const study = StudyPage.study;
    const panel = document.getElementById('apply-panel');
    const mine = StudyPage.myApplication;
    const hasApplication = mine != null && mine.status !== 'REJECTED';

    if (!hasApplication) {
        if (!auth.loggedIn || StudyPage.isOwner() || study.status !== 'RECRUITING') {
            panel.classList.add('hidden');
            return;
        }
        panel.classList.remove('hidden');
        panel.innerHTML =
            '<div class="card-head"><div class="card-title">신청</div></div>' +
            '<div class="field"><textarea id="message" placeholder="신청 메시지"></textarea></div>' +
            '<div class="alert alert-error hidden" id="apply-error"></div>' +
            '<div class="actions"><button class="primary" id="apply">신청하기</button></div>';

        document.getElementById('apply').addEventListener('click', async () => {
            try {
                await api.post('/api/studies/' + StudyPage.id + '/applications',
                    { message: document.getElementById('message').value.trim() });
                await StudyPage.reload();
            } catch (error) {
                showError(document.getElementById('apply-error'), error);
            }
        });
        return;
    }

    panel.classList.remove('hidden');
    const cancelable = mine.status === 'PENDING';
    panel.innerHTML =
        '<div class="card-head"><div class="card-title">신청</div></div>' +
        '<div class="item">' +
        '  <div class="item-meta">' + badge(mine.status) +
        '    <span>' + shortDate(mine.createdAt) + '에 신청함</span></div>' +
        (cancelable ? '<button id="cancel-apply">신청 취소</button>' : '') +
        '</div>' +
        '<div class="alert alert-error hidden" id="apply-error"></div>';

    if (cancelable) {
        document.getElementById('cancel-apply').addEventListener('click', async () => {
            if (!confirm('신청을 취소하시겠습니까?')) return;
            try {
                await api.del('/api/applications/' + mine.id);
                await StudyPage.reload();
            } catch (error) {
                showError(document.getElementById('apply-error'), error);
            }
        });
    }
});


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
