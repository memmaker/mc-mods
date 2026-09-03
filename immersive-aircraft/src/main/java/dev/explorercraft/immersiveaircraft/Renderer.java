package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.AirshipEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.BambooHopperEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.BiplaneEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.CargoAirshipEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.GyrodyneEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.QuadrocopterEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.WarshipEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.bullet.BulletEntityRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.bullet.TinyTNTRenderer;
import dev.explorercraft.immersiveaircraft.cobalt.registration.Registration;

public class Renderer {
    public static void bootstrap() {
        Registration.register(Entities.GYRODYNE.get(), GyrodyneEntityRenderer::new);
        Registration.register(Entities.BIPLANE.get(), BiplaneEntityRenderer::new);
        Registration.register(Entities.AIRSHIP.get(), AirshipEntityRenderer::new);
        Registration.register(Entities.CARGO_AIRSHIP.get(), CargoAirshipEntityRenderer::new);
        Registration.register(Entities.WARSHIP.get(), WarshipEntityRenderer::new);
        Registration.register(Entities.QUADROCOPTER.get(), QuadrocopterEntityRenderer::new);
        Registration.register(Entities.BAMBOO_HOPPER.get(), BambooHopperEntityRenderer::new);

        Registration.register(Entities.BULLET.get(), BulletEntityRenderer::new);
        Registration.register(Entities.TINY_TNT.get(), TinyTNTRenderer::new);
    }
}
