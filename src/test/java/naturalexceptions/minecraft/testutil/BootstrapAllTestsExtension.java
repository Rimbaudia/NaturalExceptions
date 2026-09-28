package naturalexceptions.minecraft.testutil;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import naturalexceptions.minecraft.registry.NEItems;

public final class BootstrapAllTestsExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        NEItems.ensureBootstrapped();
    }
}
