"""
StarFleet Idle - Progression Simulation
Simulates optimal play without real-money boosts.
Target: ~6 months (180 days) to complete.
"""
import math

# === SHIP TIERS ===
SHIP_TIERS = [
    {"id": "probe", "name": "Recon Probe", "baseCost": 10, "baseIncome": 0.2, "basePower": 1, "costMult": 1.17, "unlockPower": 0},
    {"id": "shuttle", "name": "Scout Shuttle", "baseCost": 150, "baseIncome": 1.5, "basePower": 5, "costMult": 1.17, "unlockPower": 8},
    {"id": "corvette", "name": "Patrol Corvette", "baseCost": 2500, "baseIncome": 10, "basePower": 25, "costMult": 1.17, "unlockPower": 60},
    {"id": "frigate", "name": "Cargo Frigate", "baseCost": 50000, "baseIncome": 60, "basePower": 130, "costMult": 1.17, "unlockPower": 400},
    {"id": "cruiser", "name": "Mining Cruiser", "baseCost": 1200000, "baseIncome": 350, "basePower": 700, "costMult": 1.17, "unlockPower": 2500},
    {"id": "destroyer", "name": "Battle Destroyer", "baseCost": 35000000, "baseIncome": 2200, "basePower": 4500, "costMult": 1.17, "unlockPower": 15000},
    {"id": "battlecruiser", "name": "Battlecruiser", "baseCost": 1.2e9, "baseIncome": 15000, "basePower": 30000, "costMult": 1.17, "unlockPower": 100000},
    {"id": "carrier", "name": "Carrier Flagship", "baseCost": 50e9, "baseIncome": 100000, "basePower": 200000, "costMult": 1.17, "unlockPower": 800000},
    {"id": "titan", "name": "Titan Warship", "baseCost": 2.5e12, "baseIncome": 750000, "basePower": 1500000, "costMult": 1.17, "unlockPower": 6000000},
    {"id": "dreadnought", "name": "Dreadnought", "baseCost": 150e12, "baseIncome": 6000000, "basePower": 12000000, "costMult": 1.17, "unlockPower": 50000000},
    {"id": "leviathan", "name": "Leviathan", "baseCost": 10e15, "baseIncome": 50000000, "basePower": 100000000, "costMult": 1.17, "unlockPower": 500000000},
    {"id": "dyson", "name": "Dyson Sphere", "baseCost": 800e15, "baseIncome": 500000000, "basePower": 1000000000, "costMult": 1.17, "unlockPower": 5000000000},
]

# === SECTORS ===
SECTORS = [
    {"id": "solar", "name": "Solar System", "unlockFP": 0, "costMult": 1.0, "incomeMult": 1.0, "prevPenalty": 1.0},
    {"id": "nebula", "name": "Orion Nebula", "unlockFP": 500000, "costMult": 5.0, "incomeMult": 2.5, "prevPenalty": 0.08},
    {"id": "deepspace", "name": "Deep Space", "unlockFP": 30000000, "costMult": 35.0, "incomeMult": 8.0, "prevPenalty": 0.05},
    {"id": "core", "name": "Galactic Core", "unlockFP": 800000000, "costMult": 250.0, "incomeMult": 25.0, "prevPenalty": 0.03},
    {"id": "void", "name": "The Void", "unlockFP": 3000000000, "costMult": 200.0, "incomeMult": 60.0, "prevPenalty": 0.01},
]

MILESTONE_THRESHOLDS = [10, 25, 50]

def milestone_mult(count):
    d = sum(1 for t in MILESTONE_THRESHOLDS if count >= t)
    if count >= 100:
        d += count // 100
    return 2 ** d

def prestige_coins(fp):
    if fp < 1000:
        return 0
    return int(math.sqrt(fp / 1000))

def prestige_mult(coins):
    return 1.0 + coins * 0.05


class SimState:
    def __init__(self):
        self.credits = 25.0
        self.star_coins = 0
        self.prestige_count = 0
        self.sectors = {s["id"]: {t["id"]: 0 for t in SHIP_TIERS} for s in SECTORS}
        self.active_sector_idx = 0

    def total_fleet_power(self):
        total = 0
        for sector in SECTORS:
            ships = self.sectors[sector["id"]]
            sp = 0
            for tier in SHIP_TIERS:
                count = ships[tier["id"]]
                sp += count * tier["basePower"] * milestone_mult(count)
            total += sp * sector["incomeMult"]
        return total

    def credits_per_second(self):
        total_income = 0
        for idx, sector in enumerate(SECTORS):
            ships = self.sectors[sector["id"]]
            if idx < self.active_sector_idx:
                penalty = sector["prevPenalty"]
            elif idx == self.active_sector_idx:
                penalty = 1.0
            else:
                penalty = 0.0
            if penalty <= 0:
                continue

            sector_income = 0
            for tier in SHIP_TIERS:
                count = ships[tier["id"]]
                if count == 0:
                    continue
                sector_income += tier["baseIncome"] * count * milestone_mult(count) * sector["incomeMult"]
            total_income += sector_income * penalty

        return total_income * prestige_mult(self.star_coins)

    def ship_cost(self, tier_id, sector_idx):
        tier = next(t for t in SHIP_TIERS if t["id"] == tier_id)
        sector = SECTORS[sector_idx]
        count = self.sectors[sector["id"]][tier_id]
        return tier["baseCost"] * sector["costMult"] * (tier["costMult"] ** count)

    def buy_best_ship(self):
        sector = SECTORS[self.active_sector_idx]
        ships = self.sectors[sector["id"]]
        fp = self.total_fleet_power()

        best_ratio = 0
        best_tier = None

        for tier in SHIP_TIERS:
            if fp < tier["unlockPower"]:
                continue
            cost = self.ship_cost(tier["id"], self.active_sector_idx)
            if cost > self.credits:
                continue

            count = ships[tier["id"]]
            old_income = tier["baseIncome"] * count * milestone_mult(count) * sector["incomeMult"]
            new_income = tier["baseIncome"] * (count + 1) * milestone_mult(count + 1) * sector["incomeMult"]
            income_gain = new_income - old_income
            ratio = income_gain / cost

            if ratio > best_ratio:
                best_ratio = ratio
                best_tier = tier["id"]

        if best_tier:
            cost = self.ship_cost(best_tier, self.active_sector_idx)
            self.credits -= cost
            self.sectors[sector["id"]][best_tier] += 1
            return True
        return False

    def should_prestige(self):
        # Don't prestige if we're in The Void and making good progress
        if self.active_sector_idx >= 4:
            return False
        coins = prestige_coins(self.total_fleet_power())
        if coins <= 0:
            return False
        # Only prestige if we'd gain at least 50% more coins than current
        return coins >= max(10, int(self.star_coins * 0.5))

    def do_prestige(self):
        coins = prestige_coins(self.total_fleet_power())
        self.star_coins += coins
        self.prestige_count += 1
        self.credits = 25.0
        self.sectors = {s["id"]: {t["id"]: 0 for t in SHIP_TIERS} for s in SECTORS}
        self.active_sector_idx = 0

    def try_advance_sector(self):
        if self.active_sector_idx >= len(SECTORS) - 1:
            return False
        next_sector = SECTORS[self.active_sector_idx + 1]
        if self.total_fleet_power() >= next_sector["unlockFP"]:
            self.active_sector_idx += 1
            return True
        return False


