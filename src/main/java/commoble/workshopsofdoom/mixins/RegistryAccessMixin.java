package commoble.workshopsofdoom.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.collect.ImmutableMap;

import commoble.workshopsofdoom.MixinHooks;
import net.minecraft.core.RegistryAccess;

@Mixin(RegistryAccess.class)
public abstract class RegistryAccessMixin
{
	private RegistryAccessMixin() {}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Inject(method="lambda$static$0", at=@At(value="INVOKE", target="Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;"), locals=LocalCapture.CAPTURE_FAILHARD)
	private static void whenBuildBuiltinRegistries(CallbackInfoReturnable<ImmutableMap> cir, ImmutableMap.Builder builder)
	{
		MixinHooks.whenBuildBuiltinRegistries(builder);
	}
}
