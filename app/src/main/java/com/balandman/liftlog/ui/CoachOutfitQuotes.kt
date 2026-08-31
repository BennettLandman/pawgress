package com.balandman.liftlog.ui

import com.balandman.liftlog.data.CoachTheme

/**
 * One fixed, hand-written catchphrase per coach per seasonal outfit (20
 * coaches x 8 themes = 160 lines) — deliberately not random, so a given
 * coach+outfit combination always says the same line, in that coach's voice.
 * Keyed the same way as [com.balandman.liftlog.data.Profile.unlockedOutfits]
 * (see [com.balandman.liftlog.data.outfitKey]): "coachId:themeSlug".
 */
object CoachOutfitQuotes {

    private val QUOTES: Map<String, String> = mapOf(
        // 1. Coach Moose — gentle giant, patient, steady, big-brother hype
        "1:newyear" to "New year, same steady paws — let's build this one rep at a time.",
        "1:valentine" to "Sweetheart, the only thing you need to fall for today is good form.",
        "1:spring" to "Feel that? That's new growth season, and so are you.",
        "1:summer" to "Big guy, easy pace — we've got all summer to get strong together.",
        "1:backtoschool" to "Sharpen those pencils and that squat — back-to-basics starts now.",
        "1:halloween" to "Nothing spooky about hard work, kid. I've got your back.",
        "1:thanksgiving" to "Grateful for every rep you show up for — let's stack a few more.",
        "1:winterholiday" to "Bundle up, champ. Cozy sweater, steady gains.",

        // 2. Coach Noodle — zen, floppy-chill, consistency over intensity
        "2:newyear" to "No resolutions, just gentle repetition. That's the whole secret.",
        "2:valentine" to "Love yourself enough to just... show up. Softly.",
        "2:spring" to "Flop into spring like a noodle onto a warm windowsill.",
        "2:summer" to "Slow is smooth, smooth is a nice nap after this set.",
        "2:backtoschool" to "New backpack, same chill pace. Consistency, not chaos.",
        "2:halloween" to "Even ghosts need a routine. Boo. Now let's stretch.",
        "2:thanksgiving" to "Grateful for naps, gravy, and gentle reps. In that order.",
        "2:winterholiday" to "Warm blanket, warm muscles. Let's ease into it.",

        // 3. Duchess Marmalade — glamorous diva, red-carpet moment
        "3:newyear" to "New year, new sparkle. Darling, you were born for this glow-up.",
        "3:valentine" to "Every rep is a love letter to yourself. Chin up, dahling.",
        "3:spring" to "Bloom, darling. This is your runway season.",
        "3:summer" to "Sun's out, shine's out. Work it like the paparazzi are watching.",
        "3:backtoschool" to "Class is in session, and today's lesson is fabulousness under pressure.",
        "3:halloween" to "Even in a costume, darling, I never miss my mark.",
        "3:thanksgiving" to "Grateful for good lighting and great form. Now, pose — I mean, lift.",
        "3:winterholiday" to "Sequins on, effort up. Make this rep your best accessory.",

        // 4. Coach Pancake — cozy teddy bear, big on rest days
        "4:newyear" to "New year's resolution: more naps between sets. Kidding — mostly.",
        "4:valentine" to "Sending you a warm, fluffy hug and one more gentle rep.",
        "4:spring" to "Fresh air, soft grass, and just enough effort to feel proud.",
        "4:summer" to "Stay cool, stay cozy, stay consistent. That's the pancake way.",
        "4:backtoschool" to "Homework's done when the reps are done. Then, snack time.",
        "4:halloween" to "Costume's cute, but comfort always wins. Let's move, gently.",
        "4:thanksgiving" to "Grateful for stretchy pants and steady effort. Dig in — to the workout.",
        "4:winterholiday" to "Hot cocoa after, but first, one cozy little rep for me.",

        // 5. Coach Sprocket — mischievous pixie, silly dares, acrobatic
        "5:newyear" to "Dare you to beat last year's you. Go on, I'll watch and giggle.",
        "5:valentine" to "Betcha can't do a rep without smiling. Bet lost already.",
        "5:spring" to "Boing! Spring into it — literally, if you can manage a hop.",
        "5:summer" to "Cannonball into this workout like it's a pool. Splash some effort around.",
        "5:backtoschool" to "Pop quiz: can you finish this set before I count to ten? Go!",
        "5:halloween" to "Trick or treat — trick is it's actually a workout. Sneaky, right?",
        "5:thanksgiving" to "Race you to the finish before the turkey's ready. Ready, set, go!",
        "5:winterholiday" to "Snowball fight rules: one rep equals one snowball. Throw it!",

        // 6. Coach Blaze — intense athlete, competitive, PR-obsessed
        "6:newyear" to "New year, new PR. That's the only resolution that matters.",
        "6:valentine" to "Fall in love with the burn. Nothing else compares.",
        "6:spring" to "Winter's over — time to attack every single number on that board.",
        "6:summer" to "Heat's on. So are you. Chase that record.",
        "6:backtoschool" to "Recess is over. Time to out-train everyone in this gym.",
        "6:halloween" to "Ghosts don't scare me — missing a PR does. Let's go.",
        "6:thanksgiving" to "Earn every bite today. Reps first, seconds later.",
        "6:winterholiday" to "Cold outside, fire inside. Don't you dare coast this month.",

        // 7. Sir Reggie — dry, dignified stoic, deadpan
        "7:newyear" to "Ah. Another year. How thrilling. Proceed with your set.",
        "7:valentine" to "Romance is fleeting. Proper form, however, endures.",
        "7:spring" to "The flowers bloom. You, presumably, will also try.",
        "7:summer" to "It is warm. You are sweating. This appears to be working.",
        "7:backtoschool" to "Lesson one: effort. Lesson two: more effort. Class dismissed.",
        "7:halloween" to "I've worn scarier costumes to the vet. Carry on.",
        "7:thanksgiving" to "I am, in my own understated way, quite grateful you showed up.",
        "7:winterholiday" to "Tinsel is unnecessary. This rep, however, is not.",

        // 8. Coach Tundra — rugged outdoorsman, toughen-up grit
        "8:newyear" to "New year. Same cold, same grit. Get after it.",
        "8:valentine" to "Love is soft. This workout is not. Choose the workout.",
        "8:spring" to "Thaw's coming, but we never stopped training through winter.",
        "8:summer" to "Heat, cold, doesn't matter — the mountain doesn't care about excuses.",
        "8:backtoschool" to "Forget the classroom. This gym is the only lesson that toughens you up.",
        "8:halloween" to "Real monsters are the machines you've been avoiding. Face 'em.",
        "8:thanksgiving" to "Earned this meal one hard rep at a time. Keep earning it.",
        "8:winterholiday" to "Snow's falling, standards aren't. Bundle up and get moving.",

        // 9. Coach Buck — reliable everyday buddy, just show up
        "9:newyear" to "New year, same advice: just show up. That's the whole trick.",
        "9:valentine" to "No fancy stuff needed — just you, me, and one honest rep.",
        "9:spring" to "Nothing flashy. Just another day, another set, another win.",
        "9:summer" to "Hot out, sure, but we still show up. That's the deal.",
        "9:backtoschool" to "New schedule, same habit — you, here, doing the thing.",
        "9:halloween" to "No tricks today, buddy — just the treat of a good workout.",
        "9:thanksgiving" to "Grateful you're here. That's really the whole secret to progress.",
        "9:winterholiday" to "Busy season, I know. But you showed up — that's what counts.",

        // 10. Coach Sasha — quiet confidant, soft-spoken thoughtful
        "10:newyear" to "Quietly, steadily — this is how real change actually happens.",
        "10:valentine" to "You don't need anyone's approval today. Just your own effort.",
        "10:spring" to "Something's shifting in you, I think. Let's nurture it, gently.",
        "10:summer" to "Take a breath. Then take the rep. No rush.",
        "10:backtoschool" to "New season, new page. Write it one thoughtful rep at a time.",
        "10:halloween" to "No need for bravado tonight. Just honest, quiet work.",
        "10:thanksgiving" to "I'm glad you're here. Truly. Let's make today count, softly.",
        "10:winterholiday" to "In the quiet of winter, this is still worth doing.",

        // 11. Coach Mimi — loud chatterbox, nonstop hype
        "11:newyear" to "NEW YEAR, NEW YOU, SAME LOUD ME — LET'S GOOOO!",
        "11:valentine" to "I love you, I love this workout, I love EVERYTHING, LET'S MOVE!",
        "11:spring" to "SPRING IS HERE AND SO IS YOUR ENERGY, C'MON C'MON C'MON!",
        "11:summer" to "IT'S HOT AND YOU'RE HOTTER, KEEP GOING, KEEP GOING!",
        "11:backtoschool" to "NEW BACKPACK WHO DIS — OKAY FOCUS, ONE MORE REP!",
        "11:halloween" to "BOO! DID I SCARE YOU? NOW SCARE THAT MACHINE WITH EFFORT!",
        "11:thanksgiving" to "SO MUCH TO BE GRATEFUL FOR AND ALSO SO MANY REPS LEFT, GO!",
        "11:winterholiday" to "JINGLE THOSE BELLS AND THOSE MUSCLES, LET'S CELEBRATE WITH A SET!",

        // 12. Coach Rajah — wild jungle athlete, primal, adventurous
        "12:newyear" to "The jungle doesn't wait for resolutions. Hunt this rep down.",
        "12:valentine" to "Passion isn't soft — it's a roar. Bring that energy here.",
        "12:spring" to "The wild wakes up in spring. So does the beast in you.",
        "12:summer" to "This is our territory now. Prowl through this set.",
        "12:backtoschool" to "Forget the classroom — the gym is where the real instincts sharpen.",
        "12:halloween" to "I was born looking this fierce. Now match the energy.",
        "12:thanksgiving" to "The pride eats together after the hunt. Earn your seat at the table.",
        "12:winterholiday" to "Even in the cold season, the wild in you doesn't hibernate.",

        // 13. Coach Nova — bold extrovert, no filter, confident goofy
        "13:newyear" to "New year, zero chill, all confidence. Let's be ridiculous and strong.",
        "13:valentine" to "Be your own valentine and flex about it. No shame.",
        "13:spring" to "Spring cleaning? Nah, spring FLEXING. Watch this.",
        "13:summer" to "No shirt, no shame, all muscle. Let's get weird and strong.",
        "13:backtoschool" to "Show-and-tell time: show up, then tell everyone about your gains.",
        "13:halloween" to "My costume is buff cat. It's also just... every day. Let's go.",
        "13:thanksgiving" to "Grateful, loud, and about to lift something heavy. Watch me.",
        "13:winterholiday" to "Ugly sweater, pretty impressive lifts. Let's do this.",

        // 14. Coach Lotus — serene temple sage, mystical calm
        "14:newyear" to "The wheel turns once more. Move with it, not against it.",
        "14:valentine" to "Compassion begins with the self. Offer yourself this rep.",
        "14:spring" to "As the lotus rises from stillness, so do you, rep by rep.",
        "14:summer" to "Even in the heat, the calm center within you does not waver.",
        "14:backtoschool" to "A student begins again each day. Today, begin again.",
        "14:halloween" to "Do not fear the shadows — they only mean the light is near.",
        "14:thanksgiving" to "Gratitude is the quietest, strongest form of strength.",
        "14:winterholiday" to "In stillness, in cold, the practice continues undisturbed.",

        // 15. Coach Owlie — patient listener, gently funny
        "15:newyear" to "Whooo's ready for one more rep? You, obviously. Let's hear it.",
        "15:valentine" to "I'd give a hoot about anyone, but today it's about you, kid.",
        "15:spring" to "Spring has sprung, and so should you — right off that bench.",
        "15:summer" to "Too hot to hoot, but never too hot for one more set.",
        "15:backtoschool" to "Class is in session. Don't worry, there's no pop quiz — just reps.",
        "15:halloween" to "I'm nocturnal AND spooky. You're just stalling. Let's go.",
        "15:thanksgiving" to "I'm grateful for you, and also mildly for naps. Mostly you, though.",
        "15:winterholiday" to "It's cold, it's dark, and I'm still here listening. So — one more rep?",

        // 16. Coach Ragnar — epic Viking bravado
        "16:newyear" to "A new saga begins! Write yours in iron and sweat!",
        "16:valentine" to "Even warriors have soft hearts — but hard-earned muscles too!",
        "16:spring" to "The ice thaws! The Vikings rise! So do you, from that bench!",
        "16:summer" to "Sun blazes like Valhalla's own hearth! Onward, warrior!",
        "16:backtoschool" to "Even young warriors must train their minds AND their arms!",
        "16:halloween" to "Ghosts fear no Viking! Face this set with a battle cry!",
        "16:thanksgiving" to "A feast is earned, not given! Lift like the legends before you!",
        "16:winterholiday" to "Winter is when true warriors are forged! Onward, into the cold!",

        // 17. Coach Cleo — clever wit, puzzles and wordplay
        "17:newyear" to "New year, new-tron — sorry, new you. Let's solve this rep-quation.",
        "17:valentine" to "Roses are red, violets are blue, this superset's for you.",
        "17:spring" to "Spring forward — literally, into your next set.",
        "17:summer" to "It's not the heat, it's the humidity... and also your form. Fix both.",
        "17:backtoschool" to "Pop quiz: what's heavier than that dumbbell? Your excuses. Drop them.",
        "17:halloween" to "What did the ghost say to the barbell? Boo-tiful lift.",
        "17:thanksgiving" to "Let's talk turkey: you, me, one more clever rep.",
        "17:winterholiday" to "Tis the season to be jolly... and also to finish this set.",

        // 18. Coach Bo — loyal best friend, we're in this together
        "18:newyear" to "Whatever this year brings, buddy, we're doing it together.",
        "18:valentine" to "You've got a friend right here, every single rep of the way.",
        "18:spring" to "New season, same team. Let's grow together.",
        "18:summer" to "Long days, hot workouts — but hey, at least we've got each other.",
        "18:backtoschool" to "New routine, but you know I'm right here with you, always.",
        "18:halloween" to "Scary movies are better with a buddy. So is this workout.",
        "18:thanksgiving" to "Grateful for you, pal. Let's make today one to remember, together.",
        "18:winterholiday" to "Cold outside, warm friendship in here. Let's finish strong.",

        // 19. Coach Tonka — playful all-rounder, upbeat challenges
        "19:newyear" to "New year challenge unlocked! Let's see what you've got!",
        "19:valentine" to "Challenge of the day: fall in love with just one more rep!",
        "19:spring" to "Spring challenge: outrun the season into shape. Ready, set, go!",
        "19:summer" to "Summer challenge accepted before you even said yes. Let's move!",
        "19:backtoschool" to "Extra credit challenge: finish strong before the bell rings!",
        "19:halloween" to "Dare accepted: one more set, no tricks, all treats!",
        "19:thanksgiving" to "Challenge: earn dessert one rep at a time. I believe in you!",
        "19:winterholiday" to "Holiday challenge: keep the streak alive through the snow!",

        // 20. Coach Sultan — graceful perfectionist, technique over ego
        "20:newyear" to "New year, refined form. Precision over noise, always.",
        "20:valentine" to "Grace under pressure — that is the truest form of strength.",
        "20:spring" to "Every movement should bloom with control, not chaos.",
        "20:summer" to "Even in the heat, elegance in form is non-negotiable.",
        "20:backtoschool" to "Master the fundamentals first. Flourish comes later.",
        "20:halloween" to "No gimmicks, no costumes needed — just impeccable technique.",
        "20:thanksgiving" to "Gratitude, like good form, is best expressed with quiet precision.",
        "20:winterholiday" to "Even bundled in layers, your form should remain flawless.",
    )

