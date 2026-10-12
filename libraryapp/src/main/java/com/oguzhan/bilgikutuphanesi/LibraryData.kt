package com.oguzhan.bilgikutuphanesi

data class Chapter(val title:String, val body:String, val visual:String?=null)
data class Book(val id:Int, val title:String, val category:String, val subtitle:String, val chapters:List<Chapter>)

private fun c(t:String,b:String,v:String?=null)=Chapter(t,b.trimIndent(),v)
private fun b(id:Int,t:String,cat:String,sub:String,vararg ch:Chapter)=Book(id,t,cat,sub,ch.toList())

object LibraryData {
    val books = listOf(
        b(1,"Enjeksiyon Kalıplama Ustalık Rehberi","Üretim","Makineden kalıba, valf gate'ten kalite hatalarına kadar saha odaklı tam rehber",
            c("1. Enjeksiyon kalıplama nedir?","""
Enjeksiyon kalıplama; termoplastik hammaddenin kontrollü biçimde eritilip yüksek basınçla kalıp boşluğuna doldurulması, soğutulması ve parçanın kalıptan çıkarılması işlemidir. Dört ana evreyi her zaman ayrı düşün: plastikleştirme, dolum, ütüleme/holding ve soğutma.

İyi proses yalnızca parçayı doldurmak değildir. Amaç; aynı çevrimi, aynı ağırlığı, aynı görünüşü ve aynı ölçüyü tekrar tekrar üretmektir. Bu yüzden çevrim süresi, dolum zamanı, V→P geçişi, cushion, holding basıncı/süresi ve sıcaklıklar birlikte değerlendirilir.
""","injection_cycle"),
            c("2. Enjeksiyon makinesinin ana bölümleri","""
Makinenin iki temel tarafı vardır: enjeksiyon ünitesi ve kilitleme ünitesi. Enjeksiyon ünitesinde huni, vida, kovan, rezistans bölgeleri, çekvalf ve nozül bulunur. Kilitleme tarafında sabit/hareketli plaka, tie-barlar, kilitleme mekanizması ve itici sistemi vardır.

Vida dönüşünde malzeme taşınır ve erir; ileri harekette eriyik kalıba basılır. Çekvalf, enjeksiyon sırasında eriyiğin geriye kaçmasını önler. Kilitleme ünitesi ise kalıbın enjeksiyon basıncında açılmamasını sağlar.
""","machine_parts"),
            c("3. Kalıp nedir, nasıl çalışır?","""
Kalıp parçanın geometrisini oluşturan hassas bir üretim aracıdır. Sabit taraf, hareketli taraf, core/cavity, yolluk sistemi, gate, havalandırma, soğutma kanalları, itici pimler ve gerektiğinde slider/core sistemlerinden oluşur.

Kalıp problemi ile proses problemini ayırmak ustalığın temelidir. Aynı bölgede her çevrimde aynı iz görülüyorsa kalıp yüzeyi, havalandırma, gate veya soğutma; çevrimden çevrime değişiyorsa proses, malzeme veya makine tarafı daha güçlü adaydır.
""","mold_map"),
            c("4. Sıcak yolluk ve valf gate","""
Sıcak yolluk sistemi eriyiği nozül üzerinden kavitelere sıcak halde taşır. Valf gate sisteminde her gate iğnesi belirli bir zamanda açılıp kapanabilir. Böylece uzun parçaların dengeli dolması, kaynak izi ve basınç dağılımı kontrol edilir.

Valf gate ayarında yalnızca 'erken/geç aç' mantığı yetmez. Hatanın bulunduğu bölgenin hangi gate tarafından beslendiğini, akışın hangi yönden geldiğini ve bir sonraki gate ile birleşme noktasını bilmek gerekir. Beyazlık, iz, eksik dolum ve parlaklık farklarında açılma sırası/zamanı kritik olabilir.
""","valve_gate"),
            c("5. V→P geçişi","""
V→P, makinenin hız kontrollü dolumdan basınç kontrollü holding aşamasına geçtiği noktadır. Çok erken geçiş eksik dolum ve düşük ağırlığa; çok geç geçiş aşırı basınç, çapak ve gerilime yol açabilir.

Ayar yaparken tek başına pozisyona bakma. Dolum zamanı, peak basınç, cushion ve parça ağırlığıyla birlikte değerlendir. Güvenli yöntem: küçük adımlarla değiştir, birkaç çevrim stabilize et, aynı ölçüm setini karşılaştır.
""","vp"),
            c("6. Holding / ütüleme basıncı ve süresi","""
Holding, gate donana kadar parçanın çekmesini telafi eder. Basınç düşük veya süre kısa ise çökme, boşluk ve ağırlık düşüklüğü görülebilir. Gereğinden yüksek holding ise çapak, iç gerilim, itici izi ve ölçü kaçmasına neden olabilir.

Gate donma testi pratik bir yöntemdir: holding süresini kademeli artırıp parça ağırlığını izle. Ağırlık artık artmıyorsa gate büyük ölçüde donmuştur; bunun çok üzerine çıkmak çevrimi uzatır ama fayda üretmez.
""","holding"),
            c("7. Cushion (yastıklama)","""
Cushion, enjeksiyon ve holding sonunda vidanın önünde kalan eriyik rezervidir. Sıfıra yaklaşan cushion, vidanın mekanik sona dayanmasına ve holding kontrolünün zayıflamasına yol açabilir. Aşırı yüksek cushion ise gereksiz malzeme rezervi ve uzun plastikleştirme süresi yaratır.

Asıl önemli olan tek değer değil, stabilitedir. Cushion çevrimler arasında sürekli oynuyorsa çekvalf, besleme, vida geri basıncı, dozaj veya malzeme akışı araştırılmalıdır.
"""),
            c("8. Dolum hızı ve hız profili","""
Dolum hızı yüzey kalitesi, kaynak izi, gaz sıkışması ve basınç üzerinde doğrudan etkilidir. İnce ve uzun akış yollarında yavaş hız donmaya; çok yüksek hız ise yanık, jetting, çapak veya parlaklık farkına yol açabilir.

Çok kademeli hız profilinde her kademenin hangi fiziksel bölgeyi doldurduğunu anlamaya çalış. Ayar değiştirirken 'hızı artırdım' yerine 'gate sonrası ilk akışı hızlandırdım' gibi bölgesel düşün.
"""),
            c("9. Ünite sıcaklıkları ve malzeme","""
Kovan sıcaklıkları yalnızca ekranda görülen rakamlar değildir; malzemenin viskozitesini, karışımını ve termal geçmişini belirler. Düşük sıcaklık yüksek viskozite ve eksik doluma; aşırı sıcaklık bozunma, gaz, renk değişimi ve ipliklenmeye yol açabilir.

Termokupl arızası veya enerji kesintisi sonrası gerçek sıcaklığın set değerine gelmesi zaman alabilir. Üretime dönmeden önce stabilizasyon süresi ve purge kontrolü önemlidir.
"""),
            c("10. Geri basınç ve plastikleştirme","""
Geri basınç, vida geri gelirken eriyiğin homojenleşmesini artırır. Renk/masterbatch karışımı, hava kabarcığı ve dozaj stabilitesi için faydalıdır; fakat fazla olduğunda malzeme ısınır, plastikleştirme süresi uzar ve shear artar.

Plastikleştirme süresi çevrimden uzun sürüyorsa soğutma içinde tamamlanamaz ve çevrim uzar. Hedef; kaliteli eriyik üretirken plastikleştirmeyi mümkün olduğunca soğutma süresi içinde bitirmektir.
"""),
            c("11. Çapak nasıl çözülür?","""
Önce çapak bölgesini sınıflandır: parting line, slider, ejector çevresi, gate veya lokal hasar. Kalıp mekanik problemi varsa yalnızca basınç düşürmek hatayı gizler.

Proses tarafında kontrollü sıra: V→P'nin geç olup olmadığını kontrol et; holding basıncını/süresini değerlendir; dolum sonu hızını gözden geçir; kilitleme kuvveti ve kalıp kapanma durumunu doğrula. Her değişiklikte parça ağırlığı ve diğer kalite bölgelerini de izle.
""","defect_flash"),
            c("12. Eksik dolum nasıl çözülür?","""
Eksik dolumda ilk soru 'malzeme neden o bölgeye ulaşmadı?' olmalıdır. Valf gate açılmaması, V→P'nin erken olması, düşük eriyik/kalıp sıcaklığı, yetersiz hız veya dozaj, havalandırma problemi ve çekvalf kaçırması olası nedenlerdir.

Short-shot denemesiyle akış yönünü görmek çok değerlidir. Hata gate'e yakınsa gate/nozül; en uzak uçta ise basınç-sıcaklık-viskozite dengesi ön plana çıkar. Cushion ve dolum zamanı trendi de mutlaka kontrol edilmelidir.
""","defect_short"),
            c("13. Beyazlık ve gerilim izi","""
Beyazlık çoğu zaman lokal gerilim, akış birleşmesi, aşırı holding, keskin geometri veya valf gate zamanlamasıyla ilişkilidir. Hatanın tam yeri ve akış yönü bilinmeden rastgele sıcaklık veya basınç değiştirmek doğru değildir.

Gate sırası, V→P, holding ve lokal soğutmayı birlikte düşün. Parçayı ışık altında aynı açıyla kontrol et ve 'iyi-kötü sınır örneği' oluştur. Deneme matrisinde tek değişken değiştirerek hangi parametrenin bölgeyi etkilediğini bul.
""","defect_white"),
            c("14. Çökme ve boss izi","""
Çökme; kalın kesitte soğuma sırasında oluşan hacim kaybının yeterince telafi edilememesidir. Holding basıncı/süresi, gate donma zamanı, kalıp sıcaklığı ve lokal et kalınlığı temel değişkenlerdir.

Boss çevresinde çökme varsa kalıp tasarımı etkisi güçlü olabilir. Holding artırmadan önce gate'in hâlâ açık olup olmadığını kontrol et; donmuş gate sonrası basınç artırmak fayda sağlamaz.
"""),
            c("15. Yabancı madde / siyah nokta","""
Yabancı madde için kaynağı hat boyunca izole et: hammadde ambalajı → silo → kırma → kurutucu → hortum → huni → vida-kovan → sıcak yolluk. Siyah nokta termal bozunma da olabilir; özellikle uzun bekleme ve yüksek sıcaklık sonrası artıyorsa purge ve dead-spot kontrolü gerekir.

Metal riskinde kırma taraması, silo mıknatısı ve temizlik standardı önemlidir. Renk değişiminde purge miktarı ve 'ilk onay parçası' standardı oluştur.
"""),
            c("16. İtici izi, gate izi, ipliklenme","""
İtici izi; parça yeterince soğumadan itme, yüksek tutunma veya itici yüzey problemiyle oluşabilir. Gate izi; gate geometrisi, valf iğnesi zamanlaması, basınç ve kopma koşuluyla ilişkilidir. İpliklenme ise nozul/gate bölgesinde malzemenin sıcak ve akışkan kalmasıyla artabilir.

Bu üç hatada sadece tek parametreye odaklanma; parçanın kalıptan ayrılma anını ve termal durumunu gözle.
"""),
            c("17. Arıza teşhis mantığı","""
Bir arızada önce güvenlik ve makine interlocklarını kontrol et. Ejector geri görmüyor, core pulled, half nut, mold position gibi alarmlarda sensörün gerçekten konumu görüp görmediğini, mekanik hareketin tamamlanıp tamamlanmadığını ve robot sıralamasını ayır.

Alarmı silip tekrar çalıştırmak kök neden değildir. 'Hangi şart gerçekleşmediği için bir sonraki adım başlamadı?' sorusu PLC mantığına en yakın sahadaki sorudur.
"""),
            c("18. Proses optimizasyonu ve çevrim düşürme","""
Çevrim düşürmede kaliteyi riske atmadan darboğazı bul: dolum, holding, soğutma, plastikleştirme, kalıp aç-kapa veya robot. Holding süresi gate donma sonrasına taşıyorsa kısaltılabilir; plastikleştirme soğutma içinde bitiyorsa ayrıca çevrimi uzatmamalıdır.

Her kaizende önce-sonra verisi tut: çevrim saniyesi, saatlik üretim, kalite oranı ve enerji etkisi. Sadece saniye kazanımı değil sürdürülebilirlik de hedef olmalıdır.
""","cycle"),
            c("19. Günlük proses kontrol listesi","""
Vardiya başlangıcında: ünite sıcaklıkları stabil mi, kalıp suyu normal mi, ilk parça onayı var mı, valf gate/alarm durumu normal mi, cushion-dolum zamanı standart aralıkta mı, parça ağırlığı sapmış mı? Bunları kısa bir kontrol listesine bağla.

Değişiklik yapıldığında eski değer, yeni değer, sebep, sonuç ve onaylayan kişi kaydedilmelidir. 'Ayar değişti ama kim yaptı bilinmiyor' durumu proses disiplinini bozar.
"""),
            c("20. Usta gibi problem çözme","""
Problemi önce tarif et: ne, nerede, ne zaman, kaç adet, hangi makine/kalıp, hangi vardiya? Sonra son iyi parça ile ilk kötü parça arasındaki değişikliği ara. Malzeme lotu, kalıp değişimi, şart atması, sıcaklık, bakım ve operatör değişimi güçlü ipuçlarıdır.

Hipotez kur, tek değişkenle doğrula, sonucu ölç ve standarda işle. İyi problem çözücü çok ayar yapan kişi değil; en az denemeyle kök nedeni ayırabilen kişidir.
""")
        ),
        b(2,"Güçlü Diksiyon ve Net Konuşma","İletişim","Nefes, artikülasyon, vurgu, tonlama ve sunum için günlük çalışma kitabı",
            c("1. Diksiyonun temeli","""Diksiyon; kelimeleri doğru sesletmekten daha fazlasıdır. Nefes, ses, artikülasyon, vurgu, tonlama, hız ve anlam birlikte çalışır. Amaç 'güzel konuşmak' değil, dinleyenin seni zahmetsiz anlamasıdır.

Çalışmalarda sesini kaydet. Kendi kulağın konuşurken bazı hataları filtreler; kayıt dinlemek hız, yutulan hece ve gereksiz dolgu sözcüklerini fark ettirir."""),
            c("2. Diyafram nefesi","""Omuzları kaldırmadan, alt kaburgaların çevresinin genişlediği sessiz nefesi hedefle. 4 saniye al, 6-8 saniye kontrollü ver. Konuşurken nefesi cümlenin anlam grubuna göre böl.

Uzun cümleyi tek nefeste bitirmeye çalışma. Nefes bittiğinde ses sıkışır, hız artar ve kelimeler yutulur.""","breath"),
            c("3. Çene, dudak ve dil açma","""Çeneyi zorlamadan aç-kapa, dudak titreşimi ve dil ucu egzersizleri yap. 'pa-ba-ma', 'ta-da-na', 'ka-ga' dizilerini önce yavaş sonra tempolu söyle.

Amaç abartılı ağız hareketini günlük konuşmaya taşımak değil; konuşma kaslarının hareket alanını açmaktır."""),
            c("4. Ünlüler ve netlik","""A-E-I-İ-O-Ö-U-Ü seslerini açık ve dengeli üret. Kelimenin ortasındaki ünlüyü yutmak Türkçede anlaşılabilirliği hızla düşürür.

Her gün 2 dakika gazete/metin oku; ünlüleri hafif abartılı çıkar. Sonra aynı metni doğal tonda tekrar oku."""),
            c("5. Vurgu ve tonlama","""Her cümlede bir odak kelimesi seç. Aynı cümleyi farklı kelimeyi vurgulayarak söyle ve anlamın nasıl değiştiğini dinle.

Monotonluğu kırmak için ses yüksekliğini değil, perde ve ritmi çeşitlendir. Önemli noktadan önce kısa duraklama güçlü bir araçtır."""),
            c("6. Konuşma hızı","""Hızlı konuşma çoğu zaman düşüncenin öne kaçması ve nefes planının bozulmasıdır. 120-150 kelime/dakika aralığını başlangıç egzersizi olarak hedefleyebilirsin; asıl ölçü anlaşılabilirliktir.

Nokta ve virgüllerde fiziksel durak koy. Her cümleden sonra yarım saniyelik sessizlik, kontrol hissini belirgin artırır."""),
            c("7. Dolgu kelimelerini azaltma","""'Şey, yani, eee' yerine sessiz durak kullan. Sessizlik hata değildir; düşünme alanıdır. Bir konu seç, 60 saniye konuş ve kayıttaki dolgu kelimelerini say.

Bir sonraki turda sayı azaltma hedefi koy. Tamamen sıfırlamaya çalışmak konuşmayı yapaylaştırabilir."""),
            c("8. Sunum sesi","""Sunumda ilk 30 saniyeyi ezberlemek kaygıyı azaltır. İlk cümle kısa, net ve amaç odaklı olsun. Dinleyiciye bak, cümleyi bitir, sonra ekrana dön.

Vurucu sayılarda önce durakla: 'Bu iyileştirmeyle... 4 saniye kazandık.' gibi."""),
            c("9. Günlük 10 dakikalık rutin","""2 dk nefes, 2 dk artikülasyon, 2 dk tekerleme, 2 dk sesli okuma, 2 dk serbest anlatım. Haftada bir aynı metni tekrar kaydet ve gelişimi karşılaştır.

Düzenlilik, uzun ama seyrek çalışmadan daha etkilidir."""),
            c("10. 30 günlük gelişim planı","""1. hafta netlik; 2. hafta hız ve durak; 3. hafta vurgu-tonlama; 4. hafta spontane konuşma ve sunum. Her gün tek bir ölçü seç: yutulan hece, dolgu kelimesi, süre ya da anlaşılabilirlik.

Ay sonunda ilk gün kaydıyla son gün kaydını karşılaştır. Gelişim hissine değil kayda dayanır.""")
        ),
        b(3,"Kekemelikle Daha Rahat Konuşma Çalışmaları","İletişim","Akıcılığı zorlamadan, baskıyı azaltan ve konuşma güvenini destekleyen pratikler",
            c("1. Kekemeliği anlamak","""Kekemelik kişiden kişiye değişen bir konuşma akıcılığı durumudur. Tekrarlar, uzatmalar veya bloklar görülebilir; stres bunları artırabilir ama tek neden stres değildir.

Amaç 'hiç takılmamak' baskısı kurmak yerine daha rahat, kontrollü ve iletişim odaklı konuşma geliştirmektir. Belirgin günlük etkide dil ve konuşma terapisti değerlendirmesi yararlı olabilir."""),
            c("2. Konuşma baskısını azaltmak","""Cümleyi kusursuz çıkarma zorunluluğu hissi kasılmayı artırabilir. Cümleye başlamadan önce nefesi tutma; küçük, doğal bir nefes veriş üzerinde ilk kelimeyi başlat.

Takılma olduğunda kelimeyle kavga etmek yerine kısa dur, gevşe ve yeniden başla."""),
            c("3. Yumuşak başlangıç","""Sesli harfle başlayan kelimelerde sesi bir anda patlatmak yerine yumuşak başlatma egzersizi yap. 'anne', 'evet', 'olabilir' gibi kelimeleri düşük şiddetle başlatıp doğal seviyeye getir.

Bu teknik her cümlede mekanik kullanılmamalı; amaç gergin başlangıçları fark etmektir."""),
            c("4. Yavaşlatılmış konuşma","""Kelime kelime aşırı yavaşlamak yerine cümle gruplarını biraz genişlet. 'Bugün / işte yeni bir deneme yaptık / sonuç iyi çıktı.' gibi anlam grupları arasında mini durak kullan.

Hedef doğal konuşmaya dönüşebilen kontrollü ritimdir."""),
            c("5. Gönüllü kolay konuşma","""Güvenli ortamda kısa cümlelerle konuşma provası yap. Önce yalnız, sonra aileden biri, sonra telefon görüşmesi gibi zorluk basamakları oluştur.

Her basamağı 'takılmadım mı?' ile değil, 'mesajımı tamamladım mı ve gerginliğim nasıldı?' ile değerlendir."""),
            c("6. Telefon ve sunum provası","""Arama öncesi ilk iki cümleyi yaz: kim olduğunu ve neden aradığını. Sunumda giriş cümlesini prova et; ilk 20-30 saniye kontrol kazandırır.

Konuşma sırasında takılma olursa özür dilemek zorunda değilsin. Mesaja dön ve devam et."""),
            c("7. Kayıtla takip","""Haftada 3 kez 2 dakikalık kayıt al. Konu aynı olabilir. Takılma sayısından çok konuşma gerginliği, hız, nefes tutma ve kaçınma davranışlarını gözle.

Gelişim bazen akıcılık artışı, bazen de takılma olsa bile konuşmaya devam edebilme şeklinde görülür."""),
            c("8. 6 haftalık plan","""1. hafta farkındalık ve nefes; 2. hafta yumuşak başlangıç; 3. hafta ritim/durak; 4. hafta telefon; 5. hafta sunum; 6. hafta gerçek hayat genellemesi.

Zorlanma artarsa egzersizi daha kolay basamağa indir. Profesyonel terapi alıyorsan bu uygulamayı terapistin planına yardımcı çalışma olarak kullan.""")
        ),
        b(4,"Tekerleme ve Artikülasyon Kitabı","İletişim","Dudak, dil, çene ve hız kontrolü için seviyeli 60+ çalışma",
            c("1. Isınma tekerlemeleri","""Paket paket papatya, patika boyunca parladı.
Baba bir bakır bakraç buldu, bakracı bırakıp bahçeye baktı.
Mini minnacık martılar mavi mavilikte dönüp durdu.

Önce her cümleyi 3 kez yavaş, sonra 3 kez normal, en son 3 kez tempolu söyle. Hız arttığında hece yutuyorsan bir önceki tempoya dön."""),
            c("2. P-B-M sesleri","""Bir berber bir berbere, bre berber gel beraber bir berber dükkânı açalım demiş.
Pembe panjurlu pencerede beş minik papatya bekliyordu.
Mavi minderin yanında minik bir mandal duruyordu."""),
            c("3. T-D-N sesleri","""Dal sarkar kartal kalkar, kartal kalkar dal sarkar.
Taze tarhana tencerede taşmadan tatlandı.
Narin Nermin nane demetlerini nazikçe dizdi."""),
            c("4. K-G sesleri","""Kırk küp, kırkının da kulpu kırık kara küp.
Gökten gelen gri güvercin gölgeliğe kondu.
Koca kayık kıyıya kadar kaydı."""),
            c("5. R-L sesleri","""Şu köşe yaz köşesi, şu köşe kış köşesi, ortada su şişesi.
Lale ile Rıza, renkli ruloları raflara yerleştirdi.
Rüzgâr rayların arasında ağır ağır uğuldadı."""),
            c("6. S-Ş-Z sesleri","""Siz de bizim gibi sizin bizim gibi olduğunuzu söyleyebilir misiniz?
Şemsipaşa Pasajı'nda sesi büzüşesiceler.
Zeki Ziya, uzun zamandır zeytinlikte sessizce geziyordu."""),
            c("7. Ç-C-J sesleri","""Çatalca'da topal çoban, çatal yapıp çatal satar.
Cemil, camcıdan ince cam çerçeve aldı.
Jale, jilet gibi düzgün cümlelerle jüriye cevap verdi."""),
            c("8. Karma ileri seviye","""Bir pirinç tarlasının ortasında kırmızı kırkayak kırk küçük kıvrım çizdi.
Rüzgârlı rampada renkli rulmanları sıralayan Rıza, yanlış sırayı yeniden düzenledi.
Şaşkın şoför, sisli sokakta sessiz sinyali seçemedi."""),
            c("9. Hız merdiveni","""Bir tekerlemeyi metronom olmadan 20 saniyede oku. Sonra 18, 16 ve 14 saniyeyi dene. Netlik bozulduğu basamak senin o günkü sınırındır.

Amaç en hızlı olmak değil; hız artarken artikülasyonu korumaktır."""),
            c("10. 30 günlük tekerleme planı","""Her gün 3 tekerleme seç: biri kolay, biri hedef sese yönelik, biri ileri seviye. Her birini 3 yavaş + 3 normal + 1 kayıt turu yap.

Hafta sonunda aynı tekerlemeyi yeniden kaydet ve kelime yutma, nefes, ritim açısından karşılaştır.""")
        ),
        b(5,"İnsan Gelişimi: Kendini Yönetme","Kişisel Gelişim","Disiplin, odak, özgüven, öğrenme ve sürdürülebilir gelişim sistemi",
            c("1. Gelişim sistemi kurmak","""Motivasyon dalgalanır; sistem kalır. Büyük hedefi günlük tekrarlanabilir davranışa çevir: 'daha iyi konuşacağım' yerine 'her gün 10 dakika sesli okuma yapacağım'.

Takip edemediğin hedef yönetilemez. Basit bir takvimde yaptığın günleri işaretlemek bile davranış sürekliliğini güçlendirir."""),
            c("2. Kimlik temelli alışkanlık","""Hedef yalnızca sonuç değildir. 'Kitap okuyacağım' yerine 'her gün öğrenen biriyim' gibi kimlik cümlesi davranış seçimini kolaylaştırır.

Kimliği kanıtlayan küçük davranışlar birikir. Beş dakika çalışma bile zinciri koparmamaktan daha değerlidir."""),
            c("3. Odak ve dikkat","""Telefon bildirimleri ve sürekli görev değiştirme derin çalışmayı böler. 25-45 dakikalık tek görev blokları oluştur; blok içinde tek hedef belirle.

Başlamayı zorlaştıran işleri küçült: 'QCC hazırla' yerine 'problem tanımını 10 dakikada yaz'."""),
            c("4. Özgüven nasıl oluşur?","""Özgüven yalnızca olumlu düşünce değildir; tekrar tekrar kanıtlanan yeterlilik hissidir. Küçük ama gerçek başarıları kaydet.

Kaçındığın bir konuşmayı yapmak, bir sunumu bitirmek veya bir hatayı kök nedenine indirmek beynine 'yapabiliyorum' kanıtı verir."""),
            c("5. Hata ile ilişki","""Hata kimlik değil veridir. 'Ben beceremedim' yerine 'hangi adım çalışmadı?' sorusu öğrenmeyi açar.

Bir başarısızlıktan sonra üç satır yaz: ne oldu, neden oldu, bir sonraki denemede neyi değiştireceğim."""),
            c("6. Öğrenmeyi hızlandırmak","""Pasif okumadan çok aktif geri çağırma kullan. Bölümü kapat ve ana fikirleri ezberden anlat. Sonra eksikleri kontrol et.

Bir bilgiyi başka birine anlatabiliyorsan büyük ölçüde anlamışsındır. Teknik konularda çizim ve neden-sonuç şeması öğrenmeyi güçlendirir."""),
            c("7. Enerji yönetimi","""Verimlilik sadece zaman değildir. Uykusuzluk, düzensiz yemek ve sürekli stres bilişsel performansı düşürür. Zor işi enerjinin yüksek olduğu saate koy.

Mola, üretkenliğin karşıtı değildir; doğru kullanılırsa performans döngüsünün parçasıdır."""),
            c("8. İletişim ve sınırlar","""Net sınır saldırganlık değildir. İstekleri 'durum + etki + talep' biçiminde söyle: 'Şartlar haber verilmeden değişince takip zorlaşıyor; değişiklik olduğunda gruba yazalım.'

Kişiye değil davranışa odaklanmak çatışmayı azaltır."""),
            c("9. 90 günlük gelişim planı","""Aynı anda 10 hedef yerine 3 ana alan seç: mesleki beceri, iletişim, sağlık gibi. Her alan için haftalık ölçü belirle.

Her pazar 15 dakika gözden geçir: ne ilerledi, ne tıkandı, gelecek hafta tek kritik adım ne?"""),
            c("10. Ustalık zihniyeti","""Ustalık hızlı olmak değil, ayrıntıyı görme ve doğru geri bildirim döngüsü kurma becerisidir. Temeli tekrar etmekten sıkılmamak gerekir.

Bir alanda iyi oldukça daha zor problemler görmeye başlarsın; bu gerileme değil, görüş alanının genişlemesidir.""")
        ),
        b(6,"Liderlik ve Ekip Yönetimi","İş Hayatı","Takım yönetimi, geri bildirim, vardiya iletişimi ve güven",
            c("1. Liderin işi","""Liderin görevi her işi kendi yapmak değil, doğru standardı kurup ekibin işini doğru yapabilmesini sağlamaktır. Net beklenti, görünür takip ve adil geri bildirim temel üçlüdür."""),
            c("2. Görev dağılımı","""Görev verirken ne yapılacağını, kalite kriterini, süreyi ve geri dönüş noktasını açıkla. 'Buna bak' yerine 'ilk 20 parçayı şu üç bölgeden kontrol et ve sonucu 10 dakikada bildir' daha nettir."""),
            c("3. Geri bildirim","""Kişiliği değil davranışı konuş: gözlem → etki → beklenen standart. İyi davranışı da somut şekilde belirt; böylece neyin tekrarlanacağı anlaşılır."""),
            c("4. Hata sonrası yaklaşım","""Hata sonrası ilk hedef suçlu bulmak değil kaçağın neden sistem tarafından tutulmadığını anlamaktır. İnsan hatasını süreç, eğitim, görsel standart ve poka-yoke ile nasıl zorlaştırabileceğini araştır."""),
            c("5. Vardiya devir teslim","""Devirde makine, kalıp, kalite riski, açık aksiyon, ayar değişikliği ve malzeme değişimini standart formatta aktar. Sözlü bilgi tek başına kaybolur; kısa yazılı kayıt bırak.""")
        ),
        b(7,"Problem Çözme ve Kök Neden","İş Hayatı","5 Neden, 6M, Pareto ve doğrulama mantığı",
            c("1. Problemi doğru tanımla","""İyi problem cümlesi yorum değil ölçü içerir: nerede, ne zaman, ne kadar, hangi ürün/makine? 'Kalite kötü' yerine '1600T-2'de 290 FR parçada uç bölgede 35/500 beyazlık' gibi."""),
            c("2. Pareto","""Hataları adet veya maliyetle büyükten küçüğe sırala. İlk birkaç neden toplam etkinin çoğunu oluşturuyorsa enerji önce oraya verilmelidir. Pareto öncelik aracıdır, kök neden değildir."""),
            c("3. 6M balık kılçığı","""Man, Machine, Material, Method, Measurement, Environment başlıklarında olası nedenleri topla. Her madde hipotezdir; veriyle doğrulanmadan kök neden sayılmaz."""),
            c("4. 5 Neden","""Her 'neden'in önceki cevaba mantıksal bağlanmasına dikkat et. 'Operatör dikkat etmedi' deyip durma; neden fark etmedi, standart var mıydı, kontrol mümkün müydü diye devam et."""),
            c("5. Doğrulama","""Kök nedeni doğrulamak için değişkeni kontrollü biçimde değiştir ve sonuç tekrar ediyor mu bak. Karşı önlem sonrası belirli süre/lot/vardiya boyunca veri topla.""")
        ),
        b(8,"Kaizen: Sürekli İyileştirme","İş Hayatı","Küçük iyileştirmeden ödüllük projeye kadar uygulama rehberi",
            c("1. Kaizen nedir?","""Kaizen, işi her gün daha güvenli, kaliteli, hızlı veya kolay hale getiren sistematik iyileştirmedir. Büyük yatırım şart değildir; fakat etki mutlaka ölçülmelidir."""),
            c("2. İsrafı gör","""Bekleme, taşıma, hareket, stok, hata, fazla işlem, fazla üretim ve kullanılmayan insan potansiyelini gözle. En iyi kaizen çoğu zaman her gün tekrar edilen küçük bir zorluğu ortadan kaldırır."""),
            c("3. Önce-sonra","""Fotoğraf, süre, mesafe, hata adedi veya enerji ile mevcut durumu ölç. Sonra aynı ölçüyü karşı önlem sonrası tekrar et. 'Daha iyi oldu' yerine sayısal sonuç göster."""),
            c("4. Standartlaştır","""İyileştirme kişiye bağlı kalmamalı. Yer, renk, kontrol sırası, talimat veya dijital form ile yeni yöntemi standarda bağla."""),
            c("5. Ödüllük kaizen","""Büyük etki için kalite + maliyet + güvenlik + sürdürülebilirlik birlikte düşün. Sensör, görüntü işleme, otomatik ayrıştırma ve enerji optimizasyonu gibi teknoloji destekli çözümler güçlü adaylardır.""")
        ),
        b(9,"Kalite Kontrol ve Hata Önleme","Üretim","Kontrol planı, görsel standart, poka-yoke ve hata kaçırmama",
            c("1. Kaliteyi kaynaktan kur","""Son kontrolde hata ayıklamak yerine hatanın oluşmasını veya bir sonraki prosese geçmesini engelle. Kaynakta kalite mantığı bunun temelidir."""),
            c("2. Görsel standart","""İyi, sınır ve kötü örnekleri aynı ışık/açıyla göster. 'Beyazlık olmasın' gibi soyut talimat yerine fotoğraflı limit standardı operatör kararını güçlendirir."""),
            c("3. Poka-yoke","""Yanlış parçayı, eksik felt'i, ters montajı veya hatalı setlemeyi fiziksel/sensör/yazılım yöntemiyle imkânsız ya da çok zor hale getir."""),
            c("4. Kontrol sıklığı","""Risk yüksekse sadece vardiya başı kontrol yetmez. İlk parça, değişim sonrası, belirli adet aralığı ve alarm sonrası gibi tetikleyici kontroller oluştur."""),
            c("5. Hata kaçırma analizi","""Hata neden oluştu ve neden yakalanmadı sorularını ayrı analiz et. Üretim kök nedeni ile kaçak kök nedeni çoğu zaman farklıdır.""")
        ),
        b(10,"Zaman Yönetimi ve Disiplin","Kişisel Gelişim","Vardiya, aile ve kişisel hedefleri dengeli yönetme",
            c("1. Öncelik","""Günün üç kritik işini belirle. Acil ama önemsiz işler tüm günü ele geçirmeden önce yüksek etkili işi takvime koy."""),
            c("2. Zaman blokları","""Benzer işleri grupla. Mesajları sürekli kontrol etmek yerine belirli aralıkta toplu bakmak dikkat kaybını azaltır."""),
            c("3. Erteleme","""Başlama eşiğini küçült: 30 sayfa yerine 5 dakika oku. Çoğu zaman hareket başladıktan sonra devam etmek kolaylaşır."""),
            c("4. Vardiya rutini","""Vardiya başı kontrol, orta vardiya gözden geçirme ve devir teslim için sabit mini rutin oluştur. Tekrarlanan kararları standarda bağlamak zihinsel yükü azaltır."""),
            c("5. Haftalık plan","""Haftada bir 20 dakika ayır: açık işler, aile planı, öğrenme hedefi, finans ve dinlenme. Takvimde boşluk da planlanmalıdır.""")
        ),
        b(11,"Etkili Sunum ve Topluluk Önünde Konuşma","İletişim","QCC, toplantı ve iş sunumlarında güvenli anlatım",
            c("1. Sunum iskeleti","""Problem → veri → analiz → karşı önlem → sonuç → standart. Dinleyici nereye gittiğini bilirse detayları daha kolay takip eder."""),
            c("2. İlk 30 saniye","""İlk cümleyi kısa ve net hazırla. Konu, neden önemli ve bugün ne göstereceğini söyle. Giriş ezberi başlangıç stresini azaltabilir."""),
            c("3. Slayt sadeleştirme","""Bir slayta tek ana mesaj koy. Uzun paragraf yerine büyük sayı, kısa başlık ve görsel kullan; anlatımı sen yap."""),
            c("4. Soru cevap","""Soruyu sonuna kadar dinle. Bilmiyorsan tahmin üretmek yerine 'veriyi kontrol edip netleştireceğim' de. Savunmaya geçmeden kanıta dön."""),
            c("5. Prova","""Sunumu sesli prova et ve süre tut. Slayt geçişlerini de prova et; asıl takılmalar çoğu zaman içerikten değil geçişten çıkar.""")
        ),
        b(12,"Beden Dili ve Etkili İletişim","İletişim","Duruş, göz teması, dinleme ve zor konuşmalar",
            c("1. Duruş","""Ayaklar dengeli, omuzlar rahat, çene nötr. Aşırı kasılmış 'özgüven pozu' yerine rahat ve açık duruş hedefle."""),
            c("2. Göz teması","""Tek kişiye kilitlenme. Küçük grupta cümle başına farklı kişiye bak; büyük grupta salonun bölümlerini sırayla tara."""),
            c("3. Aktif dinleme","""Cevap vermek için değil anlamak için dinle. Karşı tarafın ana fikrini kısa tekrar etmek yanlış anlaşılmayı azaltır."""),
            c("4. Zor konuşma","""Sorunu kişilikten ayır. Somut olay, etkisi ve istenen davranışı söyle. Ses tonunu düşük ve cümleleri kısa tut."""),
            c("5. İkna","""İkna baskı değildir. Karşı tarafın hedefini anla ve önerinin onun hedefiyle bağlantısını açıkla. Veri + örnek + net talep kullan.""")
        ),
        b(13,"Hafıza ve Hızlı Öğrenme","Kişisel Gelişim","Teknik bilgiyi daha kalıcı öğrenme yöntemleri",
            c("1. Aktif hatırlama","""Metni tekrar tekrar okumak yerine kapatıp anlat. Hatırlamak için çaba göstermek öğrenmeyi güçlendirir."""),
            c("2. Aralıklı tekrar","""Aynı bilgiyi 1 gün, 3 gün, 1 hafta ve 1 ay sonra kısa tekrarlarla çağır. Unutmaya yaklaşınca yapılan tekrar etkilidir."""),
            c("3. Parçalama","""Karmaşık konuyu kümelere ayır: enjeksiyonda dolum, V→P, holding, soğutma gibi. Her kümenin neden-sonuç ilişkisini kur."""),
            c("4. Öğreterek öğren","""Konuyu hiç bilmeyen birine anlatır gibi basitleştir. Takıldığın yer gerçek bilgi boşluğunu gösterir."""),
            c("5. Not sistemi","""Notu kopya değil soru-cevap şeklinde yaz. 'Holding artarsa ne olur?' gibi sorular sonraki tekrarı aktif hale getirir.""")
        ),
        b(14,"Stres Yönetimi ve Soğukkanlılık","Kişisel Gelişim","Yoğun iş ve sunum anlarında zihinsel kontrol",
            c("1. Stres sinyalleri","""Nefes hızlanması, çene sıkma, omuz yükselmesi ve düşüncenin daralması erken sinyallerdir. Sinyali fark etmek müdahalenin ilk adımıdır."""),
            c("2. Nefes ve tempo","""Uzun ve rahat veriş, konuşma öncesi tempoyu düşürebilir. Amaç stresi sıfırlamak değil, bedenin aşırı hızlanmasını azaltmaktır."""),
            c("3. Kontrol alanı","""Kontrol edebildiğin şeyleri ayır: hazırlık, veri, prova, davranış. Başkalarının ilk tepkisi kontrolünde değildir."""),
            c("4. Olay sonrası değerlendirme","""Yoğun olaydan sonra hemen kendini yargılama. Veri topla: ne oldu, ne işe yaradı, bir sonraki sefer ne değişecek?"""),
            c("5. Kronik yük","""Uzun süren uyku bozukluğu, yoğun kaygı veya günlük işlev kaybı varsa profesyonel destek düşün. Kendi kendine teknikler destekleyicidir, her durumu tek başına çözmeyebilir.""")
        ),
        b(15,"Karar Verme ve Analitik Düşünme","Kişisel Gelişim","Veriyle düşünme, seçenek kıyaslama ve risk yönetimi",
            c("1. Soruyu netleştir","""Karar vermeden önce kararın ne olduğunu tek cümle yaz. Belirsiz soruya net cevap çıkmaz."""),
            c("2. Kriter ağırlığı","""Fiyat, kalite, zaman, risk gibi kriterleri belirle ve önem ağırlığı ver. Seçenekleri aynı ölçekte kıyasla."""),
            c("3. Olasılık düşüncesi","""Kesinlik aramak yerine olasılık düşün. 'Olur mu?' yerine 'ne kadar olası ve olursa etkisi ne?' sor."""),
            c("4. Tersine düşünme","""Başarıyı değil başarısızlığı hayal et: Bu karar neden kötü sonuçlanmış olabilir? Böylece kör noktalar görünür."""),
            c("5. Karar kaydı","""Önemli kararda o gün bildiklerini ve varsayımlarını yaz. Sonuç geldiğinde karar kalitesi ile şansı ayırmak kolaylaşır.""")
        ),
        b(16,"Finansal Okuryazarlık Temelleri","Yaşam","Bütçe, borç, acil durum ve yatırım mantığının temelleri",
            c("1. Nakit akışı","""Gelir ve gideri aylık izle. 'Ay sonunda para kalıyor mu?' yerine hangi kategori ne kadar tüketiyor gör."""),
            c("2. Acil durum fonu","""Beklenmeyen giderler için likit ve düşük riskli tampon oluşturmak, yüksek faizli borca mecbur kalma riskini azaltır."""),
            c("3. Borç maliyeti","""Sadece taksite değil toplam geri ödemeye bak. Faiz, masraf ve vade birlikte gerçek maliyeti belirler."""),
            c("4. Risk ve getiri","""Yüksek getiri iddiası genellikle yüksek risk taşır. Anlamadığın ürüne, sadece vaat edilen kazanca bakarak para koyma."""),
            c("5. Dolandırıcılık işaretleri","""Garantili yüksek getiri, acele yatır baskısı, para çekmede bahane ve lisans bilgisinin belirsizliği önemli alarm işaretleridir.""")
        ),
        b(17,"Girişimcilik ve İş Kurma","İş Hayatı","Fikirden müşteriye ve kârlılığa pratik iş kurma",
            c("1. Problem seç","""İş fikrinden önce müşterinin çözülecek problemini tanımla. İnsanlar ürün değil sonuç satın alır."""),
            c("2. Küçük test","""Büyük yatırım öncesi küçük pilot yap. Gerçek müşteri para ödemeye hazır mı, bunu en erken aşamada test et."""),
            c("3. Birim ekonomi","""Satış fiyatı - değişken maliyet = katkı payı. Sabit giderleri karşılamak için kaç satış gerektiğini hesapla."""),
            c("4. Operasyon","""Sipariş, stok, kalite, iade ve müşteri mesajları için basit standartlar kur. İş büyürken kaosun büyümemesi için sistem gerekir."""),
            c("5. Nakit ve büyüme","""Kârlı olmak ile kasada para olması aynı değildir. Stok ve vadeli satış nakdi kilitleyebilir; nakit akışını ayrıca takip et.""")
        ),
        b(18,"Çocuklarla Etkili İletişim","Aile","Duygu, sınır, oyun ve günlük iletişim için pratik yaklaşım",
            c("1. Yaşa uygun beklenti","""Küçük çocukların dürtü kontrolü yetişkin gibi değildir. Talimatı kısa, tek adımlı ve somut ver."""),
            c("2. Duyguyu adlandır","""Davranışı onaylamadan duyguyu tanıyabilirsin: 'Kızdığını görüyorum, oyuncağı atamayız.' Bu hem sınır hem bağlantı kurar."""),
            c("3. Seçenek sun","""Kontrol ihtiyacını güvenli seçenekle karşıla: 'Mavi tişört mü kırmızı mı?' Sınırsız seçenek karar yükü yaratır."""),
            c("4. Oyunla öğrenme","""Temizlik, sıra bekleme ve konuşma gibi beceriler oyun içinde daha kolay öğretilebilir. Kısa ve tekrarlı oyunlar kullan."""),
            c("5. Övgü","""Sadece 'aferin' yerine davranışı söyle: 'Oyuncaklarını sepete koydun, işini tamamladın.' Böylece çocuk neyi başardığını anlar.""")
        ),
        b(19,"Sağlıklı Alışkanlıklar ve Günlük Rutin","Yaşam","Uyku, hareket, beslenme ve enerji yönetiminin pratik temelleri",
            c("1. Uyku rutini","""Benzer saatte yatıp kalkmak beden ritmini destekler. Gece ekran ve yoğun uyarıcıları azaltmak uykuya geçişi kolaylaştırabilir."""),
            c("2. Günlük hareket","""Uzun süre oturmayı kısa yürüyüşlerle böl. Sürdürülebilir hareket planı, çok ağır başlayıp bırakmaktan daha değerlidir."""),
            c("3. Beslenme düzeni","""Tek bir 'mucize' gıda yerine dengeli öğün ve düzenli su tüketimine odaklan. Özel sağlık durumunda kişisel öneri için sağlık profesyoneline danış."""),
            c("4. Sigara bırakma hazırlığı","""Tetikleyicileri ve otomatik sigara anlarını yazmak bırakma planı için veri sağlar. Nikotin bağımlılığı için kanıta dayalı destek seçeneklerini hekim/eczacıyla görüşmek başarı şansını artırabilir."""),
            c("5. Sürdürülebilirlik","""Bir rutini kaçırınca 'bozuldu' deyip bırakma. Bir sonraki fırsatta normale dön; hedef kusursuz seri değil uzun dönem ortalamadır.""")
        ),
        b(20,"Kariyer Gelişimi ve Ustalık Yolculuğu","İş Hayatı","Operatörden liderliğe, teknik uzmanlıktan yöneticiliğe gelişim planı",
            c("1. Beceri haritası","""Rolünde gereken teknik, kalite, iletişim ve liderlik becerilerini yaz. Her birine 1-5 arası seviye ver ve en büyük boşlukları seç."""),
            c("2. Görünür sonuç üret","""Terfi için sadece çalışkanlık değil, ölçülebilir etki göster: hata azalması, çevrim kazanımı, eğitim, kaizen ve ekip gelişimi."""),
            c("3. Dokümante et","""Yaptığın iyileştirmeleri önce-sonra fotoğrafı, sayı ve standart değişikliğiyle arşivle. Yıl sonunda hafızaya değil kanıta dayanırsın."""),
            c("4. İnsan yetiştir","""Bir üst role hazır olmanın göstergelerinden biri, mevcut işini başkasına öğretebilmen ve ekip kapasitesini yükseltebilmendir."""),
            c("5. 1 yıllık plan","""Her çeyreğe bir teknik, bir iletişim ve bir liderlik hedefi koy. Ay sonunda kısa öz değerlendirme yap ve hedefleri gerçek sonuçlara göre güncelle.""")
        )
    )

