package com.example.data

import com.example.model.AmbientPreset
import com.example.model.CharacterProfile
import com.example.model.DialogueBeat
import com.example.model.NovelProject
import com.example.model.NovelScene

object DemoNovelSeed {

    const val NARRATOR_ID = "narrator"
    const val LAYLA_ID = "char_layla"
    const val JULIAN_ID = "char_julian"

    fun createDemoProject(): NovelProject {
        val characters = listOf(
            CharacterProfile(
                id = NARRATOR_ID,
                isNarrator = true,
                nameEn = "The Narrator",
                nameAr = "الراوي",
                roleEn = "Omniscient Storyteller",
                roleAr = "الراوي السينمائي",
                bioEn = "The velvet voice guiding you through rain-slicked railways, whispered secrets, and candlelight corridors along the Mediterranean coast.",
                bioAr = "الصوت المخملي الذي يقودك عبر قطارات المطر والأسرار الهامسة وممرات الشموع على امتداد ساحل المتوسط.",
                bundledPortraitKey = "narrator_crest",
                elevenVoiceId = "JBFqnCBsd6RMkjVDRZzb", // George — warm dramatic narrator
                elevenStability = 0.52f,
                elevenSimilarityBoost = 0.82f,
                elevenStyle = 0.35f,
                geminiVoiceName = "Charon",
                geminiActingStyleEn = "Calm cinematic literary narrator, deep velvety cadence, unhurried dramatic pauses",
                geminiActingStyleAr = "راوٍ أدبي سينمائي هادئ، نبرة مخملية عميقة مع وقفات درامية متأنية",
                accentColorHex = 0xFFD4AF37
            ),
            CharacterProfile(
                id = LAYLA_ID,
                isNarrator = false,
                nameEn = "Layla Al-Mansoor",
                nameAr = "ليلى المنصور",
                roleEn = "Imperial Archivist",
                roleAr = "أمينة الأرشيف الملكي",
                bioEn = "A sharp-minded Levantine historian who vanished from Cairo with an encrypted brass meridian key sought by three empires.",
                bioAr = "مؤرخة حادة الذكاء اختفت من القاهرة حاملةً مفتاحاً نحاسياً مشفّراً تلاحقه ثلاث إمبراطوريات.",
                bundledPortraitKey = "char_layla",
                elevenVoiceId = "EXAVITQu4vr4xnSDxMaL", // Sarah — expressive poised female
                elevenStability = 0.42f,
                elevenSimilarityBoost = 0.85f,
                elevenStyle = 0.50f,
                geminiVoiceName = "Aoede",
                geminiActingStyleEn = "Poised aristocratic woman, guarded yet fiercely intelligent, subtle breathy tension",
                geminiActingStyleAr = "امرأة أرستقراطية رصينة، ذكية وحذرة بنبرة هامسة مشحونة بالغموض",
                accentColorHex = 0xFF3FA68A
            ),
            CharacterProfile(
                id = JULIAN_ID,
                isNarrator = false,
                nameEn = "Julian Vance",
                nameAr = "جوليان فانس",
                roleEn = "Master Horologist",
                roleAr = "صانع الساعات المخضرم",
                bioEn = "A silver-bearded horologist and former inspector whose pocket watch ticks backward whenever the Alexandria cipher draws near.",
                bioAr = "صانع ساعات مخضرم ومفتش سابق، تدور ساعته الجيبية إلى الخلف كلما اقترب من شيفرة الإسكندرية المفقودة.",
                bundledPortraitKey = "char_julian",
                elevenVoiceId = "N2lVS1w4EtoT3dr4eOWO", // Callum — raspy enigmatic older man
                elevenStability = 0.48f,
                elevenSimilarityBoost = 0.80f,
                elevenStyle = 0.44f,
                geminiVoiceName = "Fenrir",
                geminiActingStyleEn = "Deep raspy mysterious older gentleman, measured noir intrigue, understated dry wit",
                geminiActingStyleAr = "رجل مسن غامض بصوت خشن عميق ونبرة تحريات كلاسيكية متزنة",
                accentColorHex = 0xFFC98A4B
            )
        )

        val scene1 = NovelScene(
            id = "scene_1_express",
            titleEn = "Carriage Seven — Rain Over the Coast",
            titleAr = "العربة السابعة — مطر فوق الساحل",
            subtitleEn = "ACT I · 11:48 PM · COASTAL EXPRESS",
            subtitleAr = "الفصل الأول · ١١:٤٨ مساءً · القطار الساحلي",
            visualPrompt = "1920s mahogany luxury train carriage at midnight during heavy coastal rain, warm antique brass lamp glowing over velvet seats and leather journal, noir cinema chiaroscuro",
            bundledArtworkKey = "scene_orient_rain",
            ambientPreset = AmbientPreset.RAIN,
            sfxPrompt = "Heavy midnight rain tapping rhythmically against vintage train glass with distant metallic rail clatter",
            beats = listOf(
                DialogueBeat(
                    id = "beat_1_1",
                    characterId = NARRATOR_ID,
                    isNarration = true,
                    textEn = "Midnight rain hammered the arched mahogany windows of Carriage Seven, turning the distant lights of Alexandria into liquid gold across the glass.",
                    textAr = "كان مطر منتصف الليل يطرق نوافذ العربة السابعة المصنوعة من الماهوغني، فيحوّل أضواء الإسكندرية البعيدة إلى ذهبٍ سائل ينساب على الزجاج.",
                    expressionTag = "[softly]"
                ),
                DialogueBeat(
                    id = "beat_1_2",
                    characterId = JULIAN_ID,
                    isNarration = false,
                    textEn = "You have been turning that brass key in your pocket since Marseilles, Miss Al-Mansoor. Forgive an old clockmaker for recognizing the sound of sixteenth-century teeth.",
                    textAr = "منذ مغادرة مرسيليا وأنتِ تقلّبين ذلك المفتاح النحاسي في معطفك يا آنسة المنصور. اعذري صانع ساعاتٍ عجوزاً يميّز رنين تروس القرن السادس عشر من بين صوت العاصفة.",
                    expressionTag = "[sighs]"
                ),
                DialogueBeat(
                    id = "beat_1_3",
                    characterId = LAYLA_ID,
                    isNarration = false,
                    textEn = "If you truly recognized it, Inspector Vance, you would know better than to sit with your back to the corridor door.",
                    textAr = "لو كنتَ تعرف قيمته حقاً أيها المفتش فانس، لما جلستَ وظهرك مكشوفٌ لباب الممر.",
                    expressionTag = "[whispers]"
                ),
                DialogueBeat(
                    id = "beat_1_4",
                    characterId = NARRATOR_ID,
                    isNarration = true,
                    textEn = "The brass table lamp flickered once as the express curved above the dark cliffs. Julian clicked open his silver pocket watch—its gilded hands were frozen at twelve.",
                    textAr = "ارتعش مصباح الطاولة النحاسي مرةً واحدة بينما انعطف القطار فوق الجروف المظلمة. فتح جوليان ساعته الفضية؛ كانت عقاربها المذهّبة متجمدةً تماماً عند الثانية عشرة.",
                    expressionTag = "[pauses]"
                ),
                DialogueBeat(
                    id = "beat_1_5",
                    characterId = JULIAN_ID,
                    isNarration = false,
                    textEn = "Then let us reach the observatory before the bell strikes. Neither of us boarded this train to survive alone.",
                    textAr = "إذن لنصل إلى المرصد قبل أن يدق الجرس. لم يركب أحدنا هذا القطار لينجو بمفرده.",
                    expressionTag = "[dramatically]"
                )
            )
        )

        val scene2 = NovelScene(
            id = "scene_2_observatory",
            titleEn = "The Velvet Observatory",
            titleAr = "المرصد المخملي",
            subtitleEn = "ACT II · 01:12 AM · PALAZZO ARCHIVE",
            subtitleAr = "الفصل الثاني · ٠١:١٢ صباحاً · أرشيف القصر",
            visualPrompt = "Grand moonlit celestial archive observatory inside an ancient Mediterranean palazzo, antique brass astrolabe and sealed crimson letter on obsidian marble desk, golden candlelight",
            bundledArtworkKey = "scene_clocktower_vault",
            ambientPreset = AmbientPreset.NOIR_SUSPENSE,
            sfxPrompt = "Heavy ancient wooden vault door creaking open followed by crisp brass clockwork ticking in a reverberant stone hall",
            beats = listOf(
                DialogueBeat(
                    id = "beat_2_1",
                    characterId = NARRATOR_ID,
                    isNarration = true,
                    textEn = "High above the harbor, moonlight spilled through arched stone glass onto an obsidian table where a century-old astrolabe stood guard beside a crimson wax seal.",
                    textAr = "عالياً فوق الميناء، انسكب ضوء القمر عبر الزجاج الحجري المقوّس فوق طاولة من الأوبسيديان، حيث وقف أسطرلابٌ عتيق يحرس رسالةً مختومة بالشمع القرمزي.",
                    expressionTag = "[softly]"
                ),
                DialogueBeat(
                    id = "beat_2_2",
                    characterId = LAYLA_ID,
                    isNarration = false,
                    textEn = "The wax is still warm. Someone broke the meridian seal less than an hour ago—and left the ledger open to the page my grandfather erased.",
                    textAr = "الشمع ما يزال دافئاً. لقد كسر أحدهم ختم خط الطول قبل أقل من ساعة... وترك السجل مفتوحاً على الصفحة التي محاها جدي بيديه.",
                    expressionTag = "[whispers]"
                ),
                DialogueBeat(
                    id = "beat_2_3",
                    characterId = JULIAN_ID,
                    isNarration = false,
                    textEn = "Wait—listen to the astrolabe. That is not wind in the rafters, Layla. The vault mechanism has already been wound.",
                    textAr = "انتظري... أصغي إلى الأسطرلاب. ليست هذه رياحاً في السقف يا ليلى؛ آلية الخزنة قد أُعيد تدويرها للتو.",
                    expressionTag = "[dramatically]"
                ),
                DialogueBeat(
                    id = "beat_2_4",
                    characterId = LAYLA_ID,
                    isNarration = false,
                    textEn = "You brought me to a trap... or a revelation. Place your pocket watch into the brass ring—now!",
                    textAr = "لقد قُدتَني إما إلى فخّ... أو إلى الحقيقة الكاملة. ضع ساعتك الجيبية في الحلقة النحاسية الآن!",
                    expressionTag = "[laughs]"
                ),
                DialogueBeat(
                    id = "beat_2_5",
                    characterId = NARRATOR_ID,
                    isNarration = true,
                    textEn = "As gold met brass, the candlelight bowed toward the floor, and beneath the marble archives a hidden door sighed open into the dark.",
                    textAr = "وما إن لامس الذهبُ النحاس، حتى انحنى لهب الشموع نحو الأرض، وتنهّد بابٌ سري تحت أرشيف الرخام ليُفتح على المجهول.",
                    expressionTag = "[pauses]"
                )
            )
        )

        return NovelProject(
            id = "lumiere_demo_alexandria",
            titleEn = "The Midnight Express to Alexandria",
            titleAr = "قطار منتصف الليل إلى الإسكندرية",
            authorEn = "Lumière Original Showcase",
            authorAr = "إصدار لوميير السينمائي التجريبي",
            characters = characters,
            scenes = listOf(scene1, scene2)
        )
    }

