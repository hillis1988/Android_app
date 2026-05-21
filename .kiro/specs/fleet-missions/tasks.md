# Implementation Plan: Fleet Missions

## Overview

This plan implements a timed deployment system where players send ships on missions across unlocked sectors. The implementation follows the existing architecture: pure game logic in `MissionEngine`, immutable state extensions in `GameState`, persistence via `GameRepository`, and UI through Jetpack Compose with `GameViewModel` as the bridge. Tasks are ordered to build incrementally — data models first, then engine logic, then persistence, then UI, then integration.

## Tasks

- [x] 1. Define mission data models and static configuration
  - [x] 1.1 Create MissionData.kt with enums, data classes, and ship affinity table
    - Create `app/src/main/java/com/starfleet/idle/data/MissionData.kt`
    - Define `MissionType` enum (COMBAT, MINING, EXPLORATION, DIPLOMACY)
    - Define `MissionDifficulty` enum with duration options, reward multipliers, gem rewards, RP ranges, and bonus reward chance
    - Define `MissionTemplate`, `ActiveMission`, `MissionStatus`, `MissionResult`, `BonusRewardType`, `ShipAffinity`, `DeploymentValidation`, `MissionBoardState`, `MissionHistoryEntry` data classes
    - Define the `SHIP_AFFINITIES` table mapping all 12 ship tiers to their 4 mission type multipliers (all values ≥ 0.8, diplomacy ≥ 1.0)
    - Define `MISSION_TEMPLATES` list with missions for each sector scaled by sector progression
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 2.6, 2.7, 4.1, 4.5, 8.4_

  - [x] 1.2 Extend GameState with mission-related fields
    - Add `missionBoards: Map<String, MissionBoardState>`, `activeMissions: List<ActiveMission>`, `completedMissions: List<MissionResult>`, `missionHistory: List<MissionHistoryEntry>`, `missionBonusIncomeEndTime: Long`, `missionBonusFleetPowerEndTime: Long`, `missionBonusResearchEndTime: Long`, `seenMissionTutorial: Boolean` fields to `GameState`
    - Add helper properties for mission slot count, active mission count, and feature unlock check
    - _Requirements: 10.1, 7.1, 7.2, 7.3, 8.1_

  - [x] 1.3 Add COMPLETE_MISSION quest type and templates
    - Add `COMPLETE_MISSION` to `QuestType` enum in `QuestData.kt`
    - Add three new quest templates: "Fleet Commander" (1 mission), "Mission Specialist" (2 missions), "Admiral's Orders" (3 missions)
    - _Requirements: 11.1, 11.3_