def format_num(v):
    if v >= 1e33: return f"{v/1e33:.2f}Dc"
    if v >= 1e30: return f"{v/1e30:.2f}No"
    if v >= 1e27: return f"{v/1e27:.2f}Oc"
    if v >= 1e24: return f"{v/1e24:.2f}Sp"
    if v >= 1e21: return f"{v/1e21:.2f}Sx"
    if v >= 1e18: return f"{v/1e18:.2f}Qi"
    if v >= 1e15: return f"{v/1e15:.2f}Qa"
    if v >= 1e12: return f"{v/1e12:.2f}T"
    if v >= 1e9: return f"{v/1e9:.2f}B"
    if v >= 1e6: return f"{v/1e6:.2f}M"
    if v >= 1e3: return f"{v/1e3:.2f}K"
    if v >= 1: return f"{v:.1f}"
    return f"{v:.2f}"


def main():
    state = SimState()

    ACTIVE_SECONDS_PER_DAY = 1800  # 30 min
    OFFLINE_SECONDS_PER_DAY = 84600  # 23.5 hours
    OFFLINE_EFFICIENCY = 0.4
    TICK_INTERVAL = 60  # 1 min ticks during active play
    TARGET_DAYS = 400  # simulate up to ~13 months

    milestones = []
    last_sector_unlocked = 0
    game_complete = False

    print("=" * 70)
    print("STARFLEET IDLE - PROGRESSION SIMULATION")
    print("=" * 70)
    print("Assumptions: 30 min active play/day, optimal strategy, no IAP")
    print("-" * 70)

    for day in range(1, TARGET_DAYS + 1):
        # Active play phase
        active_left = ACTIVE_SECONDS_PER_DAY
        while active_left > 0:
            cps = state.credits_per_second()
            tick = min(TICK_INTERVAL, active_left)
            state.credits += cps * tick
            active_left -= tick

            # Buy ships
            bought = True
            while bought:
                bought = state.buy_best_ship()

            # Advance sector
            state.try_advance_sector()

            # Prestige check
            if state.should_prestige():
                state.do_prestige()

        # Offline phase
        cps = state.credits_per_second()
        state.credits += cps * OFFLINE_SECONDS_PER_DAY * OFFLINE_EFFICIENCY

        # Check milestones
        fp = state.total_fleet_power()
        while last_sector_unlocked < len(SECTORS) - 1:
            nxt = SECTORS[last_sector_unlocked + 1]
            if fp >= nxt["unlockFP"]:
                last_sector_unlocked += 1
                milestones.append(f"Day {day:4d}: Unlocked {nxt['name']} (FP: {format_num(fp)})")
            else:
                break

        # Check completion
        void_dysons = state.sectors["void"]["dyson"]
        if void_dysons >= 3 and not game_complete:
            game_complete = True
            milestones.append(f"Day {day:4d}: 🎉 GAME COMPLETE (3 Dyson Spheres in The Void)")

        # Weekly report
        if day % 7 == 0 or day == 1:
            cps = state.credits_per_second()
            print(f"Day {day:4d} | CPS: {format_num(cps):>12s} | FP: {format_num(fp):>12s} | "
                  f"Coins: {state.star_coins:4d} | Sector: {SECTORS[state.active_sector_idx]['name']:<14s} | "
                  f"Prestiges: {state.prestige_count}")

        if game_complete:
            break

    print("-" * 70)
    print("\nMILESTONES:")
    for m in milestones:
        print(f"  {m}")

    print("\n" + "=" * 70)
    if game_complete:
        print(f"RESULT: Game completed in {day} days (~{day//30} months)")
    else:
        print(f"RESULT: Game NOT completed in {TARGET_DAYS} days")
        print(f"  Final FP: {format_num(state.total_fleet_power())}")
        print(f"  Final CPS: {format_num(state.credits_per_second())}")
        print(f"  Active Sector: {SECTORS[state.active_sector_idx]['name']}")
        print(f"  Void Dysons: {state.sectors['void']['dyson']}")
        print(f"  Star Coins: {state.star_coins}")
    print("=" * 70)


if __name__ == "__main__":
    main()
