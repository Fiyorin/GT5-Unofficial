package gregtech.loaders.oreprocessing;

import static gregtech.api.util.GT_Recipe.GT_Recipe_Map.sCentrifugeRecipes;
import static gregtech.api.util.GT_Recipe.GT_Recipe_Map.sElectrolyzerRecipes;
import static gregtech.api.util.GT_Recipe.GT_Recipe_Map.sVacuumRecipes;
import static gregtech.api.util.GT_RecipeBuilder.TICKS;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IOreRecipeRegistrator;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_RecipeBuilder;
import gregtech.api.util.GT_Utility;

public class ProcessingCell implements IOreRecipeRegistrator {

    public ProcessingCell() {
        OrePrefixes.cell.add(this);
        OrePrefixes.cellPlasma.add(this);
    }

    @Override
    public void registerOre(OrePrefixes aPrefix, Materials aMaterial, String aOreDictName, String aModName,
        ItemStack aStack) {
        switch (aPrefix) {
            case cell -> {
                if (aMaterial == Materials.Empty) {
                    GT_ModHandler.removeRecipeByOutputDelayed(aStack);
                    if (aModName.equalsIgnoreCase("AtomicScience")) {
                        GT_ModHandler.addExtractionRecipe(ItemList.Cell_Empty.get(1L), aStack);
                    }
                } else {
                    if (aMaterial.mFuelPower > 0) {
                        GT_Values.RA.addFuel(
                            GT_Utility.copyAmount(1L, aStack),
                            GT_Utility.getFluidForFilledItem(aStack, true) == null
                                ? GT_Utility.getContainerItem(aStack, true)
                                : null,
                            aMaterial.mFuelPower,
                            aMaterial.mFuelType);
                    }
                    if ((aMaterial.mMaterialList.size() > 0) && ((aMaterial.mExtraData & 0x3) != 0)) {
                        int tAllAmount = 0;
                        for (MaterialStack tMat2 : aMaterial.mMaterialList) {
                            tAllAmount = (int) (tAllAmount + tMat2.mAmount);
                        }
                        long tItemAmount = 0L;
                        long tCapsuleCount = GT_ModHandler.getCapsuleCellContainerCountMultipliedWithStackSize(aStack)
                            * -tAllAmount;
                        long tDensityMultiplier = aMaterial.getDensity() > 3628800L ? aMaterial.getDensity() / 3628800L
                            : 1L;
                        ArrayList<ItemStack> tList = new ArrayList<>();
                        for (MaterialStack tMat : aMaterial.mMaterialList) {
                            if (tMat.mAmount > 0L) {
                                ItemStack tStack;
                                if (tMat.mMaterial == Materials.Air) {
                                    tStack = ItemList.Cell_Air.get(tMat.mAmount * tDensityMultiplier / 2L);
                                } else {
                                    tStack = GT_OreDictUnificator.get(OrePrefixes.dust, tMat.mMaterial, tMat.mAmount);
                                    if (tStack == null) {
                                        tStack = GT_OreDictUnificator
                                            .get(OrePrefixes.cell, tMat.mMaterial, tMat.mAmount);
                                    }
                                }
                                if (tItemAmount + tMat.mAmount * 3628800L
                                    <= aStack.getMaxStackSize() * aMaterial.getDensity()) {
                                    tItemAmount += tMat.mAmount * 3628800L;
                                    if (tStack != null) {
                                        tStack.stackSize = ((int) (tStack.stackSize * tDensityMultiplier));
                                        while ((tStack.stackSize > 64)
                                            && (tCapsuleCount + GT_ModHandler.getCapsuleCellContainerCount(tStack) * 64L
                                                < 0L ? tList.size() < 5 : tList.size() < 6)
                                            && (tCapsuleCount + GT_ModHandler.getCapsuleCellContainerCount(tStack) * 64L
                                                <= 64L)) {
                                            tCapsuleCount += GT_ModHandler.getCapsuleCellContainerCount(tStack) * 64L;
                                            tList.add(GT_Utility.copyAmount(64L, tStack));
                                            tStack.stackSize -= 64;
                                        }
                                        int tThisCapsuleCount = GT_ModHandler
                                            .getCapsuleCellContainerCountMultipliedWithStackSize(tStack);
                                        if (tStack.stackSize > 0 && tCapsuleCount + tThisCapsuleCount <= 64L) {
                                            if (tCapsuleCount + tThisCapsuleCount < 0L ? tList.size() < 5
                                                : tList.size() < 6) {
                                                tCapsuleCount += tThisCapsuleCount;
                                                tList.add(tStack);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        tItemAmount = GT_Utility.ceilDiv(tItemAmount * tDensityMultiplier, aMaterial.getDensity());
                        if (tList.size() > 0) {
                            if ((aMaterial.mExtraData & 0x1) != 0) {
                                // Electrolyzer recipe
                                {
                                    if (GT_Utility.getFluidForFilledItem(aStack, true) == null) {
                                        // dust stuffed cell e.g. Phosphate, Phosphorous Pentoxide
                                        GT_RecipeBuilder recipeBuilder = GT_Values.RA.stdBuilder();
                                        if (tCapsuleCount > 0L) {
                                            recipeBuilder.itemInputs(
                                                GT_Utility.copyAmount(tItemAmount, aStack),
                                                ItemList.Cell_Empty.get(tCapsuleCount));
                                        } else {
                                            recipeBuilder.itemInputs(GT_Utility.copyAmount(tItemAmount, aStack));
                                        }
                                        if (tCapsuleCount < 0L) {
                                            tList.add(ItemList.Cell_Empty.get(-tCapsuleCount));
                                        }
                                        ItemStack[] outputsArray = tList
                                            .toArray(new ItemStack[Math.min(tList.size(), 6)]);
                                        recipeBuilder.itemOutputs(outputsArray)
                                            .noFluidInputs()
                                            .noFluidOutputs()
                                            .duration(Math.max(1L, Math.abs(aMaterial.getProtons() * 2L * tItemAmount)))
                                            .eut(Math.min(4, tList.size()) * 30)
                                            .addTo(sElectrolyzerRecipes);
                                    } else {
                                        long tCellBalance = tCapsuleCount + tItemAmount - 1;
                                        GT_RecipeBuilder recipeBuilder = GT_Values.RA.stdBuilder();
                                        if (tCellBalance > 0L) {
                                            recipeBuilder.itemInputs(aStack, ItemList.Cell_Empty.get(tCellBalance));
                                        } else {
                                            recipeBuilder.itemInputs(GT_Utility.copyAmount(tItemAmount, aStack));
                                        }
                                        if (tCellBalance < 0L) {
                                            tList.add(ItemList.Cell_Empty.get(-tCellBalance));
                                        }
                                        ItemStack[] outputsArray = tList
                                            .toArray(new ItemStack[Math.min(tList.size(), 6)]);
                                        recipeBuilder.itemOutputs(outputsArray)
                                            .noFluidInputs()
                                            .noFluidOutputs()
                                            .duration(Math.max(1L, Math.abs(aMaterial.getProtons() * 8L * tItemAmount)))
                                            .eut(Math.min(4, tList.size()) * 30)
                                            .addTo(sElectrolyzerRecipes);
                                    }
                                }
                            }
                            if ((aMaterial.mExtraData & 0x2) != 0) {
                                GT_RecipeBuilder recipeBuilder = GT_Values.RA.stdBuilder();
                                if (tCapsuleCount > 0L) {
                                    recipeBuilder.itemInputs(
                                        GT_Utility.copyAmount(tItemAmount, aStack),
                                        ItemList.Cell_Empty.get(tCapsuleCount));
                                } else {
                                    recipeBuilder.itemInputs(GT_Utility.copyAmount(tItemAmount, aStack));
                                }
                                if (tCapsuleCount < 0L) {
                                    tList.add(ItemList.Cell_Empty.get(-tCapsuleCount));
                                }
                                ItemStack[] outputsArray = tList.toArray(new ItemStack[Math.min(tList.size(), 6)]);
                                recipeBuilder.itemOutputs(outputsArray)
                                    .noFluidInputs()
                                    .noFluidOutputs()
                                    .duration(Math.max(1L, Math.abs(aMaterial.getMass() * 2L * tItemAmount)))
                                    .eut(5)
                                    .addTo(sCentrifugeRecipes);
                            }
                        }
                    }
                }
            }
            case cellPlasma -> {
                if (aMaterial == Materials.Empty) {
                    GT_ModHandler.removeRecipeByOutputDelayed(aStack);
                } else {
                    GT_Values.RA.addFuel(
                        GT_Utility.copyAmount(1L, aStack),
                        GT_Utility.getFluidForFilledItem(aStack, true) == null
                            ? GT_Utility.getContainerItem(aStack, true)
                            : null,
                        (int) Math.max(1024L, 1024L * aMaterial.getMass()),
                        4);
                    if (GT_OreDictUnificator.get(OrePrefixes.cell, aMaterial, 1L) != null) {
                        GT_Values.RA.stdBuilder()
                            .itemInputs(GT_Utility.copyAmount(1L, aStack))
                            .itemOutputs(GT_OreDictUnificator.get(OrePrefixes.cell, aMaterial, 1L))
                            .noFluidInputs()
                            .noFluidOutputs()
                            .duration(((int) Math.max(aMaterial.getMass() * 2L, 1L)) * TICKS)
                            .eut(TierEU.RECIPE_MV)
                            .addTo(sVacuumRecipes);
                    }
                }
            }
            default -> {}
        }
    }
}
