"""Müfredât alıntısı önceki bir cümleye gönderme yaparak başlayan kartları tamamlar (bir kerelik düzeltme).

"Bu sebeple…", "Bu bakışla…", "İkincisi…" gibi başlayan alıntılarda göndermenin neye yapıldığı kesilmişti.
Her biri için Râğıb'ın metninden önceki cümle(ler) alıntının başına eklenir; araya atlanan kısım
varsa " … " konur. Sıralı türlerin yalnızca hangi listeye ait olduğu belirsizse (Dördüncüsü: …)
metin uzatılmaz, Türkçeye kısa bir parantez eklenir (destedeki "(Harf olan mâ'nın) ikincisi" gibi).

Kullanım: python3 scripts/fix_mufredat_heads.py   (data/kelimeler.apkg'yi yerinde günceller)
Sonra:    python3 scripts/build_deck.py
"""

import html
import shutil
import sqlite3
import sys
import tempfile
import time
import unicodedata
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APKG = ROOT / "data" / "kelimeler.apkg"
HAREKELI, MUF_AR, MUF_TR = 2, 9, 10
GAP = " … "

SIHR_AR = ("والسِّحْرُ يقال على معان: الأوّل: الخداع وتخييلات لا حقيقة لها، نحو ما يفعله المشعبذ بصرف الأبصار "
           "عمّا يفعله لخفّة يد، وما يفعله النمّام بقول مزخرف عائق للأسماع،")
SIHR_TR = ("Sihir birkaç anlamda kullanılır. Birincisi: aldatma ve gerçeği olmayan hayallerdir; hokkabazın el "
           "çabukluğuyla yaptığını gözlerden gizlemesi ve koğucunun kulakları oyalayan süslü sözü gibi.")
ZULM_AR = ("وَالظُّلْمُ عند أهل اللّغة وكثير من العلماء: وضع الشيء في غير موضعه المختصّ به، إمّا بنقصان أو بزيادة، "
           "وإمّا بعدول عن وقته أو مكانه،")
ZULM_TR = ("Zulüm, dil âlimlerine ve pek çok âlime göre bir şeyi kendine has yerinden başka yere koymaktır; "
           "ya eksiltmek ya artırmak ya da vaktinden veya yerinden saptırmak suretiyle.")
ZULM3_AR = "قال بعض الحكماء: الظُّلْمُ ثلاثةٌ: الأوّل: ظُلْمٌ بين الإنسان وبين الله تعالى، وأعظمه: الكفر والشّرك والنّفاق"
ZULM3_TR = ("Bazı hikmet ehli şöyle demiştir: Zulüm üç türlüdür: Birincisi, insanla Allah Teâlâ arasındaki "
            "zulümdür; en büyüğü küfür, şirk ve nifaktır.")
FADL_AR = ("والفَضْلُ إذا استعمل لزيادة أحد الشّيئين على الآخر فعلى ثلاثة أضرب: فضل من حيث الجنس، كفضل جنس الحيوان "
           "على جنس النّبات. وفضل من حيث النّوع، كفضل الإنسان على غيره من الحيوان" + GAP + "وفضل من حيث الذّات، كفضل "
           "رجل على آخر. فالأوّلان جوهريّان لا سبيل للناقص فيهما أن يزيل نقصه وأن يستفيد الفضل، كالفرس والحمار لا "
           "يمكنهما أن يكتسبا الفضيلة التي خصّ بها الإنسان،")
FADL_TR = ("Fazl, iki şeyden birinin diğerine üstünlüğü için kullanıldığında üç türlüdür: Cins bakımından üstünlük, "
           "hayvan cinsinin bitki cinsine üstünlüğü gibi. Tür bakımından üstünlük, insanın diğer hayvanlara üstünlüğü "
           "gibi." + GAP + "Zat bakımından üstünlük, bir adamın diğerine üstünlüğü gibi. İlk ikisi cevherîdir; eksik "
           "olanın eksikliğini giderip üstünlük kazanmasına yol yoktur; at ve eşek, insana has kılınan fazileti "
           "kazanamaz.")
TULU_AR = "طَلَعَ الشمسُ طُلُوعًا ومَطْلَعًا" + GAP + "والمَطْلِعُ: موضعُ الطُّلُوعِ،"
TULU_TR = "Güneş doğdu (talea): tulû' ve matla'." + GAP + "Matli', doğuş yeridir."
TEKELLUF_AR = ("والتَّكَلُّفُ: اسم لما يفعل بمشقّة، أو تصنّع، أو تشبّع، ولذلك صار التَّكَلُّفُ على ضربين: محمود: وهو ما "
               "يتحرّاه الإنسان ليتوصّل به إلى أن يصير الفعل الذي يتعاطاه سهلا عليه، ويصير كَلِفًا به ومحبّا له،")
