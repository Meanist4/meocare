# meocare

Ứng dụng backend Spring Boot cho hệ thống quản lý chăm sóc sức khỏe / hồ sơ y tế & đặt lịch (`meocare`), kết hợp quy trình CI/CD tách bạch giữa GitLab CI và Jenkins triển khai lên máy chủ Linux `mon-server`.

## Tech Stack
- **Backend:** Java, Spring Boot (Spring MVC, Spring Data JPA, Spring Security / JWT), Maven
- **Database & Storage:** MySQL, lưu trữ file/upload tách biệt ngoài file `.jar` (`/opt/meocare/uploads`)
- **CI/CD & Server:** GitLab CI (`.gitlab-ci.yml`), Jenkins (`Jenkinsfile` trên `jerkin-server`), SSH/SCP, `systemd` (`meocare.service`) trên Ubuntu Linux (`mon-server` - `192.168.11.110`)

## CI/CD Workflow
1. **GitLab CI (`.gitlab-ci.yml`):** Chạy stage `build` (`mvn clean package -DskipTests`) trên các nhánh (`main`, `develop`, `feature/*`, `ci/*`) để kiểm tra khả năng biên dịch và đóng gói, không tự động ghi đè lên máy chủ chạy.
2. **Webhook -> Jenkins (`Jenkinsfile`):** Khi có thay đổi/push trên GitLab, webhook kích hoạt pipeline trên Jenkins để build file `.jar`, đẩy qua `scp` tới `root@192.168.11.110:/opt/meocare/meocare.jar` và chạy `systemctl restart meocare`.

## Cấu trúc thư mục chính
- `meocare/`: Mã nguồn ứng dụng Spring Boot và cấu hình build Maven.
- `.gitlab-ci.yml`: Pipeline kiểm tra build phía GitLab CI.
- `Jenkinsfile`: Pipeline build & deploy tự động sang `mon-server`.

## Kiểm tra dịch vụ trên server
```bash
systemctl status meocare --no-pager -l