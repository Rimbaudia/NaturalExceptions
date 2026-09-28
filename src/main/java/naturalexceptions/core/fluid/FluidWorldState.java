package naturalexceptions.core.fluid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.AmbientReservoir;
import naturalexceptions.core.thermal.FiniteThermalBody;
import naturalexceptions.core.thermal.ThermalTransfer;
import naturalexceptions.core.thermal.ThermodynamicModel;

/** Persistable semantic fluid cells plus transient, bounded connected-region rebuild state. */
public final class FluidWorldState {

    public static final long MAX_CELL_VOLUME_MICROLITRES = 1_000_000L;
    public static final int MAX_CELLS = 32_768;
    // Effective cell-face geometry, calibrated so legacy copper (401 W/mK) retains the old 0.05 W/K rate.
    private static final double AMBIENT_CELL_GEOMETRY_M = 0.05 / 401.0;

    private static final class Rebuild {

        final Deque<FluidCellKey> frontier = new ArrayDeque<>();
        final Set<FluidCellKey> visited = new LinkedHashSet<>();

        Rebuild(FluidCellKey seed) {
            frontier.add(seed);
        }
    }

    private final int maxQueuedSeeds;
    private final Map<FluidCellKey, FluidParcel> cells = new LinkedHashMap<>();
    private final Map<FluidCellKey, FluidSourceBoundary> sourceBoundaries = new LinkedHashMap<>();
    private final Map<FluidCellKey, FluidRegion> regionsByCell = new LinkedHashMap<>();
    private final Set<FluidRegion> regions = new LinkedHashSet<>();
    private final LinkedHashSet<FluidCellKey> activeThermalCells = new LinkedHashSet<>();
    private final Deque<FluidCellKey> thermalWorkQueue = new ArrayDeque<>();
    private final LinkedHashSet<FluidCellKey> dirtySeeds = new LinkedHashSet<>();
    private final LinkedHashSet<FluidCellKey> hostPhaseChanges = new LinkedHashSet<>();
    private Rebuild rebuild;
    private long cellsVisited;

    public FluidWorldState(int maxQueuedSeeds) {
        if (maxQueuedSeeds <= 0) throw new IllegalArgumentException("Queue bound must be positive");
        this.maxQueuedSeeds = maxQueuedSeeds;
    }

    public FluidWorldState() {
        this(MAX_CELLS);
    }

    public synchronized void put(FluidCellKey key, FluidParcel parcel) {
        if (key == null || parcel == null) throw new IllegalArgumentException("Cell and parcel are required");
        if (cells.containsKey(key)) throw new IllegalStateException("Fluid cell already contains matter: " + key);
        if (cells.size() >= MAX_CELLS) throw new IllegalStateException("P9 fluid cell capacity reached");
        if (parcel.volumeMicrolitres() > MAX_CELL_VOLUME_MICROLITRES) {
            throw new IllegalArgumentException("One cell cannot contain more than one block-volume");
        }
        requireQueueRoom(key);
        if (parcel.reconcilePhase()) hostPhaseChanges.add(key);
        invalidateAround(key);
        cells.put(key, parcel);
        refreshThermalActivity(key, parcel);
        enqueue(key);
        enqueueNeighbors(key);
    }

    public synchronized FluidParcel remove(FluidCellKey key) {
        if (key == null) return null;
        invalidateAround(key);
        FluidParcel removed = cells.remove(key);
        activeThermalCells.remove(key);
        sourceBoundaries.remove(key);
        enqueueNeighbors(key);
        return removed;
    }

    public synchronized FluidParcel parcelAt(FluidCellKey key) {
        return cells.get(key);
    }

    /** Advances only hot/non-equilibrated cells; stable fluid cells never poll the ambient reservoir. */
    public synchronized int processThermalWork(double elapsedSeconds, int cellBudget) {
        return processThermalWork(elapsedSeconds, cellBudget, key -> true);
    }

