# Requirements Document

## Introduction

Fleet Missions is a timed deployment system for StarFleet Idle that gives fleet power a meaningful ongoing purpose. Players send ships from their fleet on timed missions across unlocked sectors, choosing mission types that match their ships' strengths. Missions run on idle-friendly timers (30 minutes to 8 hours), and upon completion players collect rewards including credits, gems, research points, and temporary boosts. Harder missions offer better rewards but carry a risk of failure. The system integrates with the existing sector progression and provides long-term motivation to grow fleet power beyond sector unlocks.

## Glossary

- **Mission_System**: The subsystem responsible for managing fleet mission lifecycle including availability, deployment, completion, and reward distribution.
- **Mission**: A timed task that a player deploys ships to complete, defined by a type, difficulty, sector, duration, fleet power requirement, and reward table.
- **Mission_Type**: A category of mission that determines which ship bonuses apply. Types are Combat, Mining, Exploration, and Diplomacy.
- **Mission_Difficulty**: A tier (Easy, Medium, Hard, Elite) that determines fleet power requirements, reward multipliers, and failure risk.
- **Deployed_Fleet**: The set of ships a player assigns to an active mission, temporarily removing their fleet power contribution from the player's total.
- **Ship_Affinity**: A bonus multiplier that each ship tier has for specific mission types, representing that ship's specialization.
- **Success_Chance**: A percentage (0-100) representing the probability that a deployed fleet completes a mission successfully, calculated from deployed fleet power relative to mission difficulty.
- **Mission_Slot**: A position in which a player can run a concurrent mission. Players start with 1 slot and can unlock up to 3.
- **Mission_Board**: The UI panel displaying available missions for the player's currently active sector.
- **Cooldown_Timer**: A per-sector timer that prevents the same mission from being immediately re-run after completion or failure.
- **Bonus_Reward**: A temporary multiplier (income boost, fleet power boost, or research speed boost) granted as a mission reward that expires after a set duration.
- **Mission_Loot_Table**: The set of possible rewards for a mission, with probabilities influenced by difficulty and success margin.

## Requirements

### Requirement 1: Mission Availability

**User Story:** As a player, I want missions to appear based on my unlocked sectors, so that mission content scales with my progression.

#### Acceptance Criteria

1. WHEN a player opens the Mission_Board, THE Mission_System SHALL display between 3 and 6 available missions for the player's currently active sector.
2. WHEN a player has not yet unlocked a sector, THE Mission_System SHALL hide all missions associated with that locked sector.
3. WHEN a player unlocks a new sector, THE Mission_System SHALL make missions for that sector available on the next Mission_Board refresh.
4. THE Mission_System SHALL refresh the available mission pool for each sector every 4 hours.
5. WHEN fewer than 3 missions are available for a sector due to active deployments, THE Mission_System SHALL display remaining available missions without generating replacements until the next scheduled refresh.

### Requirement 2: Mission Types and Ship Affinities

**User Story:** As a player, I want different ship types to excel at different mission types, so that fleet composition matters beyond raw power.

#### Acceptance Criteria

1. THE Mission_System SHALL support four Mission_Types: Combat, Mining, Exploration, and Diplomacy.
2. THE Mission_System SHALL assign each ship tier a Ship_Affinity multiplier (ranging from 0.5x to 2.0x) for each Mission_Type.
3. WHEN calculating deployed fleet power for a mission, THE Mission_System SHALL multiply each deployed ship's base power by its Ship_Affinity for the mission's type.
4. THE Mission_System SHALL assign Combat affinity of 1.5x or higher to military-themed ships (Battle Destroyer, Battlecruiser, Titan Warship, Dreadnought).
5. THE Mission_System SHALL assign Mining affinity of 1.5x or higher to resource-themed ships (Cargo Frigate, Mining Cruiser).
6. THE Mission_System SHALL assign Exploration affinity of 1.5x or higher to scout-themed ships (Recon Probe, Scout Shuttle, Carrier Flagship).
7. THE Mission_System SHALL assign Diplomacy affinity of 1.0x or higher to all ships, with no ship receiving below 0.8x for any Mission_Type.

