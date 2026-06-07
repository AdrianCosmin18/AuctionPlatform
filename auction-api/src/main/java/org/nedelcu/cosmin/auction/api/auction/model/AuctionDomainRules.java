package org.nedelcu.cosmin.auction.api.auction.model;

import java.util.Map;
import java.util.Set;

public final class AuctionDomainRules {

    public static final Map<String, Set<String>> CATEGORY_SUBCATEGORY_MAP = Map.of(
            "RARE_BOOKS", Set.of("FIRST_EDITIONS", "LIMITED_EDITIONS", "SIGNED_COPIES", "ANTIQUE_BOOKS", "ART_ALBUMS"),
            "MANUSCRIPTS", Set.of("LITERARY_MANUSCRIPTS", "HISTORICAL_MANUSCRIPTS", "PERSONAL_NOTES", "ORIGINAL_LETTERS"),
            "HISTORICAL_DOCUMENTS", Set.of("OFFICIAL_ACTS", "POLITICAL_DOCUMENTS", "MILITARY_DOCUMENTS", "ROYAL_NOBLE_DOCUMENTS"),
            "MAPS_AND_ATLASES", Set.of("ANTIQUE_MAPS", "ATLASES", "URBAN_PLANS", "MILITARY_MAPS"),
            "PHOTOGRAPHS_AND_VISUAL_ARCHIVES", Set.of("HISTORICAL_PHOTOGRAPHS", "PORTRAITS", "ARCHIVE_ALBUMS", "POSTCARDS"),
            "COLLECTIBLE_PRINTS", Set.of("NEWSPAPERS", "RARE_MAGAZINES", "POSTERS", "EVENT_PROGRAMS"),
            "SCIENTIFIC_AND_ACADEMIC_DOCUMENTS", Set.of("ACADEMIC_PAPERS", "RESEARCH_NOTES", "PATENTS", "RARE_PUBLICATIONS")
    );

    public static final Set<String> ITEM_CONDITIONS = Set.of(
            "EXCELLENT",
            "VERY_GOOD",
            "GOOD",
            "FAIR",
            "FRAGILE",
            "DETERIORATED"
    );

    public static final Set<String> AUTHENTICITY_STATUSES = Set.of(
            "UNVERIFIED",
            "IN_REVIEW",
            "VERIFIED",
            "REJECTED"
    );

    private AuctionDomainRules() {
    }

    public static boolean isValidCategory(String categoryCode) {
        return CATEGORY_SUBCATEGORY_MAP.containsKey(categoryCode);
    }

    public static boolean isValidSubcategory(String categoryCode, String subcategoryCode) {
        return categoryCode != null
                && subcategoryCode != null
                && CATEGORY_SUBCATEGORY_MAP.getOrDefault(categoryCode, Set.of()).contains(subcategoryCode);
    }

    public static boolean isValidCondition(String conditionCode) {
        return ITEM_CONDITIONS.contains(conditionCode);
    }

    public static boolean isValidAuthenticityStatus(String authenticityStatus) {
        return AUTHENTICITY_STATUSES.contains(authenticityStatus);
    }
}
