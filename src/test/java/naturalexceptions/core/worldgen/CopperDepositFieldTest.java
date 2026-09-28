package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;

final class CopperDepositFieldTest {

    @Test
    void copperDepositsOnlyUseTheirDeclaredBasalticHostContext() {
        CopperDepositField field = new CopperDepositField(new OverworldGeneration(882731L, WorldgenEpoch.INITIAL));
        int accepted = 0;
        for (int cellX = -80; cellX < 80; cellX++) {
            for (int cellZ = -80; cellZ < 80; cellZ++) {
                CopperDeposit deposit = field.depositAtCell(cellX, cellZ);
                if (deposit != null) {
                    accepted++;
                    assertEquals(GeologicalProvince.BASALTIC, deposit.hostProvince());
                }
            }
        }
        assertTrue(accepted > 100, "copper remains practical to find across an ordinary explored area");
    }

    @Test
    void depositIdentityAndBlockShapeAreStableAcrossChunkQueries() {
        CopperDepositField first = new CopperDepositField(new OverworldGeneration(-0x123456789L, WorldgenEpoch.of(3)));
        CopperDepositField second = new CopperDepositField(new OverworldGeneration(-0x123456789L, WorldgenEpoch.of(3)));
        Set<String> firstIds = new HashSet<>();
        Set<String> secondIds = new HashSet<>();
        for (int chunkX = -16; chunkX <= 16; chunkX++) {
            for (int chunkZ = -16; chunkZ <= 16; chunkZ++) {
                List<CopperDeposit> deposits = first.depositsForChunk(chunkX, chunkZ);
                assertEquals(deposits, second.depositsForChunk(chunkX, chunkZ));
                for (CopperDeposit deposit : deposits) firstIds.add(
                    deposit.id()
                        .hex());
            }
        }
        assertTrue(firstIds.size() > 0);
        assertEquals(firstIds, secondIdsFor(first, second));
    }

    @Test
    void ordinarySeedProducesEnoughCopperForEarlyProgression() {
        CopperDepositField field = new CopperDepositField(new OverworldGeneration(2309187741L, WorldgenEpoch.CURRENT));
        Set<String> deposits = new HashSet<>();
        int potentialOreBlocks = 0;
        for (int chunkX = 0; chunkX < 32; chunkX++) {
            for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                for (CopperDeposit deposit : field.depositsForChunk(chunkX, chunkZ)) {
                    deposits.add(
                        deposit.id()
                            .hex());
                    int extent = deposit.horizontalExtent();
                    for (int x = deposit.centerX() - extent; x <= deposit.centerX() + extent; x++) {
                        for (int z = deposit.centerZ() - extent; z <= deposit.centerZ() + extent; z++) {
                            for (int y = deposit.centerY() - deposit.verticalExtent(); y
                                <= deposit.centerY() + deposit.verticalExtent(); y++) {
                                if (deposit.contains(x, y, z)) potentialOreBlocks++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(deposits.size() >= 24, "prospecting a modest area should encounter multiple basaltic seams");
        assertTrue(potentialOreBlocks >= 500, "seams should yield enough host ore for starter metallurgy");
    }

    private Set<String> secondIdsFor(CopperDepositField first, CopperDepositField second) {
        Set<String> ids = new HashSet<>();
        for (int chunkX = -16; chunkX <= 16; chunkX++) {
            for (int chunkZ = -16; chunkZ <= 16; chunkZ++) {
                for (CopperDeposit deposit : second.depositsForChunk(chunkX, chunkZ)) ids.add(
                    deposit.id()
                        .hex());
            }
        }
        return ids;
    }
}