### Requirement 3: Mission Deployment

**User Story:** As a player, I want to select which ships to send on a mission, so that I can make strategic choices about fleet allocation.

#### Acceptance Criteria

1. WHEN a player selects a mission, THE Mission_System SHALL display the mission's fleet power requirement, duration, reward preview, and success chance estimate.
2. WHEN a player deploys ships to a mission, THE Mission_System SHALL verify that the player owns at least 1 of each selected ship tier.
3. THE Mission_System SHALL allow a player to assign between 1 and 5 distinct ship tiers to a single mission.
4. WHEN a player confirms deployment, THE Mission_System SHALL deduct the deployed ship count (1 per tier assigned) from the player's available fleet and reduce the player's active fleet power accordingly.
5. WHILE a mission is active, THE Mission_System SHALL display a countdown timer showing remaining time until completion.
6. THE Mission_System SHALL limit concurrent active missions to the number of unlocked Mission_Slots (starting at 1, maximum of 3).
7. IF a player attempts to deploy ships when all Mission_Slots are occupied, THEN THE Mission_System SHALL display a message indicating no slots are available.

### Requirement 4: Mission Duration and Idle Integration

**User Story:** As a player, I want missions to progress while I am away from the game, so that the system fits the idle gameplay loop.

#### Acceptance Criteria

1. THE Mission_System SHALL support mission durations of 30 minutes, 1 hour, 2 hours, 4 hours, and 8 hours.
2. WHEN a player returns to the game after being offline, THE Mission_System SHALL calculate mission progress based on real elapsed time and mark completed missions as ready to collect.
3. WHILE a mission timer is running, THE Mission_System SHALL continue counting down regardless of whether the app is in the foreground.
4. WHEN a mission completes while the player is offline, THE Mission_System SHALL hold the rewards until the player opens the game and manually collects them.
5. THE Mission_System SHALL associate shorter missions (30 minutes, 1 hour) with Easy and Medium difficulties, and longer missions (4 hours, 8 hours) with Hard and Elite difficulties.

### Requirement 5: Success and Failure Calculation

**User Story:** As a player, I want harder missions to carry risk of failure, so that there is a meaningful risk-reward tradeoff.

#### Acceptance Criteria

1. WHEN a mission completes, THE Mission_System SHALL calculate Success_Chance based on the ratio of effective deployed fleet power to the mission's required fleet power.
2. WHEN effective deployed fleet power equals the mission's required fleet power, THE Mission_System SHALL set Success_Chance to 70%.
3. WHEN effective deployed fleet power exceeds the mission's required fleet power by 50% or more, THE Mission_System SHALL set Success_Chance to 95% (maximum).
4. WHEN effective deployed fleet power is below 75% of the mission's required fleet power, THE Mission_System SHALL set Success_Chance to 30% (minimum).
5. IF a mission fails, THEN THE Mission_System SHALL return all deployed ships to the player's fleet without granting rewards.
6. IF a mission fails, THEN THE Mission_System SHALL display a failure notification with the calculated Success_Chance that was used.
7. THE Mission_System SHALL determine success or failure at the moment of mission completion using a single random roll against the calculated Success_Chance.

### Requirement 6: Mission Rewards

**User Story:** As a player, I want missions to grant meaningful rewards, so that deploying my fleet feels worthwhile.

#### Acceptance Criteria

1. WHEN a mission succeeds, THE Mission_System SHALL grant a base credit reward equal to the player's credits-per-second multiplied by a duration factor (mission duration in seconds multiplied by a difficulty multiplier between 0.3 and 0.8).
2. WHEN a mission succeeds, THE Mission_System SHALL grant between 1 and 5 gems based on Mission_Difficulty (Easy: 1, Medium: 2, Hard: 3, Elite: 5).
3. WHEN a Hard or Elite mission succeeds, THE Mission_System SHALL grant between 1 and 3 research points.
4. WHEN an Elite mission succeeds, THE Mission_System SHALL have a 25% chance to grant a Bonus_Reward (temporary 2-hour boost of either +20% income, +20% fleet power, or +30% research speed).
5. WHEN a mission succeeds, THE Mission_System SHALL return all deployed ships to the player's fleet.
6. THE Mission_System SHALL scale credit rewards proportionally to the player's current income rate at the time of collection, preventing stockpiling of missions for later collection at higher income.

