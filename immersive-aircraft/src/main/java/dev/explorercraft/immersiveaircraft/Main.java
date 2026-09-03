package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.network.NetworkManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mariuszgromada.math.mxparser.License;

public final class Main {
    public static final String SHORT_MOD_ID = "ic_air";
    public static final String MOD_ID = "immersiveaircraft";
    public static String MOD_LOADER = "unknown";
    public static final Logger LOGGER = LogManager.getLogger();
    public static NetworkManager networkManager;
    public static CameraGetter cameraGetter = () -> Vec3.ZERO;
    public static FirstPersonGetter firstPersonGetter = () -> false;

    public static float frameTime = 0.0f;

    static {
        License.iConfirmNonCommercialUse("Conczin");
    }

    public static Identifier locate(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public interface CameraGetter {
        Vec3 getPosition();
    }

    public interface FirstPersonGetter {
        boolean isFirstPerson();
    }
}
