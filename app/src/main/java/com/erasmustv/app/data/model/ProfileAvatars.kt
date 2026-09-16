package com.erasmustv.app.data.model

data class AvatarItem(
    val id: String,
    val name: String,
    val show: String,
    val fileName: String
) {
    val assetUrl: String get() = "file:///android_asset/avatars/" + fileName
}

data class AvatarCategory(
    val title: String,
    val avatars: List<AvatarItem>
)

object ProfileAvatarRegistry {
    private val cat_stranger_things = AvatarCategory(
        title = "STRANGER THINGS",
        avatars = listOf(
            AvatarItem("avatar_065", "Eleven", "Stranger Things", "avatar_065_eleven.png"),
            AvatarItem("avatar_066", "Mike", "Stranger Things", "avatar_066_mike.png"),
            AvatarItem("avatar_067", "Lucas", "Stranger Things", "avatar_067_lucas.png"),
            AvatarItem("avatar_068", "Will", "Stranger Things", "avatar_068_will.png"),
            AvatarItem("avatar_069", "Dustin", "Stranger Things", "avatar_069_dustin.png"),
            AvatarItem("avatar_070", "Steve", "Stranger Things", "avatar_070_steve.png"),
            AvatarItem("avatar_071", "Nancy", "Stranger Things", "avatar_071_nancy.png"),
            AvatarItem("avatar_072", "Jonathan", "Stranger Things", "avatar_072_jonathan.png"),
            AvatarItem("avatar_073", "Joyce", "Stranger Things", "avatar_073_joyce.png"),
            AvatarItem("avatar_074", "Hopper", "Stranger Things", "avatar_074_hopper.png"),
            AvatarItem("avatar_075", "Demogorgon", "Stranger Things", "avatar_075_demogorgon.png"),
            AvatarItem("avatar_077", "Billy", "Stranger Things", "avatar_077_billy.png"),
            AvatarItem("avatar_078", "Demogorgon (2)", "Stranger Things", "avatar_078_demogorgon_2.png"),
            AvatarItem("avatar_079", "Dustin (2)", "Stranger Things", "avatar_079_dustin_2.png"),
            AvatarItem("avatar_080", "Dustin_s Hat", "Stranger Things", "avatar_080_dustin_s_hat.png"),
            AvatarItem("avatar_081", "Hopper _RARE Never Officially Released_", "Stranger Things", "avatar_081_hopper__rare_never_officially_released_.png"),
            AvatarItem("avatar_082", "Jonathan (2)", "Stranger Things", "avatar_082_jonathan_2.png"),
            AvatarItem("avatar_083", "Joyce _RARE Never Officially Released_", "Stranger Things", "avatar_083_joyce__rare_never_officially_released_.png"),
            AvatarItem("avatar_084", "Lucas (2)", "Stranger Things", "avatar_084_lucas_2.png"),
            AvatarItem("avatar_085", "Max", "Stranger Things", "avatar_085_max.png"),
            AvatarItem("avatar_086", "Mike (2)", "Stranger Things", "avatar_086_mike_2.png"),
            AvatarItem("avatar_087", "Nancy (2)", "Stranger Things", "avatar_087_nancy_2.png"),
            AvatarItem("avatar_088", "Steve (2)", "Stranger Things", "avatar_088_steve_2.png"),
            AvatarItem("avatar_089", "Will (2)", "Stranger Things", "avatar_089_will_2.png"),
            AvatarItem("avatar_091", "Dustin (3)", "Stranger Things", "avatar_091_dustin_3.png"),
            AvatarItem("avatar_092", "Eleven (2)", "Stranger Things", "avatar_092_eleven_2.png"),
            AvatarItem("avatar_093", "Erica", "Stranger Things", "avatar_093_erica.png"),
            AvatarItem("avatar_094", "Hopper (2)", "Stranger Things", "avatar_094_hopper_2.png"),
            AvatarItem("avatar_095", "Jonathan (3)", "Stranger Things", "avatar_095_jonathan_3.png"),
            AvatarItem("avatar_096", "Joyce (2)", "Stranger Things", "avatar_096_joyce_2.png"),
            AvatarItem("avatar_097", "Lucas (3)", "Stranger Things", "avatar_097_lucas_3.png"),
            AvatarItem("avatar_098", "Max (2)", "Stranger Things", "avatar_098_max_2.png"),
            AvatarItem("avatar_099", "Nancy (3)", "Stranger Things", "avatar_099_nancy_3.png"),
            AvatarItem("avatar_100", "Robin", "Stranger Things", "avatar_100_robin.png"),
            AvatarItem("avatar_101", "Steve (3)", "Stranger Things", "avatar_101_steve_3.png"),
            AvatarItem("avatar_102", "Robin _Upside Down_", "Stranger Things", "avatar_102_robin__upside_down_.png"),
            AvatarItem("avatar_103", "Steve _Upside Down_", "Stranger Things", "avatar_103_steve__upside_down_.png"),
            AvatarItem("avatar_105", "Eleven (3)", "Stranger Things", "avatar_105_eleven_3.png"),
            AvatarItem("avatar_106", "Dustin (4)", "Stranger Things", "avatar_106_dustin_4.png"),
            AvatarItem("avatar_107", "Mike (3)", "Stranger Things", "avatar_107_mike_3.png"),
            AvatarItem("avatar_108", "Lucas (4)", "Stranger Things", "avatar_108_lucas_4.png"),
            AvatarItem("avatar_109", "Will (3)", "Stranger Things", "avatar_109_will_3.png"),
            AvatarItem("avatar_110", "Max (3)", "Stranger Things", "avatar_110_max_3.png"),
            AvatarItem("avatar_111", "Hopper (3)", "Stranger Things", "avatar_111_hopper_3.png"),
            AvatarItem("avatar_112", "Nancy (4)", "Stranger Things", "avatar_112_nancy_4.png"),
            AvatarItem("avatar_113", "Joyce (3)", "Stranger Things", "avatar_113_joyce_3.png"),
            AvatarItem("avatar_114", "Steve (4)", "Stranger Things", "avatar_114_steve_4.png"),
            AvatarItem("avatar_115", "Eddie", "Stranger Things", "avatar_115_eddie.png"),
            AvatarItem("avatar_116", "Robbin", "Stranger Things", "avatar_116_robbin.png"),
            AvatarItem("avatar_117", "Jonathan (4)", "Stranger Things", "avatar_117_jonathan_4.png"),
            AvatarItem("avatar_118", "Erica (2)", "Stranger Things", "avatar_118_erica_2.png"),
            AvatarItem("avatar_119", "Argyle", "Stranger Things", "avatar_119_argyle.png"),
            AvatarItem("avatar_120", "Vecna", "Stranger Things", "avatar_120_vecna.png"),
            AvatarItem("avatar_121", "Murray", "Stranger Things", "avatar_121_murray.png"),
            AvatarItem("avatar_122", "Karen Wheeler", "Stranger Things", "avatar_122_karen_wheeler.png"),
        )
    )

