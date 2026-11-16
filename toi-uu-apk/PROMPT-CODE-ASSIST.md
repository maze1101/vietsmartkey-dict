# Lời nhắc cho trợ lý code (dán nguyên phần dưới đây)

---

Tôi có project Android **VietSmartKey** (bàn phím IME, package Java `vn.personal.vietkey`). Hãy áp
các thay đổi giảm lag sau. Tôi đã chép `Fast.java` vào `app/src/main/java/vn/personal/vietkey/Fast.java`.
**Không thay đổi logic gợi ý/sửa lỗi.** Mỗi hàm gốc giữ nguyên thân hàm, chỉ đổi tên thành
`xxxOrig`, rồi thêm hàm cùng tên cũ gọi sang `Fast`.

## A. Đổi tên gói cài đặt (để cài được như app mới)
Trong `app/build.gradle(.kts)`: đặt `applicationId "vn.personal.vietkey.fast"`, giữ
`namespace "vn.personal.vietkey"`. Tăng `versionCode` lên 63. Đổi `android:label` của app thành
"VietSmartKey Fast" và label của service IME thành "VietSmartKey Fast · Tiếng Việt".
Nếu manifest có `authorities` hoặc `permission` ghi cứng `vn.personal.vietkey...`, đổi sang
`${applicationId}...`.

## B. Bỏ `private` (để Fast truy cập được trong cùng package)
- `VietKeyboardService`: các field `raw`, `data`, `foreign`, `keyboard`, `learningHandler`.
- `CompositionBuffer`: các field `seed`, `keys`.
- `KeyboardView`: field `themePrefs`, các hàm `prefs()`, `dynamicColor(String)`.
- `PersonalData`: các field `words`, `generalWords`, `ids`, `next`, `phraseNext`, `personalScore`,
  `alternatives`, `frequencies`, `learned`, `strength`; các field `text/folded/keys/id/score` của
  `PersonalData.Word`; field `score` của `PersonalData.Candidate`; các hàm `known`, `expansion`,
  `weakPersonal`, `preserveCase`.
- `LearnedModel`: các field `words`, `version`; các hàm `count`, `pairCount`.
- `TypoModel`: các hàm `insert`, `delete`, `substitute`, `distance`, `toneKey`, `shapeKey`, `validSyllable`.
- `Telex`: các hàm `convert`, `tone`.

## C. Đổi tên hàm gốc thành `…Orig` và thêm hàm chuyển tiếp
| Lớp | Hàm gốc → đổi tên | Hàm mới (cùng chữ ký cũ) |
|---|---|---|
| PersonalData | `static fold(String)` → `foldOrig` | `return Fast.fold(s);` |
| PersonalData | `suggest(String,String,String,int,int)` → `suggestOrig` | `return Fast.suggest(this,a,b,c,n,m);` |
| PersonalData | `correct(String,String,String,int)` → `correctOrig` | `return Fast.correct(this,a,b,c,m);` |
| PersonalData | `nextKeyOdds(String)` → `nextKeyOddsOrig` | `return Fast.nextKeyOdds(this,s);` |
| LearnedModel | `static clean(String)` → `cleanOrig` | `return Fast.clean(s);` |
| TypoModel | `static cost(String,String,double)` → `costOrig` | `return Fast.cost(a,b,d);` |
| Telex | `static tone(String)` → `toneOrig` | `return Fast.tone(s);` |
| Telex | `static bare(char)` → `bareOrig` | `return Fast.bare(c);` |
| VietKeyboardService | `compose()` → `composeOrig` | `return Fast.compose(this);` |
| VietKeyboardService | `previousWord()` → `previousWordOrig` | `return Fast.previousWord(this);` |
| VietKeyboardService | `refreshSuggestions()` → `refreshSuggestionsOrig` | `Fast.refresh(this);` |
| VietKeyboardService | `reloadActiveMacros()` → `reloadActiveMacrosOrig` | `Fast.reloadMacros(this);` |
| KeyboardView | `themeColor(String,int)` → `themeColorOrig` | `return Fast.themeColor(this,name,def);` |

## D. Các chỗ sửa nhỏ
1. `PersonalData.loadMacros(Reader)`: thêm `Fast.macrosChanged();` ở dòng đầu và dòng cuối.
2. `KeyboardView.refresh()`: thêm `Fast.themeChanged();` ở dòng đầu và dòng cuối.
3. `KeyboardView`: thay mọi `TextUtils.ellipsize(…)` bằng `Fast.ellipsize(…)` (cùng tham số).
4. `VietKeyboardService.onUpdateSelection(...)`: thay đoạn cuối
   `refreshSuggestions(); if (keyboard != null) { keyboard.rebuild(); keyboard.invalidate(); }`
   bằng `Fast.postRefresh(this);`.
5. `VietKeyboardService`: mọi `learningHandler.postDelayed(persistLearning, 1000)` đổi thành `3000`.
6. Trong toàn bộ package `vn.personal.vietkey`, thay `x.matches(re)` bằng `Fast.matches(x, re)`,
   `x.replaceAll(re, r)` bằng `Fast.replaceAll(x, re, r)`, `x.split(re)` bằng `Fast.split(x, re)`,
   và `x.split(re, n)` bằng `Fast.split(x, re, n)`. Không sửa trong `Fast.java`.

## E. Kiểm tra
Build bản release (R8 bật). Cài lên máy, gõ thử một đoạn văn bản dài, sau đó kiểm tra:
- Gợi ý vẫn hiện bình thường.
- Gõ sai rồi nhấn cách thì từ được tự sửa.
- Nhấn xoá ngay sau dấu cách thì hoàn tác được.
- Đổi chủ đề thì màu bàn phím đổi theo.

---

## F. (Bản 0.58-fast) Đoán từ theo ngữ cảnh và học thói quen
1. Không cần sửa thêm gì cho phần đoán từ: `Fast.suggest` đã tự dùng `contextNext` (các cặp từ
   Wikipedia/Tatoeba trong từ điển mở rộng). Hằng `Fast.ctxWeight = 14`, và luôn giữ 1 ô cho từ
   phổ biến nhất theo cách xếp hạng cũ.
2. `VietKeyboardService.remember(String,String)`: đổi tên hàm gốc thành `rememberOrig`, thêm hàm
   `remember(a, b) { Fast.remember(this, a, b); }`. Bỏ `private` ở `ForeignWords.english`.
   Từ nay app chỉ tự học âm tiết tiếng Việt hợp lệ, từ đã biết, hoặc từ tiếng Anh. Chữ gõ nhầm
   không được học nữa.
