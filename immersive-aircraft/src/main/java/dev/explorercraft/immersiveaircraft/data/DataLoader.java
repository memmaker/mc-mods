package dev.explorercraft.immersiveaircraft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleStat;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleUpgrade;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

// ponytail: SimpleJsonResourceReloadListener switched from (Gson, directory) to
// (Codec<T>, FileToIdConverter). Using Codec<JsonElement> keeps every subclass's hand-rolled
// JsonObject parsing untouched instead of redesigning them around typed codecs.
public abstract class DataLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public DataLoader(String directory) {
        super(ExtraCodecs.JSON, FileToIdConverter.json(directory));
    }

    @NotNull
    static VehicleUpgrade getAircraftUpgrade(JsonObject jsonObject) {
        VehicleUpgrade upgrade = new VehicleUpgrade();
        for (String key : jsonObject.keySet()) {
            VehicleStat stat = VehicleStat.STATS.get(key);
            if (stat != null) {
                upgrade.set(stat, jsonObject.get(key).getAsFloat());
            }
        }
        return upgrade;
    }
}