    private val cat_squid_game = AvatarCategory(
        title = "SQUID GAME",
        avatars = listOf(
            AvatarItem("avatar_140", "Deok-su", "Squid Game", "avatar_140_deok_su.png"),
            AvatarItem("avatar_141", "Mi-nyeo", "Squid Game", "avatar_141_mi_nyeo.png"),
            AvatarItem("avatar_142", "Sae-Byeok", "Squid Game", "avatar_142_sae_byeok.png"),
            AvatarItem("avatar_144", "Front Man", "Squid Game", "avatar_144_front_man.png"),
            AvatarItem("avatar_145", "Masked Manager", "Squid Game", "avatar_145_masked_manager.png"),
            AvatarItem("avatar_146", "Masked Soldier", "Squid Game", "avatar_146_masked_soldier.png"),
            AvatarItem("avatar_147", "Masked Worker", "Squid Game", "avatar_147_masked_worker.png"),
            AvatarItem("avatar_148", "Masked Officer", "Squid Game", "avatar_148_masked_officer.png"),
            AvatarItem("avatar_149", "Young-hee", "Squid Game", "avatar_149_young_hee.png"),
            AvatarItem("avatar_150", "O Button", "Squid Game", "avatar_150_o_button.png"),
            AvatarItem("avatar_151", "X Button", "Squid Game", "avatar_151_x_button.png"),
            AvatarItem("avatar_152", "Gi Hun", "Squid Game", "avatar_152_gi_hun.png"),
            AvatarItem("avatar_153", "Gi Hun Season 2", "Squid Game", "avatar_153_gi_hun_season_2.png"),
            AvatarItem("avatar_154", "In-ho", "Squid Game", "avatar_154_in_ho.png"),
            AvatarItem("avatar_155", "Myung-gi", "Squid Game", "avatar_155_myung_gi.png"),
            AvatarItem("avatar_156", "Hyun-ju", "Squid Game", "avatar_156_hyun_ju.png"),
            AvatarItem("avatar_157", "Thanos", "Squid Game", "avatar_157_thanos.png"),
            AvatarItem("avatar_158", "Jun-hee", "Squid Game", "avatar_158_jun_hee.png"),
            AvatarItem("avatar_159", "Jun-ho", "Squid Game", "avatar_159_jun_ho.png"),
            AvatarItem("avatar_160", "Recruiter", "Squid Game", "avatar_160_recruiter.png"),
            AvatarItem("avatar_161", "No-Eul", "Squid Game", "avatar_161_no_eul.png"),
            AvatarItem("avatar_162", "Piggy Bank", "Squid Game", "avatar_162_piggy_bank.png"),
            AvatarItem("avatar_163", "Dalgona", "Squid Game", "avatar_163_dalgona.png"),
            AvatarItem("avatar_164", "Carousel Horse", "Squid Game", "avatar_164_carousel_horse.png"),
            AvatarItem("avatar_165", "Gong-gi", "Squid Game", "avatar_165_gong_gi.png"),
        )
    )