    /** Loaded-world variant: active thermal history pauses while its chunk is unloaded. */
    public synchronized int processThermalWork(double elapsedSeconds, int cellBudget,
        java.util.function.Predicate<FluidCellKey> loadedCell) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0.0)
            throw new IllegalArgumentException("Elapsed seconds must be finite and nonnegative");
        if (loadedCell == null) throw new NullPointerException("loadedCell");
        if (cellBudget <= 0 || elapsedSeconds == 0.0) return 0;
        int worked = 0;
        int inspected = 0;
        int initialQueueSize = thermalWorkQueue.size();
        while (worked < cellBudget && inspected < initialQueueSize && !thermalWorkQueue.isEmpty()) {
            FluidCellKey key = thermalWorkQueue.pollFirst();
            inspected++;
            FluidParcel parcel = cells.get(key);
            if (parcel == null || !activeThermalCells.contains(key)) {
                activeThermalCells.remove(key);
                worked++;
                continue;
            }
            if (!loadedCell.test(key)) {
                thermalWorkQueue.addLast(key);
                continue;
            }
            worked++;
            int bandBefore = incandescenceBand(
                parcel.thermalState()
                    .temperatureKelvin());
            FiniteThermalBody body = new FiniteThermalBody(parcel.thermalState());
            ThermalTransfer.exchangeElapsed(
                body,
                new AmbientReservoir(ThermodynamicModel.DEFAULT_AMBIENT_TEMPERATURE_K),
                ambientConductance(parcel),
                elapsedSeconds);
            if (parcel.reconcilePhase() || incandescenceBand(
                parcel.thermalState()
                    .temperatureKelvin())
                != bandBefore) hostPhaseChanges.add(key);
            refreshThermalActivity(key, parcel);
            if (activeThermalCells.contains(key)) thermalWorkQueue.addLast(key);
        }
        return worked;
    }

    /**
     * Coarse visible-glow band (0 none, 1 dull red, 2 bright, 3 white-hot). A band change is a visible host change
     * even without a phase change, so it is queued with phase changes for host refresh.
     */
    public static int incandescenceBand(double kelvin) {
        if (kelvin >= 1300.0) return 3;
        if (kelvin >= 1000.0) return 2;
        if (kelvin >= 798.0) return 1;
        return 0;
    }

    private static double ambientConductance(FluidParcel parcel) {
        double conductivity = parcel.thermalState()
            .properties()
            .thermalConductivity();
        if (!Double.isFinite(conductivity) || conductivity <= 0.0) conductivity = 0.1;
        return Math.max(0.001, Math.min(0.25, conductivity * AMBIENT_CELL_GEOMETRY_M));
    }

    private void refreshThermalActivity(FluidCellKey key, FluidParcel parcel) {
        if (parcel.thermalState()
            .isEquilibratedWith(ThermodynamicModel.DEFAULT_AMBIENT_TEMPERATURE_K, 2.0)) activeThermalCells.remove(key);
        else if (activeThermalCells.add(key)) thermalWorkQueue.addLast(key);
    }

    public synchronized int activeThermalCellCount() {
        return activeThermalCells.size();
    }

    /** Drains phase changes for event-driven Minecraft block-host synchronization. */
    public synchronized List<FluidCellKey> drainHostPhaseChanges(int budget) {
        return drainHostPhaseChanges(budget, key -> true);
    }

    /** Leaves unloaded host changes queued for the next chunk-load opportunity. */
    public synchronized List<FluidCellKey> drainHostPhaseChanges(int budget,
        java.util.function.Predicate<FluidCellKey> loadedCell) {
        if (loadedCell == null) throw new NullPointerException("loadedCell");
        if (budget <= 0 || hostPhaseChanges.isEmpty()) return Collections.emptyList();
        List<FluidCellKey> drained = new ArrayList<>(Math.min(budget, hostPhaseChanges.size()));
        int inspected = 0;
        int inspectLimit = Math.min(budget, hostPhaseChanges.size());
        while (drained.size() < budget && inspected < inspectLimit && !hostPhaseChanges.isEmpty()) {
            FluidCellKey key = hostPhaseChanges.iterator()
                .next();
            hostPhaseChanges.remove(key);
            inspected++;
            if (loadedCell.test(key)) drained.add(key);
            else hostPhaseChanges.add(key);
        }
        return Collections.unmodifiableList(drained);
    }

    public synchronized void registerSourceBoundary(FluidCellKey key, FluidSourceBoundary boundary) {
        if (key == null || boundary == null) throw new IllegalArgumentException("Source key and boundary are required");
        FluidParcel parcel = cells.get(key);
        if (parcel != null && !parcel.materialRef()
            .equals(boundary.materialRef()))
            throw new IllegalArgumentException("Boundary material does not match the represented cell");
        sourceBoundaries.put(key, boundary);
    }

    public synchronized FluidSourceBoundary sourceAt(FluidCellKey key) {
        return sourceBoundaries.get(key);
    }

    /** Transfers a finite vessel portion to an empty spatial cell. */
    public synchronized boolean spillFrom(FluidVessel vessel, FluidCellKey key, MaterialQuantity amount) {
        if (vessel == null || key == null || amount == null || cells.containsKey(key)) return false;
        FluidParcel preview = vessel.contents()
            .isEmpty() ? null
                : vessel.contents()
                    .get(0);
        if (preview == null || amount.compareTo(preview.mass()) > 0) return false;
        if (amount.portion(1.0)
            .isZero()) return false;
        double volume = preview.volumeMicrolitres() * (double) amount.milligrams()
            / preview.mass()
                .milligrams();
        if (volume > MAX_CELL_VOLUME_MICROLITRES) return false;
        if (cells.size() >= MAX_CELLS || !hasQueueRoom(key)) return false;
        FluidParcel spilled = vessel.drain(amount);
        if (spilled == null) return false;
        if (spilled.volumeMicrolitres() > MAX_CELL_VOLUME_MICROLITRES) {
            vessel.restoreDrained(spilled);
            return false;
        }
        try {
            put(key, spilled);
        } catch (RuntimeException refused) {
            // Only rebuild bookkeeping can fail after the cell accepted the parcel; the matter is then in the world.
            if (cells.get(key) == spilled) return true;
            vessel.restoreDrained(spilled);
            return false;
        }
        return true;
    }

    /** Limited one-cell displacement; no flow is advanced unless a caller explicitly disturbs/moves the cell. */
    public synchronized boolean move(FluidCellKey from, FluidCellKey to) {
        if (from == null || to == null || !cells.containsKey(from) || cells.containsKey(to)) return false;
        // Removal and placement together can enqueue both neighbourhoods; refuse before moving any matter.
        if (!hasQueueRoom(from) || !hasQueueRoom(to)
            || dirtySeeds.size() + 14 + regionSize(from) + regionSize(to) > maxQueuedSeeds) return false;
        FluidParcel parcel = remove(from);
        put(to, parcel);
        return true;
    }

    /** A disturbance schedules only its connected body and face-adjacent liquid cells. */
    public synchronized void disturb(FluidCellKey key) {
        if (key == null) return;
        invalidateAround(key);
        enqueue(key);
        enqueueNeighbors(key);
    }

    /** Conservative upper bound on the dirty seeds a put at {@code key} can enqueue, checked before mutation. */
    private boolean hasQueueRoom(FluidCellKey key) {
        Set<FluidRegion> affected = new LinkedHashSet<>();
        FluidRegion own = regionsByCell.get(key);
        if (own != null) affected.add(own);
        int needed = 1;
        for (FluidCellKey n : key.neighbors()) {
            FluidRegion region = regionsByCell.get(n);
            if (region != null) affected.add(region);
            if (cells.containsKey(n)) needed++;
        }
        for (FluidRegion region : affected) needed += region.cells()
            .size();
        return dirtySeeds.size() + needed <= maxQueuedSeeds;
    }

    private int regionSize(FluidCellKey key) {
        FluidRegion region = regionsByCell.get(key);
        return region == null ? 0
            : region.cells()
                .size();
    }

    private void requireQueueRoom(FluidCellKey key) {
        if (!hasQueueRoom(key)) throw new IllegalStateException("Fluid dirty seed queue capacity exceeded");
    }

    private void invalidateAround(FluidCellKey key) {
        Set<FluidRegion> affected = new LinkedHashSet<>();
        FluidRegion own = regionsByCell.get(key);
        if (own != null) affected.add(own);
        for (FluidCellKey n : key.neighbors()) {
            FluidRegion region = regionsByCell.get(n);
            if (region != null) affected.add(region);
        }
        for (FluidRegion region : affected) {
            regions.remove(region);
            for (FluidCellKey member : region.cells()) {
                regionsByCell.remove(member);
                enqueue(member);
            }
        }
        rebuild = null;
    }

    private void enqueueNeighbors(FluidCellKey key) {
        for (FluidCellKey n : key.neighbors()) if (cells.containsKey(n)) enqueue(n);
    }

    private void enqueue(FluidCellKey key) {
        if (cells.containsKey(key) && !regionsByCell.containsKey(key)) {
            if (dirtySeeds.size() >= maxQueuedSeeds) {
                throw new IllegalStateException("Fluid dirty seed queue capacity exceeded");
            }
            dirtySeeds.add(key);
        }
    }

    /** Visits at most {@code cellBudget} cells and resumes an unfinished flood fill next call. */
    public synchronized int processDirty(int cellBudget) {
        if (cellBudget <= 0) return 0;
        int worked = 0;
        while (worked < cellBudget) {
            if (rebuild == null) {
                FluidCellKey seed = pollSeed();
                if (seed == null) break;
                rebuild = new Rebuild(seed);
            }
            FluidCellKey next = rebuild.frontier.pollFirst();
            if (next == null) {
                finishRebuild();
                continue;
            }
            if (!cells.containsKey(next) || regionsByCell.containsKey(next) || !rebuild.visited.add(next)) continue;
            worked++;
            cellsVisited++;
            for (FluidCellKey neighbor : next.neighbors()) {
                if (cells.containsKey(neighbor) && !regionsByCell.containsKey(neighbor)
                    && !rebuild.visited.contains(neighbor)) rebuild.frontier.addLast(neighbor);
            }
            if (rebuild.frontier.isEmpty()) finishRebuild();
        }
        return worked;
    }

    private FluidCellKey pollSeed() {
        while (!dirtySeeds.isEmpty()) {
            FluidCellKey seed = dirtySeeds.iterator()
                .next();
            dirtySeeds.remove(seed);
            if (cells.containsKey(seed) && !regionsByCell.containsKey(seed)) return seed;
        }
        return null;
    }

    private void finishRebuild() {
        if (rebuild == null) return;
        if (!rebuild.visited.isEmpty()) {
            FluidRegion region = new FluidRegion(rebuild.visited);
            regions.add(region);
            for (FluidCellKey member : region.cells()) regionsByCell.put(member, region);
        }
        rebuild = null;
    }

    public synchronized FluidRegion regionAt(FluidCellKey key) {
        return regionsByCell.get(key);
    }

    public synchronized boolean isDirty(FluidCellKey key) {
        return dirtySeeds.contains(key)
            || (rebuild != null && (rebuild.visited.contains(key) || rebuild.frontier.contains(key)));
    }

    public synchronized int regionCount() {
        return regions.size();
    }

    public synchronized int sleepingRegionCount() {
        return regions.size();
    }

    public synchronized int dirtyCellCount() {
        return dirtySeeds.size() + (rebuild == null ? 0 : rebuild.visited.size() + rebuild.frontier.size());
    }

    public synchronized long cellsVisited() {
        return cellsVisited;
    }

    public synchronized Map<FluidCellKey, FluidParcel> snapshotCells() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(cells));
    }

    public synchronized Map<FluidCellKey, FluidSourceBoundary> sourceBoundariesSnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(sourceBoundaries));
    }

    public synchronized MaterialQuantity totalMass() {
        MaterialQuantity result = MaterialQuantity.ZERO;
        for (FluidParcel p : cells.values()) result = result.add(p.mass());
        return result;
    }
}
