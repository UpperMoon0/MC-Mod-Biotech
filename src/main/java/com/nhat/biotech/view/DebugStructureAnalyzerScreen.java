package com.nhat.biotech.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nhat.biotech.Biotech;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DebugStructureAnalyzerScreen extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Biotech.MOD_ID, "textures/gui/debug_structure_analyzer.png");

    private final Level level;

    private EditBox firstCornerX;
    private EditBox firstCornerY;
    private EditBox firstCornerZ;
    private EditBox secondCornerX;
    private EditBox secondCornerY;
    private EditBox secondCornerZ;

    public DebugStructureAnalyzerScreen(Level level) {
        super(Component.literal("Debug Structure Analyzer"));
        this.level = level;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // First Corner
        this.firstCornerX = new EditBox(this.font, centerX - 70, centerY - 40, 40, 15, Component.literal("X"));
        this.firstCornerY = new EditBox(this.font, centerX - 20, centerY - 40, 40, 15, Component.literal("Y"));
        this.firstCornerZ = new EditBox(this.font, centerX + 30, centerY - 40, 40, 15, Component.literal("Z"));
        this.addRenderableWidget(this.firstCornerX);
        this.addRenderableWidget(this.firstCornerY);
        this.addRenderableWidget(this.firstCornerZ);

        // Second Corner
        this.secondCornerX = new EditBox(this.font, centerX - 70, centerY + 10, 40, 15, Component.literal("X"));
        this.secondCornerY = new EditBox(this.font, centerX - 20, centerY + 10, 40, 15, Component.literal("Y"));
        this.secondCornerZ = new EditBox(this.font, centerX + 30, centerY + 10, 40, 15, Component.literal("Z"));
        this.addRenderableWidget(this.secondCornerX);
        this.addRenderableWidget(this.secondCornerY);
        this.addRenderableWidget(this.secondCornerZ);

        // Save Button
        Button saveButton = Button.builder(Component.literal("Save"), this::onSave)
                .pos(centerX - 50, centerY + 50)
                .size(100, 20)
                .build();
        this.addRenderableWidget(saveButton);

        // Labels
        StringWidget firstCornerLabel = new StringWidget(centerX - 89, centerY - 60, 100, 20, Component.literal("First Corner"), this.font);
        StringWidget secondCornerLabel = new StringWidget(centerX - 84, centerY - 10, 100, 20, Component.literal("Second Corner"), this.font);
        this.addRenderableWidget(firstCornerLabel);
        this.addRenderableWidget(secondCornerLabel);
    }

    private void onSave(Button button) {
        int x1 = Integer.parseInt(this.firstCornerX.getValue());
        int y1 = Integer.parseInt(this.firstCornerY.getValue());
        int z1 = Integer.parseInt(this.firstCornerZ.getValue());
        int x2 = Integer.parseInt(this.secondCornerX.getValue());
        int y2 = Integer.parseInt(this.secondCornerY.getValue());
        int z2 = Integer.parseInt(this.secondCornerZ.getValue());

        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        List<List<String>> pattern = new ArrayList<>();
        Map<String, String> mapping = new HashMap<>();
        char currentChar = 'a';

        // Generate the pattern and mapping by iterating over the structure area
        for (int y = maxY; y >= minY; y--) {
            List<String> layer = new ArrayList<>();
            for (int z = minZ - 1; z <= maxZ - 1; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    String blockKey = Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(state.getBlock())).toString();

                    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                        blockKey += "[facing=" + facing.getName() + "]";
                    }

                    if (blockKey.equals("minecraft:air")) {
                        row.append(" ");
                    } else {
                        String finalBlockKey = blockKey;
                        String symbol = mapping.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(finalBlockKey))
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse(null);

                        if (symbol == null) {
                            symbol = String.valueOf(currentChar);
                            mapping.put(symbol, blockKey);
                            currentChar++;
                        }

                        row.append(symbol);
                    }
                }
                layer.add(row.toString());
            }
            pattern.add(layer);
        }

        // Call methods to write JSON and TXT files
        writeJson(pattern, mapping);
        writeTxt(pattern, mapping);
    }

    private void writeJson(List<List<String>> pattern, Map<String, String> mapping) {
        // Create the JSON structure
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("type", "patchouli:multiblock");

        Map<String, Object> multiblockData = new HashMap<>();
        multiblockData.put("pattern", pattern);
        multiblockData.put("mapping", mapping);
        multiblockData.put("symmetrical", true);
        jsonData.put("multiblock", multiblockData);

        // Serialize to JSON and save to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("D:\\Dev\\Workspace\\Java\\MCMods\\Biotech-Minecraft-Mod\\src\\generated\\resources\\output\\structures\\structure_patchouli.json")) {
            gson.toJson(jsonData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTxt(List<List<String>> pattern, Map<String, String> mapping) {
        try (FileWriter writer = new FileWriter("D:\\Dev\\Workspace\\Java\\MCMods\\Biotech-Minecraft-Mod\\src\\generated\\resources\\output\\structures\\structure_pattern.txt")) {
            writer.write("@Override\n");
            writer.write("public StructurePattern getStructurePattern()\n{\n");

            // Write block declarations
            writer.write("    Block ");
            List<String> blockVars = new ArrayList<>();
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String var = entry.getKey();
                String block = entry.getValue().replace("minecraft:", "Blocks.").replace("biotech:", "BlockRegistries.");
                blockVars.add(var + " = " + block);
            }
            writer.write(String.join(",\n            ", blockVars) + ";\n\n");

            // Write block array
            writer.write("    Block[][][] blockArray = new Block[][][]{\n");
            for (List<String> layer : pattern) {
                writer.write("        {\n");
                for (String row : layer) {
                    writer.write("            {");
                    List<String> blockRow = new ArrayList<>();
                    for (char symbol : row.toCharArray()) {
                        blockRow.add(symbol == ' ' ? "a" : String.valueOf(symbol));
                    }
                    writer.write(String.join(", ", blockRow));
                    writer.write("},\n");
                }
                writer.write("        },\n");
            }
            writer.write("    };\n\n");
            writer.write("    return new StructurePattern(blockArray, false);\n}\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int screenHeight = 166;
        int screenWidth = 176;
        graphics.blit(TEXTURE, (this.width - screenWidth) / 2, (this.height - screenHeight) / 2, 0, 0, screenWidth, screenHeight);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.firstCornerX.render(graphics, mouseX, mouseY, partialTicks);
        this.firstCornerY.render(graphics, mouseX, mouseY, partialTicks);
        this.firstCornerZ.render(graphics, mouseX, mouseY, partialTicks);
        this.secondCornerX.render(graphics, mouseX, mouseY, partialTicks);
        this.secondCornerY.render(graphics, mouseX, mouseY, partialTicks);
        this.secondCornerZ.render(graphics, mouseX, mouseY, partialTicks);
    }
}