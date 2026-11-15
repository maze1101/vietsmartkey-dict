# VietSmartKey 0.56-fast – mã nguồn

## Trong gói có gì

| Đường dẫn | Nội dung |
|---|---|
| `app/src/main/java/vn/personal/vietkey/` | Mã của app (71 tệp), **đã có sẵn các sửa lỗi lag**. |
| `app/src/main/java/vn/personal/vietkey/Fast.java` | Lớp tăng tốc mới thêm. |
| `app/src/main/java/` (các thư mục khác) | Thư viện (androidx, kotlin, ML Kit…) bị dịch ngược lẫn vào. |
| `app/src/main/res`, `assets`, `AndroidManifest.xml` | Tài nguyên, từ điển, manifest. Gói đã đổi thành `vn.personal.vietkey.fast`. |
| `Fast-goc.java` | Bản `Fast.java` gốc em viết, có chú thích đầy đủ. |
| `PROMPT-CODE-ASSIST.md` | Lời nhắc dán thẳng vào trợ lý code để nó áp các sửa lỗi vào **project gốc của anh**. |

## Đọc kỹ trước khi build

Mã trong `app/` được **dịch ngược từ APK**, không phải mã gốc anh viết:
- Tên các lớp lambda bị R8 đổi thành `c0`, `v2`, `mo`, `kl`… (nằm trong thư mục `defpackage`).
- Một số hàm dài bị dịch ngược sai cấu trúc, jadx có ghi chú `JADX WARN`.
- Các thư viện bị trộn vào thay vì khai báo dependency trong Gradle.

Vì vậy project này **không build ngay được**. Có hai cách:

1. **Cách nên làm (nếu anh còn project gốc):** mở project gốc trong Android Studio, chép
   `Fast-goc.java` vào `vn/personal/vietkey/Fast.java`, rồi dán nội dung `PROMPT-CODE-ASSIST.md`
   cho trợ lý code. Các thay đổi chỉ khoảng 20 chỗ, rất gọn.
2. **Nếu mất project gốc:** dùng thư mục `app/src/main/java/vn/personal/vietkey/` làm cơ sở và
   nhờ trợ lý code làm các việc sau:
   - Xoá thư viện bị dịch ngược, khai báo lại trong `build.gradle` (androidx core/appcompat,
     ML Kit genai, kotlinx-coroutines…).
   - Thay các lớp `defpackage` bằng lambda Java bình thường.
   - Sửa các hàm có `JADX WARN`, đối chiếu với file `.smali` nếu cần.

   Việc này nhiều hơn cách 1 khá nhiều.

## Vì sao đổi tên gói

Trên máy anh, cả bản 0.56 gốc cũng không cài lại được, tức là hệ thống còn giữ "dấu vết" của gói
`vn.personal.vietkey` cũ. Đổi `applicationId` thành `vn.personal.vietkey.fast` thì Android coi
đây là một app mới, không bị dấu vết đó chặn. Khi build lại, anh có thể đặt tên khác tuỳ ý. Chỉ
cần đổi `applicationId` trong `build.gradle`, giữ nguyên `namespace "vn.personal.vietkey"` để
không phải sửa `R.` hay tên package Java.