    private val cat_cobra_kai = AvatarCategory(
        title = "COBRA KAI",
        avatars = listOf(
            AvatarItem("avatar_048", "Daniel", "Cobra Kai", "avatar_048_daniel.png"),
            AvatarItem("avatar_049", "Johnny", "Cobra Kai", "avatar_049_johnny.png"),
            AvatarItem("avatar_050", "Kreese", "Cobra Kai", "avatar_050_kreese.png"),
            AvatarItem("avatar_051", "Miguel", "Cobra Kai", "avatar_051_miguel.png"),
            AvatarItem("avatar_052", "Robbie", "Cobra Kai", "avatar_052_robbie.png"),
            AvatarItem("avatar_053", "Samantha", "Cobra Kai", "avatar_053_samantha.png"),
            AvatarItem("avatar_054", "Tory", "Cobra Kai", "avatar_054_tory.png"),
            AvatarItem("avatar_055", "Hawk", "Cobra Kai", "avatar_055_hawk.png"),
            AvatarItem("avatar_056", "Demetri", "Cobra Kai", "avatar_056_demetri.png"),
            AvatarItem("avatar_057", "Terry Silver", "Cobra Kai", "avatar_057_terry_silver.png"),
            AvatarItem("avatar_058", "Amanda", "Cobra Kai", "avatar_058_amanda.png"),
            AvatarItem("avatar_059", "Carmen", "Cobra Kai", "avatar_059_carmen.png"),
            AvatarItem("avatar_060", "Mr. Miyagi", "Cobra Kai", "avatar_060_mr_miyagi.png"),
            AvatarItem("avatar_061", "Cobra Kai", "Cobra Kai", "avatar_061_cobra_kai.png"),
            AvatarItem("avatar_062", "Miyagi-Do", "Cobra Kai", "avatar_062_miyagi_do.png"),
        )
    )

    private val cat_game_of_thrones = AvatarCategory(
        title = "GAME OF THRONES",
        avatars = listOf(
            AvatarItem("avatar_324", "Daenerys Targaryen", "Game of Thrones", "avatar_324_daenerys_targaryen.png"),
            AvatarItem("avatar_326", "Jon Snow", "Game of Thrones", "avatar_326_jon_snow.png"),
            AvatarItem("avatar_328", "Tyrion Lannister", "Game of Thrones", "avatar_328_tyrion_lannister.png"),
            AvatarItem("avatar_329", "Arya Stark", "Game of Thrones", "avatar_329_arya_stark.png"),
            AvatarItem("avatar_330", "Sansa Stark", "Game of Thrones", "avatar_330_sansa_stark.png"),
            AvatarItem("avatar_340", "Petyr Baelish", "Game of Thrones", "avatar_340_petyr_baelish.png"),
        )
    )

