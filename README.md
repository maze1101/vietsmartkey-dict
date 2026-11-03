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
- `N<TAB>Tên riêng` – tên viết hoa khi nhập giọng nói (giữ đúng kiểu chữ: iPhone, DiVit).

App chỉ nhận âm tiết tiếng Việt hợp lệ làm từ đã biết, nên một gói sai không làm tắt chức năng sửa lỗi.

## Thêm từ và phát hành bản mới

1. Sửa các tệp trong `nguon/` (mỗi dòng một mục; `#` là chú thích):
   `dia-danh.txt`, `ho-ten.txt`, `thuong-hieu.txt` (tên riêng) và `tu-ghep.txt` (từ ghép, có thể thêm `<TAB>số`).
2. Đóng gói (cần Java 17): `java DongGoi.java dict-2`
3. Commit và đẩy lên GitHub. Bàn phím tải bản mới khi người dùng bấm **Tải về / cập nhật**.

## Giấy phép

Danh sách do dự án tự soạn từ kiến thức chung (tên địa danh, họ tên, thương hiệu là dữ kiện công khai),
phát hành theo **CC0 1.0** (https://creativecommons.org/publicdomain/zero/1.0/). Tên thương hiệu thuộc về chủ sở hữu tương ứng.
