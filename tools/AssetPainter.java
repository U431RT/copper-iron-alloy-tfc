import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Paints metal textures by mixing TerraFirmaCraft's own sprites.
 * A luminance ramp over copper (or a recolored vanilla anvil) flattens the
 * hand-painted shading, so each alloy is a real blend of the two TFC metals
 * it is made from. Tool handles stay the original wood pixels.
 */
public final class AssetPainter {
    private static final String MOD = "copper_iron_alloy";
    private static final String[] TOOLS = {
            "pickaxe", "axe", "shovel", "hoe", "sword", "knife",
            "hammer", "chisel", "mace", "propick", "scythe"
    };
    private static final String[] HEAD = {
            "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "sword_blade", "knife_blade",
            "hammer_head", "chisel_head", "mace_head", "propick_head", "scythe_blade"
    };
    private static final String[] FORMS = {"ingot", "double_ingot", "sheet", "double_sheet"};

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--fridge-only".equals(args[0])) {
            Path root = Path.of(args.length > 1 ? args[1] : ".");
            Path res = root.resolve("src/main/resources/assets/" + MOD);
            paintFridgeEntity(res);
            paintBrittleCabinet(res);
            System.out.println("fridge painted");
            return;
        }
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        Path res = root.resolve("src/main/resources/assets/" + MOD);
        Path tfcJar = root.resolve("libs/TerraFirmaCraft-Forge-1.20.1-3.2.12.jar");
        Path vanillaJar = Path.of("D:/GAME/Minecraft/.minecraft/versions/1.20.1/1.20.1.jar");
        try (ZipFile tfc = new ZipFile(tfcJar.toFile())) {
            paintMetals(tfc, res);
            paintFluids(tfc, res);
            paintVessels(res);
            paintFridges(res);
            paintExtras(tfc, res);
            paintFilledBuckets(tfc, res);
            paintGear(tfc, res);
            if (Files.exists(vanillaJar)) {
                try (ZipFile vanilla = new ZipFile(vanillaJar.toFile())) {
                    paintChest(vanilla, tfc, res);
                }
            }
            writePreview(res, root.resolve("preview_metals.png"));
        }
        writeAnvilModels(root.resolve("src/main/resources/assets/" + MOD + "/models/block"));
        System.out.println("painted");
    }

    private static void paintMetals(ZipFile tfc, Path res) throws Exception {
        for (String form : FORMS) {
            save(blendForm(tfc, form, "iron_copper_alloy"), res.resolve("textures/item/iron_copper_alloy_" + form + ".png"));
            save(blendForm(tfc, form, "steel_gold"), res.resolve("textures/item/steel_gold_" + form + ".png"));
            save(blendForm(tfc, form, "brittle_white_steel"), res.resolve("textures/item/brittle_white_steel_" + form + ".png"));
            save(blendForm(tfc, form, "white_steel"), res.resolve("textures/item/white_steel_" + form + ".png"));
        }
        save(blendSmooth(tfc, "iron_copper_alloy"), res.resolve("textures/block/metal/smooth/iron_copper_alloy.png"));
        save(blendSmooth(tfc, "steel_gold"), res.resolve("textures/block/metal/smooth/steel_gold.png"));
        save(blendSmooth(tfc, "brittle_white_steel"), res.resolve("textures/block/metal/smooth/brittle_white_steel.png"));
        save(blendSmooth(tfc, "white_steel"), res.resolve("textures/block/metal/smooth/white_steel.png"));

        for (int i = 0; i < TOOLS.length; i++) {
            paintTool(tfc, res, TOOLS[i], HEAD[i]);
        }
        paintArmor(tfc, res, "helmet", "helmet");
        paintArmor(tfc, res, "chestplate", "chestplate");
        paintArmor(tfc, res, "greaves", "leggings");
        paintArmor(tfc, res, "boots", "boots");
        for (String metal : new String[]{"iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"}) {
            BufferedImage a = layer(tfc, metal, 1);
            BufferedImage b = layer(tfc, metal, 2);
            save(a, res.resolve("textures/models/armor/" + metal + "_layer_1.png"));
            save(b, res.resolve("textures/models/armor/" + metal + "_layer_2.png"));
        }
    }

    private static void paintTool(ZipFile tfc, Path res, String tool, String head) throws Exception {
        BufferedImage copper = read(tfc, "assets/tfc/textures/item/metal/" + tool + "/copper.png");
        BufferedImage iron = read(tfc, "assets/tfc/textures/item/metal/" + tool + "/wrought_iron.png");
        boolean[][] handle = handleMask(copper, iron);
        BufferedImage steelTool = read(tfc, "assets/tfc/textures/item/metal/" + tool + "/steel.png");
        save(ironCopper(steelTool, handle), res.resolve("textures/item/iron_copper_alloy_" + tool + ".png"));
        save(steelGold(steelTool, handle), res.resolve("textures/item/steel_gold_" + tool + ".png"));
        BufferedImage brittle = blendMasked(
                read(tfc, "assets/tfc/textures/item/metal/" + tool + "/blue_steel.png"),
                read(tfc, "assets/tfc/textures/item/metal/" + tool + "/red_steel.png"),
                0.50f, handle);
        save(paleAndCrack(brittle, handle, 0.42f), res.resolve("textures/item/brittle_white_steel_" + tool + ".png"));
        save(silver(read(tfc, "assets/tfc/textures/item/metal/" + tool + "/steel.png"), handle),
                res.resolve("textures/item/white_steel_" + tool + ".png"));

        BufferedImage steelHead = read(tfc, "assets/tfc/textures/item/metal/" + head + "/steel.png");
        save(ironCopper(steelHead, null), res.resolve("textures/item/iron_copper_alloy_" + tool + "_head.png"));
        save(steelGold(steelHead, null), res.resolve("textures/item/steel_gold_" + tool + "_head.png"));
        BufferedImage brittleHead = blend(
                read(tfc, "assets/tfc/textures/item/metal/" + head + "/blue_steel.png"),
                read(tfc, "assets/tfc/textures/item/metal/" + head + "/red_steel.png"), 0.50f);
        save(paleAndCrack(brittleHead, null, 0.42f), res.resolve("textures/item/brittle_white_steel_" + tool + "_head.png"));
        save(silver(read(tfc, "assets/tfc/textures/item/metal/" + head + "/steel.png"), null),
                res.resolve("textures/item/white_steel_" + tool + "_head.png"));
    }

    private static void paintArmor(ZipFile tfc, Path res, String tfcName, String ourName) throws Exception {
        BufferedImage steelArmor = read(tfc, "assets/tfc/textures/item/metal/" + tfcName + "/steel.png");
        save(ironCopper(steelArmor, null), res.resolve("textures/item/iron_copper_alloy_" + ourName + ".png"));
        save(steelGold(steelArmor, null), res.resolve("textures/item/steel_gold_" + ourName + ".png"));
        save(paleAndCrack(blend(
                read(tfc, "assets/tfc/textures/item/metal/" + tfcName + "/blue_steel.png"),
                read(tfc, "assets/tfc/textures/item/metal/" + tfcName + "/red_steel.png"), 0.50f), null, 0.42f),
                res.resolve("textures/item/brittle_white_steel_" + ourName + ".png"));
        save(silver(read(tfc, "assets/tfc/textures/item/metal/" + tfcName + "/steel.png"), null),
                res.resolve("textures/item/white_steel_" + ourName + ".png"));
    }

    private static BufferedImage layer(ZipFile tfc, String metal, int layer) throws Exception {
        String path = "assets/tfc/textures/models/armor/%s_layer_" + layer + ".png";
        return switch (metal) {
            case "iron_copper_alloy" -> ironCopper(read(tfc, path.formatted("steel")), null);
            case "steel_gold" -> steelGold(read(tfc, path.formatted("steel")), null);
            case "brittle_white_steel" -> paleAndCrack(blend(
                    read(tfc, path.formatted("blue_steel")), read(tfc, path.formatted("red_steel")), 0.50f), null, 0.42f);
            default -> silver(read(tfc, path.formatted("steel")), null);
        };
    }

    /** t is the weight of the second sprite. Forms have no wooden handle. */
    private static BufferedImage blendForm(ZipFile tfc, String form, String metal) throws Exception {
        String path = "assets/tfc/textures/item/metal/" + form + "/%s.png";
        return switch (metal) {
            case "iron_copper_alloy" -> ironCopper(read(tfc, path.formatted("steel")), null);
            case "steel_gold" -> steelGold(read(tfc, path.formatted("steel")), null);
            case "brittle_white_steel" -> paleAndCrack(blend(
                    read(tfc, path.formatted("blue_steel")), read(tfc, path.formatted("red_steel")), 0.50f), null, 0.42f);
            default -> silver(read(tfc, path.formatted("steel")), null);
        };
    }

    private static BufferedImage blendSmooth(ZipFile tfc, String metal) throws Exception {
        String path = "assets/tfc/textures/block/metal/smooth/%s.png";
        return switch (metal) {
            case "iron_copper_alloy" -> ironCopper(read(tfc, path.formatted("steel")), null);
            case "steel_gold" -> steelGold(read(tfc, path.formatted("steel")), null);
            case "brittle_white_steel" -> paleAndCrack(blend(
                    read(tfc, path.formatted("blue_steel")), read(tfc, path.formatted("red_steel")), 0.50f), null, 0.38f);
            default -> silver(read(tfc, path.formatted("steel")), null);
        };
    }

    private static void paintFluids(ZipFile tfc, Path res) throws Exception {
        BufferedImage still = grayscale(read(tfc, "assets/tfc/textures/block/molten_still.png"));
        BufferedImage flow = grayscale(read(tfc, "assets/tfc/textures/block/molten_flow.png"));
        int[] tint = {0xFFFFFF, 0xFFFFFF, 0xD5DEE8, 0xF2F6FA};
        String[] names = {"iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"};
        for (int i = 0; i < names.length; i++) {
            // Iron-copper and steel-gold stay grayscale so the fluid tint is the picked color.
            save(tintGray(still, tint[i]), res.resolve("textures/block/fluid/" + names[i] + ".png"));
            save(tintGray(flow, tint[i]), res.resolve("textures/block/fluid/" + names[i] + "_flow.png"));
            copyEntry(tfc, "assets/tfc/textures/block/molten_still.png.mcmeta",
                    res.resolve("textures/block/fluid/" + names[i] + ".png.mcmeta"));
            copyEntry(tfc, "assets/tfc/textures/block/molten_flow.png.mcmeta",
                    res.resolve("textures/block/fluid/" + names[i] + "_flow.png.mcmeta"));
        }
    }

    /** Kettle and cup were a recolored sheet. Draw small forged vessels instead. */
    private static void paintVessels(Path res) throws Exception {
        int body = 0xFF6A7A22;
        int mid = 0xFFA3B43F;
        int hi = 0xFFC6D45A;
        int dark = 0xFF3C4A12;
        BufferedImage kettle = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // belly
        fill(kettle, 4, 6, 11, 13, body);
        fill(kettle, 5, 5, 10, 5, mid);
        fill(kettle, 5, 7, 6, 11, hi);
        fill(kettle, 6, 14, 9, 14, dark);
        // spout
        fill(kettle, 11, 8, 14, 9, mid);
        px(kettle, 14, 7, hi);
        // handle
        fill(kettle, 2, 7, 3, 11, dark);
        px(kettle, 3, 6, dark);
        px(kettle, 3, 12, dark);
        // lid
        fill(kettle, 5, 3, 10, 4, mid);
        px(kettle, 7, 2, hi);
        px(kettle, 8, 2, hi);
        save(kettle, res.resolve("textures/item/steel_gold_kettle.png"));

        BufferedImage cup = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(cup, 5, 6, 10, 12, body);
        fill(cup, 4, 5, 11, 6, mid);
        fill(cup, 6, 7, 7, 11, hi);
        fill(cup, 6, 13, 9, 13, dark);
        fill(cup, 11, 7, 13, 10, dark);
        px(cup, 12, 6, dark);
        px(cup, 12, 11, dark);
        save(cup, res.resolve("textures/item/steel_gold_cup.png"));
    }

    private static void paintFridges(Path res) throws Exception {
        // Iron-copper cabinet: wood seams, metal door, black handle. Open face is black.
        int wood = 0xFF8A5A32;
        int woodHi = 0xFFC48A58;
        int woodDark = 0xFF5C3A22;
        int metal = 0xFFBA7C3E;
        int metalHi = 0xFFD8A070;
        int black = 0xFF141414;
        int handle = 0xFF1A1A1A;
        BufferedImage side = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(side, 0, 0, 15, 15, wood);
        for (int y = 0; y < 16; y += 4) {
            fill(side, 0, y, 15, y, woodDark);
        }
        fill(side, 0, 0, 0, 15, woodHi);
        fill(side, 15, 0, 15, 15, woodDark);
        save(side, res.resolve("textures/block/iron_copper_alloy_fridge_side.png"));
        save(side, res.resolve("textures/block/iron_copper_alloy_fridge_top.png"));

        BufferedImage front = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(front, 0, 0, 15, 15, wood);
        fill(front, 2, 2, 13, 13, metal);
        fill(front, 2, 2, 13, 2, metalHi);
        fill(front, 7, 2, 8, 13, woodDark); // seam
        fill(front, 11, 6, 12, 10, handle);
        px(front, 11, 6, 0xFF3A3A3A);
        save(front, res.resolve("textures/block/iron_copper_alloy_fridge_front.png"));

        BufferedImage open = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(open, 0, 0, 15, 15, wood);
        fill(open, 2, 2, 13, 13, black);
        fill(open, 13, 6, 14, 10, handle);
        save(open, res.resolve("textures/block/iron_copper_alloy_fridge_front_open.png"));

        // Steel-gold fridge: the same yellow-green as the ingots.
        int steel = 0xFF4E5C18;
        int steelHi = 0xFF6A7A22;
        int rose = 0xFFA3B43F;
        int roseHi = 0xFFC6D45A;
        BufferedImage sgSide = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(sgSide, 0, 0, 15, 15, steel);
        fill(sgSide, 0, 0, 15, 1, rose);
        fill(sgSide, 0, 14, 15, 15, rose);
        fill(sgSide, 1, 4, 14, 4, steelHi);
        fill(sgSide, 1, 11, 14, 11, steelHi);
        save(sgSide, res.resolve("textures/block/steel_gold_fridge_side.png"));
        save(sgSide, res.resolve("textures/block/steel_gold_fridge_top.png"));

        BufferedImage sgFront = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(sgFront, 0, 0, 15, 15, steel);
        fill(sgFront, 0, 0, 15, 1, rose);
        fill(sgFront, 0, 14, 15, 15, rose);
        fill(sgFront, 0, 0, 1, 15, rose);
        fill(sgFront, 14, 0, 15, 15, rose);
        fill(sgFront, 3, 3, 12, 12, steelHi);
        fill(sgFront, 11, 6, 12, 10, rose);
        px(sgFront, 11, 6, roseHi);
        save(sgFront, res.resolve("textures/block/steel_gold_fridge_front.png"));

        BufferedImage sgOpen = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(sgOpen, 0, 0, 15, 15, rose);
        fill(sgOpen, 2, 2, 13, 13, 0xFF101014);
        fill(sgOpen, 13, 6, 14, 10, roseHi);
        save(sgOpen, res.resolve("textures/block/steel_gold_fridge_front_open.png"));

        int silver = 0xFFD5DCE4;
        int silverHi = 0xFFF4F7FA;
        int panel = 0xFFC5CED8;
        int silverDark = 0xFF8E99A6;
        int wsHandle = 0xFFF7FAFC;
        BufferedImage wsSide = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(wsSide, 0, 0, 15, 15, silver);
        fill(wsSide, 0, 0, 15, 0, silverHi);
        fill(wsSide, 0, 15, 15, 15, silverDark);
        fill(wsSide, 0, 0, 0, 15, silverHi);
        fill(wsSide, 15, 0, 15, 15, silverDark);
        save(wsSide, res.resolve("textures/block/white_steel_fridge_side.png"));
        BufferedImage wsTop = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(wsTop, 0, 0, 15, 15, silverHi);
        fill(wsTop, 0, 0, 15, 0, silverDark);
        fill(wsTop, 0, 15, 15, 15, silverDark);
        fill(wsTop, 0, 0, 0, 15, silverDark);
        fill(wsTop, 15, 0, 15, 15, silverDark);
        save(wsTop, res.resolve("textures/block/white_steel_fridge_top.png"));
        BufferedImage wsFront = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(wsFront, 0, 0, 15, 15, silver);
        fill(wsFront, 0, 0, 15, 0, silverHi);
        fill(wsFront, 0, 15, 15, 15, silverDark);
        fill(wsFront, 0, 0, 0, 15, silverHi);
        fill(wsFront, 15, 0, 15, 15, silverDark);
        fill(wsFront, 2, 2, 13, 6, panel);
        fill(wsFront, 2, 2, 13, 2, silverHi);
        fill(wsFront, 0, 7, 15, 7, silverDark);
        fill(wsFront, 2, 9, 13, 13, silver);
        fill(wsFront, 2, 9, 13, 9, silverHi);
        fill(wsFront, 12, 3, 12, 5, wsHandle);
        fill(wsFront, 12, 10, 12, 12, wsHandle);
        save(wsFront, res.resolve("textures/block/white_steel_fridge_front.png"));
        BufferedImage wsOpen = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(wsOpen, 0, 0, 15, 15, silver);
        fill(wsOpen, 0, 0, 15, 0, silverHi);
        fill(wsOpen, 0, 15, 15, 15, silverDark);
        fill(wsOpen, 0, 0, 0, 15, silverHi);
        fill(wsOpen, 15, 0, 15, 15, silverDark);
        fill(wsOpen, 2, 2, 13, 13, 0xFF12161C);
        fill(wsOpen, 13, 6, 13, 10, wsHandle);
        save(wsOpen, res.resolve("textures/block/white_steel_fridge_front_open.png"));
        paintBrittleCabinet(res);
    }

    /** Front-opening cabinet. Pale silver with a crack, not the white-steel freezer. */
    private static void paintBrittleCabinet(Path res) throws Exception {
        int base = 0xFF9C84A0;
        int hi = 0xFFC8B0CC;
        int dark = 0xFF6A5874;
        int crack = 0xFF4A3C56;
        int grip = 0xFFE4D4EA;
        BufferedImage side = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(side, 0, 0, 15, 15, base);
        fill(side, 0, 0, 15, 0, hi);
        fill(side, 0, 15, 15, 15, dark);
        fill(side, 0, 0, 0, 15, hi);
        fill(side, 15, 0, 15, 15, dark);
        fill(side, 4, 3, 4, 12, crack);
        fill(side, 5, 8, 9, 8, crack);
        save(side, res.resolve("textures/block/brittle_white_steel_fridge_side.png"));
        BufferedImage top = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(top, 0, 0, 15, 15, hi);
        fill(top, 0, 0, 15, 0, dark);
        fill(top, 0, 15, 15, 15, dark);
        fill(top, 0, 0, 0, 15, dark);
        fill(top, 15, 0, 15, 15, dark);
        fill(top, 3, 4, 11, 4, crack);
        save(top, res.resolve("textures/block/brittle_white_steel_fridge_top.png"));
        BufferedImage front = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(front, 0, 0, 15, 15, base);
        fill(front, 0, 0, 15, 0, hi);
        fill(front, 0, 15, 15, 15, dark);
        fill(front, 0, 0, 0, 15, hi);
        fill(front, 15, 0, 15, 15, dark);
        fill(front, 2, 2, 13, 6, 0xFFA58BAC);
        fill(front, 2, 2, 13, 2, hi);
        fill(front, 0, 7, 15, 7, dark);
        fill(front, 2, 9, 13, 13, base);
        fill(front, 2, 9, 13, 9, hi);
        fill(front, 6, 4, 10, 4, crack);
        fill(front, 12, 3, 12, 5, grip);
        fill(front, 12, 10, 12, 12, grip);
        save(front, res.resolve("textures/block/brittle_white_steel_fridge_front.png"));
        BufferedImage open = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(open, 0, 0, 15, 15, base);
        fill(open, 0, 0, 15, 0, hi);
        fill(open, 0, 15, 15, 15, dark);
        fill(open, 0, 0, 0, 15, hi);
        fill(open, 15, 0, 15, 15, dark);
        fill(open, 2, 2, 13, 13, 0xFF12161C);
        fill(open, 13, 6, 13, 10, grip);
        save(open, res.resolve("textures/block/brittle_white_steel_fridge_front_open.png"));
    }

    /**
     * 128 sheet for the white-steel entity. Flat panels only.
     * Single freezer tiles sit on the first row. The lid seam is pixel row 4.
     * The lid-top handle is on the low-Y edge, which the mesh maps to the front.
     * Vertical door tiles sit on the second row, handle on the left (the free edge).
     */
    private static void paintFridgeEntity(Path res) throws Exception {
        int hi = 0xFFF5F8FB;
        int base = 0xFFD5DCE4;
        int panel = 0xFFC5CED8;
        int edge = 0xFF8E99A6;
        int deep = 0xFF5E6974;
        int interior = 0xFF12161C;
        int cavity = 0xFF1C2430;
        int shelf = 0xFF2C3544;
        int grip = 0xFFF7FBFF;
        BufferedImage sheet = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                sheet.setRGB(x, y, 0x00000000);
            }
        }
        metalTile(sheet, 0, 0, base, hi, edge, deep, panel);
        metalTile(sheet, 16, 0, base, hi, edge, deep, panel);
        fill(sheet, 21, 1, 26, 2, grip);
        fill(sheet, 21, 3, 26, 3, deep);
        metalTile(sheet, 32, 0, base, hi, edge, deep, panel);
        metalTile(sheet, 48, 0, hi, hi, edge, edge, hi);
        fill(sheet, 52, 1, 59, 3, grip);
        fill(sheet, 52, 4, 59, 4, deep);
        fill(sheet, 64, 0, 79, 15, interior);
        fill(sheet, 66, 2, 77, 13, cavity);
        fill(sheet, 66, 6, 77, 6, shelf);
        fill(sheet, 66, 11, 77, 11, shelf);
        metalTile(sheet, 80, 0, base, hi, edge, deep, panel);
        metalTile(sheet, 0, 32, base, hi, edge, deep, panel);
        fill(sheet, 1, 44, 3, 47, grip);
        metalTile(sheet, 16, 32, base, hi, edge, deep, panel);
        fill(sheet, 17, 32, 19, 35, grip);
        metalTile(sheet, 32, 32, base, hi, edge, deep, panel);
        fill(sheet, 32, 47, 47, 47, deep);
        metalTile(sheet, 48, 32, base, hi, edge, deep, panel);
        fill(sheet, 48, 32, 63, 32, deep);
        metalTile(sheet, 64, 32, hi, hi, edge, edge, hi);
        save(sheet, res.resolve("textures/entity/fridge/white_steel.png"));
    }

    private static void metalTile(BufferedImage sheet, int x, int y, int base, int hi, int edge, int deep, int panel) {
        fill(sheet, x, y, x + 15, y + 15, base);
        fill(sheet, x, y, x + 15, y, hi);
        fill(sheet, x, y + 15, x + 15, y + 15, deep);
        fill(sheet, x, y, x, y + 15, hi);
        fill(sheet, x + 15, y, x + 15, y + 15, edge);
        fill(sheet, x, y + 4, x + 15, y + 4, deep);
        fill(sheet, x + 2, y + 6, x + 13, y + 13, panel);
        fill(sheet, x + 2, y + 6, x + 13, y + 6, hi);
    }

    private static void paintExtras(ZipFile tfc, Path res) throws Exception {
        Path bucket = res.resolve("textures/item/steel_gold_bucket.png");
        if (Files.exists(bucket)) {
            BufferedImage existing = javax.imageio.ImageIO.read(bucket.toFile());
            BufferedImage argb = new BufferedImage(existing.getWidth(), existing.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(existing, 0, 0, null);
            save(tone(argb, null, STEEL_GOLD_COLOR), bucket);
        }
        save(tone(read(tfc, "assets/tfc/textures/item/powder/diamond.png"), null, 0x3A78D0),
                res.resolve("textures/item/brittle_blue_steel_powder.png"));
        save(tone(read(tfc, "assets/tfc/textures/item/powder/diamond.png"), null, 0xD05048),
                res.resolve("textures/item/brittle_red_steel_powder.png"));
        save(tone(read(tfc, "assets/tfc/textures/item/powder/diamond.png"), null, 0x9FE7FF),
                res.resolve("textures/item/diamond_catalyst.png"));
    }

    /** Shears, hooks, rods, lamps, chains, and the plated sheet-on-plank block. */
    private static void paintGear(ZipFile tfc, Path res) throws Exception {
        String[] metals = {"iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"};
        String[] items = {"shears", "fish_hook", "fishing_rod", "unfinished_lamp", "lamp"};
        BufferedImage steelBlock = read(tfc, "assets/tfc/textures/block/metal/block/steel.png");
        boolean[][] wood = handleMask(steelBlock, read(tfc, "assets/tfc/textures/block/metal/block/bismuth_bronze.png"));
        BufferedImage blueBlock = read(tfc, "assets/tfc/textures/block/metal/block/blue_steel.png");
        BufferedImage redBlock = read(tfc, "assets/tfc/textures/block/metal/block/red_steel.png");
        BufferedImage steelChain = read(tfc, "assets/tfc/textures/block/metal/chain/steel.png");
        BufferedImage blueChain = read(tfc, "assets/tfc/textures/block/metal/chain/blue_steel.png");
        BufferedImage redChain = read(tfc, "assets/tfc/textures/block/metal/chain/red_steel.png");
        boolean[][] rodHandle = handleMask(
                read(tfc, "assets/tfc/textures/item/metal/fishing_rod/copper.png"),
                read(tfc, "assets/tfc/textures/item/metal/fishing_rod/wrought_iron.png"));
        for (String metal : metals) {
            BufferedImage block = switch (metal) {
                case "iron_copper_alloy" -> ironCopper(steelBlock, wood);
                case "steel_gold" -> steelGold(steelBlock, wood);
                case "brittle_white_steel" -> paleAndCrack(blend(blueBlock, redBlock, 0.50f), wood, 0.42f);
                default -> silver(steelBlock, wood);
            };
            save(block, res.resolve("textures/block/metal/block/" + metal + ".png"));
            BufferedImage chain = switch (metal) {
                case "iron_copper_alloy" -> ironCopper(steelChain, null);
                case "steel_gold" -> steelGold(steelChain, null);
                case "brittle_white_steel" -> paleAndCrack(blend(blueChain, redChain, 0.50f), null, 0.42f);
                default -> silver(steelChain, null);
            };
            save(chain, res.resolve("textures/block/metal/chain/" + metal + ".png"));
            for (String item : items) {
                boolean[][] keep = item.equals("fishing_rod") ? rodHandle : null;
                BufferedImage steel = read(tfc, "assets/tfc/textures/item/metal/" + item + "/steel.png");
                BufferedImage painted = switch (metal) {
                    case "iron_copper_alloy" -> ironCopper(steel, keep);
                    case "steel_gold" -> steelGold(steel, keep);
                    case "brittle_white_steel" -> paleAndCrack(blend(
                            read(tfc, "assets/tfc/textures/item/metal/" + item + "/blue_steel.png"),
                            read(tfc, "assets/tfc/textures/item/metal/" + item + "/red_steel.png"), 0.50f), keep, 0.42f);
                    default -> silver(steel, keep);
                };
                save(painted, res.resolve("textures/item/" + metal + "_" + item + ".png"));
            }
        }
    }

    /** Fluid sits in the TFC metal-bucket mouth, on top of the current empty bucket. */
    private static void paintFilledBuckets(ZipFile tfc, Path res) throws Exception {
        BufferedImage overlay = read(tfc, "assets/tfc/textures/item/metal/bucket/overlay.png");
        String[] bases = {"iron_copper_alloy_bucket", "steel_gold_bucket"};
        String[] names = {"water", "milk", "lava", "iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"};
        int[] colors = {0x3A6FE0, 0xF4F1E8, 0xFF5A18, 0xBA7C3E, 0xA3B43F, 0xD8E2EA, 0xF2F6FA};
        for (String base : bases) {
            BufferedImage bucket = ImageIO.read(res.resolve("textures/item/" + base + ".png").toFile());
            for (int i = 0; i < names.length; i++) {
                save(pour(bucket, overlay, colors[i]), res.resolve("textures/item/" + base + "_" + names[i] + ".png"));
            }
        }
    }

    private static BufferedImage pour(BufferedImage bucket, BufferedImage overlay, int key) {
        float[] keyHsb = java.awt.Color.RGBtoHSB((key >> 16) & 255, (key >> 8) & 255, key & 255, null);
        BufferedImage out = new BufferedImage(bucket.getWidth(), bucket.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < bucket.getHeight(); y++) {
            for (int x = 0; x < bucket.getWidth(); x++) {
                int base = bucket.getRGB(x, y);
                int mark = (x < overlay.getWidth() && y < overlay.getHeight()) ? overlay.getRGB(x, y) : 0;
                if (((base >>> 24) & 255) < 16 || ((mark >>> 24) & 255) < 16) {
                    out.setRGB(x, y, base);
                    continue;
                }
                float shade = (((mark >> 16) & 255) + ((mark >> 8) & 255) + (mark & 255)) / (3.0F * 255.0F);
                float value = 0.42F + 0.55F * shade;
                int rgb = java.awt.Color.HSBtoRGB(keyHsb[0], Math.min(0.92F, Math.max(0.35F, keyHsb[1])), Math.min(1.0F, value));
                out.setRGB(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
            }
        }
        return out;
    }

    private static void paintChest(ZipFile vanilla, ZipFile tfc, Path res) throws Exception {
        paintFridgeEntity(res);
    }

    private static void writePreview(Path res, Path out) throws Exception {
        String[] metals = {"iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"};
        String[] forms = {"ingot", "sheet", "pickaxe", "helmet"};
        int scale = 8;
        int cell = 16 * scale;
        BufferedImage sheet = new BufferedImage(forms.length * cell, metals.length * cell, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < metals.length; y++) {
            for (int x = 0; x < forms.length; x++) {
                BufferedImage sprite = ImageIO.read(res.resolve("textures/item/" + metals[y] + "_" + forms[x] + ".png").toFile());
                for (int py = 0; py < sprite.getHeight() && py < 16; py++) {
                    for (int px = 0; px < sprite.getWidth() && px < 16; px++) {
                        int argb = sprite.getRGB(px, py);
                        for (int dy = 0; dy < scale; dy++) {
                            for (int dx = 0; dx < scale; dx++) {
                                sheet.setRGB(x * cell + px * scale + dx, y * cell + py * scale + dy, argb);
                            }
                        }
                    }
                }
            }
        }
        save(sheet, out);
    }

    private static void writeAnvilModels(Path dir) throws Exception {
        for (String metal : new String[]{"iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"}) {
            String json = """
                    {
                      "parent": "tfc:block/anvil",
                      "textures": {
                        "all": "copper_iron_alloy:block/metal/smooth/%s",
                        "particle": "copper_iron_alloy:block/metal/smooth/%s"
                      }
                    }
                    """.formatted(metal, metal);
            Files.writeString(dir.resolve(metal + "_anvil.json"), json);
        }
    }

    private static float[] roseHue(ZipFile tfc) throws Exception {
        BufferedImage rose = read(tfc, "assets/tfc/textures/item/metal/ingot/rose_gold.png");
        long r = 0, g = 0, b = 0, n = 0;
        float[] hsb = new float[3];
        for (int y = 0; y < rose.getHeight(); y++) {
            for (int x = 0; x < rose.getWidth(); x++) {
                int c = rose.getRGB(x, y);
                if (((c >>> 24) & 255) == 0) continue;
                int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
                java.awt.Color.RGBtoHSB(rr, gg, bb, hsb);
                if (hsb[1] < 0.18f) continue;
                r += rr;
                g += gg;
                b += bb;
                n++;
            }
        }
        if (n == 0) n = 1;
        java.awt.Color.RGBtoHSB((int) (r / n), (int) (g / n), (int) (b / n), hsb);
        System.out.println("rose hue " + hsb[0] + " sat " + hsb[1]);
        return hsb;
    }

    /** Second picker, the saturated swatch: warm orange. */
    private static final int IRON_COPPER_COLOR = 0xBA7C3E;
    /** First picker, the saturated swatch: yellow-green. */
    private static final int STEEL_GOLD_COLOR = 0xA3B43F;

    private static BufferedImage ironCopper(BufferedImage src, boolean[][] keep) {
        return tone(src, keep, IRON_COPPER_COLOR);
    }

    private static BufferedImage steelGold(BufferedImage src, boolean[][] keep) {
        return tone(src, keep, STEEL_GOLD_COLOR);
    }

    /**
     * Replace the hue of every metal pixel. Shading stays, but nothing is mixed
     * back toward the old steel or copper color, and highlights are not washed white.
     */
    private static BufferedImage tone(BufferedImage src, boolean[][] keep, int key) {
        float[] keyHsb = java.awt.Color.RGBtoHSB((key >> 16) & 255, (key >> 8) & 255, key & 255, null);
        float hue = keyHsb[0];
        float sat = Math.max(0.62f, keyHsb[1]);
        int min = 255, max = 0, count = 0;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) continue;
                int l = lum(c);
                min = Math.min(min, l);
                max = Math.max(max, l);
                count++;
            }
        }
        if (count == 0 || max <= min) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    out.setRGB(x, y, c);
                    continue;
                }
                float u = (lum(c) - min) / (float) (max - min);
                float value = 0.34f + 0.52f * u;
                int rgb = java.awt.Color.HSBtoRGB(hue, Math.min(0.9f, sat), value);
                out.setRGB(x, y, (c & 0xFF000000) | (rgb & 0xFFFFFF));
            }
        }
        return out;
    }

    private static BufferedImage lift(BufferedImage src, boolean[][] keep, float towardWhite) {
        int wr = 236, wg = 232, wb = 226;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    out.setRGB(x, y, c);
                    continue;
                }
                int r = lerp((c >> 16) & 255, wr, towardWhite);
                int g = lerp((c >> 8) & 255, wg, towardWhite);
                int b = lerp(c & 255, wb, towardWhite);
                out.setRGB(x, y, (c & 0xFF000000) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private static BufferedImage darken(BufferedImage src, boolean[][] keep, float factor) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    out.setRGB(x, y, c);
                    continue;
                }
                int r = clamp((int) (((c >> 16) & 255) * factor));
                int g = clamp((int) (((c >> 8) & 255) * factor));
                int b = clamp((int) ((c & 255) * factor));
                out.setRGB(x, y, (c & 0xFF000000) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private static BufferedImage blend(BufferedImage a, BufferedImage b, float t) {
        return blendMasked(a, b, t, null);
    }

    private static BufferedImage blendMasked(BufferedImage a, BufferedImage b, float t, boolean[][] keep) {
        BufferedImage out = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int ca = a.getRGB(x, y);
                int cb = (x < b.getWidth() && y < b.getHeight()) ? b.getRGB(x, y) : 0;
                if (keep != null && keep[y][x]) {
                    out.setRGB(x, y, ca);
                    continue;
                }
                int aa = (ca >>> 24) & 255;
                int ab = (cb >>> 24) & 255;
                if (aa == 0 && ab == 0) continue;
                if (aa == 0) {
                    out.setRGB(x, y, cb);
                    continue;
                }
                if (ab == 0) {
                    out.setRGB(x, y, ca);
                    continue;
                }
                int r = lerp((ca >> 16) & 255, (cb >> 16) & 255, t);
                int g = lerp((ca >> 8) & 255, (cb >> 8) & 255, t);
                int bl = lerp(ca & 255, cb & 255, t);
                out.setRGB(x, y, (Math.max(aa, ab) << 24) | (r << 16) | (g << 8) | bl);
            }
        }
        return out;
    }

    private static boolean[][] handleMask(BufferedImage a, BufferedImage b) {
        boolean[][] mask = new boolean[a.getHeight()][a.getWidth()];
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int ca = a.getRGB(x, y);
                int cb = b.getRGB(x, y);
                mask[y][x] = ca == cb && ((ca >>> 24) & 255) > 0;
            }
        }
        return mask;
    }

    /**
     * Recolor forged shading into rose gold. Gray steel has no stable hue, so the
     * target hue is used outright and only the pixel's brightness is kept.
     * {@code satMix} and {@code hueMix} are unused; the signature stays so call sites read clearly.
     */
    private static BufferedImage hueShift(BufferedImage src, boolean[][] keep, float[] targetHsb, float satMix, float hueMix) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    out.setRGB(x, y, c);
                    continue;
                }
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                java.awt.Color.RGBtoHSB(r, g, b, hsb);
                float sat = Math.min(0.85f, targetHsb[1] * (0.35f + 0.65f * hsb[2]));
                int rgb = java.awt.Color.HSBtoRGB(targetHsb[0], sat, hsb[2]);
                out.setRGB(x, y, ((c >>> 24) << 24) | (rgb & 0xFFFFFF));
            }
        }
        return out;
    }

    private static BufferedImage paleAndCrack(BufferedImage src, boolean[][] keep, float towardWhite) {
        BufferedImage pale = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    pale.setRGB(x, y, c);
                    continue;
                }
                int r = lerp((c >> 16) & 255, 236, towardWhite);
                int g = lerp((c >> 8) & 255, 240, towardWhite);
                int b = lerp(c & 255, 244, towardWhite);
                pale.setRGB(x, y, (c & 0xFF000000) | (r << 16) | (g << 8) | b);
            }
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pale.getHeight(); y++) {
            for (int x = 0; x < pale.getWidth(); x++) {
                int c = pale.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    out.setRGB(x, y, c);
                    continue;
                }
                int lum = lum(c);
                int neighbors = 0;
                int sum = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= pale.getWidth() || ny >= pale.getHeight()) continue;
                        int n = pale.getRGB(nx, ny);
                        if (((n >>> 24) & 255) == 0) continue;
                        sum += lum(n);
                        neighbors++;
                    }
                }
                int avg = neighbors == 0 ? lum : sum / neighbors;
                if (lum + 28 < avg) {
                    int r = (int) (((c >> 16) & 255) * 0.55f);
                    int g = (int) (((c >> 8) & 255) * 0.55f);
                    int b = (int) ((c & 255) * 0.62f);
                    out.setRGB(x, y, (c & 0xFF000000) | (r << 16) | (g << 8) | b);
                } else {
                    out.setRGB(x, y, c);
                }
            }
        }
        return out;
    }

    /** Remap shading onto a cool silver ramp. Handle pixels, when masked, stay wood. */
    private static BufferedImage silver(BufferedImage src, boolean[][] keep) {
        int min = 255, max = 0;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) continue;
                int l = lum(c);
                min = Math.min(min, l);
                max = Math.max(max, l);
            }
        }
        if (max <= min) max = min + 1;
        int[] shadow = {150, 164, 180};
        int[] light = {248, 250, 252};
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 255) == 0 || (keep != null && keep[y][x])) {
                    out.setRGB(x, y, c);
                    continue;
                }
                float t = (lum(c) - min) / (float) (max - min);
                int r = (int) (shadow[0] + (light[0] - shadow[0]) * t);
                int g = (int) (shadow[1] + (light[1] - shadow[1]) * t);
                int b = (int) (shadow[2] + (light[2] - shadow[2]) * t);
                out.setRGB(x, y, (c & 0xFF000000) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
            }
        }
        return out;
    }

    private static BufferedImage grayscale(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                int a = (c >>> 24) & 255;
                int l = lum(c);
                // Keep the molten highlight range bright so the fluid tint reads as the metal color.
                int v = Math.min(255, (int) (l * 1.15f + 24));
                out.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
            }
        }
        return out;
    }

    private static BufferedImage tintGray(BufferedImage src, int tint) {
        int tr = (tint >> 16) & 255, tg = (tint >> 8) & 255, tb = tint & 255;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                int a = (c >>> 24) & 255;
                int v = (c >> 16) & 255;
                int r = v * tr / 255;
                int g = v * tg / 255;
                int b = v * tb / 255;
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private static int lum(int c) {
        return (int) (0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255));
    }

    private static int lerp(int a, int b, float t) {
        return clamp((int) (a + (b - a) * t));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void fill(BufferedImage img, int x0, int y0, int x1, int y1, int argb) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                img.setRGB(x, y, argb);
            }
        }
    }

    private static void px(BufferedImage img, int x, int y, int argb) {
        img.setRGB(x, y, argb);
    }

    private static BufferedImage read(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IllegalStateException("missing " + name);
        }
        try (InputStream in = zip.getInputStream(entry)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IllegalStateException("unreadable " + name);
            }
            BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(image, 0, 0, null);
            return argb;
        }
    }

    private static void copyEntry(ZipFile zip, String name, Path dest) throws Exception {
        Files.createDirectories(dest.getParent());
        try (InputStream in = zip.getInputStream(zip.getEntry(name))) {
            Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void save(BufferedImage image, Path dest) throws Exception {
        Files.createDirectories(dest.getParent());
        ImageIO.write(image, "png", dest.toFile());
    }

    private AssetPainter() {
    }
}