    private val cat_house_of_the_dragon = AvatarCategory(
        title = "HOUSE OF THE DRAGON",
        avatars = listOf(
            AvatarItem("avatar_331", "Viserys Targaryen", "House of the Dragon", "avatar_331_viserys_targaryen.png"),
            AvatarItem("avatar_332", "Rhaenyra Targaryen", "House of the Dragon", "avatar_332_rhaenyra_targaryen.png"),
            AvatarItem("avatar_334", "Alicent Hightower", "House of the Dragon", "avatar_334_alicent_hightower.png"),
        )
    )

    private val cat_the_crown = AvatarCategory(
        title = "THE CROWN",
        avatars = listOf(
            AvatarItem("avatar_125", "Corgi", "The Crown", "avatar_125_corgi.png"),
            AvatarItem("avatar_126", "Claire Foy", "The Crown", "avatar_126_claire_foy.png"),
            AvatarItem("avatar_127", "Olivia Coleman", "The Crown", "avatar_127_olivia_coleman.png"),
            AvatarItem("avatar_128", "Imelda Staunton", "The Crown", "avatar_128_imelda_staunton.png"),
            AvatarItem("avatar_129", "Josh O_Connor", "The Crown", "avatar_129_josh_o_connor.png"),
            AvatarItem("avatar_130", "Dominic West", "The Crown", "avatar_130_dominic_west.png"),
            AvatarItem("avatar_131", "Lady Diana", "The Crown", "avatar_131_lady_diana.png"),
            AvatarItem("avatar_132", "Princess Diana", "The Crown", "avatar_132_princess_diana.png"),
            AvatarItem("avatar_133", "Matt Smith", "The Crown", "avatar_133_matt_smith.png"),
            AvatarItem("avatar_134", "Tobias Menzies", "The Crown", "avatar_134_tobias_menzies.png"),
            AvatarItem("avatar_135", "Jonathan Pryce", "The Crown", "avatar_135_jonathan_pryce.png"),
            AvatarItem("avatar_136", "Vanessa Kirby", "The Crown", "avatar_136_vanessa_kirby.png"),
            AvatarItem("avatar_137", "Helena Bonham Carter", "The Crown", "avatar_137_helena_bonham_carter.png"),
            AvatarItem("avatar_138", "Lesley Manville", "The Crown", "avatar_138_lesley_manville.png"),
        )
    )

    private val cat_our_planet = AvatarCategory(
        title = "OUR PLANET",
        avatars = listOf(
            AvatarItem("avatar_034", "Tiger", "Our Planet", "avatar_034_tiger.png"),
            AvatarItem("avatar_035", "Rhino", "Our Planet", "avatar_035_rhino.png"),
            AvatarItem("avatar_036", "Frog", "Our Planet", "avatar_036_frog.png"),
            AvatarItem("avatar_037", "Fox", "Our Planet", "avatar_037_fox.png"),
            AvatarItem("avatar_038", "Penguin", "Our Planet", "avatar_038_penguin.png"),
            AvatarItem("avatar_039", "Turtle", "Our Planet", "avatar_039_turtle.png"),
            AvatarItem("avatar_040", "Monkey", "Our Planet", "avatar_040_monkey.png"),
            AvatarItem("avatar_041", "Capybara", "Our Planet", "avatar_041_capybara.png"),
            AvatarItem("avatar_042", "Butterfly", "Our Planet", "avatar_042_butterfly.png"),
            AvatarItem("avatar_043", "Polar Bear", "Our Planet", "avatar_043_polar_bear.png"),
            AvatarItem("avatar_044", "Shark", "Our Planet", "avatar_044_shark.png"),
            AvatarItem("avatar_045", "Orangutan", "Our Planet", "avatar_045_orangutan.png"),
            AvatarItem("avatar_046", "Bird", "Our Planet", "avatar_046_bird.png"),
        )
    )

