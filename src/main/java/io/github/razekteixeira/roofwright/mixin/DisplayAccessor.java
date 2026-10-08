package io.github.razekteixeira.roofwright.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.math.Transformation;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;

/** Display setters are private in 26.3; the preview ghosts need them before the entity data is sent. */
@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setGlowColorOverride")
	void roofwright$setGlowColorOverride(int color);

	@Invoker("setBrightnessOverride")
	void roofwright$setBrightnessOverride(Brightness brightness);

	@Invoker("setTransformation")
	void roofwright$setTransformation(Transformation transformation);

	@Invoker("setViewRange")
	void roofwright$setViewRange(float range);
}
