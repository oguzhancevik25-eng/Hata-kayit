# Operatör Takip Android Uygulaması

Telefon üzerinde çevrimdışı çalışan operatör performans takip uygulaması.

## Dönem
01.08.2026 – 31.07.2027

## Özellikler
- Günlük kayıt: kaçan hata, yakalanan hata, KY, Kaizen, mesai, yıllık izin, günlük izin, rapor, devamsızlık
- Hata türüne göre katsayılı kalite puanı
- 100 puan üzerinden otomatik performans hesaplama
- A / B / C / D sınıfı
- Yönetici dashboard
- Personel performans kartı
- Aylık puan trendi
- Kayıt geçmişi ve kayıt silme
- İnternetsiz kullanım; veriler telefon üzerinde saklanır

## APK oluşturma
`main` dalına her güncellemede GitHub Actions otomatik olarak debug APK üretir.

GitHub'da **Actions → Android APK Build → son başarılı çalışma → Artifacts → Operator-Takip-APK** yolundan APK indirilebilir.

> İlk sürüm yerel veri saklar. Sonraki sürümlerde Excel/PDF dışa aktarma, bulut yedekleme, kullanıcı girişi ve gelişmiş grafikler eklenebilir.
