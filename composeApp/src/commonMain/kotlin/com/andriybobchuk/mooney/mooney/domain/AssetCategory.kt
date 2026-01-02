package com.andriybobchuk.mooney.mooney.domain

enum class AssetCategory(
    val displayName: String,
    val emoji: String,
    val description: String,
    val color: Long = 0xFF6750A4, // Default Material3 Primary
    val riskLevel: RiskLevel = RiskLevel.MEDIUM
) {
    BANK_ACCOUNT(
        displayName = "Bank Account",
        emoji = "🏦",
        description = "Traditional bank accounts and deposits",
        color = 0xFF4285F4, // Blue
        riskLevel = RiskLevel.LOW
    ),
    CASH(
        displayName = "Cash Reserve",
        emoji = "💵",
        description = "Physical cash holdings",
        color = 0xFF34A853, // Green
        riskLevel = RiskLevel.VERY_LOW
    ),
    REAL_ESTATE(
        displayName = "Real Estate",
        emoji = "🏠",
        description = "Property and real estate investments",
        color = 0xFF795548, // Brown
        riskLevel = RiskLevel.LOW
    ),
    STOCKS(
        displayName = "Stocks",
        emoji = "📈",
        description = "Stock market investments",
        color = 0xFFE91E63, // Pink
        riskLevel = RiskLevel.HIGH
    ),
    BONDS(
        displayName = "Bonds",
        emoji = "📜",
        description = "Government and corporate bonds",
        color = 0xFF9C27B0, // Purple
        riskLevel = RiskLevel.LOW
    ),
    CRYPTO(
        displayName = "Cryptocurrency",
        emoji = "₿",
        description = "Digital assets and cryptocurrencies",
        color = 0xFFF57C00, // Orange
        riskLevel = RiskLevel.VERY_HIGH
    ),
    PRECIOUS_METALS(
        displayName = "Precious Metals",
        emoji = "🥇",
        description = "Gold, silver, and other precious metals",
        color = 0xFFFFD700, // Gold
        riskLevel = RiskLevel.MEDIUM
    ),
    RETIREMENT(
        displayName = "Retirement Fund",
        emoji = "🏖️",
        description = "401k, IRA, pension funds",
        color = 0xFF00BCD4, // Cyan
        riskLevel = RiskLevel.MEDIUM
    ),
    BUSINESS(
        displayName = "Business Assets",
        emoji = "💼",
        description = "Business ownership and investments",
        color = 0xFF607D8B, // Blue Grey
        riskLevel = RiskLevel.HIGH
    ),
    COLLECTIBLES(
        displayName = "Collectibles",
        emoji = "🎨",
        description = "Art, antiques, and collectible items",
        color = 0xFFFF5722, // Deep Orange
        riskLevel = RiskLevel.MEDIUM
    ),
    OTHER(
        displayName = "Other Assets",
        emoji = "📦",
        description = "Miscellaneous assets",
        color = 0xFF9E9E9E, // Grey
        riskLevel = RiskLevel.MEDIUM
    );

    companion object {
        fun fromString(value: String): AssetCategory {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                BANK_ACCOUNT // Default fallback
            }
        }
    }
}

enum class RiskLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}