TEKELLUF_TR = ("Tekellüf, zahmetle, yapmacıklıkla ya da olmadığı hâlde varmış gibi gösterilerek yapılan şeyin adıdır. "
               "Bu yüzden tekellüf iki türlüdür: Övülen tekellüf, insanın yaptığı işin kendisine kolay gelmesine, ona "
               "düşkün olup onu sevmesine ulaşmak için yöneldiği şeydir.")
SAD_AR = "الصَّدْعُ: الشّقّ في الأجسام الصّلبة كالزّجاج والحديد ونحوهما. يقال: صَدَعْتُهُ فَانْصَدَعَ، وصَدَّعْتُهُ فَتَصَدَّعَ"
SAD_TR = ("Sad', cam ve demir gibi sert cisimlerdeki yarıktır; 'sadaatuhû fe'nsadea' ve 'saddaatuhû fe-tesaddaa' "
          "(onu yardım, o da yarıldı) denir.")

# (harekeli, mevcut Arapça alıntının başı, Arapça önek, araya " … " mı, Türkçe önek)
PREFIXES = [
    ("ظَالِم", "ولذلك قيل لآدم", ZULM_AR + GAP + "والظُّلْمُ يقال في مجاوزة الحقّ الذي يجري مجرى نقطة الدّائرة، ويقال فيما يكثر وفيما يقلّ من التّجاوز، ولهذا يستعمل في الذّنب الكبير، وفي الذّنب الصّغير،", False,
     ZULM_TR + GAP + "Zulüm, dairenin merkez noktası gibi olan hakkı aşmak hakkında söylenir; aşmanın çoğu için de azı için de kullanılır. Bu yüzden büyük günah için de küçük günah için de kullanılır."),
    ("ظَلَمَ", "ومن هذا يقال: ظَلَمْتُ", ZULM_AR, False, ZULM_TR),
    ("ظَالِمِي", "والثالث: ظُلْمٌ", ZULM3_AR + GAP + "والثاني: ظُلْمٌ بينه وبين الناس", True,
     ZULM3_TR + GAP + "İkincisi, insanla insanlar arasındaki zulümdür."),
    ("مَظْلُوم", "والثاني: ظُلْمٌ", ZULM3_AR, True, ZULM3_TR),
    ("مُجْرِم", "واستعير ذلك", "أصل الجَرْم: قطع الثّمرة عن الشجر، ورجل جَارِم، وقوم جِرَام، وثمر جَرِيم", True,
     "Cerm'in aslı meyveyi ağaçtan koparmaktır; (koparan) adama cârim, topluluğa cirâm, (koparılan) meyveye cerîm denir."),
    ("لَوْلَا", "والثاني: بمعنى هلّا", "«لَوْلَا» يجيء على وجهين: أحدهما: بمعنى امتناع الشيء لوقوع غيره، ويلزم خبره الحذف، ويستغنى بجوابه عن الخبر. نحو: لَوْلا أَنْتُمْ لَكُنَّا مُؤْمِنِينَ.", False,
     "«Levlâ» iki şekilde gelir: Birincisi, bir şeyin başka bir şey var olduğu için gerçekleşmemesi anlamındadır; haberi hazfedilir, cevabı haberin yerini tutar: «Siz olmasaydınız, elbette biz mümin kimseler olurduk» gibi."),
    ("سَاحِر", "وبهذا النّظر", SIHR_AR, True, SIHR_TR),
    ("فَضَّلَ", "والفضل الثالث", FADL_AR, False, FADL_TR),
    ("تَفْضِيل", "والفضل الثالث", FADL_AR, False, FADL_TR),
    ("تَعَالَى", "وعلى ذلك يقال", "والعَليُّ: هو الرّفيع القدر من: عَلِيَ، وإذا وصف الله تعالى به في قوله: أَنَّ اللَّهَ هُوَ الْعَلِيُّ الْكَبِيرُ، فمعناه: يعلو أن يحيط به وصف الواصفين بل علم العارفين.", False,
     "Aliyy, 'aliye' fiilinden, kadri yüce olandır. Allah Teâlâ «Şüphesiz Allah, en yüce (aliyy) ve en büyük olandır» âyetinde bununla nitelendiğinde anlamı şudur: O, vasfedenlerin vasfının, hatta ârif olanların bilgisinin kuşatmasından yücedir."),
    ("عَابِد", "وعلى هذا النحو", "وعَبْدٌ للدّنيا وأعراضها، وهو المعتكف على خدمتها ومراعاتها، وإيّاه قصد النّبي عليه الصلاة والسلام بقوله: «تعس عَبْدُ الدّرهمِ، تعس عَبْدُ الدّينار»،", False,
     "(Bir de) dünyanın ve onun geçici metaının kulu vardır; bu, ona hizmet edip onu gözetmeye kendini vermiş olandır. Peygamber aleyhisselâm «Dirhemin kulu helak oldu, dinarın kulu helak oldu» sözüyle bunu kastetmiştir."),
    ("قَدَر", "وعلى ذلك قوله", "فَتَقْدِيرُ الله على وجهين: أحدهما بالحكم منه أن يكون كذا أو لا يكون كذا، إمّا على سبيل الوجوب، وإمّا على سبيل الإمكان.", False,
     "Allah'ın takdiri iki şekildedir: Birincisi, bir şeyin şöyle olması ya da olmaması yönünde, ya zorunluluk ya da imkân yoluyla hükmetmesidir."),
    ("صَدَّقَ", "وعلى ذلك قوله", "وقوله تعالى: لَقَدْ صَدَقَ اللَّهُ رَسُولَهُ الرُّؤْيا بِالْحَقِ، فهذا صِدْقٌ بالفعل وهو التّحقّق، أي: حقّق رؤيته،", False,
     "«Andolsun, Allah Resûlü'nün rüyasını doğru çıkardı» âyetindeki sıdk, fiil ile sıdktır; bu da gerçekleştirmektir, yani rüyasını gerçekleştirdi."),
    ("خَفَّفَ", "الثالث: يقال خفيف", "الخَفِيف: بإزاء الثّقيل، ويقال ذلك تارة باعتبار المضايفة بالوزن، وقياس شيئين أحدهما بالآخر، نحو: درهم خفيف، ودرهم ثقيل. والثاني: يقال باعتبار مضايفة الزّمان، نحو: فرس خفيف، وفرس ثقيل: إذا عدا أحدهما أكثر من الآخر في زمان واحد.", False,
     "Hafif, ağırın karşılığıdır. Bu bazen ağırlık bakımından birbirine nispetle ve iki şeyi birbiriyle kıyaslayarak söylenir: hafif dirhem, ağır dirhem gibi. İkincisi zaman bakımından nispetle söylenir: biri aynı zamanda diğerinden daha çok koştuğunda hafif at, ağır at denmesi gibi."),
    ("اطَّلَعَ", "وعنه استعير", TULU_AR, False, TULU_TR),
    ("مُطَّلِع", "وعنه استعير", TULU_AR, False, TULU_TR),
    ("كَلَّفَ", "وبهذا النّظر", TEKELLUF_AR, False, TEKELLUF_TR),
    ("مُتَكَلِّفٌ", "والثاني: مذموم", TEKELLUF_AR + " وبهذا النّظر يستعمل التَّكْلِيفُ في تكلّف العبادات.", False,
     TEKELLUF_TR + " Bu bakış açısıyla teklîf, ibadetleri zahmetle üstlenmek hakkında kullanılır."),
    ("قَيِّم", "وعلى هذا قوله", "وقوله: دِينًا قِيَمًا، أي: ثابتا مُقَوِّمًا لأمور معاشهم ومعادهم", True,
     "«Dosdoğru (kıyem) bir din» âyetindeki kıyem, sabit olan ve insanların dünya ve ahiret işlerini ayakta tutan demektir."),
    ("مَرَدّ", "ومن هذا الرَّدُّ", "ومن الرّدّ إلى حالة كان عليها قوله: يَرُدُّوكُمْ عَلى أَعْقابِكُمْ", True,
     "Bir şeyi önceki hâline döndürmek anlamındaki redde şu âyet örnektir: «Sizi gerisin geri döndürürler»."),
    ("وَحْد", "ولصعوبة هذه", "وإذا وصف الله تعالى بالواحد فمعناه: هو الذي لا يصحّ عليه التّجزّي ولا التكثّر،", False,
     "Allah Teâlâ 'vâhid' (bir) diye nitelendiğinde anlamı şudur: O, parçalanması da çoğalması da mümkün olmayandır."),
    ("أَسْكَنَ", "ومن الثاني يقال", "السُّكُونُ: ثبوت الشيء بعد تحرّك، ويستعمل في الاستيطان نحو: سَكَنَ فلان مكان كذا، أي: استوطنه" + GAP + "فمن الأوّل يقال: سكّنته،", False,
     "Sükûn, bir şeyin hareketten sonra durmasıdır; yurt edinmek anlamında da kullanılır: 'Falan şu yerde oturdu (sekene)', yani orayı yurt edindi." + GAP + "Birinci (durma) manasından 'sekkentuhû' (onu durdurdum) denir."),
    ("اسْتَخْلَفَ", "وعلى هذا الوجه", "والخِلافةُ النّيابة عن الغير إمّا لغيبة المنوب عنه، وإمّا لموته، وإمّا لعجزه، وإمّا لتشريف المستخلف.", False,
     "Hilâfet, başkasının yerine geçmektir: ya yerine geçilenin yokluğu, ya ölümü, ya acizliği sebebiyle, ya da halife kılınanı şereflendirmek için."),
    ("خَلِيل", "يقال منه", "والخُلَّةُ: المودّة، إمّا لأنّها تتخلّل النّفس، أي: تتوسّطها، وإمّا لأنّها تخلّ النّفس، فتؤثّر فيها تأثير السّهم في الرّميّة، وإمّا لفرط الحاجة إليها،", False,
     "Hulle sevgidir; ya nefsin içine işleyip ortasına yerleştiği için, ya okun hedefte iz bırakması gibi nefse işleyip onda iz bıraktığı için, ya da ona duyulan ihtiyacın çokluğundan dolayı (bu adı almıştır)."),
    ("خَائِف", "ولذلك قيل", "والخوف من الله لا يراد به ما يخطر بالبال من الرّعب، كاستشعار الخوف من الأسد، بل إنما يراد به الكفّ عن المعاصي واختيار الطّاعات،", False,
     "Allah'tan korkmakla, aslandan korkmayı hissetmek gibi akla gelen dehşet kastedilmez; ancak günahlardan el çekmek ve itaatleri seçmek kastedilir."),
    ("سَحَرَ", "وعلى ذلك قوله", SIHR_AR, False, None),  # Türkçesi aşağıdaki değiştirmede
    ("عَنِتَ", "ولهذا يقال", "الْمُعَانَتَةُ كالمعاندة لكن المُعَانَتَةُ أبلغ، لأنها معاندة فيها خوف وهلاك،", False,
     "Muânete, muânede (inatlaşma) gibidir; ancak muânete daha ileridir, çünkü içinde korku ve helak bulunan bir inatlaşmadır."),
    ("مَعِيشَة", "ويشتقّ منه", "العَيْشُ: الحياة المختصّة بالحيوان، وهو أخصّ من الحياة، لأنّ الحياة تقال في الحيوان، وفي الباري تعالى، وفي الملك،", False,
     "Ayş, canlılara has hayattır; hayattan daha özeldir, çünkü hayat canlılar, Yüce Bârî ve melek hakkında da söylenir."),
    ("مُؤْتَفِكَة", "ومنه قيل", "الإفك: كل مصروف عن وجهه الذي يحق أن يكون عليه،", False,
     "İfk, olması gereken yönünden çevrilmiş her şeydir."),
    ("أَنْكَرَ", "وقد يُستعمَلُ", "الإِنْكَارُ: ضِدُّ العِرْفَانِ. يقال: أَنْكَرْتُ كذا، ونَكَرْتُ، وأصلُه أن يَرِدَ على القَلْبِ ما لا يتصوَّره، وذلك ضَرْبٌ من الجَهْلِ.", True,
     "İnkâr, tanımanın (irfân) zıddıdır; 'enkertu kezâ' ve 'nekirtu' denir. Aslı, kalbe tasavvur edemediği bir şeyin gelmesidir; bu da bir tür bilgisizliktir."),
    ("مُقْتَصِد", "والثاني يكنّى", "والِاقْتِصَادُ على ضربين: أحدهما محمود على الإطلاق، وذلك فيما له طرفان: إفراط وتفريط كالجود، فإنه بين الإسراف والبخل، وكالشّجاعة فإنّها بين التّهوّر والجبن", True,
     "İktisat iki türlüdür: Birincisi mutlak olarak övülendir; bu, ifrat ve tefrit olmak üzere iki ucu bulunan şeylerdedir: israf ile cimrilik arasında olan cömertlik ve atılganlık ile korkaklık arasında olan cesaret gibi."),
    ("أَحْكَم", "ومن هذا الوجه", "والحِكْمَةُ: إصابة الحق بالعلم والعقل، فالحكمة من الله تعالى: معرفة الأشياء وإيجادها على غاية الإحكام، ومن الإنسان: معرفة الموجودات وفعل الخيرات" + GAP + "فإذا قيل في الله تعالى: هو حَكِيم، فمعناه بخلاف معناه إذا وصف به غيره،", False,
     "Hikmet, ilim ve akılla hakka isabet etmektir. Allah Teâlâ'dan olan hikmet, eşyayı bilmek ve onları en sağlam şekilde var etmektir; insandan olan hikmet ise varlıkları bilmek ve hayırları işlemektir." + GAP + "Allah Teâlâ için 'hakîm' dendiğinde anlamı, başkası bununla nitelendiğindeki anlamından farklıdır."),
    ("أَخْطَأَ", "والثاني: أن يريد", "الخَطَأ: العدول عن الجهة، وذلك أضرب: أحدها: أن تريد غير ما تحسن إرادته فتفعله، وهذا هو الخطأ التامّ المأخوذ به الإنسان", True,
     "Hata, (doğru) yönden sapmaktır ve birkaç türlüdür: Birincisi, istenmesi güzel olmayanı isteyip yapmandır; bu, insanın sorumlu tutulduğu tam hatadır."),
    ("أَصْدَق", "ولذلك قال", "الصِّدْقُ والكذب أصلهما في القول، ماضيا كان أو مستقبلا، وعدا كان أو غيره، ولا يكونان بالقصد الأوّل إلّا في القول، ولا يكونان في القول إلّا في الخبر دون غيره من أصناف الكلام،", False,
     "Doğruluk (sıdk) ile yalanın aslı sözdedir; geçmişe ya da geleceğe dair olsun, vaat olsun ya da olmasın. İkisi de asıl itibarıyla ancak sözde, sözde de ancak haberde bulunur; sözün diğer türlerinde bulunmaz."),
    ("أَعْلَام", "وسمّي الجبل", "والعَلَمُ: الأثر الذي يُعْلَمُ به الشيء كعلم الطّريق وعلم الجيش،", False,
     "Alem, bir şeyin kendisiyle bilindiği izdir; yol işareti ve ordu sancağı gibi."),
    ("ثَابِت", "ويقال ذلك", "الثَّبَات ضدّ الزوال، يقال: ثَبَتَ يَثْبُتُ ثَبَاتًا", True,
     "Sebât, yok olmanın zıddıdır; 'sebete yesbutu sebâten' denir."),
    ("عَمٍ", "وفي الثاني", "العَمَى يقال في افتقاد البصر والبصيرة، ويقال في الأوّل: أَعْمَى،", False,
     "Körlük (amâ), gözün ve basiretin yitirilmesi hakkında söylenir. Birincisinde (göz körlüğünde) 'a'mâ' denir."),
    ("مَثُوبَة", "وكذلك المَثُوبَة", "والثّواب يقال في الخير والشّر، لكن الأكثر المتعارف في الخير", True,
     "Sevap hayır için de şer için de söylenir; ancak yaygın ve bilinen kullanımı daha çok hayır içindir."),
    ("مُسَحَّر", "وعلى ذلك قوله", "وقال تعالى: بَلْ نَحْنُ قَوْمٌ مَسْحُورُونَ، أي: مصروفون عن معرفتنا بالسّحر.", False,
     "«Belki de biz büyülenmiş bir topluluğuz» âyetinin anlamı: sihirle bilgimizden alıkonulmuşuz, demektir."),
    ("نَافِلَة", "وأصل ذلك", "النَّفَلُ قيل: هو الغَنِيمَةُ بعَيْنِهَا لكن اختلفتِ العبارةُ عنه لاختلافِ الاعْتِبَارِ، فإنه إذا اعتُبِر بكونه مظفورًا به يقال له: غَنِيمَةٌ، وإذا اعْتُبِرَ بكونه مِنْحَةً من الله ابتداءً من غير وجوبٍ يقال له: نَفَلٌ", True,
     "Nefel'in ganimetin ta kendisi olduğu söylenmiştir; ancak bakış açısı farklı olduğu için adı farklılaşmıştır: Zaferle elde edilen şey olarak düşünülünce ona ganimet, Allah'ın bir borcu olmaksızın baştan verdiği bir bağış olarak düşünülünce nefel denir."),
    ("قَنَتَ", "وعلى هذا قيل", "القُنُوتُ: لزوم الطّاعة مع الخضوع، وفسّر بكلّ واحد منهما في قوله تعالى: وَقُومُوا لِلَّهِ قانِتِينَ", True,
     "Kunût, huşu ile birlikte itaate devam etmektir; «Allah'a kânitler olarak (divan durarak) kalkın» âyeti bunların her biriyle tefsir edilmiştir."),
    ("صَائِمٌ", "ولذلك قيل", "الصَّوْمُ في الأصل: الإمساك عن الفعل مطعما كان، أو كلاما، أو مشيا،", False,
     "Savm, aslında bir işten kendini tutmaktır; yemek, konuşmak ya da yürümek olsun."),
    ("أَسْخَطَ", "وهو من الله", "السَّخَطُ والسُّخْطُ: الغضب الشديد المقتضي للعقوبة", True,
     "Sahat ve suht, cezayı gerektiren şiddetli öfkedir."),
    ("اثَّاقَلَ", "والثاني أن يستعمل", "والثقيل والخفيف يستعمل على وجهين: أحدهما على سبيل المضايفة، وهو أن لا يقال لشيء ثقيل أو خفيف إلا باعتباره بغيره", True,
     "Ağır ve hafif iki şekilde kullanılır: Birincisi nispet yoluyladır; bir şeye ancak başka bir şeyle kıyaslanarak ağır ya da hafif denir."),
    ("صَدَعَ", "وعنه استعير", SAD_AR, True, SAD_TR),
    ("صَدَّعَ", "وكذا استعير", SAD_AR + GAP + "وعنه استعير: صَدَعَ الأمرَ، أي: فَصَلَهُ،", True,
     SAD_TR + GAP + "Ondan istiare edilerek 'sadea'l-emra', yani işi ayırıp açıkça ortaya koydu denmiştir."),
    ("بَلِيغ", "والثاني: أن يكون", "والبلاغة تقال على وجهين: أحدهما: أن يكون بذاته بليغا، وذلك بأن يجمع ثلاثة أوصاف: صوبا في موضوع لغته، وطبقا للمعنى المقصود به، وصدقا في نفسه، ومتى اخترم وصف من ذلك كان ناقصا في البلاغة.", False,
     "Belâgat iki şekilde söylenir: Birincisi, sözün bizzat kendisinin beliğ olmasıdır; bu da üç vasfı bir araya getirmesiyle olur: dilin konuluşuna göre doğru, kastedilen manaya uygun ve kendi içinde doğru olmak. Bunlardan biri eksik olursa söz belâgatte eksik kalır."),
    ("بِدْع", "وكذلك البِدْعُ", "والبديع يقال للمُبْدِعِ، نحو قوله تعالى: بَدِيعُ السَّماواتِ وَالْأَرْضِ، ويقال للمُبْدَعِ نحو: ركيّة بديع،", False,
     "Bedî', yaratan (mübdi') için kullanılır: «Göklerin ve yerin bedî'i (örneksiz yaratanı)» gibi; yaratılan (mübde') için de kullanılır: 'yeni kazılmış kuyu (rekiyyetun bedî')' gibi."),
    ("تَبَرُّج", "ويدلّ على ذلك", "وثوب مُبَرَّج: صوّرت عليه بروج، واعتبر حسنه، فقيل: تَبَرَّجَتِ المرأة أي: تشبّهت به في إظهار المحاسن، وقيل: ظهرت من برجها، أي: قصرها،", False,
     "Üzerine burç resimleri işlenmiş elbiseye 'sevbun müberrac' denir. Onun güzelliği göz önüne alınarak 'kadın teberruc etti' denmiştir; yani güzelliklerini açığa vurmakta ona benzedi. Burcundan, yani köşkünden dışarı çıktı (anlamında olduğu) da söylenmiştir."),
    ("حَسَّ", "ولمّا كان ذلك", "فَحَسَسْتُ يقال على وجهين: أحدهما: يقال: أصبته بحسّي، نحو عنته ورعته، والثاني: أصبت حاسّته، نحو: كبدته وفأدته،", False,
     "'Hasestu' iki şekilde söylenir: Birincisi 'onu duyumla algıladım' anlamındadır ('intuhû' ve 'ru'tuhû' gibi); ikincisi 'onun duyusuna isabet ettim' anlamındadır, 'kebedtuhû' (ciğerine vurdum) ve 'fe'edtuhû' (kalbine vurdum) gibi."),
    ("خَدّ", "وأصل ذلك", "الخَدُّ والأخدود: شقّ في الأرض مستطيل غائص، وجمع الأخدود أَخَادِيد،", False,
     "Hadd ve uhdûd, yerde uzunlamasına ve derine giden yarıktır; uhdûdun çoğulu ehâdîd'dir."),
    ("عَبُوس", "ومنه قيل", "العُبُوسُ: قُطُوبُ الوجهِ من ضيق الصّدر", True,
     "Ubûs, göğüs darlığından dolayı yüzü ekşitmektir."),
    ("قَتَر", "وأصل ذلك", "القَتْرُ: تقليل النّفقة، وهو بإزاء الإسراف، وكلاهما مذمومان", True,
     "Katr, harcamayı azaltmaktır; israfın karşılığıdır ve ikisi de yerilmiştir."),
    ("كَثَّرَ", "وعلى ذلك قوله", "ويكنّى بِالْقِلَّةِ عن الذّلّة اعتبارا بما قال الشاعر: ولست بالأكثر منهم حصا ... وإنما العزّة للكاثر", False,
     "Azlıkla zilletten kinaye yapılır; şairin şu sözü göz önüne alınarak: 'Sen onlardan sayıca çok değilsin; izzet ancak çok olanındır.'"),
    ("مَخْتُوم", "والثاني: الأثر", "الخَتْمُ والطّبع يقال على وجهين: مصدر خَتَمْتُ وطبعت، وهو تأثير الشيء كنقش الخاتم والطّابع.", False,
     "Hatm ve tab' iki şekilde söylenir: Birincisi 'hatemtu' ve 'taba'tu' fiillerinin mastarıdır; bu, mühür ve damganın nakşı gibi bir şeyin iz bırakmasıdır."),
    ("مُتَجَانِف", "وعلى هذا", "أصل الجَنَف ميل في الحكم، فقوله تعالى: فَمَنْ خافَ مِنْ مُوصٍ جَنَفًا، أي: ميلا ظاهرا،", False,
     "Cenef'in aslı hükümde meyletmektir; «Vasiyet edenin haksızlığa sapmasından (cenef) korkan kimse» âyetindeki cenef, açık bir meyil demektir."),
    ("مُتَّكَأ", "ومنه أَوْكَأْتُ", "الوِكَاءُ: رباط الشيء، وقد يجعل الوِكَاءُ اسما لما يجعل فيه الشيء فيشدّ به،", False,
     "Vikâ', bir şeyin bağıdır; bazen içine bir şey konup bağlanan şeyin adı da yapılır."),
    ("مُحَرَّر", "ومن الثاني", "والحريّة ضربان: الأول: من لم يجر عليه حكم الشيء، نحو: الْحُرُّ بِالْحُرِّ. والثاني: من لم تتملّكه الصفات الذميمة من الحرص والشّره على المقتنيات الدنيوية" + GAP + "والتحريرُ: جعل الإنسان حرّا، فمن الأول: فَتَحْرِيرُ رَقَبَةٍ مُؤْمِنَةٍ،", False,
     "Hürriyet iki türlüdür: Birincisi, üzerinde başka bir şeyin hükmü cereyan etmeyen kimsedir: «Hüre hür» gibi. İkincisi, dünya mallarına karşı hırs ve açgözlülük gibi kötü sıfatların kendisine sahip olmadığı kimsedir." + GAP + "Tahrîr, insanı hür kılmaktır. Birinci (tür hürriyetten) olanı: «Mümin bir köle azat etmek (tahrîr)»."),
    ("مُدْحَض", "وأصله من", "يقال: أدحضت فلانا في حجّته فَدَحَضَ" + GAP + "وأدحضت حجّته فَدَحَضَتْ،", False,
     "'Falanı delilinde çürüttüm, o da çürüdü (dehada)' denir" + GAP + "'Delilini çürüttüm, o da çürüdü' (denir)."),
    ("مُرْجًى", "ووجه ذلك", "والرَّجَاءُ ظنّ يقتضي حصول ما فيه مسرّة، وقوله تعالى: ما لَكُمْ لا تَرْجُونَ لِلَّهِ وَقارًا، قيل: ما لكم لا تخافون", True,
     "Recâ, sevinç veren bir şeyin gerçekleşmesini gerektiren bir zandır. «Size ne oluyor ki Allah'ın azametinden ummuyorsunuz?» âyeti için 'Size ne oluyor ki korkmuyorsunuz' anlamında olduğu söylenmiştir."),
    ("مُزْجَاة", "ومنه: رجل", "التَّزْجِيَةُ: دَفْعُ الشّيء لينساق، كَتَزْجِيَةِ رديء البعير، وتَزْجِيَةِ الرّيح السّحاب", True,
     "Tezciye, bir şeyi sürüklensin diye itmektir; kötü deveyi sürmek ve rüzgârın bulutu sürmesi gibi."),
    ("مُصِيبٌ", "وذلك هو المراد", "أَصَابَ كذا، أي: وجد ما طلب، كقولك: أَصَابَهُ السّهمُ، وذلك على أضرب: الأوّل: أن يقصد ما يحسن قصده فيفعله، وذلك هو الصَّوَابُ التّامُّ المحمودُ به الإنسان. والثاني: أن يقصد ما يحسن فعله، فيتأتّى منه غيره لتقديره بعد اجتهاده أنّه صَوَابٌ،", False,
     "'Esâbe kezâ', aradığını buldu demektir; 'ok ona isabet etti' sözü gibi. Bu birkaç türlüdür: Birincisi, kastedilmesi güzel olanı kastedip onu yapmaktır; bu, insanın övüldüğü tam doğruluktur. İkincisi, yapılması güzel olanı kastettiği hâlde, içtihadından sonra doğru olduğunu düşündüğü için ondan başkasının meydana gelmesidir."),
    ("نَاهٍ", "وكذا النهي", "وقوله: وَنَهَى النَّفْسَ عَنِ الْهَوى، فإنه لم يعن أن يقول لنفسه: لا تفعل كذا، بل أراد قمعها عن شهوتها ودفعها عمّا نزعت إليه وهمّت به،", False,
     "«Nefsini hevâdan alıkoyan» âyetinde, kişinin nefsine 'şunu yapma' demesi kastedilmemiş; onu arzusundan bastırıp meylettiği ve yöneldiği şeyden uzaklaştırması kastedilmiştir."),
    ("قَلَّلَ", "وكذلك قوله", "وقوله: وَلا تَزالُ تَطَّلِعُ عَلى خائِنَةٍ مِنْهُمْ إِلَّا قَلِيلًا، أي: جماعة قَلِيلَةً،", False,
     "«Pek azı (kalîl) hariç, onlardan daima bir hainlik görürsün» âyetindeki kalîl, az bir topluluk demektir."),
]

