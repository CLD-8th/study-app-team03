/*
 * 신청 목록 구획 · 담당 4
 *
 * 모집자만 볼 수 있음. 이 구획은 자료를 직접 조회함.
 */

StudyPage.register(async function renderApplications() {
    /*
     * TODO 47 · 신청 목록 표시
     *
     * 기능        모집자가 아니면 구획을 숨김
     *             신청자 별명 · 상태 · 메시지를 그림
     *             대기 상태에만 수락과 거절 단추를 둠
     *             정원이 찼으면 더 수락할 수 없다는 안내를 아래에 둠
     * 활용메소드  StudyPage.isOwner() · StudyPage.study   제공됨
     *             api.get()                              api.js · 제공됨
     *             badge() · shortDate() · escapeHtml()   common.js · 제공됨
     *             GET /api/studies/{id}/applications     TODO 46 · 같은 담당
     * 받는자료    List<ApplicationResponse> · TODO.md 응답 형태 참고
     * 그릴위치    SC-02 · #application-panel
     *             조각은 parts.html 의 "신청 목록"
     * 동작결과    남의 글에서는 구획이 보이지 않음 · 신청이 없으면 빈 화면 문구
     */
    const study = StudyPage.study;
    const panel = document.getElementById('application-panel');
    if (!StudyPage.isOwner()) {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        return;
    }

    panel.classList.remove('hidden');
    panel.innerHTML = '<div class="card-head"><div class="card-title">신청 목록</div></div>' +
        '<div class="alert alert-error hidden" id="process-error"></div>';

    try {
        const applications = await api.get('/api/studies/' + StudyPage.id + '/applications');
        const isFull = study.acceptedCount >= study.capacity;

        panel.innerHTML = `
            <div class="card-head">
                <div class="card-title">신청 목록</div>
                <span class="card-count">${applications.length}건</span>
            </div>
            ${applications.length === 0 ? '<div class="empty">신청 내역이 없습니다</div>' :
                applications.map((application, index) => `
                    <div class="item">
                        <div>
                            <div class="item-title">${escapeHtml(application.applicantNickname)} ${badge(escapeHtml(application.status))}</div>
                            <div class="item-meta"><span>${escapeHtml(application.message)}</span></div>
                        </div>
                        ${application.status === 'PENDING' ? `
                            <div class="actions">
                                <button class="primary" data-index="${index}" data-action="accept" ${isFull ? 'disabled' : ''}>수락</button>
                                <button data-index="${index}" data-action="reject">거절</button>
                            </div>` : `<span class="item-meta">${escapeHtml(shortDate(application.createdAt))}</span>`}
                    </div>
                `).join('')}
            ${isFull ? '<div class="alert alert-error">정원이 찼습니다. 더 수락할 수 없습니다</div>' : ''}
            <div class="alert alert-error hidden" id="process-error"></div>
        `;

        panel.querySelectorAll('button[data-action]').forEach(button => {
            button.addEventListener('click', () => {
                const application = applications[Number(button.dataset.index)];
                processApplication(application.id, button.dataset.action);
            });
        });
    } catch (error) {
        showError(document.getElementById('process-error'), error);
    }
});

async function processApplication(applicationId, action) {
    /*
     * TODO 48 · 수락과 거절 처리
     *
     * 기능        수락과 거절은 주소의 끝만 다르므로 하나로 묶음
     *             성공하면 다시 그려 인원과 상태를 갱신함
     *             실패하면 사유를 안내에 표시함
     * 활용메소드  api.patch()          api.js · 제공됨
     *             StudyPage.reload()   제공됨 · 상세의 인원도 함께 바뀜
     *             showError()          common.js · 제공됨
     *             PATCH /api/applications/{id}/accept · reject   TODO 46 · 같은 담당
     * 받는자료    ApplicationResponse · 실패는 ErrorResponse
     * 그릴위치    SC-02 · #process-error
     *             조각은 parts.html 의 "실패 안내 · 문구만"
     * 동작결과    마지막 자리를 수락하면 상세의 배지가 마감으로 바뀜
     *             정원이 찬 뒤 수락하면 400 CAPACITY_EXCEEDED
     */
    const errorBox = document.getElementById('process-error');
    errorBox.textContent = '';
    errorBox.classList.add('hidden');

    try {
        await api.patch('/api/applications/' + applicationId + '/' + action);
        await StudyPage.reload();
    } catch (error) {
        showError(document.getElementById('process-error'), error);
    }
}
