package minecrafttransportsimulator.systems;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.Set;
import java.util.List;
import java.util.WeakHashMap;

import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

public final class TemperatureModCompat {
    private static final UUID LSO_TEMP_MODIFIER_ID = UUID.fromString("d4c6cbb9-5f31-4e20-8b31-40c954b04f3f");
    private static final double LSO_STEP = 0.05D;

    private static boolean initialized;
    private static boolean tanPresent;
    private static boolean lsoPresent;
    private static boolean coldSweatPresent;

    private static final Set<UUID> airConditionedPlayers = new HashSet<>();
    private static final Set<Object> airConditionedPlayerObjects = Collections.newSetFromMap(new WeakHashMap<>());

    private static Class<?> mcPlayerClass;
    private static Class<?> mcLivingEntityClass;
    private static Method mcGetUUIDMethod;
    private static Method mcGetUUIDMethodObf;
    private static Method mcGetUniqueIDMethod;
    private static Method mcGetUniqueIDMethodObf;

    private static Method tanGetTemperatureData;
    private static Method tanSetLevel;
    private static Method tanSetTargetLevel;
    private static Method tanSetChangeDelayTicks;
    private static Method tanSetHyperthermiaTicks;
    private static Method tanSetHypothermiaTicks;
    private static Method tanSetExtremityDelayTicks;
    private static Method tanSetLastLevel;
    private static Method tanSetLastHyperthermiaTicks;
    private static Method tanSetDryTicks;
    private static Method tanSetLastNearbyThermoregulators;
    private static Method tanSetNearbyThermoregulators;
    private static Object tanNeutralLevel;
    private static Object tanPlayerModifier;
    private static Field tanPlayerModifiersField;
    private static boolean tanClemencyInitialized;
    private static Object tanClemencyEffect;
    private static Constructor<?> tanClemencyEffectInstanceCtor;
    private static Method tanPlayerAddEffectMethod;
    private static boolean tanLegacy;
    private static Method tanLegacySetTemperature;
    private static Method tanLegacySetChangeTime;
    private static Method tanLegacyGetScaleMidpoint;
    private static Constructor<?> tanLegacyTemperatureCtor;
    private static Object tanLegacyNeutralTemperature;
    private static Method tanLegacyRegisterModifier;
    private static Class<?> tanLegacyTemperatureClass;

    private static Method lsoGetTargetTemp;
    private static Method lsoAddTempModifier;
    private static double lsoNormalTemp;
    private static Method lsoGetTempCapability;
    private static Object lsoTempCapabilityToken;
    private static Field lsoTempUtilInternalField;
    private static Object lsoTempUtilInternalOriginal;
    private static boolean lsoTempUtilProxyInstalled;
    private static Object lsoTempImmunityEffect;
    private static Method lsoRegistryObjectGet;
    private static Constructor<?> lsoEffectInstanceCtor;
    private static Method lsoPlayerAddEffect;
    private static Method lsoPlayerRemoveEffect;
    private static Field lsoTempCapabilityField;
    private static Method lsoGetCapabilityWithDir;
    private static Method lsoGetCapabilityNoDir;
    private static Method lsoLazyOptionalOrElse;
    private static Method lsoLazyOptionalResolve;
    private static Method lsoOptionalIsPresent;
    private static Method lsoOptionalGet;
    private static Method lsoSetTempLevel;
    private static Method lsoSetTargetTempLevel;
    private static Method lsoSetTempTickTimer;
    private static Method lsoSetFreezeTickTimer;

    private static Method csSetTemp;
    private static Method csGetNeutralTemp;
    private static Object csCoreTrait;
    private static Object csBodyTrait;

    private TemperatureModCompat() {
    }

    public static void setAirConditioned(IWrapperPlayer rider, boolean active) {
        if (rider == null) {
            return;
        }
        if (!ConfigSystem.settings.general.performModCompatFunctions.value) {
            return;
        }
        init();
        Object player = getMinecraftPlayer(rider);
        if (player == null) {
            return;
        }
        UUID playerId = getPlayerUUID(player);
        if (active) {
            if (playerId != null) {
                airConditionedPlayers.add(playerId);
            } else {
                airConditionedPlayerObjects.add(player);
            }
            applyAirConditioningIfActive(player);
        } else {
            if (playerId != null) {
                airConditionedPlayers.remove(playerId);
            } else {
                airConditionedPlayerObjects.remove(player);
            }
            if (lsoPresent) {
                clearLegendarySurvivalOverhaul(player);
            }
        }
    }

    public static void applyAirConditioningIfActive(Object player) {
        if (player == null) {
            return;
        }
        if (!ConfigSystem.settings.general.performModCompatFunctions.value) {
            return;
        }
        init();
        if (!isAirConditioned(player)) {
            return;
        }
        if (tanPresent) {
            applyToughAsNails(player);
        }
        if (lsoPresent) {
            applyLegendarySurvivalOverhaul(player);
        }
        if (coldSweatPresent) {
            applyColdSweat(player);
        }
    }