    private val cat_lupin = AvatarCategory(
        title = "LUPIN",
        avatars = listOf(
            AvatarItem("avatar_201", "Assane Cabbie _the gentleman theif_", "Lupin", "avatar_201_assane_cabbie__the_gentleman_theif_.png"),
            AvatarItem("avatar_202", "Assane Pimp _the Sapeur_", "Lupin", "avatar_202_assane_pimp__the_sapeur_.png"),
            AvatarItem("avatar_203", "Assane Beanie _the Trash Collector_", "Lupin", "avatar_203_assane_beanie__the_trash_collector_.png"),
            AvatarItem("avatar_204", "Assane Grey Beard _the Suave Man_", "Lupin", "avatar_204_assane_grey_beard__the_suave_man_.png"),
            AvatarItem("avatar_205", "Assane Platinum Hair _the Security Guard_", "Lupin", "avatar_205_assane_platinum_hair__the_security_guard_.png"),
            AvatarItem("avatar_206", "Assane Trilby _ Shades _the Hat_", "Lupin", "avatar_206_assane_trilby___shades__the_hat_.png"),
            AvatarItem("avatar_207", "Lupin The Black Pearl", "Lupin", "avatar_207_lupin_the_black_pearl.png"),
            AvatarItem("avatar_208", "Assane the Gangster", "Lupin", "avatar_208_assane_the_gangster.png"),
            AvatarItem("avatar_209", "Assane the Geek", "Lupin", "avatar_209_assane_the_geek.png"),
            AvatarItem("avatar_210", "Assane the Businessman", "Lupin", "avatar_210_assane_the_businessman.png"),
            AvatarItem("avatar_211", "Lupin J_accuse", "Lupin", "avatar_211_lupin_j_accuse.png"),
            AvatarItem("avatar_212", "Assane the old man", "Lupin", "avatar_212_assane_the_old_man.png"),
        )
    )

    private val cat_heeramandi = AvatarCategory(
        title = "HEERAMANDI",
        avatars = listOf(
            AvatarItem("avatar_246", "Mallikajaan", "Heeramandi", "avatar_246_mallikajaan.png"),
            AvatarItem("avatar_247", "Fareedan", "Heeramandi", "avatar_247_fareedan.png"),
            AvatarItem("avatar_248", "Bibbojaan", "Heeramandi", "avatar_248_bibbojaan.png"),
            AvatarItem("avatar_249", "Alamzeb", "Heeramandi", "avatar_249_alamzeb.png"),
            AvatarItem("avatar_250", "Lajjo", "Heeramandi", "avatar_250_lajjo.png"),
            AvatarItem("avatar_251", "Tajdar Baloch", "Heeramandi", "avatar_251_tajdar_baloch.png"),
            AvatarItem("avatar_252", "Zorawar", "Heeramandi", "avatar_252_zorawar.png"),
            AvatarItem("avatar_253", "Waheeda", "Heeramandi", "avatar_253_waheeda.png"),
            AvatarItem("avatar_254", "Zulfikar", "Heeramandi", "avatar_254_zulfikar.png"),
            AvatarItem("avatar_255", "Wali Mohammed", "Heeramandi", "avatar_255_wali_mohammed.png"),
            AvatarItem("avatar_256", "Alastair Cartwright", "Heeramandi", "avatar_256_alastair_cartwright.png"),
            AvatarItem("avatar_257", "Ustaadji", "Heeramandi", "avatar_257_ustaadji.png"),
            AvatarItem("avatar_258", "Qudsia Begum", "Heeramandi", "avatar_258_qudsia_begum.png"),
        )
    )

    private val cat_wwe = AvatarCategory(
        title = "WWE",
        avatars = listOf(
            AvatarItem("avatar_237", "Roman Reigns", "WWE", "avatar_237_roman_reigns.png"),
            AvatarItem("avatar_238", "John Cena", "WWE", "avatar_238_john_cena.png"),
            AvatarItem("avatar_239", "Cody Rhodes", "WWE", "avatar_239_cody_rhodes.png"),
            AvatarItem("avatar_240", "Bianca Belair", "WWE", "avatar_240_bianca_belair.png"),
            AvatarItem("avatar_241", "Liv Morgan", "WWE", "avatar_241_liv_morgan.png"),
            AvatarItem("avatar_242", "Rey Mysterio", "WWE", "avatar_242_rey_mysterio.png"),
            AvatarItem("avatar_243", "CM Punk", "WWE", "avatar_243_cm_punk.png"),
            AvatarItem("avatar_244", "Rhea Ripley", "WWE", "avatar_244_rhea_ripley.png"),
        )
    )

