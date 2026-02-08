package com.novaTech.Nova.controller.AI;

import com.novaTech.Nova.Services.AI.ExternalApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService service;

    // --- Netflix ---
    @GetMapping("/netflix/search")
    public ResponseEntity<String> searchNetflix(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limitTitles,
            @RequestParam(defaultValue = "20") int limitSuggestions,
            @RequestParam(defaultValue = "en") String lang
    ) {
        try {
            return ResponseEntity.ok(service.searchNetflix(query, offset, limitTitles, limitSuggestions, lang));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Netflix API error: " + e.getMessage());
        }
    }

    // --- Spotify ---
    @GetMapping("/spotify/search")
    public ResponseEntity<String> searchSpotify(
            @RequestParam(name = "q") String q,  // <-- must match Spotify API
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "5") int numberOfTopResults
    ) {
        try {
            return ResponseEntity.ok(service.searchSpotify(q, offset, limit, numberOfTopResults));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Spotify API error: " + e.getMessage());
        }
    }


    // --- AI Workout Planner ---
    @PostMapping("/workout/generate")
    public ResponseEntity<String> generateWorkout(@RequestBody WorkoutRequest request) {
        try {
            return ResponseEntity.ok(service.generateWorkoutPlan(
                    request.goal,
                    request.fitnessLevel,
                    request.preferences,
                    request.healthConditions,
                    request.daysPerWeek,
                    request.sessionDuration,
                    request.planDurationWeeks,
                    request.lang
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Workout Planner API error: " + e.getMessage());
        }
    }

    @PostMapping("/workout/nutrition")
    public ResponseEntity<String> nutritionAdvice(@RequestBody NutritionRequest request) {
        try {
            return ResponseEntity.ok(service.nutritionAdvice(
                    request.goal,
                    request.dietaryRestrictions,
                    request.currentWeight,
                    request.targetWeight,
                    request.dailyActivityLevel,
                    request.lang
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Nutrition Advice API error: " + e.getMessage());
        }
    }

    @PostMapping("/workout/exercise")
    public ResponseEntity<String> exerciseDetails(@RequestBody ExerciseRequest request) {
        try {
            return ResponseEntity.ok(service.exerciseDetails(
                    request.exerciseName,
                    request.lang
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Exercise Details API error: " + e.getMessage());
        }
    }


    // --- Request DTOs ---
    public static class WorkoutRequest {
        public String goal;
        public String fitnessLevel;
        public String[] preferences;
        public String[] healthConditions;
        public int daysPerWeek;
        public int sessionDuration;
        public int planDurationWeeks;
        public String lang;
    }

    public static class NutritionRequest {
        public String goal;
        public String[] dietaryRestrictions;
        public double currentWeight;
        public double targetWeight;
        public String dailyActivityLevel;
        public String lang;
    }

    public static class ExerciseRequest {
        public String exerciseName;
        public String lang;
    }
}