# Türkçe başı belirsiz ama metni uzatmaya değmeyenler: (harekeli, eski baş, yeni baş). Hepsi düz metin.
TR_REPLACE = [
    ("الْ", "Dördüncüsü:", "(Kelime başına gelen elif türlerinin) dördüncüsü:"),
    ("لَ", "Üçüncüsü:", "(Harf olan lâmın türlerinden) üçüncüsü:"),
    ("إِنْ", "Aynı şekilde 'in' dört türlüdür:", "('En' gibi) 'in' de dört türlüdür:"),
    ("جَاعِل", "Beşincisi,", "(Ca'l fiilinin kullanımlarından) beşincisi,"),
    ("هَالِكٌ", "Dördüncüsü:", "(Helâkin dört türünden) dördüncüsü:"),
    ("مَكْرُوهٌ", "Beşincisi:", "(«Dinde zorlama yoktur» âyeti hakkındaki görüşlerin) beşincisi:"),
    ("سَحَرَ", "(Aldatıcı hayal anlamındaki sihir için) Allah Teâlâ buyurdu:", SIHR_TR + " Şu âyetler buna göredir:"),
]


def same(a, b):
    """Harekelerin yazılış sırası (şedde/üstün) farklı olabilir."""
    return unicodedata.normalize("NFC", a) == unicodedata.normalize("NFC", b)


