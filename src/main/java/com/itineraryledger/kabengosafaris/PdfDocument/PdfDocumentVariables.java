package com.itineraryledger.kabengosafaris.PdfDocument;

/**
 * Defines system-wide variables/schema for each PDF document type.
 * These variables are immutable and cannot be modified via API.
 * Template creators can reference these paths using Thymeleaf syntax: ${itinerary.name}
 *
 * Variable Format:
 * - path: The variable path in the DTO (e.g., "name", "days[].title")
 * - type: The Java type (String, Integer, List, etc.)
 * - description: Human-readable description
 * - isRequired: Whether the variable is always present
 * - children: Nested fields for complex types
 */
public class PdfDocumentVariables {

    /**
     * Get the system-defined variables schema for a specific PDF document type
     *
     * @param documentName The name of the PDF document type (e.g., "FULL_ITINERARY")
     * @return JSON string containing variable definitions
     */
    public static String getVariablesForDocument(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> FULL_ITINERARY_SCHEMA;
            default -> "[]";
        };
    }

    /**
     * Get the display name for a document type
     */
    public static String getDisplayName(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "Full Safari Itinerary";
            default -> documentName;
        };
    }

    /**
     * Get the description for a document type
     */
    public static String getDescription(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "Complete itinerary document with all days, parks, activities, accommodations, and passenger configurations. Ideal for client proposals and booking confirmations.";
            default -> "";
        };
    }

    /**
     * Get the DTO class name for a document type
     */
    public static String getDataSourceClass(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO";
            default -> "";
        };
    }

    /**
     * Get the root variable name for templates
     */
    public static String getRootVariableName(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "itinerary";
            default -> "data";
        };
    }

    /**
     * Get all supported PDF document type names
     *
     * @return Array of PDF document type names that have defined variables
     */
    public static String[] getSupportedDocuments() {
        return new String[]{
            "FULL_ITINERARY"
            // Future document types:
            // "SAFARI_QUOTE",
            // "BOOKING_CONFIRMATION",
            // "INVOICE"
        };
    }

    /**
     * Schema for FULL_ITINERARY document type
     * Based on FullItineraryDTO structure
     */
    private static final String FULL_ITINERARY_SCHEMA = """
        [
            {
                "path": "id",
                "type": "String",
                "description": "Obfuscated itinerary ID",
                "isRequired": true
            },
            {
                "path": "name",
                "type": "String",
                "description": "Itinerary name (e.g., '7-Day Serengeti Safari')",
                "isRequired": true
            },
            {
                "path": "code",
                "type": "String",
                "description": "Auto-generated itinerary code (e.g., 'ITI-7D6N-001')",
                "isRequired": true
            },
            {
                "path": "status",
                "type": "Enum",
                "description": "Itinerary status (DRAFT, COMPLETE, PUBLISHED, ARCHIVED)",
                "isRequired": true
            },
            {
                "path": "statusDisplayName",
                "type": "String",
                "description": "Human-readable status name",
                "isRequired": false
            },
            {
                "path": "tripType",
                "type": "Enum",
                "description": "Trip type (PRIVATE, GROUP, etc.)",
                "isRequired": false
            },
            {
                "path": "tripTypeDisplayName",
                "type": "String",
                "description": "Human-readable trip type name",
                "isRequired": false
            },
            {
                "path": "tripTypeDescription",
                "type": "String",
                "description": "Trip type description",
                "isRequired": false
            },
            {
                "path": "budgetCategory",
                "type": "Enum",
                "description": "Budget category (LUXURY, MID_RANGE, BUDGET)",
                "isRequired": false
            },
            {
                "path": "budgetCategoryDisplayName",
                "type": "String",
                "description": "Human-readable budget category name",
                "isRequired": false
            },
            {
                "path": "budgetCategoryDescription",
                "type": "String",
                "description": "Budget category description",
                "isRequired": false
            },
            {
                "path": "budgetCategoryTier",
                "type": "Integer",
                "description": "Budget tier number (1-5)",
                "isRequired": false
            },
            {
                "path": "totalDays",
                "type": "Integer",
                "description": "Total number of days",
                "isRequired": true
            },
            {
                "path": "totalNights",
                "type": "Integer",
                "description": "Total number of nights",
                "isRequired": true
            },
            {
                "path": "isDayTrip",
                "type": "Boolean",
                "description": "Whether this is a single-day trip",
                "isRequired": false
            },
            {
                "path": "carCount",
                "type": "Integer",
                "description": "Number of vehicles needed",
                "isRequired": false
            },
            {
                "path": "description",
                "type": "String",
                "description": "Full itinerary description",
                "isRequired": false
            },
            {
                "path": "highlights",
                "type": "String",
                "description": "Key highlights (JSON array)",
                "isRequired": false
            },
            {
                "path": "startLocation",
                "type": "String",
                "description": "Starting location",
                "isRequired": false
            },
            {
                "path": "endLocation",
                "type": "String",
                "description": "Ending location",
                "isRequired": false
            },
            {
                "path": "isActive",
                "type": "Boolean",
                "description": "Whether itinerary is active",
                "isRequired": false
            },
            {
                "path": "createdAt",
                "type": "LocalDateTime",
                "description": "Creation timestamp",
                "isRequired": false
            },
            {
                "path": "updatedAt",
                "type": "LocalDateTime",
                "description": "Last update timestamp",
                "isRequired": false
            },
            {
                "path": "totalPaxCount",
                "type": "Integer",
                "description": "Total passenger count",
                "isRequired": false
            },
            {
                "path": "totalDaysCount",
                "type": "Integer",
                "description": "Total days count",
                "isRequired": false
            },
            {
                "path": "totalParksCount",
                "type": "Integer",
                "description": "Total parks visited",
                "isRequired": false
            },
            {
                "path": "totalActivitiesCount",
                "type": "Integer",
                "description": "Total activities count",
                "isRequired": false
            },
            {
                "path": "totalAccommodationsCount",
                "type": "Integer",
                "description": "Total accommodations count",
                "isRequired": false
            },
            {
                "path": "paxList",
                "type": "List<PaxDTO>",
                "description": "List of passenger configurations. Access with: th:each='pax : ${itinerary.paxList}'",
                "isRequired": false,
                "children": [
                    {"path": "id", "type": "String", "description": "Obfuscated pax configuration ID"},
                    {"path": "nationCategoryId", "type": "String", "description": "Obfuscated nation category ID"},
                    {"path": "nationCategoryName", "type": "String", "description": "e.g., 'Non-Resident', 'East African Resident'"},
                    {"path": "ageCategoryId", "type": "String", "description": "Obfuscated age category ID"},
                    {"path": "ageCategoryName", "type": "String", "description": "e.g., 'Adult', 'Child', 'Infant'"},
                    {"path": "count", "type": "Integer", "description": "Number of passengers in this category"},
                    {"path": "notes", "type": "String", "description": "Additional notes for this passenger group"}
                ]
            },
            {
                "path": "days",
                "type": "List<DayDTO>",
                "description": "List of days ordered by dayNumber. Access with: th:each='day : ${itinerary.days}'",
                "isRequired": false,
                "children": [
                    {"path": "id", "type": "String", "description": "Obfuscated day ID"},
                    {"path": "dayNumber", "type": "Integer", "description": "Sequential day number (1, 2, 3...)"},
                    {"path": "dayTag", "type": "String", "description": "Display tag (e.g., 'Day 1', 'Day 2')"},
                    {"path": "title", "type": "String", "description": "Day title (e.g., 'Arrival in Arusha')"},
                    {"path": "description", "type": "String", "description": "Full day description"},
                    {"path": "morningActivities", "type": "String", "description": "Morning activities text"},
                    {"path": "afternoonActivities", "type": "String", "description": "Afternoon activities text"},
                    {"path": "eveningActivities", "type": "String", "description": "Evening activities text"},
                    {"path": "wildlifeHighlights", "type": "String", "description": "Wildlife highlights for this day"},
                    {"path": "scenicHighlights", "type": "String", "description": "Scenic highlights for this day"},
                    {"path": "specialNotes", "type": "String", "description": "Special notes or instructions"},
                    {"path": "startLocation", "type": "String", "description": "Day start location"},
                    {"path": "endLocation", "type": "String", "description": "Day end location"},
                    {"path": "distanceKm", "type": "Integer", "description": "Driving distance in kilometers"},
                    {"path": "isOvernight", "type": "Boolean", "description": "Whether day includes overnight stay"},
                    {"path": "mealsIncluded", "type": "String", "description": "Meals included (e.g., 'B,L,D' for Breakfast, Lunch, Dinner)"},
                    {"path": "createdAt", "type": "LocalDateTime", "description": "Day creation timestamp"},
                    {
                        "path": "activities",
                        "type": "List<DayActivityDTO>",
                        "description": "Standalone activities for this day. Access: th:each='activity : ${day.activities}'",
                        "children": [
                            {"path": "id", "type": "String", "description": "Obfuscated day activity ID"},
                            {"path": "activityId", "type": "String", "description": "Obfuscated activity reference ID"},
                            {"path": "activityName", "type": "String", "description": "Activity name (e.g., 'City Tour')"},
                            {"path": "activitySlug", "type": "String", "description": "Activity URL slug"},
                            {"path": "sortOrder", "type": "Integer", "description": "Display order"},
                            {"path": "durationHours", "type": "BigDecimal", "description": "Duration in hours"},
                            {"path": "startTime", "type": "String", "description": "Start time (e.g., '08:00')"},
                            {"path": "endTime", "type": "String", "description": "End time (e.g., '12:00')"},
                            {"path": "notes", "type": "String", "description": "Activity notes"},
                            {"path": "isIncludedInPrice", "type": "Boolean", "description": "Whether included in package price"},
                            {"path": "isOptional", "type": "Boolean", "description": "Whether activity is optional"}
                        ]
                    },
                    {
                        "path": "accommodations",
                        "type": "List<DayAccommodationDTO>",
                        "description": "Accommodations for this day. Access: th:each='accommodation : ${day.accommodations}'",
                        "children": [
                            {"path": "id", "type": "String", "description": "Obfuscated day accommodation ID"},
                            {"path": "accommodationId", "type": "String", "description": "Obfuscated accommodation reference ID"},
                            {"path": "accommodationName", "type": "String", "description": "Accommodation name (e.g., 'Serengeti Safari Lodge')"},
                            {"path": "accommodationSlug", "type": "String", "description": "Accommodation URL slug"},
                            {"path": "roomTypeId", "type": "String", "description": "Obfuscated room type ID"},
                            {"path": "roomTypeName", "type": "String", "description": "Room type (e.g., 'Deluxe Tent', 'Standard Room')"},
                            {"path": "roomTypeMaxOccupancy", "type": "Integer", "description": "Maximum room occupancy"},
                            {"path": "roomTypeMinOccupancy", "type": "Integer", "description": "Minimum room occupancy"},
                            {"path": "roomStandardId", "type": "String", "description": "Obfuscated room standard ID"},
                            {"path": "roomStandardName", "type": "String", "description": "Room standard (e.g., 'Luxury', 'Standard')"},
                            {"path": "boardTypeId", "type": "String", "description": "Obfuscated board type ID"},
                            {"path": "boardTypeName", "type": "String", "description": "Board type (e.g., 'Full Board', 'Half Board', 'B&B')"},
                            {"path": "roomCount", "type": "Integer", "description": "Number of rooms"},
                            {"path": "isAlternative", "type": "Boolean", "description": "Whether this is an alternative/backup option"},
                            {"path": "notes", "type": "String", "description": "Accommodation notes"}
                        ]
                    },
                    {
                        "path": "parks",
                        "type": "List<DayParkDTO>",
                        "description": "Park visits for this day. Access: th:each='park : ${day.parks}'",
                        "children": [
                            {"path": "id", "type": "String", "description": "Obfuscated day park ID"},
                            {"path": "parkId", "type": "String", "description": "Obfuscated park reference ID"},
                            {"path": "parkName", "type": "String", "description": "Park name (e.g., 'Serengeti National Park')"},
                            {"path": "parkSlug", "type": "String", "description": "Park URL slug"},
                            {"path": "entryType", "type": "Enum", "description": "Entry type (TRANSIT, DAY_TRIP, SLEEP_OVER)"},
                            {"path": "entryTypeDisplayName", "type": "String", "description": "Human-readable entry type"},
                            {"path": "sortOrder", "type": "Integer", "description": "Display order"},
                            {"path": "arrivalTime", "type": "String", "description": "Arrival time (e.g., '07:00')"},
                            {"path": "departureTime", "type": "String", "description": "Departure time (e.g., '18:00')"},
                            {"path": "notes", "type": "String", "description": "Park visit notes"},
                            {
                                "path": "activities",
                                "type": "List<ParkActivityDTO>",
                                "description": "Activities within this park. Access: th:each='activity : ${park.activities}'",
                                "children": [
                                    {"path": "id", "type": "String", "description": "Obfuscated park activity ID"},
                                    {"path": "activityId", "type": "String", "description": "Obfuscated activity reference ID"},
                                    {"path": "activityName", "type": "String", "description": "Activity name (e.g., 'Game Drive', 'Walking Safari')"},
                                    {"path": "sortOrder", "type": "Integer", "description": "Display order"},
                                    {"path": "durationHours", "type": "BigDecimal", "description": "Duration in hours"},
                                    {"path": "startTime", "type": "String", "description": "Start time"},
                                    {"path": "endTime", "type": "String", "description": "End time"},
                                    {"path": "notes", "type": "String", "description": "Activity notes"},
                                    {"path": "isIncludedInPrice", "type": "Boolean", "description": "Whether included in package price"}
                                ]
                            },
                            {
                                "path": "tariffs",
                                "type": "List<ParkTariffDTO>",
                                "description": "Tariffs for this park visit. Access: th:each='tariff : ${park.tariffs}'",
                                "children": [
                                    {"path": "id", "type": "String", "description": "Obfuscated park tariff ID"},
                                    {"path": "tariffId", "type": "String", "description": "Obfuscated tariff reference ID"},
                                    {"path": "tariffName", "type": "String", "description": "Tariff name (e.g., 'Park Entry Fee', 'Conservation Fee')"},
                                    {"path": "notes", "type": "String", "description": "Tariff notes"},
                                    {"path": "isIncludedInPrice", "type": "Boolean", "description": "Whether included in package price"}
                                ]
                            }
                        ]
                    }
                ]
            }
        ]
        """;
}