    public static void applyToughAsNailsClemency1182(Object player) {
        if (player == null) {
            return;
        }
        if (!ConfigSystem.settings.general.performModCompatFunctions.value) {
            return;
        }
        init();
        if (!tanPresent) {
            return;
        }
        if (!isAirConditioned(player)) {
            return;
        }
        initToughAsNailsClemency();
        if (tanClemencyEffect == null || tanClemencyEffectInstanceCtor == null || tanPlayerAddEffectMethod == null) {
            return;
        }
        try {
            Object effectInstance = createEffectInstance(tanClemencyEffect, tanClemencyEffectInstanceCtor, 60);
            if (effectInstance != null) {
                tanPlayerAddEffectMethod.invoke(player, effectInstance);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void applyToughAsNailsClemency1201(Object player) {
        applyToughAsNailsClemency1182(player);
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (InterfaceManager.coreInterface != null) {
            tanPresent = InterfaceManager.coreInterface.isModPresent("toughasnails");
            lsoPresent = InterfaceManager.coreInterface.isModPresent("legendarysurvivaloverhaul");
            coldSweatPresent = InterfaceManager.coreInterface.isModPresent("cold_sweat");
        }
        try {
            mcPlayerClass = Class.forName("net.minecraft.world.entity.player.Player");
        } catch (Throwable ignored) {
            try {
                mcPlayerClass = Class.forName("net.minecraft.entity.player.PlayerEntity");
            } catch (Throwable ignored2) {
                try {
                    mcPlayerClass = Class.forName("net.minecraft.entity.player.EntityPlayer");
                } catch (Throwable ignored3) {
                }
            }
        }
        try {
            mcLivingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");
        } catch (Throwable ignored) {
            try {
                mcLivingEntityClass = Class.forName("net.minecraft.entity.LivingEntity");
            } catch (Throwable ignored2) {
                try {
                    mcLivingEntityClass = Class.forName("net.minecraft.entity.EntityLivingBase");
                } catch (Throwable ignored3) {
                }
            }
        }
        if (mcPlayerClass != null) {
            try {
                mcGetUUIDMethod = mcPlayerClass.getMethod("getUUID");
            } catch (Throwable ignored) {
            }
            try {
                mcGetUUIDMethodObf = mcPlayerClass.getMethod("m_20148_");
            } catch (Throwable ignored) {
            }
            try {
                mcGetUniqueIDMethod = mcPlayerClass.getMethod("getUniqueID");
            } catch (Throwable ignored) {
            }
            try {
                mcGetUniqueIDMethodObf = mcPlayerClass.getMethod("func_110124_au");
            } catch (Throwable ignored) {
            }
        }
        if (tanPresent) {
            initToughAsNails();
        }
        // Try LSO init even if mod presence detection fails, so class-based detection can still work.
        initLegendarySurvivalOverhaul();
        if (coldSweatPresent) {
            initColdSweat();
        }
    }

    private static void initToughAsNails() {
        try {
            Class<?> helperClass = Class.forName("toughasnails.api.temperature.TemperatureHelper");
            Class<?> tempLevelClass = Class.forName("toughasnails.api.temperature.TemperatureLevel");
            Class<?> tempDataClass = Class.forName("toughasnails.api.temperature.ITemperature");
            Class<?> modifierInterface = Class.forName("toughasnails.api.temperature.IPlayerTemperatureModifier");
            tanGetTemperatureData = helperClass.getMethod("getTemperatureData", mcPlayerClass);
            try {
                tanSetLevel = tempDataClass.getMethod("setLevel", tempLevelClass);
            } catch (Throwable ignored) {
                tanSetLevel = null;
            }
            tanSetTargetLevel = tempDataClass.getMethod("setTargetLevel", tempLevelClass);
            try {
                tanSetChangeDelayTicks = tempDataClass.getMethod("setChangeDelayTicks", int.class);
            } catch (Throwable ignored) {
                tanSetChangeDelayTicks = null;
            }
            try {
                tanSetHyperthermiaTicks = tempDataClass.getMethod("setHyperthermiaTicks", int.class);
            } catch (Throwable ignored) {
                tanSetHyperthermiaTicks = null;
            }
            try {
                tanSetHypothermiaTicks = tempDataClass.getMethod("setHypothermiaTicks", int.class);
            } catch (Throwable ignored) {
                tanSetHypothermiaTicks = null;
            }
            try {
                tanSetExtremityDelayTicks = tempDataClass.getMethod("setExtremityDelayTicks", int.class);
            } catch (Throwable ignored) {
                tanSetExtremityDelayTicks = null;
            }
            try {
                tanSetLastLevel = tempDataClass.getMethod("setLastLevel", tempLevelClass);
            } catch (Throwable ignored) {
                tanSetLastLevel = null;
            }
            try {
                tanSetLastHyperthermiaTicks = tempDataClass.getMethod("setLastHyperthermiaTicks", int.class);
            } catch (Throwable ignored) {
                tanSetLastHyperthermiaTicks = null;
            }
            try {
                tanSetDryTicks = tempDataClass.getMethod("setDryTicks", int.class);
            } catch (Throwable ignored) {
                tanSetDryTicks = null;
            }
            try {
                tanSetLastNearbyThermoregulators = tempDataClass.getMethod("setLastNearbyThermoregulators", Set.class);
            } catch (Throwable ignored) {
                tanSetLastNearbyThermoregulators = null;
            }
            try {
                tanSetNearbyThermoregulators = tempDataClass.getMethod("setNearbyThermoregulators", Set.class);
            } catch (Throwable ignored) {
                tanSetNearbyThermoregulators = null;
            }
            @SuppressWarnings("unchecked")
            Object neutral = Enum.valueOf((Class<Enum>) tempLevelClass, "NEUTRAL");
            tanNeutralLevel = neutral;
            Method registerModifier = helperClass.getMethod("registerPlayerTemperatureModifier", modifierInterface);
            Object proxy = Proxy.newProxyInstance(modifierInterface.getClassLoader(), new Class<?>[]{modifierInterface}, (proxyObj, method, args) -> {
                String name = method.getName();
                if ("modify".equals(name)) {
                    Object player = args[0];
                    Object level = args[1];
                    return isAirConditioned(player) ? tanNeutralLevel : level;
                }
                if ("toString".equals(name)) {
                    return "MTS_AirConditioning";
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxyObj);
                }
                if ("equals".equals(name)) {
                    return proxyObj == args[0];
                }
                return null;
            });
            tanPlayerModifier = proxy;
            try {
                registerModifier.invoke(null, proxy);
            } catch (Throwable ignored) {
            }
            try {
                Class<?> helperImplClass = Class.forName("toughasnails.temperature.TemperatureHelperImpl");
                tanPlayerModifiersField = helperImplClass.getDeclaredField("playerModifiers");
                tanPlayerModifiersField.setAccessible(true);
            } catch (Throwable ignored) {
                tanPlayerModifiersField = null;
            }
            tanLegacy = false;
            return;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> helperClass = Class.forName("toughasnails.api.temperature.TemperatureHelper");
            Class<?> tempClass = Class.forName("toughasnails.api.temperature.Temperature");
            Class<?> tempScaleClass = Class.forName("toughasnails.api.temperature.TemperatureScale");
            Class<?> tempDataClass = Class.forName("toughasnails.api.stat.capability.ITemperature");
            Class<?> modifierInterface = Class.forName("toughasnails.api.temperature.ITemperatureModifier");
            tanGetTemperatureData = helperClass.getMethod("getTemperatureData", mcPlayerClass);
            tanLegacySetTemperature = tempDataClass.getMethod("setTemperature", tempClass);
            tanLegacySetChangeTime = tempDataClass.getMethod("setChangeTime", int.class);
            tanLegacyGetScaleMidpoint = tempScaleClass.getMethod("getScaleMidpoint");
            tanLegacyTemperatureCtor = tempClass.getConstructor(int.class);
            tanLegacyTemperatureClass = tempClass;
            tanLegacyNeutralTemperature = null;
            tanLegacyRegisterModifier = helperClass.getMethod("registerTemperatureModifier", modifierInterface);
            Object proxy = Proxy.newProxyInstance(modifierInterface.getClassLoader(), new Class<?>[]{modifierInterface}, (proxyObj, method, args) -> {
                String name = method.getName();
                if ("applyPlayerModifiers".equals(name)) {
                    Object player = args[0];
                    Object temperature = args[1];
                    if (isAirConditioned(player)) {
                        return getLegacyNeutralTemperature();
                    }
                    return temperature;
                }
                if ("applyEnvironmentModifiers".equals(name)) {
                    return args[2];
                }
                if ("isPlayerSpecific".equals(name)) {
                    return true;
                }
                if ("getId".equals(name)) {
                    return "MTS_AirConditioning";
                }
                if ("toString".equals(name)) {
                    return "MTS_AirConditioning";
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxyObj);
                }
                if ("equals".equals(name)) {
                    return proxyObj == args[0];
                }
                return null;
            });
            tanLegacyRegisterModifier.invoke(null, proxy);
            tanLegacy = true;
            return;
        } catch (Throwable ignored) {
        }
        tanPresent = false;
        tanGetTemperatureData = null;
        tanSetLevel = null;
        tanSetTargetLevel = null;
        tanSetChangeDelayTicks = null;
        tanSetHyperthermiaTicks = null;
        tanSetHypothermiaTicks = null;
        tanSetExtremityDelayTicks = null;
        tanSetLastLevel = null;
        tanSetLastHyperthermiaTicks = null;
        tanSetDryTicks = null;
        tanSetLastNearbyThermoregulators = null;
        tanSetNearbyThermoregulators = null;
        tanNeutralLevel = null;
        tanPlayerModifier = null;
        tanPlayerModifiersField = null;
        tanLegacySetTemperature = null;
        tanLegacySetChangeTime = null;
        tanLegacyGetScaleMidpoint = null;
        tanLegacyTemperatureCtor = null;
        tanLegacyNeutralTemperature = null;
        tanLegacyRegisterModifier = null;
        tanLegacyTemperatureClass = null;
        tanLegacy = false;
    }

    private static void initToughAsNailsClemency() {
        if (tanClemencyInitialized) {
            return;
        }
        tanClemencyInitialized = true;
        tanClemencyEffect = resolveToughAsNailsEffect("toughasnails.api.potion.TANEffects", "CLIMATE_CLEMENCY");
        if (tanClemencyEffect == null) {
            tanClemencyEffect = resolveToughAsNailsEffect("toughasnails.init.ModPotions", "CLIMATE_CLEMENCY");
        }
        if (tanClemencyEffect == null) {
            tanClemencyEffect = resolveEffectFromRegistry("toughasnails", "climate_clemency");
        }
        if (tanClemencyEffect == null) {
            return;
        }
        try {
            Class<?> effectInstanceClass;
            try {
                effectInstanceClass = Class.forName("net.minecraft.world.effect.MobEffectInstance");
            } catch (Throwable ignored) {
                effectInstanceClass = Class.forName("net.minecraft.potion.EffectInstance");
            }
            tanClemencyEffectInstanceCtor = findEffectInstanceConstructorForEffect(tanClemencyEffect, effectInstanceClass);
            if (mcLivingEntityClass != null && tanClemencyEffectInstanceCtor != null) {
                try {
                    tanPlayerAddEffectMethod = mcLivingEntityClass.getMethod("addEffect", effectInstanceClass);
                } catch (Throwable ignored) {
                    tanPlayerAddEffectMethod = null;
                }
                if (tanPlayerAddEffectMethod == null) {
                    try {
                        tanPlayerAddEffectMethod = mcLivingEntityClass.getMethod("addPotionEffect", effectInstanceClass);
                    } catch (Throwable ignored) {
                        tanPlayerAddEffectMethod = null;
                    }
                }
            }
        } catch (Throwable ignored) {
            tanClemencyEffectInstanceCtor = null;
            tanPlayerAddEffectMethod = null;
        }
    }

    private static void initLegendarySurvivalOverhaul() {
        try {
            Class<?> tempUtilClass = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil");
            Class<?> tempEnumClass = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureEnum");
            lsoGetTargetTemp = tempUtilClass.getMethod("getPlayerTargetTemperature", mcPlayerClass);
            lsoAddTempModifier = tempUtilClass.getMethod("addTemperatureModifier", mcPlayerClass, double.class, UUID.class);
            lsoTempUtilInternalField = tempUtilClass.getField("internal");
            @SuppressWarnings("unchecked")
            Object normalEnum = Enum.valueOf((Class<Enum>) tempEnumClass, "NORMAL");
            boolean normalSet = false;
            try {
                Method getValue = tempEnumClass.getMethod("getValue");
                lsoNormalTemp = ((Number) getValue.invoke(normalEnum)).doubleValue();
                normalSet = true;
            } catch (Throwable ignored) {
            }
            if (!normalSet) {
                try {
                    Method getMiddle = tempEnumClass.getMethod("getMiddle");
                    lsoNormalTemp = ((Number) getMiddle.invoke(normalEnum)).doubleValue();
                    normalSet = true;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
            lsoGetTargetTemp = null;
            lsoAddTempModifier = null;
            lsoNormalTemp = 0D;
        }
        try {
            Class<?> providerClass = Class.forName("sfiomn.legendarysurvivaloverhaul.common.capabilities.temperature.TemperatureProvider");
            lsoTempCapabilityField = providerClass.getField("TEMPERATURE_CAPABILITY");
            Class<?> capabilityClass = Class.forName("net.minecraftforge.common.capabilities.Capability");
            Class<?> directionClass = getForgeDirectionClass();
            if (mcPlayerClass != null) {
                if (directionClass != null) {
                    try {
                        lsoGetCapabilityWithDir = mcPlayerClass.getMethod("getCapability", capabilityClass, directionClass);
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    lsoGetCapabilityNoDir = mcPlayerClass.getMethod("getCapability", capabilityClass);
                } catch (Throwable ignored) {
                }
            }
            Class<?> lazyOptionalClass = Class.forName("net.minecraftforge.common.util.LazyOptional");
            try {
                lsoLazyOptionalOrElse = lazyOptionalClass.getMethod("orElse", Object.class);
            } catch (Throwable ignored) {
            }
            try {
                lsoLazyOptionalResolve = lazyOptionalClass.getMethod("resolve");
            } catch (Throwable ignored) {
            }
            if (lsoLazyOptionalResolve != null) {
                Class<?> optionalClass = Class.forName("java.util.Optional");
                lsoOptionalIsPresent = optionalClass.getMethod("isPresent");
                lsoOptionalGet = optionalClass.getMethod("get");
            }
            Class<?> tempCapClass = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.ITemperatureCapability");
            lsoSetTempLevel = tempCapClass.getMethod("setTemperatureLevel", float.class);
            lsoSetTargetTempLevel = tempCapClass.getMethod("setTargetTemperatureLevel", float.class);
            lsoSetTempTickTimer = tempCapClass.getMethod("setTemperatureTickTimer", int.class);
            lsoSetFreezeTickTimer = tempCapClass.getMethod("setFreezeTickTimer", int.class);
        } catch (Throwable ignored) {
            lsoTempCapabilityField = null;
            lsoGetCapabilityWithDir = null;
            lsoGetCapabilityNoDir = null;
            lsoLazyOptionalOrElse = null;
            lsoLazyOptionalResolve = null;
            lsoOptionalIsPresent = null;
            lsoOptionalGet = null;
            lsoSetTempLevel = null;
            lsoSetTargetTempLevel = null;
            lsoSetTempTickTimer = null;
            lsoSetFreezeTickTimer = null;
        }
        try {
            Class<?> capabilityUtilClass = Class.forName("sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil");
            lsoGetTempCapability = capabilityUtilClass.getMethod("getTempCapability", mcPlayerClass);
        } catch (Throwable ignored) {
            lsoGetTempCapability = null;
        }
        try {
            Class<?> lsoMainClass = Class.forName("sfiomn.legendarysurvivaloverhaul.LegendarySurvivalOverhaul");
            lsoTempCapabilityToken = lsoMainClass.getField("TEMPERATURE_CAP").get(null);
        } catch (Throwable ignored) {
            lsoTempCapabilityToken = null;
        }
        try {
            Class<?> effectRegistryClass = Class.forName("sfiomn.legendarysurvivaloverhaul.registry.EffectRegistry");
            Object registryObject = effectRegistryClass.getField("TEMPERATURE_IMMUNITY").get(null);
            if (registryObject != null) {
                lsoRegistryObjectGet = registryObject.getClass().getMethod("get");
                lsoTempImmunityEffect = lsoRegistryObjectGet.invoke(registryObject);
            }
        } catch (Throwable ignored) {
            lsoTempImmunityEffect = null;
            lsoRegistryObjectGet = null;
        }
        if (lsoTempImmunityEffect != null) {
            initLegendarySurvivalOverhaulEffectHooks();
        }
        initLegendarySurvivalOverhaulTemperatureUtilProxy();
        if (lsoGetTargetTemp == null && lsoTempCapabilityField == null && lsoTempCapabilityToken == null && lsoGetTempCapability == null) {
            lsoPresent = false;
        } else {
            lsoPresent = true;
        }
        if (lsoNormalTemp == 0D && (lsoGetTargetTemp != null || lsoTempCapabilityField != null || lsoGetTempCapability != null)) {
            lsoNormalTemp = 20D;
        }
    }

    private static void initColdSweat() {
        try {
            Class<?> tempClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
            Class<?> traitClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Trait");
            csSetTemp = tempClass.getMethod("set", mcLivingEntityClass, traitClass, double.class);
            csGetNeutralTemp = tempClass.getMethod("getNeutralWorldTemp", mcLivingEntityClass);
            @SuppressWarnings("unchecked")
            Object coreTrait = Enum.valueOf((Class<Enum>) traitClass, "CORE");
            csCoreTrait = coreTrait;
            @SuppressWarnings("unchecked")
            Object bodyTrait = Enum.valueOf((Class<Enum>) traitClass, "BODY");
            csBodyTrait = bodyTrait;
        } catch (Throwable t) {
            coldSweatPresent = false;
            csSetTemp = null;
            csGetNeutralTemp = null;
            csCoreTrait = null;
            csBodyTrait = null;
        }
    }

    private static Object getMinecraftPlayer(IWrapperPlayer rider) {
        try {
            Field playerField = rider.getClass().getDeclaredField("player");
            playerField.setAccessible(true);
            return playerField.get(rider);
        } catch (Throwable ignored) {
        }
        try {
            Field entityField = rider.getClass().getSuperclass().getDeclaredField("entity");
            entityField.setAccessible(true);
            return entityField.get(rider);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static UUID getPlayerUUID(Object player) {
        try {
            if (mcGetUUIDMethod != null) {
                return (UUID) mcGetUUIDMethod.invoke(player);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (mcGetUUIDMethodObf != null) {
                return (UUID) mcGetUUIDMethodObf.invoke(player);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (mcGetUniqueIDMethod != null) {
                return (UUID) mcGetUniqueIDMethod.invoke(player);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (mcGetUniqueIDMethodObf != null) {
                return (UUID) mcGetUniqueIDMethodObf.invoke(player);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isAirConditioned(Object player) {
        UUID id = getPlayerUUID(player);
        if (id != null) {
            return airConditionedPlayers.contains(id);
        }
        return airConditionedPlayerObjects.contains(player);
    }

    private static void applyToughAsNails(Object player) {
        if (tanLegacy) {
            if (tanGetTemperatureData == null || tanLegacySetTemperature == null || tanLegacyGetScaleMidpoint == null || tanLegacyTemperatureCtor == null) {
                return;
            }
            try {
                Object tempData = tanGetTemperatureData.invoke(null, player);
                if (tempData != null) {
                    Object neutralTemp = getLegacyNeutralTemperature();
                    if (neutralTemp != null) {
                        tanLegacySetTemperature.invoke(tempData, neutralTemp);
                    }
                    if (tanLegacySetChangeTime != null) {
                        tanLegacySetChangeTime.invoke(tempData, 0);
                    }
                }
            } catch (Throwable ignored) {
            }
            return;
        }
        if (tanGetTemperatureData == null || tanSetTargetLevel == null || tanNeutralLevel == null) {
            return;
        }
        ensureTanModifierRegistered();
        try {
            Object tempData = tanGetTemperatureData.invoke(null, player);
            if (tempData != null) {
                if (tanSetLevel != null) {
                    try {
                        tanSetLevel.invoke(tempData, tanNeutralLevel);
                    } catch (Throwable ignored) {
                    }
                }
                tanSetTargetLevel.invoke(tempData, tanNeutralLevel);
                if (tanSetChangeDelayTicks != null) {
                    try {
                        tanSetChangeDelayTicks.invoke(tempData, 0);
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetExtremityDelayTicks != null) {
                    try {
                        tanSetExtremityDelayTicks.invoke(tempData, 0);
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetDryTicks != null) {
                    try {
                        tanSetDryTicks.invoke(tempData, 0);
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetHyperthermiaTicks != null) {
                    try {
                        tanSetHyperthermiaTicks.invoke(tempData, 0);
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetLastHyperthermiaTicks != null) {
                    try {
                        tanSetLastHyperthermiaTicks.invoke(tempData, 0);
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetLastLevel != null) {
                    try {
                        tanSetLastLevel.invoke(tempData, tanNeutralLevel);
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetLastNearbyThermoregulators != null) {
                    try {
                        tanSetLastNearbyThermoregulators.invoke(tempData, Collections.emptySet());
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetNearbyThermoregulators != null) {
                    try {
                        tanSetNearbyThermoregulators.invoke(tempData, Collections.emptySet());
                    } catch (Throwable ignored) {
                    }
                }
                if (tanSetHypothermiaTicks != null) {
                    try {
                        tanSetHypothermiaTicks.invoke(tempData, 0);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void ensureTanModifierRegistered() {
        if (tanPlayerModifier == null || tanPlayerModifiersField == null) {
            return;
        }
        try {
            Object listObj = tanPlayerModifiersField.get(null);
            if (listObj instanceof List) {
                @SuppressWarnings("rawtypes")
                List list = (List) listObj;
                if (!list.contains(tanPlayerModifier)) {
                    list.add(tanPlayerModifier);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyLegendarySurvivalOverhaul(Object player) {
        applyLegendarySurvivalOverhaulImmunity(player);
        Object capability = getLegendarySurvivalOverhaulCapability(player);
        if (capability != null && (lsoSetTempLevel != null || lsoSetTargetTempLevel != null)) {
            try {
                float normal = (float) lsoNormalTemp;
                if (lsoSetTempLevel != null) {
                    lsoSetTempLevel.invoke(capability, normal);
                }
                if (lsoSetTargetTempLevel != null) {
                    lsoSetTargetTempLevel.invoke(capability, normal);
                }
                if (lsoSetTempTickTimer != null) {
                    lsoSetTempTickTimer.invoke(capability, 0);
                }
                if (lsoSetFreezeTickTimer != null) {
                    lsoSetFreezeTickTimer.invoke(capability, 0);
                }
            } catch (Throwable ignored) {
            }
            // For 1.16 LSO (no static capability field), also apply attribute modifier fallback.
            if (lsoTempCapabilityField != null) {
                return;
            }
        }
        if (lsoGetTargetTemp == null || lsoAddTempModifier == null) {
            return;
        }
        try {
            double target = ((Number) lsoGetTargetTemp.invoke(null, player)).doubleValue();
            double delta = lsoNormalTemp - target;
            lsoAddTempModifier.invoke(null, player, delta, LSO_TEMP_MODIFIER_ID);
        } catch (Throwable ignored) {
        }
    }

    private static void clearLegendarySurvivalOverhaul(Object player) {
        clearLegendarySurvivalOverhaulImmunity(player);
        if (lsoAddTempModifier == null) {
            return;
        }
        try {
            lsoAddTempModifier.invoke(null, player, 0D, LSO_TEMP_MODIFIER_ID);
        } catch (Throwable ignored) {
        }
    }

    private static void applyColdSweat(Object player) {
        if (csSetTemp == null || csGetNeutralTemp == null || csCoreTrait == null || csBodyTrait == null) {
            return;
        }
        try {
            double neutral = ((Number) csGetNeutralTemp.invoke(null, player)).doubleValue();
            csSetTemp.invoke(null, player, csCoreTrait, neutral);
            csSetTemp.invoke(null, player, csBodyTrait, neutral);
        } catch (Throwable ignored) {
        }
    }

    private static void initLegendarySurvivalOverhaulTemperatureUtilProxy() {
        if (lsoTempUtilProxyInstalled || lsoTempUtilInternalField == null) {
            return;
        }
        try {
            Object original = lsoTempUtilInternalField.get(null);
            if (original == null) {
                return;
            }
            Class<?> tempUtilInterface = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.ITemperatureUtil");
            Object proxy = Proxy.newProxyInstance(tempUtilInterface.getClassLoader(), new Class<?>[]{tempUtilInterface}, (proxyObj, method, args) -> {
                String name = method.getName();
                if ("getPlayerTargetTemperature".equals(name) && args != null && args.length > 0) {
                    Object player = args[0];
                    if (isAirConditioned(player)) {
                        return (float) lsoNormalTemp;
                    }
                }
                try {
                    return method.invoke(original, args);
                } catch (Throwable t) {
                    throw t.getCause() != null ? t.getCause() : t;
                }
            });
            lsoTempUtilInternalOriginal = original;
            lsoTempUtilInternalField.set(null, proxy);
            lsoTempUtilProxyInstalled = true;
        } catch (Throwable ignored) {
            lsoTempUtilInternalOriginal = null;
            lsoTempUtilProxyInstalled = false;
        }
    }

    private static void initLegendarySurvivalOverhaulEffectHooks() {
        try {
            Class<?> effectInstanceClass;
            try {
                effectInstanceClass = Class.forName("net.minecraft.world.effect.MobEffectInstance");
            } catch (Throwable ignored) {
                effectInstanceClass = Class.forName("net.minecraft.potion.EffectInstance");
            }
            lsoEffectInstanceCtor = findEffectInstanceConstructor(effectInstanceClass);
            if (mcLivingEntityClass != null) {
                try {
                    lsoPlayerAddEffect = mcLivingEntityClass.getMethod("addEffect", effectInstanceClass);
                } catch (Throwable ignored) {
                }
                try {
                    lsoPlayerAddEffect = mcLivingEntityClass.getMethod("addPotionEffect", effectInstanceClass);
                } catch (Throwable ignored) {
                }
                if (lsoTempImmunityEffect != null) {
                    Class<?> effectClass = lsoTempImmunityEffect.getClass().getSuperclass();
                    if (effectClass == null) {
                        effectClass = lsoTempImmunityEffect.getClass();
                    }
                    try {
                        lsoPlayerRemoveEffect = mcLivingEntityClass.getMethod("removeEffect", effectClass);
                    } catch (Throwable ignored) {
                    }
                    try {
                        lsoPlayerRemoveEffect = mcLivingEntityClass.getMethod("removePotionEffect", effectClass);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
            lsoEffectInstanceCtor = null;
            lsoPlayerAddEffect = null;
            lsoPlayerRemoveEffect = null;
        }
    }

    private static Constructor<?> findEffectInstanceConstructor(Class<?> effectInstanceClass) {
        if (lsoTempImmunityEffect == null) {
            return null;
        }
        Class<?> effectClass = lsoTempImmunityEffect.getClass().getSuperclass();
        if (effectClass == null) {
            effectClass = lsoTempImmunityEffect.getClass();
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class, int.class, boolean.class, boolean.class, boolean.class);
        } catch (Throwable ignored) {
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class, int.class, boolean.class, boolean.class);
        } catch (Throwable ignored) {
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class, int.class);
        } catch (Throwable ignored) {
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Constructor<?> findEffectInstanceConstructorForEffect(Object effect, Class<?> effectInstanceClass) {
        if (effect == null) {
            return null;
        }
        Class<?> effectClass = effect.getClass().getSuperclass();
        if (effectClass == null) {
            effectClass = effect.getClass();
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class, int.class, boolean.class, boolean.class, boolean.class);
        } catch (Throwable ignored) {
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class, int.class, boolean.class, boolean.class);
        } catch (Throwable ignored) {
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class, int.class);
        } catch (Throwable ignored) {
        }
        try {
            return effectInstanceClass.getConstructor(effectClass, int.class);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object resolveToughAsNailsEffect(String className, String fieldName) {
        try {
            Class<?> effectsClass = Class.forName(className);
            Object fieldValue = effectsClass.getField(fieldName).get(null);
            if (fieldValue == null) {
                return null;
            }
            Object resolved = resolveRegistryObject(fieldValue);
            return resolved != null ? resolved : fieldValue;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object resolveRegistryObject(Object maybeRegistryObject) {
        if (maybeRegistryObject == null) {
            return null;
        }
        try {
            Method getMethod = maybeRegistryObject.getClass().getMethod("get");
            return getMethod.invoke(maybeRegistryObject);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object resolveEffectFromRegistry(String namespace, String path) {
        try {
            Class<?> forgeRegistriesClass = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            Object mobEffectsRegistry = forgeRegistriesClass.getField("MOB_EFFECTS").get(null);
            if (mobEffectsRegistry == null) {
                return null;
            }
            Class<?> resourceLocationClass;
            try {
                resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            } catch (Throwable ignored) {
                resourceLocationClass = Class.forName("net.minecraft.util.ResourceLocation");
            }
            Object key;
            try {
                Constructor<?> ctor = resourceLocationClass.getConstructor(String.class, String.class);
                key = ctor.newInstance(namespace, path);
            } catch (Throwable ignored) {
                Constructor<?> ctor = resourceLocationClass.getConstructor(String.class);
                key = ctor.newInstance(namespace + ":" + path);
            }
            Method getValue = mobEffectsRegistry.getClass().getMethod("getValue", resourceLocationClass);
            return getValue.invoke(mobEffectsRegistry, key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void applyLegendarySurvivalOverhaulImmunity(Object player) {
        if (lsoTempImmunityEffect == null || lsoEffectInstanceCtor == null || lsoPlayerAddEffect == null) {
            return;
        }
        try {
            Object effectInstance = createEffectInstance(lsoTempImmunityEffect, 60);
            if (effectInstance != null) {
                lsoPlayerAddEffect.invoke(player, effectInstance);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void clearLegendarySurvivalOverhaulImmunity(Object player) {
        if (lsoTempImmunityEffect == null || lsoPlayerRemoveEffect == null) {
            return;
        }
        try {
            lsoPlayerRemoveEffect.invoke(player, lsoTempImmunityEffect);
        } catch (Throwable ignored) {
        }
    }

    private static Object createEffectInstance(Object effect, int durationTicks) {
        if (effect == null || lsoEffectInstanceCtor == null) {
            return null;
        }
        try {
            Class<?>[] params = lsoEffectInstanceCtor.getParameterTypes();
            if (params.length == 6) {
                return lsoEffectInstanceCtor.newInstance(effect, durationTicks, 0, true, false, true);
            }
            if (params.length == 5) {
                return lsoEffectInstanceCtor.newInstance(effect, durationTicks, 0, true, false);
            }
            if (params.length == 3) {
                return lsoEffectInstanceCtor.newInstance(effect, durationTicks, 0);
            }
            if (params.length == 2) {
                return lsoEffectInstanceCtor.newInstance(effect, durationTicks);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object createEffectInstance(Object effect, Constructor<?> ctor, int durationTicks) {
        if (effect == null || ctor == null) {
            return null;
        }
        try {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 6) {
                return ctor.newInstance(effect, durationTicks, 0, true, false, true);
            }
            if (params.length == 5) {
                return ctor.newInstance(effect, durationTicks, 0, true, false);
            }
            if (params.length == 3) {
                return ctor.newInstance(effect, durationTicks, 0);
            }
            if (params.length == 2) {
                return ctor.newInstance(effect, durationTicks);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getLegacyNeutralTemperature() {
        if (tanLegacyNeutralTemperature != null) {
            return tanLegacyNeutralTemperature;
        }
        if (tanLegacyGetScaleMidpoint == null || tanLegacyTemperatureCtor == null) {
            return null;
        }
        try {
            int midpoint = ((Number) tanLegacyGetScaleMidpoint.invoke(null)).intValue();
            Object neutralTemp = tanLegacyTemperatureCtor.newInstance(midpoint);
            tanLegacyNeutralTemperature = neutralTemp;
            return neutralTemp;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getLegendarySurvivalOverhaulCapability(Object player) {
        if (player == null) {
            return null;
        }
        if (lsoTempCapabilityToken != null) {
            Object cap = getCapabilityFromToken(player, lsoTempCapabilityToken);
            if (cap != null) {
                return cap;
            }
        }
        if (lsoTempCapabilityField == null) {
            if (lsoGetTempCapability == null) {
                return null;
            }
            try {
                return lsoGetTempCapability.invoke(null, player);
            } catch (Throwable ignored) {
                return null;
            }
        }
        try {
            Object capToken = lsoTempCapabilityField.get(null);
            if (capToken == null) {
                return null;
            }
            return getCapabilityFromToken(player, capToken);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getCapabilityFromToken(Object player, Object capToken) {
        if (capToken == null) {
            return null;
        }
        try {
            Object lazyOptional;
            if (lsoGetCapabilityWithDir != null) {
                lazyOptional = lsoGetCapabilityWithDir.invoke(player, capToken, null);
            } else if (lsoGetCapabilityNoDir != null) {
                lazyOptional = lsoGetCapabilityNoDir.invoke(player, capToken);
            } else {
                return null;
            }
            if (lazyOptional == null) {
                return null;
            }
            if (lsoLazyOptionalResolve != null && lsoOptionalIsPresent != null && lsoOptionalGet != null) {
                Object optional = lsoLazyOptionalResolve.invoke(lazyOptional);
                if (optional != null && (Boolean) lsoOptionalIsPresent.invoke(optional)) {
                    return lsoOptionalGet.invoke(optional);
                }
                return null;
            }
            if (lsoLazyOptionalOrElse != null) {
                return lsoLazyOptionalOrElse.invoke(lazyOptional, new Object[]{null});
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Class<?> getForgeDirectionClass() {
        try {
            return Class.forName("net.minecraft.core.Direction");
        } catch (Throwable ignored) {
        }
        try {
            return Class.forName("net.minecraft.util.Direction");
        } catch (Throwable ignored) {
        }
        try {
            return Class.forName("net.minecraft.util.EnumFacing");
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
