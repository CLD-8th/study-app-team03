/*
 * 모집글 목록 · 담당 1
 *
 * SC-01 모집글 목록 화면을 그림.
 */

let listPage = 0;

function renderList(data) {
    /*
     * TODO 13 · 목록 표시
     *
     * 기능        건수를 표시하고 목록을 그림
     *             한 건도 없으면 빈 화면 문구를 두고 쪽 이동도 비움
     *             마감된 항목은 흐리게 표시함
     * 활용메소드  badge() · shortDate() · escapeHtml()   common.js · 제공됨
     *             renderPager()                          같은 파일 · TODO 14
     * 받는자료    PageResponse<StudyListResponse> · TODO.md 응답 형태 참고
     *             content 안에 목록이 들어 있음
     * 그릴위치    SC-01 · #total 과 #list
     *             조각은 parts.html 의 "목록 항목"
     * 동작결과    제목을 누르면 /study.html?id= 로 이동
     */
    document.getElementById('total').textContent = data.totalElements +'건';
    const listEl = document.getElementById('list');
    if (data.content.length === 0) {
        listEl. innerHTML = '<div class="empty">등록된 모집글이 없습니다</div>';
        document.getElementById('pager').innerHTML ='';
        return;
    }
    listEl.innerHTML = data.content.map(post => `
        <div class="item ${post.status === 'CLOSED' ? 'closed' : ''}">
            <div>
                <div class="item-title"><a href="/study.html?id=${post.id}">${escapeHtml(post.title)}</a></div>
                <div class="item-meta">
                    <span>${escapeHtml(post.writerNickname)}</span>
                    <span>${post.acceptedCount} / ${post.capacity}명</span>
                </div>
            </div>
            <div class="item-meta">
                ${badge(post.status)}
                <span>~ ${shortDate(post.deadline)}</span>
            </div>
        </div>
    `).join('');
    renderPager(data);
}

function renderPager(data) {
    /*
     * TODO 14 · 쪽 이동 표시
     *
     * 기능        전체 쪽 수만큼 단추를 만들고 현재 쪽에 current 를 붙임
     *             단추를 누르면 listPage 를 바꾸고 다시 조회함
     * 활용메소드  loadList()   같은 파일 · TODO 15
     *             document.createElement()   요소를 만듦
     * 받는자료    PageResponse · page 와 totalPages 를 씀
     * 그릴위치    SC-01 · #pager
     *             조각은 parts.html 의 "쪽 이동"
     * 동작결과    쪽 단추를 누르면 그 쪽이 조회됨
     */
    const pagerEl =document.getElementById('pager');
    pagerEl.innerHTML = '';

    for (let i = 0; i < data.totalPages; i++) {
        const button = document.createElement('button');
        button.textContent = i + 1;

        if (i === data.page) {
            button.classList.add('current');
        }

        button.addEventListener('click', () => {
            listPage = i;
            loadList();
        });
        pagerEl.appendChild(button);
    }
}

async function loadList() {
    /*
     * TODO 15 · 목록 조회
     *
     * 기능        검색어와 상태가 비어 있으면 질의 값에서 뺌
     *             실패하면 안내를 보이고 목록과 쪽 이동을 비움
     * 활용메소드  api.get()            api.js · 제공됨
     *             renderList()         같은 파일 · TODO 13
     *             URLSearchParams()    질의 문자열을 만듦
     *             GET /api/studies     TODO 12 · 같은 담당
     * 받는자료    PageResponse<StudyListResponse>
     * 그릴위치    SC-01 · #load-error 에 실패 안내
     *             조각은 parts.html 의 "실패 안내 · 다시 시도 포함"
     * 동작결과    검색어를 넣으면 제목에 포함된 것만 나옴
     */
    const params = new URLSearchParams();
    params.set('page', listPage);

    const keyword = document.getElementById('keyword').value.trim();
    if (keyword) {
        params.set('keyword', keyword);
    }

    const status = document.getElementById('status').value;
    if (status) {
        params.set('status', status);
    }
    try {
        const data = await api.get('/api/studies?' + params.toString());

        document.getElementById('load-error').classList.add('hidden');
        renderList(data);

    } catch (e) {
        document.getElementById('load-error').classList.remove('hidden');
        document.getElementById('list').innerHTML = '';
        document.getElementById('pager').innerHTML = '';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('search').addEventListener('click', () => { listPage = 0; loadList(); });
    document.getElementById('retry').addEventListener('click', loadList);
    document.getElementById('create').addEventListener('click', () => location.href = '/form.html');

    // 등록 단추는 로그인한 경우에만 보임.
    if (auth.loggedIn) {
        document.getElementById('create').classList.remove('hidden');
    }
    loadList();
});
