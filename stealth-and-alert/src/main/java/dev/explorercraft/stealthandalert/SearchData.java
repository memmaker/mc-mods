package dev.explorercraft.stealthandalert;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/// Where a searching mob is walking right now, and how long it stands there looking around.
public record SearchData(boolean searchingAround, boolean moving, int stayTicks, Optional<Vec3> targetPos) {
    public static final Codec<SearchData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("searching_around", false).forGetter(SearchData::searchingAround),
                    Codec.BOOL.optionalFieldOf("moving", true).forGetter(SearchData::moving),
                    Codec.INT.optionalFieldOf("stay_ticks", 0).forGetter(SearchData::stayTicks),
                    Vec3.CODEC.optionalFieldOf("target_pos").forGetter(SearchData::targetPos)
            ).apply(instance, SearchData::new)
    );

    public static final SearchData DEFAULT = new SearchData(false, true, 0, Optional.empty());
}
