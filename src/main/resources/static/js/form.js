/*
 * 모집글 등록 · 수정 · 담당 1
 *
 * SC-03 화면. 주소에 id 가 없으면 등록, 있으면 수정임.
 */

const formId = param('id');

function defaultDeadline() {
    const date = new Date();
    date.setDate(date.getDate() + 7);
    return date.toISOString().substring(0, 10);
}

async function initForm() {
    /*
     * TODO 16 · 등록 · 수정 화면 준비
     *
     * 기능        로그인하지 않았으면 로그인 화면으로 보냄
     *             id 가 있으면 제목을 수정으로 바꾸고 기존 값을 입력란에 채움
     *             id 가 없으면 마감일 기본값만 채움
     * 활용메소드  requireLogin()       common.js · 제공됨
     *             defaultDeadline()    같은 파일 · 제공됨
     *             api.get()            api.js · 제공됨
     *             GET /api/studies/{id}   TODO 26 · 담당 2
     * 받는자료    StudyDetailResponse · TODO.md 응답 형태 참고
     * 그릴위치    SC-03 · #page-title · #title · #content · #capacity · #deadline
     * 동작결과    수정으로 들어가면 기존 값이 채워져 있음
     */
    if (!requireLogin()) {
        return;
    }

    if (formId) {
        document.getElementById('page-title').textContent = '모집글 수정';

        const data = await api.get('/api/studies/' + formId);
        document.getElementById('title').value = data.title;
        document.getElementById('content').value = data.content;
        document.getElementById('capacity').value = data.capacity;
        document.getElementById('deadline').value = data.deadline;
    } else {
        document.getElementById('deadline').value = defaultDeadline();
    }
}

async function saveForm() {
    /*
     * TODO 17 · 저장
     *
     * 기능        id 가 없으면 등록, 있으면 수정으로 보냄
     *             저장에 성공하면 상세 화면으로 이동함
     *             항목별 사유가 오면 입력란 아래에 표시하고 아니면 하단에 안내함
     * 활용메소드  api.post() · api.put()   api.js · 제공됨
     *             showFieldErrors()        common.js · 제공됨 · 사유가 없으면 false
     *             showError()              common.js · 제공됨
     *             POST · PUT /api/studies   TODO 26 · 담당 2
     * 받는자료    StudyDetailResponse · 실패는 ErrorResponse
     * 그릴위치    SC-03 · #save-error 와 각 입력란의 -error 요소
     *             조각은 parts.html 의 "입력란 · 검증 실패"
     * 동작결과    빈 제목으로 저장하면 제목 아래에 사유가 표시됨
     */
    const body = {
        title: document.getElementById('title').value,
        content: document.getElementById('content').value,
        capacity: Number(document.getElementById('capacity').value),
        deadline: document.getElementById('deadline').value
    };

    try {
        const data = formId
            ? await api.put('/api/studies/' + formId, body)
            : await api.post('/api/studies', body);
        location.href = '/study.html?id=' + data.id;
    } catch (e) {
        if (!showFieldErrors(e, '')) {
            showError(document.getElementById('save-error'), e);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('save').addEventListener('click', saveForm);
    document.getElementById('cancel').addEventListener('click', () => history.back());
    initForm();
});
