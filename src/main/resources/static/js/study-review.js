/*
 * 후기 구획 · 담당 5
 *
 * 마감된 뒤에 참여자만 작성 가능하며 한 번 쓰면 입력란을 두지 않음.
 * 참여자는 모집자와 수락된 신청자를 가리킴.
 */

StudyPage.register(async function renderReviews() {
    /*
     * TODO 57 · 후기 목록과 입력란
     *
     * 기능        후기 목록을 조회해 그림 · 손님도 볼 수 있음
     *             입력란은 로그인 · 마감 · 참여자 · 미작성을 모두 만족할 때만 둠
     *             참여자는 모집자이거나 내 신청이 수락된 경우임
     *             자기 후기에만 삭제 단추를 둠
     * 활용메소드  StudyPage.isOwner() · StudyPage.myApplication   제공됨
     *             auth.memberId                                  api.js · 제공됨
     *             api.get() · dateTime() · escapeHtml()          제공됨
     *             GET /api/studies/{id}/reviews                  TODO 56 · 같은 담당
     * 받는자료    List<ReviewResponse> · writerId 로 내 후기를 가림
     * 그릴위치    SC-02 · #review-panel
     *             조각은 parts.html 의 "후기 입력" 과 "후기 항목"
     * 동작결과    모집 중에는 입력란이 없음 · 신청하지 않은 사람도 목록은 보임
     */
    const panel = document.getElementById('review-panel');
    if(!panel) return;

    //후기 목록 조회
    const reviews = await api.get('/api/studies/' + StudyPage.id + '/reviews');

    //자신이 이미 후기 작성했는지
    const hasMyReview = auth.loggedIn && reviews.some(review => review.writerId === auth.memberId);

    //참여자인지 (모집자 + 신청자)
    const isParticipant = StudyPage.isOwner() || (StudyPage.myApplication && StudyPage.myApplication.status === 'ACCEPTED');

    //모집 마감인지
    const isClosed = StudyPage.study.status === 'CLOSED';

    //후기 작성칸
    let html =
        '<div class="card">' +
        '<div class="card-head">' +
        '<div class="card-title">후기</div>' +
        '</div>';
    //로그인 + 마감 + 참여자 + 미작성
    if (auth.loggedIn && isClosed && isParticipant && !hasMyReview) {
        html +=
            '<div id="write-review" class="card" style="background:#fafbfc; margin-bottom:14px;">' +
            '<div class="field" style="display:flex; gap:10px; align-items:center;">' +
            '<label for="review-rating">평점</label>' +
            '<select id="review-rating" style="width:90px;">' +
            '<option value="5">5</option>' +
            '<option value="4">4</option>' +
            '<option value="3">3</option>' +
            '<option value="2">2</option>' +
            '<option value="1">1</option>' +
            '</select>' +
            '</div>' +
            '<div class="field">' +
            '<textarea id="review-content" placeholder="후기를 남겨 주세요"></textarea>' +
            '<div id="review-content-error" class="field-error hidden"></div>' +
            '</div>' +
            '<div id="review-error" class="alert alert-error hidden"></div>' +
            '<div class="actions">' +
            '<button id="review-submit" class="primary">등록</button>' +
            '</div>' +
            '</div>';
    }

    if (reviews.length === 0) {
        html += '<div class="empty">등록된 후기가 없습니다</div>';
    } else {
        reviews.forEach(review => {
            const stars = '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating);

            html +=
                '<div class="item">' +
                '<div>' +
                '<div class="item-title">' +
                escapeHtml(review.writerNickname || review.writerName || '') +
                ' <span style="color:#4f6ef0;">' + stars + '</span>' +
                '</div>' +
                '<div class="item-meta">' +
                '<span>' + escapeHtml(review.content) + '</span>' +
                '</div>' +
                '</div>' +
                '<div class="item-meta">' +
                '<span>' + dateTime(review.createdAt) + '</span>';

            // 작성자 본인일 때만 삭제 버튼 출력
            if (auth.loggedIn && review.writerId === auth.memberId) {
                html += '<button class="danger" data-review="' + review.id + '">삭제</button>';
            }

            html +=
                '</div>' +
                '</div>';
        });
    }

    html += '</div>'; // card 닫기
    panel.innerHTML = html;
    /*
     * TODO 58 · 후기 등록과 삭제
     *
     * 기능        평점과 내용을 보내고 성공하면 다시 그림
     *             삭제는 확인을 받은 뒤 요청함
     *             항목별 사유가 오면 입력란 아래에 표시함
     * 활용메소드  api.post() · api.del()   api.js · 제공됨
     *             StudyPage.reload()       제공됨
     *             showFieldErrors() · showError()   common.js · 제공됨
     *             POST · DELETE 후기 주소   TODO 56 · 같은 담당
     * 받는자료    ReviewResponse · 실패는 ErrorResponse
     * 그릴위치    SC-02 · #review-error · #write-review · data-review
     * 동작결과    두 번째 작성은 400 DUPLICATE_REVIEW
     *             남의 후기에는 삭제 단추가 없음
     */
    //후기 등록
    const submitButton = document.getElementById('review-submit');
    if(submitButton){
        submitButton.addEventListener('click', async () => {
            const rating = Number(
                document.getElementById('review-rating').value
            );
            const content = document.getElementById('review-content').value;

            const errorBox = document.getElementById('review-error');
            errorBox.classList.add('hidden');

            try{
                await api.post('/api/studies/' + StudyPage.id + '/reviews', {
                    rating: rating,
                    content: content
                });
                //성공하면 그림
                await StudyPage.reload();
            }catch(error){
                const handled = showFieldErrors(error, 'review-');

                if(!handled){
                    showError(errorBox, error);
                }
            }
        });
    }

    //후기 삭제
    document.querySelectorAll('[data-review]').forEach(button => {
        button.addEventListener('click', async ()=> {
            const reviewId = button.dataset.review;
            if(!confirm('후기를 삭제 하시겠습니까?')){
                return;
            }
            try{
                await api.del('/api/reviews/'+reviewId);

                //성공하면 다시 그림
                await StudyPage.reload();
            } catch (error){
                const errorBox = document.getElementById('review-error');
                if (errorBox){
                    showError(errorBox, error);
                }
            }
        });
    });
});