# VietSmartKey 0.56 – bản vá giảm lag (0.56-fast, versionCode 63)

Bản vá cho APK VietSmartKey 0.56 (versionCode 62). Code được dịch ngược từ APK, sửa ở mức smali,
rồi đóng gói và ký lại. Giao diện, cài đặt và dữ liệu giữ nguyên. Kết quả gợi ý và sửa lỗi
**giống hệt bản gốc**; riêng một trường hợp có chủ đích được cải thiện (mục 3).

## 1. Nguyên nhân gây lag (xếp theo mức độ)

Mỗi lần chạm một phím, bàn phím chạy toàn bộ phần gợi ý và sửa lỗi ngay trên luồng giao diện. Nếu
phần này mất 50–300 ms thì phím đó bị khựng lại.

| # | Chỗ gây lag | Vì sao chậm |
|---|---|---|
| 1 | `PersonalData.suggest()` chạy **2 lần mỗi phím** (1 lần trong `key()`, 1 lần trong `onUpdateSelection()`) | Lần nào cũng quét lại toàn bộ ~9.000 từ, các cặp từ và tối đa 2.000 từ đã học |
| 2 | Vòng quét từ đã học trong `suggest()` | Với mỗi từ: `fold()` (Normalizer + **biên dịch lại regex `\p{M}`**) và `pairCount()` (2 lần Normalizer). **Càng dùng lâu, mô hình tự học càng đầy, máy càng lag** |
| 3 | `String.matches / replaceAll / split` (117 chỗ) | Mỗi lần gọi là một lần biên dịch lại regex. Có chỗ dùng regex âm tiết rất dài, gọi nhiều lần mỗi phím |
| 4 | `TypoModel.cost()` (khoảng cách gõ phím) | Gọi khoảng 6.000 lần mỗi phím. Mỗi lần tạo mảng 2 chiều bằng phản xạ (`Array.newInstance`) và gọi `Math.hypot` liên tục, nên bộ dọn rác (GC) chạy dày |
| 5 | `Telex.tone()` trong `ruleCorrection()` | Chạy khoảng 9.000 lần mỗi lần sửa lỗi; lần nào cũng dò lại 12 chuỗi nguyên âm |
| 6 | `nextKeyOdds()` lúc **chạm xuống** phím | Quét lại toàn bộ từ điển trước khi phím kịp sáng lên |
| 7 | `refreshSuggestions()` gọi `previousWord()` 2 lần | Mỗi lần là một lệnh IPC đồng bộ sang app đang gõ (Messenger, Zalo…). Nếu app đó đang bận thì bàn phím phải đợi |
| 8 | `KeyboardView.onDraw()` | Khoảng 1.000 lần đọc SharedPreferences (`themeColor`) mỗi khung hình (hiệu ứng RGB vẽ lại 25 lần/giây), và `TextUtils.ellipsize` cho mọi phím sau mỗi lần gõ |
| 9 | Lưu mô hình tự học sau 1 giây | Ghi cả mô hình (khoảng 150 KB) liên tục ngay giữa câu đang gõ |

## 2. Đã sửa gì

Toàn bộ code mới nằm trong `Fast.java`. Mỗi hàm gốc được đổi tên thành `xxxOrig` và chuyển qua
`Fast`:

| Hàm gốc | Thay bằng |
|---|---|
| `String.matches/replaceAll/split` (117 chỗ trong `vn.personal.vietkey`) | `Fast.matches/replaceAll/split`: Pattern biên dịch sẵn, kết quả giống hệt |
| `PersonalData.fold`, `LearnedModel.clean` | Nhớ kết quả (ConcurrentHashMap) |
| `TypoModel.cost` | Mảng 1 chiều dùng lại, bảng khoảng cách phím tính sẵn (giá trị double y hệt) |
| `Telex.tone`, `Telex.bare` | Bảng tra |
| `PersonalData.suggest` | Ghi nhớ kết quả theo tham số và phiên bản mô hình học; chỉ quét các từ có cùng chữ cái đầu (giữ nguyên thứ tự nên kết quả và điểm số y hệt); bỏ phần tính thừa |
| `PersonalData.nextKeyOdds` | Chỉ quét nhóm từ có cùng phím đầu, giữ thứ tự nên tổng float y hệt |
| `PersonalData.correct` | Ghi nhớ kết quả, cộng thêm cải thiện độ chính xác ở mục 3 |
| `VietKeyboardService.compose` | Ghi nhớ theo (chữ khôi phục, phím gõ, kiểu gõ, phiên bản mô hình) |
| `VietKeyboardService.refreshSuggestions / previousWord` | Chỉ 1 lệnh IPC mỗi lần làm mới, thay vì 2 |
| `onUpdateSelection` | Gộp các lần cập nhật con trỏ liên tiếp thành một lần làm mới (qua Handler) |
| `reloadActiveMacros` | Chỉ phân tích lại gõ tắt khi chuỗi gõ tắt thay đổi |
| `KeyboardView.themeColor`, `TextUtils.ellipsize` | Nhớ theo chủ đề; xoá khi `refresh()` chạy (mỗi lần mở bàn phím hoặc đổi cài đặt) |
| Lưu mô hình tự học | Lưu 3 giây sau lần gõ cuối, thay vì 1 giây. Vẫn lưu ngay khi rời ô nhập |

## 3. Độ chính xác

Ở mức tự sửa **"Vừa"** (mặc định): nếu chữ gõ ra **không phải âm tiết tiếng Việt hợp lệ** (vd
`thnh`, `chihs`, `pương`) và không phải từ đã biết, app dùng ngưỡng tin cậy của mức "Mạnh" để
quyết định có sửa hay không. Từ gõ đúng luôn là âm tiết hợp lệ, nên bước này không bao giờ sửa nhầm
một từ đúng. Mức "Nhẹ" giữ nguyên như cũ.