- [x] 2. Implement MissionEngine core logic
  - [x] 2.1 Create MissionEngine.kt with board generation and feature unlock
    - Create `app/src/main/java/com/starfleet/idle/engine/MissionEngine.kt`
    - Implement `isFeatureUnlocked(gameState)` — checks if Orion Nebula sector is unlocked (total fleet power ≥ 500,000)
    - Implement `getUnlockedSlotCount(totalFleetPower)` — returns 1/2/3 based on 500K/50M/1B thresholds
    - Implement `generateMissionBoard(sectorId, activeMissions, currentBoardMissions, seed)` — returns 3-6 missions for the sector, excluding cooldown missions and active missions
    - Implement board refresh logic: refresh if elapsed time ≥ 4 hours (14,400,000 ms)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1, 7.2, 7.3, 8.1, 8.2, 8.4_

  - [x] 2.2 Write property tests for board generation and feature unlock
    - **Property 1: Board generation size invariant** — generateMissionBoard always returns 3-6 missions for unlocked sectors
    - **Property 2: Locked sector exclusion** — no missions generated for locked sectors
    - **Property 3: Board refresh timing** — board refreshes if and only if elapsed ≥ 4 hours
    - **Validates: Requirements 1.1, 1.2, 1.4, 8.1**

  - [x] 2.3 Implement effective fleet power calculation and success chance
    - Implement `calculateEffectiveFleetPower(deployedShipTiers, shipStates, missionType, gameState)` — sums each ship's base power × affinity multiplier for the mission type
    - Implement `calculateSuccessChance(effectivePower, requiredPower)` — linear interpolation: ratio ≤ 0.75 → 30%, ratio = 1.0 → 70%, ratio ≥ 1.5 → 95%
    - _Requirements: 2.3, 5.1, 5.2, 5.3, 5.4_

  - [x] 2.4 Write property tests for fleet power and success chance
    - **Property 4: Ship affinity bounds** — all affinities in [0.8, 2.0], diplomacy ≥ 1.0
    - **Property 5: Effective fleet power calculation** — equals sum of base power × affinity
    - **Property 12: Success chance formula correctness** — monotonically non-decreasing, bounded [0.30, 0.95], exact values at ratio 0.75/1.0/1.5
    - **Validates: Requirements 2.2, 2.3, 2.7, 5.1, 5.2, 5.3, 5.4**

  - [x] 2.5 Implement deployment validation and mission deployment
    - Implement `validateDeployment(missionTemplate, selectedTiers, gameState)` — checks ownership (≥1 of each tier), tier count (1-5), slot availability, no duplicate tiers across active missions
    - Implement `deployMission(gameState, missionTemplate, selectedTiers, now)` — deducts 1 ship per tier, creates ActiveMission, reduces fleet power, starts timer
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6, 3.7, 9.3_

  - [x] 2.6 Write property tests for deployment validation and execution
    - **Property 6: Deployment ownership validation** — rejects if player owns 0 of any selected tier
    - **Property 7: Deployment tier count bounds** — accepts 1-5 tiers, rejects 0 or >5
    - **Property 8: Ship deduction on deployment** — exactly 1 fewer ship per deployed tier
    - **Property 9: Mission slot limit enforcement** — active missions never exceed slot count
    - **Property 19: No duplicate tier across concurrent missions** — rejects tiers already deployed
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.6, 3.7, 9.3**

  - [x] 2.7 Implement mission completion, success/failure resolution, and reward collection
    - Implement `checkMissionCompletions(gameState, now)` — transitions IN_PROGRESS missions to COMPLETED_SUCCESS or COMPLETED_FAILURE based on elapsed time and random roll against success chance
    - Implement `collectMissionReward(gameState, missionId)` — calculates credit reward (CPS × duration × multiplier, capped at 50% hourly), grants gems/RP/bonus rewards, returns ships, progresses COMPLETE_MISSION quests
    - Implement time manipulation detection: if clock jump > 10 minutes forward, cap at last known good timestamp
    - _Requirements: 4.2, 4.4, 5.5, 5.6, 5.7, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 9.2, 9.4, 9.5, 11.2_

  - [x] 2.8 Write property tests for mission completion and rewards
    - **Property 10: Offline mission completion detection** — missions complete when elapsed ≥ duration
    - **Property 11: Rewards held until explicit collection** — no state change until collectMissionReward called
    - **Property 13: Ship return on mission completion** — all deployed tiers restored by 1
    - **Property 14: Credit reward formula with cap** — min(CPS × duration × mult, CPS × 1800)
    - **Property 15: Research points for Hard and Elite missions** — Hard: [1,3], Elite: [2,3], Easy/Medium: 0
    - **Property 16: Rewards use collection-time CPS** — calculated at collection, not deployment
    - **Property 24: Quest progress on successful mission completion** — COMPLETE_MISSION quest incremented
    - **Validates: Requirements 4.2, 4.4, 5.5, 6.1, 6.3, 6.5, 6.6, 9.2, 9.4, 11.2**

  - [x] 2.9 Implement cooldown enforcement and prestige cancellation
    - Implement cooldown logic: completed missions get `cooldownUntil = completionTime + 30 minutes`, excluded from board generation
    - Implement `cancelAllMissions(gameState)` — clears activeMissions and completedMissions, returns ships (for prestige)
    - _Requirements: 9.1, 10.5_

  - [x] 2.10 Write property tests for cooldowns and prestige
    - **Property 18: Cooldown enforcement** — completed missions excluded from board for 30 minutes
    - **Property 23: Prestige cancels missions without rewards** — empty lists, no reward changes
    - **Validates: Requirements 9.1, 10.5**

- [x] 3. Checkpoint - Ensure all engine logic tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement persistence and state management
  - [x] 4.1 Extend GameRepository to serialize/deserialize mission state
    - Add JSON serialization for `missionBoards`, `activeMissions`, `completedMissions`, `missionHistory`, bonus end times, and `seenMissionTutorial` in `GameRepository.save()`
    - Add JSON deserialization with safe defaults in `GameRepository.load()` — missing fields default to empty/0/false for backward compatibility
    - Handle corrupted mission data gracefully: if parsing fails, initialize with empty mission state
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 4.2 Write property test for serialization round-trip
    - **Property 21: Mission state serialization round-trip** — serialize then deserialize produces equivalent state
    - **Validates: Requirements 10.1**

  - [x] 4.3 Implement mission history cap logic
    - When adding to `missionHistory`, cap at 20 entries by removing oldest when at capacity
    - _Requirements: 10.4_

  - [x] 4.4 Write property test for history cap
    - **Property 22: Mission history cap** — history never exceeds 20 entries
    - **Validates: Requirements 10.4**

