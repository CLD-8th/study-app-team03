/*
 * 모집글 상세 구획 · 담당 2
 *
 * StudyPage.study 를 읽어 표시함. 자료를 직접 조회하지 않음.
 * 이 구획이 그려져야 담당 3 · 4 · 5 가 모집자 여부를 판단할 수 있음.
 */

function showDetailError(error) {
    const box = document.getElementById('page-error');
    const code = error.code ? '[' + error.code + '] ' : '';

    box.textContent = code + (error.message || '요청에 실패했습니다');
    box.classList.remove('hidden');
}

StudyPage.register(async function renderDetail() {
    /*
     * TODO 27 · 상세 표시
     *
     * 기능        제목 · 상태 · 모집자 · 인원 · 마감일 · 소개를 그림
     *             모집자 본인일 때만 단추를 둠
     *             마감된 모집글은 수정과 마감 단추를 숨기고 삭제만 둠
     * 활용메소드  StudyPage.study · StudyPage.isOwner()   study-page.js · 제공됨
     *             badge() · shortDate() · dateTime()      common.js · 제공됨
     *             escapeHtml()                            common.js · 제공됨
     * 받는자료    StudyDetailResponse · TODO.md 응답 형태 참고
     * 그릴위치    SC-02 · #study-detail
     *             조각은 parts.html 의 "상세 머리"
     * 동작결과    남의 글에서는 단추가 보이지 않음
     */
    const study = StudyPage.study;
    const detail = document.getElementById('study-detail');
    document.getElementById('page-error').classList.add('hidden');
    let actions = '';

    if (StudyPage.isOwner()) {
        actions =
            '<div class="actions">' +
            (study.status === 'RECRUITING' ? '<button id="edit">수정</button>' : '') +
            '<button class="danger" id="remove">삭제</button>' +
            (study.status === 'RECRUITING'
                ? '<button class="primary" id="close">모집 마감</button>'
                : '') +
            '</div>';
    }

    detail.innerHTML =
        '<div class="card-head">' +
        '<div class="card-title" style="font-size:19px;">' + escapeHtml(study.title) + '</div>' +
        badge(study.status) +
        '</div>' +
        '<div class="item-meta" style="margin-bottom:12px;">' +
        '<span>' + escapeHtml(study.writerNickname) + '</span>' +
        '<span>' + study.acceptedCount + ' / ' + study.capacity + '명</span>' +
        '<span>~ ' + escapeHtml(shortDate(study.deadline)) + '</span>' +
        '<span>' + escapeHtml(dateTime(study.createdAt)) + '</span>' +
        '</div>' +
        '<div style="font-size:13px; line-height:1.7; white-space:pre-wrap;">' +
        escapeHtml(study.content) +
        '</div>' +
        actions;
    detail.classList.remove('hidden');

    /*
     * TODO 28 · 단추 동작
     *
     * 기능        수정은 /form.html?id= 로 이동
     *             삭제와 마감은 확인을 받은 뒤 요청하며 마감 후에는 다시 그림
     *             삭제 후에는 목록으로 이동
     * 활용메소드  api.del() · api.patch()   api.js · 제공됨
     *             StudyPage.reload()        study-page.js · 제공됨 · 네 구획을 다시 그림
     *             confirm()                 확인 창
     *             DELETE · PATCH /api/studies   TODO 26 · 같은 담당
     * 받는자료    마감은 StudyDetailResponse · 삭제는 없음
     * 그릴위치    SC-02 · #edit · #remove · #close
     * 동작결과    마감을 누르면 배지가 마감으로 바뀌고 신청 구획이 사라짐
     */
    const edit = document.getElementById('edit');
    if (edit) {
        edit.addEventListener('click', () => {
            location.href = '/form.html?id=' + StudyPage.id;
        });
    }

    const remove = document.getElementById('remove');
    if (remove) {
        remove.addEventListener('click', async () => {
            if (!confirm('모집글을 삭제할까요?')) return;

            try {
                await api.del('/api/studies/' + StudyPage.id);
                location.href = '/index.html';
            } catch (error) {
                showDetailError(error);
            }
        });
    }

    const close = document.getElementById('close');
    if (close) {
        close.addEventListener('click', async () => {
            if (!confirm('모집을 마감할까요?')) return;

            try {
                await api.patch('/api/studies/' + StudyPage.id + '/close');
                await StudyPage.reload();
            } catch (error) {
                showDetailError(error);
            }
        });
    }
});