### Requirement 7: Mission Slots and Progression

**User Story:** As a player, I want to unlock additional mission slots as I progress, so that the system grows with my fleet.

#### Acceptance Criteria

1. THE Mission_System SHALL provide 1 Mission_Slot to all players upon unlocking the Fleet Missions feature.
2. WHEN a player's total fleet power reaches 50,000,000 (Deep Space tier), THE Mission_System SHALL unlock a second Mission_Slot.
3. WHEN a player's total fleet power reaches 1,000,000,000 (Galactic Core tier), THE Mission_System SHALL unlock a third Mission_Slot.
4. THE Mission_System SHALL display locked Mission_Slots with their unlock requirements visible to the player.

### Requirement 8: Feature Unlock and Sector Integration

**User Story:** As a player, I want Fleet Missions to unlock naturally as I progress, so that it does not overwhelm new players.

#### Acceptance Criteria

1. THE Mission_System SHALL become available to the player after unlocking the Orion Nebula sector (total fleet power of 500,000).
2. WHEN the player has not unlocked the Orion Nebula sector, THE Mission_System SHALL not display any mission-related UI elements.
3. WHEN the player first unlocks Fleet Missions, THE Mission_System SHALL display a brief tutorial explaining mission mechanics.
4. THE Mission_System SHALL generate missions with fleet power requirements scaled to the sector they belong to (Solar System missions require less power than Galactic Core missions).
5. WHILE a player is viewing a sector in the main game, THE Mission_System SHALL show a notification badge on the Mission_Board button when completed missions are ready to collect.

### Requirement 9: Cooldowns and Anti-Exploitation

**User Story:** As a player, I want the mission system to have pacing that prevents it from replacing normal gameplay, so that the game's 6-month progression remains intact.

#### Acceptance Criteria

1. WHEN a mission is completed (success or failure), THE Mission_System SHALL apply a Cooldown_Timer of 30 minutes before that specific mission becomes available again.
2. THE Mission_System SHALL cap total credit rewards from missions at 50% of the player's hourly passive income per mission, preventing missions from doubling effective income.
3. THE Mission_System SHALL not allow a player to deploy the same ship tier to multiple concurrent missions.
4. THE Mission_System SHALL calculate mission rewards based on the player's credits-per-second at the time of mission completion, not at the time of deployment.
5. IF a player attempts to manipulate time (detected clock change exceeding 10 minutes forward), THEN THE Mission_System SHALL complete missions based on server-validated time or last known good timestamp.

### Requirement 10: Persistence and State Management

**User Story:** As a player, I want my mission progress to be saved, so that I do not lose progress when closing the app.

#### Acceptance Criteria

1. THE Mission_System SHALL persist all active mission state (deployed ships, start time, mission parameters) to local storage whenever the game state is saved.
2. WHEN the game loads, THE Mission_System SHALL restore all active missions and calculate elapsed time to update their status.
3. IF the game crashes during a mission, THEN THE Mission_System SHALL recover the mission state from the last saved checkpoint without losing progress.
4. THE Mission_System SHALL persist mission completion history (last 20 missions) for display in a mission log.
5. WHEN a prestige reset occurs, THE Mission_System SHALL cancel all active missions, return deployed ships, and reset the mission board without granting rewards.

### Requirement 11: Daily Quest Integration

**User Story:** As a player, I want Fleet Missions to integrate with the existing daily quest system, so that missions contribute to my daily goals.

#### Acceptance Criteria

1. THE Mission_System SHALL add a new QuestType (COMPLETE_MISSION) to the daily quest pool.
2. WHEN a player successfully completes a fleet mission, THE Mission_System SHALL increment progress on any active COMPLETE_MISSION daily quests.
3. THE Mission_System SHALL support quest templates requiring completion of 1, 2, or 3 missions per day.