- [x] 5. Integrate MissionEngine with GameViewModel and GameEngine
  - [x] 5.1 Add mission lifecycle methods to GameViewModel
    - Add `deployMission(missionTemplate, selectedTiers)` method
    - Add `collectMissionReward(missionId)` method
    - Add `refreshMissionBoard()` method
    - Add `dismissMissionTutorial()` method
    - Add periodic mission completion check (every 10 seconds) in the game loop
    - Add mission board refresh check (every 60 seconds) in the game loop
    - Expose mission-related state flows for UI (available missions, active missions, completed missions, slot info)
    - _Requirements: 3.1, 3.5, 4.2, 4.3, 8.3, 8.5_

  - [x] 5.2 Integrate with prestige and offline earnings flow
    - Call `MissionEngine.cancelAllMissions()` in `GameEngine.prestige()`
    - Call `MissionEngine.checkMissionCompletions()` in `GameViewModel.loadGame()` for offline completion detection
    - Apply bonus reward multipliers (income, fleet power, research) to relevant GameState calculations
    - _Requirements: 4.2, 10.5, 6.4_

  - [x] 5.3 Write unit tests for ViewModel integration
    - Test that deploying a mission updates state correctly
    - Test that offline load detects completed missions
    - Test that prestige cancels active missions
    - _Requirements: 3.4, 4.2, 10.5_

- [x] 6. Checkpoint - Ensure all logic and integration tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement Mission Board UI
  - [x] 7.1 Create MissionBoardScreen composable
    - Create `app/src/main/java/com/starfleet/idle/ui/MissionBoardScreen.kt`
    - Display 3-6 available missions for the active sector with type emoji, name, difficulty badge, duration, and reward preview
    - Display active missions with countdown timers showing remaining time
    - Display completed missions with "Collect" button
    - Show locked mission slots with unlock requirements (fleet power thresholds)
    - Show notification badge when completed missions are ready to collect
    - _Requirements: 1.1, 3.1, 3.5, 4.1, 7.4, 8.5_

  - [x] 7.2 Create MissionDetailDialog composable
    - Create `app/src/main/java/com/starfleet/idle/ui/MissionDetailDialog.kt`
    - Display mission details: fleet power requirement, duration, reward preview, success chance estimate
    - Ship selection UI: allow selecting 1-5 ship tiers with affinity indicators
    - Show real-time success chance calculation as ships are selected
    - Deploy confirmation button with validation feedback
    - Show error messages for invalid deployments (no slots, missing ships, duplicate tiers)
    - _Requirements: 3.1, 3.2, 3.3, 3.6, 3.7_

  - [x] 7.3 Create mission tutorial and integrate navigation
    - Add brief tutorial overlay shown on first access explaining mission mechanics
    - Add Mission Board button to the main game screen (visible only when feature is unlocked)
    - Add notification badge on Mission Board button when completed missions are pending
    - Wire navigation between main game screen and Mission Board
    - _Requirements: 8.1, 8.2, 8.3, 8.5_

- [x] 8. Final checkpoint - Ensure all tests pass and feature compiles
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation uses Kotlin throughout, matching the existing codebase
- MissionEngine is separated from GameEngine to keep file sizes manageable
- All mission logic uses pure functions and timestamp comparison (no coroutines/timers for mission progress)
- Kotest Property Testing library should be added to test dependencies for PBT tasks

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "2.3"] },
    { "id": 3, "tasks": ["2.2", "2.4", "2.5"] },
    { "id": 4, "tasks": ["2.6", "2.7"] },
    { "id": 5, "tasks": ["2.8", "2.9"] },
    { "id": 6, "tasks": ["2.10", "4.1"] },
    { "id": 7, "tasks": ["4.2", "4.3"] },
    { "id": 8, "tasks": ["4.4", "5.1", "5.2"] },
    { "id": 9, "tasks": ["5.3", "7.1"] },
    { "id": 10, "tasks": ["7.2", "7.3"] }
  ]
}
```
