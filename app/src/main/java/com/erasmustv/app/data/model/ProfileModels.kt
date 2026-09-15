package com.erasmustv.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchProfile(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val name: String,
    @SerialName("avatar_key") val avatarKey: String = "slate",
    @SerialName("birth_year") val birthYear: Int? = null,
    val preferences: ProfilePreferences = ProfilePreferences(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val derivedAge: Int
        get() {
            val year = birthYear ?: return 18
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            return (currentYear - year).coerceAtLeast(0)
        }

    val isKidsProfile: Boolean get() = derivedAge < 13
}

@Serializable
data class ProfilePreferences(
    val watchRegion: String? = "US",
    val language: String? = "en"
)

@Serializable
data class CreateProfileRequest(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("avatar_key") val avatarKey: String = "slate",
    @SerialName("birth_year") val birthYear: Int? = null
)

data class AvatarOption(
    val key: String,
    val label: String
)

val AVATAR_PRESETS = listOf(
    AvatarOption("slate", "Slate"),
    AvatarOption("ocean", "Ocean"),
    AvatarOption("violet", "Violet"),
    AvatarOption("rose", "Rose"),
    AvatarOption("amber", "Amber"),
    AvatarOption("forest", "Forest"),
    AvatarOption("crimson", "Crimson"),
    AvatarOption("indigo", "Indigo")
)