    private val cat_aggretsuko = AvatarCategory(
        title = "AGGRETSUKO",
        avatars = listOf(
            AvatarItem("avatar_167", "Death Metal Retsuko", "Aggretsuko", "avatar_167_death_metal_retsuko.png"),
            AvatarItem("avatar_168", "Retsuko", "Aggretsuko", "avatar_168_retsuko.png"),
            AvatarItem("avatar_169", "Haida", "Aggretsuko", "avatar_169_haida.png"),
            AvatarItem("avatar_170", "Feneko", "Aggretsuko", "avatar_170_feneko.png"),
            AvatarItem("avatar_171", "Washimi", "Aggretsuko", "avatar_171_washimi.png"),
            AvatarItem("avatar_172", "Gori", "Aggretsuko", "avatar_172_gori.png"),
            AvatarItem("avatar_173", "Tadano", "Aggretsuko", "avatar_173_tadano.png"),
            AvatarItem("avatar_174", "Kabae", "Aggretsuko", "avatar_174_kabae.png"),
            AvatarItem("avatar_175", "Anai", "Aggretsuko", "avatar_175_anai.png"),
            AvatarItem("avatar_176", "Tsunoda", "Aggretsuko", "avatar_176_tsunoda.png"),
            AvatarItem("avatar_177", "Tsubone", "Aggretsuko", "avatar_177_tsubone.png"),
            AvatarItem("avatar_178", "Ton", "Aggretsuko", "avatar_178_ton.png"),
            AvatarItem("avatar_179", "Komiya", "Aggretsuko", "avatar_179_komiya.png"),
            AvatarItem("avatar_180", "Resasuke", "Aggretsuko", "avatar_180_resasuke.png"),
            AvatarItem("avatar_181", "Puko", "Aggretsuko", "avatar_181_puko.png"),
            AvatarItem("avatar_182", "Retsuko_s Mother", "Aggretsuko", "avatar_182_retsuko_s_mother.png"),
            AvatarItem("avatar_183", "Karaoke Microphone", "Aggretsuko", "avatar_183_karaoke_microphone.png"),
        )
    )

    private val cat_jurassic_world_camp_cretaceous = AvatarCategory(
        title = "JURASSIC WORLD: CAMP CRETACEOUS",
        avatars = listOf(
            AvatarItem("avatar_268", "Allosaurus", "Jurassic World: Camp Cretaceous", "avatar_268_allosaurus.png"),
            AvatarItem("avatar_269", "Ghost", "Jurassic World: Camp Cretaceous", "avatar_269_ghost.png"),
            AvatarItem("avatar_270", "Bumpy", "Jurassic World: Camp Cretaceous", "avatar_270_bumpy.png"),
            AvatarItem("avatar_271", "Atrociraptor", "Jurassic World: Camp Cretaceous", "avatar_271_atrociraptor.png"),
            AvatarItem("avatar_272", "Nasu", "Jurassic World: Camp Cretaceous", "avatar_272_nasu.png"),
            AvatarItem("avatar_273", "TRex", "Jurassic World: Camp Cretaceous", "avatar_273_trex.png"),
            AvatarItem("avatar_274", "Darius", "Jurassic World: Camp Cretaceous", "avatar_274_darius.png"),
            AvatarItem("avatar_275", "Ben", "Jurassic World: Camp Cretaceous", "avatar_275_ben.png"),
            AvatarItem("avatar_276", "Kenji", "Jurassic World: Camp Cretaceous", "avatar_276_kenji.png"),
            AvatarItem("avatar_277", "Sammy", "Jurassic World: Camp Cretaceous", "avatar_277_sammy.png"),
            AvatarItem("avatar_278", "Yaz", "Jurassic World: Camp Cretaceous", "avatar_278_yaz.png"),
            AvatarItem("avatar_279", "Brooklynn", "Jurassic World: Camp Cretaceous", "avatar_279_brooklynn.png"),
        )
    )