    val injectionDefects = linkedMapOf(
        "Çapak" to "Kalıp ayırma hattını ve kapanmayı kontrol et. V→P geç kalıyor mu, holding basıncı/süresi yüksek mi, dolum sonu hız aşırı mı bak. Tek değişkenle küçük adım dene ve diğer bölgelerde eksik oluşmadığını doğrula.",
        "Eksik Dolum" to "Valf gate açılması, V→P'nin erken olması, dolum hızı, dozaj/cushion, ünite-kalıp sıcaklığı ve havalandırmayı sırayla kontrol et. Short-shot ile akış yönünü doğrula.",
        "Beyazlık" to "Hata bölgesinin hangi gate tarafından beslendiğini belirle. Valf gate zamanlaması, V→P, holding, lokal soğutma ve akış birleşmesini değerlendir. Foto limit standardı ve tek değişkenli deneme kullan.",
        "Çökme" to "Gate donma testi yap. Holding süresi/basıncı, lokal et kalınlığı ve soğutmayı kontrol et. Gate donduktan sonra basınç artırmak genelde fayda sağlamaz.",
        "Yabancı Madde / Siyah Nokta" to "Hammadde→silo→kırma→kurutucu→hortum→vida→sıcak yolluk zincirinde kaynağı izole et. Purge, temizlik, mıknatıs/filtre ve uzun bekleme sonrası termal bozunmayı kontrol et.",
        "Gate İzi" to "Valf iğnesi açma-kapama zamanını, gate sıcaklığını, holding etkisini ve gate mekanik yüzeyini kontrol et.",
        "İtici İzi" to "Parça yeterli soğudu mu, kalıba tutunma fazla mı, itici yüzeyi/itme hızı uygun mu kontrol et. Soğutmayı gereksiz uzatmadan doğrula.",
        "İpliklenme" to "Nozül/gate sıcaklığı, dekompresyon, bekleme süresi ve malzemenin termal durumunu kontrol et. Aşırı sıcaklık ve sızıntıyı ayır.",
        "Renk Kaybı / Parlaklık" to "Malzeme-masterbatch karışımı, sıcaklık, shear, hız profili ve kalıp yüzey sıcaklığını kıyasla. Bölgesel ise akış/gate etkisini ayrıca incele."
    )

    val tongueTwisters = listOf(
        "Bir berber bir berbere, bre berber gel beraber bir berber dükkânı açalım demiş.",
        "Şu köşe yaz köşesi, şu köşe kış köşesi, ortada su şişesi.",
        "Kırk küp, kırkının da kulpu kırık kara küp.",
        "Dal sarkar kartal kalkar, kartal kalkar dal sarkar.",
        "Çatalca'da topal çoban, çatal yapıp çatal satar.",
        "Pembe panjurlu pencerede beş minik papatya bekliyordu.",
        "Rüzgârlı rampada renkli ruloları sıralayan Rıza, yanlış sırayı yeniden düzenledi.",
        "Taze tarhana tencerede taşmadan tatlandı.",
        "Gökten gelen gri güvercin gölgeliğe kondu.",
        "Şaşkın şoför sisli sokakta sessiz sinyali seçemedi."
    )
}