    const val SAMPLE_PASTE_EN = """The harbor fog curled beneath the antique lanterns of Venice as the gondola scraped softly against the marble steps.
"You are late, Clara," murmured Sebastian from the shadow of the archway, folding a rain-dampened telegram into his coat.
"The bells of San Marco were ringing for the wrong ship," Clara whispered, holding up a small gilded compass whose needle spun wildly. "Look at it, Sebastian. The vault under the opera house is awake."
A distant cello echoed across the black water while the iron gate swung open on its own."""

    const val SAMPLE_PASTE_AR = """انزلق الضباب الفضي تحت فوانيس الميناء العتيقة في البندقية بينما لامس القارب درجات الرخام بصمتٍ رهيب.
همس سيباستيان من ظل القوس الحجري وهو يطوي برقيةً مبللة بالمطر: «لقد تأخرتِ يا كلارا، ظننتكِ لن تحضري قبل الفجر».
أجابت كلارا بصوتٍ خافت وهي ترفع بوصلةً ذهبية تدور إبرتها بجنون: «أجراس سان ماركو قرعت للسفينة الخطأ يا سيباستيان... انظر، الخزنة تحت دار الأوبرا قد استيقظت».
في تلك اللحظة، تردد صدى آلة تشيلو بعيدة فوق المياه السوداء، وانفتح الباب الحديدي وحده ببطء."""
}