    /**
     * One fixed catchphrase per coach for their base (un-costumed) look —
     * deliberately distinct from [com.balandman.liftlog.data.Coach.personality],
     * which is a third-person descriptor shown elsewhere on the card. This is
     * what the coach actually "says" when no outfit is being previewed.
     */
    private val BASE_QUOTES: Map<Int, String> = mapOf(
        1 to "One step at a time, kid — I've got nowhere else I'd rather be.",
        2 to "No rush. We'll get there exactly on time, however long it takes.",
        3 to "Darling, effortless doesn't mean easy — now let's begin.",
        4 to "Comfy shorts, steady effort. That's the whole plan today.",
        5 to "Bet you can't make this next rep look boring. Go on, try.",
        6 to "Every rep here is a number I'm going to beat later.",
        7 to "I shall observe. You shall perspire. A fair arrangement.",
        8 to "No shortcuts out here. Just you, the weight, and the will to move it.",
        9 to "Same me, same you, same good habit. Let's get to it.",
        10 to "Whenever you're ready. I'm not going anywhere.",
        11 to "OKAY OKAY OKAY LET'S GOOO I'VE BEEN WAITING ALL DAY!",
        12 to "The hunt begins the moment you pick up that first weight.",
        13 to "Confidence is free. Good form? Also free. Let's use both.",
        14 to "Breathe in effort, breathe out doubt. Begin.",
        15 to "I've got all night. Literally. Let's hear that first rep.",
        16 to "Every legend started with one unimpressive first rep. Onward!",
        17 to "Let's do the math: effort plus consistency equals results. Simple.",
        18 to "Right here, right next to you, like always. Let's go.",
        19 to "New day, new challenge, same energy. Let's see what you've got!",
        20 to "Precision first. Power will follow naturally.",
    )

    /** This coach+theme's fixed catchphrase, or null if somehow not written yet. */
    fun quoteFor(coachId: Int, theme: CoachTheme): String? = QUOTES["$coachId:${theme.slug}"]

    /** This coach's fixed catchphrase for their base look, or null if not written yet. */
    fun baseQuoteFor(coachId: Int): String? = BASE_QUOTES[coachId]
}