Kết quả đo trên 3.000 cặp từ thật (Tatoeba + Wikipedia), tự sinh lỗi gõ ngẫu nhiên:

| | Bản gốc | Bản vá |
|---|---|---|
| Từ gõ đúng vẫn giữ đúng | 100% | 100% |
| Lỗi gõ được tự sửa đúng | 61,4% | **64,7%** |
| Lỗi gõ bị sửa thành từ khác | 1,3% | 1,6% |
| Lỗi tạo ra từ vô nghĩa được tự sửa | 78,1% | **82,2%** |

Các con số chi tiết:
- Lỗi tạo ra từ vô nghĩa có từ đúng ở lựa chọn tự sửa hoặc trong 3 gợi ý: 95,2% (cả hai bản như nhau).
- Mỗi lần bị sửa sai, anh nhấn xoá ngay sau dấu cách là hoàn tác, và app sẽ nhớ để không sửa từ đó nữa.

## 4. Tốc độ (đo trên máy tính, mô hình tự học đầy 2.000 từ / 6.000 cặp, cài từ điển dict-3)

| | Bản gốc | Bản vá |
|---|---|---|
| Xử lý mỗi phím (JIT) | ~5,3 ms | ~0,9 ms |
| Xử lý mỗi phím (chế độ thông dịch, gần với máy yếu) | ~366 ms | **~29 ms** (nhanh ~12 lần) |
| Phím chậm nhất trong phiên (chế độ thông dịch) | ~1.257 ms | ~266 ms |
| Ca sửa lỗi nặng (vd `ddwowcj`, `traar`) | 12–17 ms | ~3 ms |

Trên điện thoại, số tuyệt đối sẽ lớn hơn (ART chậm hơn JIT trên máy tính), nhưng tỉ lệ cải thiện
tương tự hoặc tốt hơn, vì các phần bị loại bỏ (biên dịch regex, mảng phản xạ, cấp phát nhiều) tốn
trên ART nhiều hơn.

Kiểm tra tương đương:
- `do-kiem/Equiv.java` so sánh hàm mới với hàm gốc: 300.000 ca `cost`, 100.000 ca `fold/clean`,
  50.000 ca regex, toàn bộ 65.536 ký tự cho `tone/bare`, với **0 sai khác**.
- `do-kiem/Bench.java` ghi lại từng phím của cả phiên gõ (gợi ý, điểm số, tự sửa, xác suất phím)
  trên bản gốc và bản vá. Hai bản giống nhau, trừ vài dòng thuộc đúng trường hợp ở mục 3.

## 5. Cài đặt

APK mới được ký bằng **khoá khác** với khoá của anh, nên Android không cho cài đè:
1. Trong app: **Sao lưu** → xuất tệp sao lưu (gõ tắt, từ đã học, chủ đề…).
2. Gỡ VietSmartKey cũ, cài `VietSmartKey-0.56-fast.apk`.
3. Bật lại bàn phím trong Cài đặt Android, rồi khôi phục tệp sao lưu.
4. (Tuỳ chọn, để chạy nhanh nhất ngay từ đầu) Nếu có máy tính:
   `adb shell cmd package compile -m speed -f vn.personal.vietkey`

Muốn cập nhật đè không cần gỡ, có hai cách: áp các thay đổi vào mã nguồn của anh (mục 6) rồi ký
bằng khoá của anh, hoặc ký lại bản này bằng keystore của anh.

## 6. Áp vào mã nguồn gốc

1. Chép `Fast.java` vào package `vn.personal.vietkey`.
2. Với mỗi hàm trong bảng mục 2: đổi tên hàm gốc thành `xxxOrig` và thêm hàm cùng tên gọi
   `Fast.xxx(this, …)`. Ví dụ:
   ```java
   public List<Candidate> suggest(String a, String b, String c, int n, int m) { return Fast.suggest(this, a, b, c, n, m); }
   ```
   Hai chỗ chỉ cần thêm một dòng: gọi `Fast.macrosChanged()` ở đầu và cuối `loadMacros()`, gọi
   `Fast.themeChanged()` ở đầu và cuối `KeyboardView.refresh()`.
3. Bỏ `private` ở `VietKeyboardService.raw` và ở `CompositionBuffer.seed` / `keys`.
4. Trong `onUpdateSelection()`, thay đoạn cuối `refreshSuggestions(); keyboard.rebuild(); keyboard.invalidate();`
   bằng `Fast.postRefresh(this);`.
5. Đổi `postDelayed(persistLearning, 1000)` thành `3000`.
6. Thay `str.matches("…")` bằng `Fast.matches(str, "…")`, hoặc tốt hơn là dùng hằng `static final Pattern`.

`cong-cu/patch.py` làm đúng các bước trên trên smali; đọc file này để biết chính xác từng chỗ sửa.

## 7. Tạo lại APK từ đầu

Cần Java 17+ và Python 3. Thư viện tải từ Maven Central: smali/baksmali 2.5.2, jadx 1.5.6,
dalvik-dx 16.0.1, dex2jar 2.4.39, apksig 2.3.0, android-all (Robolectric).
```
baksmali d --di false classes.dex -o smali_nd       # tách dex gốc
cong-cu/build.sh                                     # vá smali, biên dịch Fast, ghép dex
python3 cong-cu/build_apk.py goc.apk build/classes.dex unsigned.apk 63
java ... Sign <keystore> <mật-khẩu.txt> unsigned.apk VietSmartKey-0.56-fast.apk
```
`build_apk.py` giữ nguyên các tệp khác, căn chỉnh 4 byte cho tệp không nén, và bỏ
`assets/dexopt/baseline.prof` (profile của dex cũ, checksum không còn khớp).
