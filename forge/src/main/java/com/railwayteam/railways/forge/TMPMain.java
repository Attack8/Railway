package com.railwayteam.railways.forge;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
/*
This class is just for testing asm transformation and should not be called during normal mod use
 */
@ApiStatus.Internal
@Deprecated(forRemoval = true) // not actually going to remove this, just want using it to be a very bad warning
public class TMPMain {
    public static void main(String[] args) {
        CRLaunchPluginService plugin = new CRLaunchPluginService();
        Path path = Path.of("/home/sam/MinecraftFabric/Railway/forge/run/config/in.class");
        ClassReader reader;
        try {
            reader = new ClassReader(Files.readAllBytes(path));
        } catch (IOException ignored) {
            System.out.println("Failed");
            return;
        }
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        System.out.println("Processing...");
        plugin.processRollingMode(classNode);
        System.out.println("Success!");
    }
}
