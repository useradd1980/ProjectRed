package mrtjp.projectred.expansion.part;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import mrtjp.projectred.api.pneumatics.PneumaticPayload;
import mrtjp.projectred.api.pneumatics.PneumaticPayloadData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class PneumaticTubePayload implements PneumaticPayload {

    private static final String PAYLOAD_DATA_KEY = "projectred_payload_data";

    public static final int MAX_PROGRESS = 255;

    private int progress;
    private int speed;

    private int inputSide = -1;
    private int outputSide = -1;

    private ItemStack itemStack;
    private final PneumaticPayloadData customData = new PneumaticPayloadData();

    public PneumaticTubePayload(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public PneumaticTubePayload() {
        this(ItemStack.EMPTY);
    }

    public int getProgress() {
        return progress;
    }

    public int getSpeed() {
        return speed;
    }

    public void setProgress(int prog) {
        progress = prog;
    }

    public void setSpeed(int spd) {
        speed = spd;
    }

    public void incrementProgress() {
        progress += speed;
    }

    public void setInputSide(int s) {
        inputSide = s;
    }

    public void setOutputSide(int s) {
        outputSide = s;
    }

    public int getInputSide() {
        return inputSide;
    }

    public int getOutputSide() {
        return outputSide;
    }

    public boolean hasOutputSide() {
        return outputSide != -1;
    }

    public void resetOutput() {
        outputSide = -1;
    }

    public void resetProgress() {
        progress = Math.max(0, progress - MAX_PROGRESS);
    }

    public boolean isPassedHalfWay() {
        return progress > MAX_PROGRESS / 2;
    }

    public int getCurrentSide() {
        return progress < MAX_PROGRESS / 2 ? inputSide : outputSide;
    }

    @Override
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public boolean hasData(ResourceLocation key) {
        return customData.has(key);
    }

    @Override
    public CompoundTag getData(ResourceLocation key) {
        return customData.get(key);
    }

    @Override
    public void setData(ResourceLocation key, CompoundTag value) {
        customData.put(key, value);
    }

    @Override
    public void removeData(ResourceLocation key) {
        customData.remove(key);
    }

    public void save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        tag.putInt("progress", progress); //TODO size
        tag.putInt("speed", speed);
        tag.putInt("input_dir", inputSide);
        tag.putInt("output_dir", outputSide);
        tag.put("item_stack", itemStack.save(lookupProvider));
        if (customData.isEmpty()) {
            tag.remove(PAYLOAD_DATA_KEY);
        } else {
            tag.put(PAYLOAD_DATA_KEY, customData.save());
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        progress = tag.getInt("progress");
        speed = tag.getInt("speed");
        inputSide = tag.getInt("input_dir");
        outputSide = tag.getInt("output_dir");
        itemStack = ItemStack.parseOptional(lookupProvider, tag.getCompound("item_stack"));
        customData.load(tag.contains(PAYLOAD_DATA_KEY, Tag.TAG_COMPOUND)
                ? tag.getCompound(PAYLOAD_DATA_KEY) : new CompoundTag());
    }

    public void writeDesc(MCDataOutput output) {
        output.writeByte(progress);
        output.writeByte(speed);
        output.writeByte(inputSide);
        output.writeByte(outputSide);
        output.writeItemStack(itemStack);
        output.writeCompoundNBT(customData.save());
    }

    public void readDesc(MCDataInput input) {
        progress = input.readByte();
        speed = input.readByte();
        inputSide = input.readByte();
        outputSide = input.readByte();
        itemStack = input.readItemStack();
        customData.load(input.readCompoundNBT());
    }
}
