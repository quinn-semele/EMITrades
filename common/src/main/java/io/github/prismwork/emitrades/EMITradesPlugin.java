package io.github.prismwork.emitrades;

import com.google.common.collect.ImmutableSet;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.prismwork.emitrades.config.EMITradesConfig;
import io.github.prismwork.emitrades.recipe.VillagerTrade;
import io.github.prismwork.emitrades.util.EntityEmiStack;
import io.github.prismwork.emitrades.util.TradeProfile;
import io.github.prismwork.emitrades.util.XPlatUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

@EmiEntrypoint
public class EMITradesPlugin implements EmiPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("EMI Trades");
    public static final VillagerProfession WANDERING_TRADER_PLACEHOLDER = new VillagerProfession(
            "wandering_trader",
            entry -> false,
            entry -> false,
            ImmutableSet.<Item>builder().build(),
            ImmutableSet.<Block>builder().build(),
            SoundEvents.ENTITY_WANDERING_TRADER_YES
    );
    public static final EmiRecipeCategory VILLAGER_TRADES
            = new EmiRecipeCategory(Identifier.of("emitrades", "villager_trades"), EmiStack.of(Items.EMERALD));
    public static EMITradesConfig.Config CONFIG;
    private static final File CONFIG_FILE = XPlatUtils.getConfigPath().resolve("emitrades.json5").toFile();

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void register(EmiRegistry registry) {
        CONFIG = EMITradesConfig.load(CONFIG_FILE);
        registry.addCategory(VILLAGER_TRADES);
        Random random = Random.create();
        for (VillagerProfession profession : Registries.VILLAGER_PROFESSION) {
            VillagerEntity villager = (VillagerEntity)
                    Registries.ENTITY_TYPE.get(Identifier.of("minecraft", "villager")).create(MinecraftClient.getInstance().world);
            if (villager != null) {
                villager.setVillagerData(villager.getVillagerData().withProfession(profession).withLevel(5));
                registry.addWorkstation(VILLAGER_TRADES, EntityEmiStack.ofScaled(villager, 8.0f));
            }
            AtomicInteger id = new AtomicInteger();
            Int2ObjectMap<TradeOffers.Factory[]> offers = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession);
            if (offers == null || offers.isEmpty()) continue;
            int level = 0;
            while (level < 5) {
                VillagerEntity villager1 = (VillagerEntity)
                        Registries.ENTITY_TYPE.get(Identifier.of("minecraft", "villager")).create(MinecraftClient.getInstance().world);
                if (villager1 != null) {
                    villager1.setVillagerData(villager1.getVillagerData().withProfession(profession).withLevel(level + 1));
                }
                for (TradeOffers.Factory offer : offers.get(level + 1)) {
                    if (isVanillaFactory(offer)) {
                        registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(profession, offer, level + 1, villager1), id.get()));
                        id.getAndIncrement();
                    } else {
                        try {
                            int attempts = 5;
                            TreeSet<TradeOffer> genOffers = new TreeSet<>(this::compareOffers);
                            TradeOffer inOffer;
                            while (attempts > 0) {
                                inOffer = offer.create(MinecraftClient.getInstance().player, random);
                                if (genOffers.add(inOffer))
                                    attempts++;
                                else
                                    attempts--;
                            }
                            int finalLevel = level;
                            genOffers.forEach(tradeOffer -> {
                                registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(profession, new FakeFactory(tradeOffer), finalLevel + 1, villager1), id.get()));
                                id.getAndIncrement();
                            });
                        } catch (Exception ignored) {}
                    }
                }
                level++;
            }
        }
        WanderingTraderEntity wanderingTrader = (WanderingTraderEntity) Registries.ENTITY_TYPE.get(Identifier.of("minecraft", "wandering_trader"))
                .create(MinecraftClient.getInstance().world);
        registry.addWorkstation(VILLAGER_TRADES, EntityEmiStack.of(wanderingTrader));
        AtomicInteger wanderingTraderId = new AtomicInteger();
        TradeOffers.WANDERING_TRADER_TRADES.forEach((lvl, offers) -> {
            for (TradeOffers.Factory offer : offers) {
                if (isVanillaFactory(offer)) {
                    registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(WANDERING_TRADER_PLACEHOLDER, offer, lvl, wanderingTrader), wanderingTraderId.get()));
                    wanderingTraderId.getAndIncrement();
                } else {
                    try {
                        int attempts = 5;
                        TreeSet<TradeOffer> genOffers = new TreeSet<>(this::compareOffers);
                        TradeOffer inOffer;
                        while (attempts > 0) {
                            inOffer = offer.create(MinecraftClient.getInstance().player, random);
                            if (genOffers.add(inOffer))
                                attempts++;
                            else
                                attempts--;
                        }
                        int finalLevel = lvl;
                        genOffers.forEach(tradeOffer -> {
                            registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(WANDERING_TRADER_PLACEHOLDER, new FakeFactory(tradeOffer), finalLevel, wanderingTrader), wanderingTraderId.get()));
                            wanderingTraderId.getAndIncrement();
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
        LOGGER.info("Reloaded.");
    }

    private static boolean isVanillaFactory(TradeOffers.Factory offer) {
        return offer instanceof TradeOffers.SellSuspiciousStewFactory ||
                offer instanceof TradeOffers.SellEnchantedToolFactory ||
                offer instanceof TradeOffers.EnchantBookFactory ||
                offer instanceof TradeOffers.SellMapFactory ||
                offer instanceof TradeOffers.SellPotionHoldingItemFactory ||
                offer instanceof TradeOffers.SellDyedArmorFactory ||
                offer instanceof TradeOffers.TypeAwareBuyForOneEmeraldFactory ||
                offer instanceof TradeOffers.SellItemFactory ||
                offer instanceof TradeOffers.BuyItemFactory ||
                offer instanceof TradeOffers.ProcessItemFactory;
    }

    private int compareOffers(@NotNull TradeOffer a, @NotNull TradeOffer b) {
        int diff = Registries.ITEM.getRawId(a.getOriginalFirstBuyItem().getItem()) - Registries.ITEM.getRawId(b.getOriginalFirstBuyItem().getItem());
        if (diff != 0) return diff;
        diff = a.getSecondBuyItem().map(offer -> Registries.ITEM.getRawId(offer.item().value())).orElse(Registries.ITEM.size()) - b.getSecondBuyItem().map(offer -> Registries.ITEM.getRawId(offer.item().value())).orElse(Registries.ITEM.size());
        if (diff != 0) return diff;
        diff = Registries.ITEM.getRawId(a.getSellItem().getItem()) - Registries.ITEM.getRawId(b.getSellItem().getItem());
        return diff;
    }

    @ApiStatus.Internal
    public static final class FakeFactory
            implements TradeOffers.Factory {
        public final ItemStack first;
        public final ItemStack second;
        public final ItemStack sell;

        public FakeFactory(TradeOffer offer) {
            this.first = offer.getOriginalFirstBuyItem();
            this.second = offer.getSecondBuyItem().map(TradedItem::itemStack).orElse(ItemStack.EMPTY);
            this.sell = offer.getSellItem();
        }

        @Nullable
        @Override
        public TradeOffer create(Entity entity, Random random) {
            throw new AssertionError("Nobody should use this");
        }
    }
}