def esc(text):
    return html.escape(text, quote=True)


def main():
    work = Path(tempfile.mkdtemp())
    with zipfile.ZipFile(APKG) as archive:
        archive.extractall(work)
        names = archive.namelist()
    db = sqlite3.connect(work / "collection.anki2")
    notes = {nid: flds.split("\x1f") for nid, flds in db.execute("select id, flds from notes")}

    changed, missing = set(), []
    for word, head, ar, gap, tr in PREFIXES:
        hits = [nid for nid, f in notes.items() if same(f[HAREKELI], word) and f[MUF_AR].startswith(head)]
        if not hits:
            missing.append(f"{word} / {head}")
        sep = GAP if gap else " "
        for nid in hits:
            f = notes[nid]
            f[MUF_AR] = esc(ar) + sep + f[MUF_AR]
            if tr is not None:
                f[MUF_TR] = esc(tr) + sep + f[MUF_TR]
            changed.add(nid)
    for word, old, new in TR_REPLACE:
        hits = [nid for nid, f in notes.items() if same(f[HAREKELI], word) and f[MUF_TR].startswith(esc(old))]
        if not hits:
            missing.append(f"{word} / {old}")
        for nid in hits:
            notes[nid][MUF_TR] = esc(new) + notes[nid][MUF_TR][len(esc(old)):]
            changed.add(nid)
    if missing:
        sys.exit("Bulunamadı (zaten düzeltilmiş olabilir):\n  " + "\n  ".join(missing))

    now = int(time.time())
    with db:
        db.executemany("update notes set flds = ?, mod = ?, usn = -1 where id = ?",
                       [("\x1f".join(notes[nid]), now, nid) for nid in changed])
    db.execute("VACUUM")
    db.close()

    tmp = work / "kelimeler.apkg"
    with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as archive:
        for name in names:
            archive.write(work / name, name)
    shutil.copy(tmp, APKG)
    print(f"{len(changed)} kartın Müfredât alıntısı tamamlandı → {APKG}")


if __name__ == "__main__":
    main()
