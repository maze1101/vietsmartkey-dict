# VietSmartKey – từ điển mở rộng

Gói từ điển tùy chọn cho bàn phím **VietSmartKey** (Android): địa danh, họ tên, thương hiệu và từ ghép thông dụng,
giúp sửa lỗi gõ và viết hoa khi nhập giọng nói chính xác hơn. Bàn phím vẫn chạy bình thường khi không cài gói này.

## Tệp phát hành

| Tệp | Nội dung |
|---|---|
| `phat-hanh/vi-extra.vskdict` | Gói từ điển (văn bản UTF-8 nén gzip) |
| `phat-hanh/vi-extra.vskdict.sha256` | Mã kiểm tra SHA-256, app dùng để xác nhận tệp tải về không bị hỏng |

Trong app: **Cài đặt › Chung › Từ điển mở rộng › Tải về / cập nhật**. Địa chỉ mặc định là tệp
`phat-hanh/vi-extra.vskdict` của repo này.

## Định dạng `.vskdict`

Dòng đầu: `VSKDICT<TAB>1<TAB><phiên bản><TAB><mô tả>`. Mỗi dòng sau:

- `W<TAB>từ hoặc cụm từ[<TAB>độ phổ biến]` – từ ghép, dùng cho ngữ cảnh khi sửa lỗi gõ.
- `B<TAB>cặp từ<TAB>số lần` – hai từ hay đi liền trong hội thoại (không phải từ ghép), chỉ làm ngữ cảnh phụ.
- `N<TAB>Tên riêng` – tên viết hoa khi nhập giọng nói (giữ đúng kiểu chữ: iPhone, DiVit).

App chỉ nhận âm tiết tiếng Việt hợp lệ làm từ đã biết, nên một gói sai không làm tắt chức năng sửa lỗi.

## Thêm từ và phát hành bản mới

1. Sửa các tệp trong `nguon/` (mỗi dòng một mục; `#` là chú thích):
   `dia-danh.txt`, `ho-ten.txt`, `thuong-hieu.txt` (tên riêng) và `tu-ghep.txt` (từ ghép, có thể thêm `<TAB>số`).
2. Đóng gói (cần Java 17): `java DongGoi.java dict-2`
3. Commit và đẩy lên GitHub. Bàn phím tải bản mới khi người dùng bấm **Tải về / cập nhật**.

## Nguồn và giấy phép

Gói gộp dữ liệu cộng đồng (đơn vị hành chính dvhcvn, câu Tatoeba, tần suất phụ đề OpenSubtitles qua FrequencyWords) và danh sách tự soạn;
phát hành theo **GNU GPL v3**. Chi tiết ghi nguồn: `NGUON-VA-GIAY-PHEP.md`.

Tạo lại dữ liệu cộng đồng: tải các tệp gốc rồi chạy `java TrichNguon.java <thư mục tải> <phrases.tsv>`, sau đó `java DongGoi.java dict-N`.