    private val cat_fuller_house = AvatarCategory(
        title = "FULLER HOUSE",
        avatars = listOf(
            AvatarItem("avatar_281", "DJ", "Fuller House", "avatar_281_dj.png"),
            AvatarItem("avatar_282", "Stephanie", "Fuller House", "avatar_282_stephanie.png"),
            AvatarItem("avatar_283", "Kimmy", "Fuller House", "avatar_283_kimmy.png"),
            AvatarItem("avatar_284", "Jackson", "Fuller House", "avatar_284_jackson.png"),
            AvatarItem("avatar_285", "Max (4)", "Fuller House", "avatar_285_max_4.png"),
            AvatarItem("avatar_286", "Ramona", "Fuller House", "avatar_286_ramona.png"),
            AvatarItem("avatar_287", "Cosmo", "Fuller House", "avatar_287_cosmo.png"),
            AvatarItem("avatar_288", "House", "Fuller House", "avatar_288_house.png"),
        )
    )

    private val cat_voltron_legendary_defender = AvatarCategory(
        title = "VOLTRON: LEGENDARY DEFENDER",
        avatars = listOf(
            AvatarItem("avatar_222", "Keith _Red Lion_", "Voltron: Legendary Defender", "avatar_222_keith__red_lion_.png"),
            AvatarItem("avatar_223", "Lance _Blue Lion_", "Voltron: Legendary Defender", "avatar_223_lance__blue_lion_.png"),
            AvatarItem("avatar_224", "Pidge _Green Lion_", "Voltron: Legendary Defender", "avatar_224_pidge__green_lion_.png"),
            AvatarItem("avatar_225", "Allura", "Voltron: Legendary Defender", "avatar_225_allura.png"),
            AvatarItem("avatar_226", "Shiro _Black Lion_", "Voltron: Legendary Defender", "avatar_226_shiro__black_lion_.png"),
            AvatarItem("avatar_227", "Haggar", "Voltron: Legendary Defender", "avatar_227_haggar.png"),
            AvatarItem("avatar_228", "Voltron", "Voltron: Legendary Defender", "avatar_228_voltron.png"),
            AvatarItem("avatar_229", "Hunk _Yellow Lion_", "Voltron: Legendary Defender", "avatar_229_hunk__yellow_lion_.png"),
        )
    )

    private val cat_she_ra_and_the_princesses_of_power = AvatarCategory(
        title = "SHE-RA AND THE PRINCESSES OF POWER",
        avatars = listOf(
            AvatarItem("avatar_214", "She-Ra", "She-Ra and the Princesses of Power", "avatar_214_she_ra.png"),
            AvatarItem("avatar_215", "Adora", "She-Ra and the Princesses of Power", "avatar_215_adora.png"),
            AvatarItem("avatar_216", "Swift Wind", "She-Ra and the Princesses of Power", "avatar_216_swift_wind.png"),
            AvatarItem("avatar_217", "Catra", "She-Ra and the Princesses of Power", "avatar_217_catra.png"),
            AvatarItem("avatar_218", "Bow", "She-Ra and the Princesses of Power", "avatar_218_bow.png"),
            AvatarItem("avatar_219", "Glimmer", "She-Ra and the Princesses of Power", "avatar_219_glimmer.png"),
            AvatarItem("avatar_220", "Shadow Weaver", "She-Ra and the Princesses of Power", "avatar_220_shadow_weaver.png"),
        )
    )

    private val cat_lost_in_space = AvatarCategory(
        title = "LOST IN SPACE",
        avatars = listOf(
            AvatarItem("avatar_191", "John", "Lost in Space", "avatar_191_john.png"),
            AvatarItem("avatar_192", "Maureen", "Lost in Space", "avatar_192_maureen.png"),
            AvatarItem("avatar_193", "Judy", "Lost in Space", "avatar_193_judy.png"),
            AvatarItem("avatar_194", "Penny", "Lost in Space", "avatar_194_penny.png"),
            AvatarItem("avatar_195", "Will (4)", "Lost in Space", "avatar_195_will_4.png"),
            AvatarItem("avatar_196", "Don West", "Lost in Space", "avatar_196_don_west.png"),
            AvatarItem("avatar_197", "Dr. Smith", "Lost in Space", "avatar_197_dr_smith.png"),
            AvatarItem("avatar_198", "Robot", "Lost in Space", "avatar_198_robot.png"),
            AvatarItem("avatar_199", "Chicken LIS", "Lost in Space", "avatar_199_chicken_lis.png"),
        )
    )

    private val cat_shaun_the_sheep = AvatarCategory(
        title = "SHAUN THE SHEEP",
        avatars = listOf(
            AvatarItem("avatar_260", "Shaun", "Shaun the Sheep", "avatar_260_shaun.png"),
            AvatarItem("avatar_261", "Bitzer", "Shaun the Sheep", "avatar_261_bitzer.png"),
            AvatarItem("avatar_262", "Farmer", "Shaun the Sheep", "avatar_262_farmer.png"),
            AvatarItem("avatar_263", "Timmy", "Shaun the Sheep", "avatar_263_timmy.png"),
            AvatarItem("avatar_264", "Shirley", "Shaun the Sheep", "avatar_264_shirley.png"),
            AvatarItem("avatar_265", "Pig", "Shaun the Sheep", "avatar_265_pig.png"),
            AvatarItem("avatar_266", "Stash", "Shaun the Sheep", "avatar_266_stash.png"),
        )
    )

    private val cat_castlevania = AvatarCategory(
        title = "CASTLEVANIA",
        avatars = listOf(
            AvatarItem("avatar_185", "Alucard", "Castlevania", "avatar_185_alucard.png"),
            AvatarItem("avatar_186", "Dracula", "Castlevania", "avatar_186_dracula.png"),
            AvatarItem("avatar_187", "Trevor", "Castlevania", "avatar_187_trevor.png"),
        )
    )

    private val cat_the_end_of_the_fing_world = AvatarCategory(
        title = "THE END OF THE F***ING WORLD",
        avatars = listOf(
            AvatarItem("avatar_231", "James", "The End of the F***ing World", "avatar_231_james.png"),
            AvatarItem("avatar_232", "James In Disguise _Hawaiian Shirt_", "The End of the F***ing World", "avatar_232_james_in_disguise__hawaiian_shirt_.png"),
            AvatarItem("avatar_233", "Alyssa", "The End of the F***ing World", "avatar_233_alyssa.png"),
        )
    )

    private val cat_cowboy_bebop = AvatarCategory(
        title = "COWBOY BEBOP",
        avatars = listOf(
            AvatarItem("avatar_189", "Ein", "Cowboy Bebop", "avatar_189_ein.png"),
        )
    )

    private val cat_unbreakable_kimmy_schmidt = AvatarCategory(
        title = "UNBREAKABLE KIMMY SCHMIDT",
        avatars = listOf(
            AvatarItem("avatar_235", "Titus", "Unbreakable Kimmy Schmidt", "avatar_235_titus.png"),
        )
    )

    private val cat_miss_americana = AvatarCategory(
        title = "MISS AMERICANA",
        avatars = listOf(
            AvatarItem("avatar_032", "Taylor Swift", "Miss Americana", "avatar_032_taylor_swift.png"),
        )
    )

    val categories: List<AvatarCategory> = listOf(
        cat_stranger_things,
        cat_squid_game,
        cat_cobra_kai,
        cat_game_of_thrones,
        cat_house_of_the_dragon,
        cat_the_crown,
        cat_our_planet,
        cat_lupin,
        cat_heeramandi,
        cat_wwe,
        cat_aggretsuko,
        cat_jurassic_world_camp_cretaceous,
        cat_fuller_house,
        cat_voltron_legendary_defender,
        cat_she_ra_and_the_princesses_of_power,
        cat_lost_in_space,
        cat_shaun_the_sheep,
        cat_castlevania,
        cat_the_end_of_the_fing_world,
        cat_cowboy_bebop,
        cat_unbreakable_kimmy_schmidt,
        cat_miss_americana,
    )

    val allAvatars: List<AvatarItem> by lazy { categories.flatMap { it.avatars } }

    fun findByFileName(fileName: String): AvatarItem? {
        return allAvatars.firstOrNull { it.fileName == fileName }
    }

    fun findById(id: String): AvatarItem? {
        return allAvatars.firstOrNull { it.id == id }
    }
}
