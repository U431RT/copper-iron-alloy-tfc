"""Generates recipes, textures, models and lang for the TFC port."""
import io, json, os, zipfile
from PIL import Image

MOD = "copper_iron_alloy"
RES = "src/main/resources"
TFC = "libs/TerraFirmaCraft-Forge-1.20.1-3.2.12.jar"
VANILLA = "D:/GAME/Minecraft/.minecraft/versions/1.20.1/1.20.1.jar"
SRC_TEX = r"D:/dshworkshop/copper_iron_alloy/src/main/resources/assets/copper_iron_alloy/textures"

METALS = ["iron_copper_alloy", "steel_gold", "brittle_white_steel", "white_steel"]
TOOLS = ["pickaxe", "axe", "shovel", "hoe", "sword", "knife", "hammer", "chisel", "mace", "propick", "scythe"]
HEAD_NAME = {"pickaxe": "pickaxe_head", "axe": "axe_head", "shovel": "shovel_head",
             "hoe": "hoe_head", "sword": "sword_blade", "knife": "knife_blade",
             "hammer": "hammer_head", "chisel": "chisel_head", "mace": "mace_head",
             "propick": "propick_head", "scythe": "scythe_blade"}
MOLD_NAME = {"pickaxe": "pickaxe_head_mold", "axe": "axe_head_mold", "shovel": "shovel_head_mold",
             "hoe": "hoe_head_mold", "sword": "sword_blade_mold", "knife": "knife_blade_mold",
             "hammer": "hammer_head_mold", "chisel": "chisel_head_mold", "mace": "mace_head_mold",
             "propick": "propick_head_mold", "scythe": "scythe_blade_mold"}

PALETTE = {
    "iron_copper_alloy":   [(120, 78, 52), (168, 112, 72), (206, 150, 104), (232, 190, 150)],
    "steel_gold":          [(40, 40, 46), (78, 78, 88), (128, 128, 140), (206, 182, 96)],
    "brittle_white_steel": [(96, 104, 116), (140, 152, 168), (188, 200, 214), (232, 240, 248)],
    "white_steel":         [(110, 126, 140), (160, 180, 196), (208, 226, 240), (248, 254, 255)],
}

ZH = {
    "iron_copper_alloy": "铁铜合金", "steel_gold": "钢金",
    "brittle_white_steel": "脆白钢", "white_steel": "白钢",
}
TOOL_ZH = {"pickaxe": "镐", "axe": "斧", "shovel": "锹", "hoe": "锄", "sword": "剑", "knife": "刀", "hammer": "锤", "chisel": "凿", "mace": "狼牙棒", "propick": "探矿镐", "scythe": "镰刀"}
TOOL_EN = {"pickaxe": "Pickaxe", "axe": "Axe", "shovel": "Shovel", "hoe": "Hoe", "sword": "Sword", "knife": "Knife", "hammer": "Hammer", "chisel": "Chisel", "mace": "Mace", "propick": "Prospector's Pick", "scythe": "Scythe"}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def shades(colors, lum, tint=None):
    t = max(0.0, min(1.0, lum / 255.0))
    pos = t * (len(colors) - 1)
    i = int(pos)
    a, b = colors[min(i, len(colors) - 1)], colors[min(i + 1, len(colors) - 1)]
    f = pos - i
    rgb = tuple(int(a[k] + (b[k] - a[k]) * f) for k in range(3))
    if tint:
        rgb = tuple(min(255, int(rgb[k] * 0.75 + tint[k] * 0.25)) for k in range(3))
    return rgb


def handle_mask(img_a, img_b):
    """Pixels that are identical in two different metal sprites are the wooden handle."""
    mask = set()
    pa, pb = img_a.load(), img_b.load()
    for y in range(img_a.size[1]):
        for x in range(img_a.size[0]):
            if pa[x, y] == pb[x, y] and pa[x, y][3] > 0:
                mask.add((x, y))
    return mask


def recolor_masked(img, colors, mask):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    px, dst = img.load(), out.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if (x, y) in mask:
                dst[x, y] = (r, g, b, a)
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            rgb = shades(colors, lum)
            dst[x, y] = (rgb[0], rgb[1], rgb[2], a)
    return out


def recolor(img, colors, keep_wood=False):
    import colorsys
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    px, dst = img.load(), out.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if keep_wood:
                h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
                if 0.03 <= h <= 0.16 and s > 0.25:      # wooden handle / rod pixels
                    dst[x, y] = (r, g, b, a)
                    continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            rgb = shades(colors, lum)
            dst[x, y] = (rgb[0], rgb[1], rgb[2], a)
    return out


def save(img, rel):
    path = f"{RES}/assets/{MOD}/textures/{rel}.png"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def gen_recipes():
    for metal in METALS:
        for tool in TOOLS:
            # casting from the fired tool head mold
            write(f"{RES}/data/{MOD}/recipes/casting/{metal}_{tool}_head.json", {
                "type": "tfc:casting",
                "mold": {"item": f"tfc:ceramic/{MOLD_NAME[tool]}"},
                "fluid": {"ingredient": f"{MOD}:metal/{metal}", "amount": 100},
                "result": {"item": f"{MOD}:{metal}_{tool}_head"},
                "break_chance": 0.1,
            })
            # forging the head out of an ingot
            rules = {
                "pickaxe": ["punch_last", "bend_not_last", "draw_not_last"],
                "axe": ["punch_last", "shrink_second_last", "draw_third_last"],
                "shovel": ["flatten_last", "draw_second_last", "draw_third_last"],
                "hoe": ["punch_last", "draw_second_last", "draw_third_last"],
                "sword": ["bend_last", "bend_second_last", "bend_third_last"],
                "knife": ["bend_last", "draw_second_last", "draw_third_last"],
                "hammer": ["upset_last", "punch_second_last", "punch_third_last"],
                "chisel": ["upset_last", "draw_second_last", "draw_third_last"],
                "mace": ["upset_last", "shrink_second_last", "hit_third_last"],
                "propick": ["punch_last", "draw_second_last", "draw_third_last"],
                "scythe": ["bend_last", "draw_second_last", "draw_third_last"],
            }[tool]
            write(f"{RES}/data/{MOD}/recipes/anvil/{metal}_{tool}_head.json", {
                "type": "tfc:anvil",
                "input": {"tag": f"forge:ingots/{metal}"},
                "result": {"item": f"{MOD}:{metal}_{tool}_head"},
                "tier": {"iron_copper_alloy": 3, "steel_gold": 4,
                         "brittle_white_steel": 5, "white_steel": 6}[metal],
                "rules": rules,
                "apply_forging_bonus": True,
            })
            # assemble head + rod -> tool (keeps the forging bonus)
            patterns = {
                "pickaxe": ["X", "Y"], "axe": ["X", "Y"], "shovel": ["X", "Y"],
                "hoe": ["XX", " Y", " Y"], "sword": ["X", "Y"], "knife": ["X", "Y"],
                "hammer": ["XX", " Y"], "chisel": ["X", "Y"], "mace": ["X", "Y"],
                "propick": ["X", "Y"], "scythe": ["XX", " Y"],
            }
            pattern = patterns[tool]
            write(f"{RES}/data/{MOD}/recipes/crafting/{metal}_{tool}.json", {
                "type": "tfc:advanced_shaped_crafting",
                "pattern": pattern,
                "key": {"X": {"item": f"{MOD}:{metal}_{tool}_head"},
                        "Y": {"tag": "forge:rods/wooden"}},
                "result": {"stack": {"item": f"{MOD}:{metal}_{tool}"},
                           "modifiers": ["tfc:copy_forging_bonus"]},
                "input_row": 0, "input_column": 0,
            })
        # double ingot -> sheet, on the anvil
        write(f"{RES}/data/{MOD}/recipes/anvil/{metal}_sheet.json", {
            "type": "tfc:anvil",
            "input": {"tag": f"forge:double_ingots/{metal}"},
            "result": {"item": f"{MOD}:{metal}_sheet"},
            "tier": 3 if metal == "iron_copper_alloy" else 4,
            "rules": ["hit_last", "hit_second_last", "hit_third_last"],
        })
        # two sheets -> double sheet
        write(f"{RES}/data/{MOD}/recipes/crafting/{metal}_double_sheet.json", {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": f"{MOD}:{metal}_sheet"}, {"item": f"{MOD}:{metal}_sheet"}],
            "result": {"item": f"{MOD}:{metal}_double_sheet"},
        })
        # double ingot from two ingots
        write(f"{RES}/data/{MOD}/recipes/crafting/{metal}_double_ingot.json", {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": f"{MOD}:{metal}_ingot"}, {"item": f"{MOD}:{metal}_ingot"}],
            "result": {"item": f"{MOD}:{metal}_double_ingot"},
        })


def gen_textures():
    """Textures are painted by tools/AssetPainter.java from TFC sprites.

    A luminance recolor of copper (and of the vanilla anvil) is what made the
    metals look like dyed vanilla. Re-run AssetPainter instead of recoloring here.
    """
    print("textures: run tools/AssetPainter.java (TFC sprite blend)")
    return
    with zipfile.ZipFile(TFC) as z:
        def load(path):
            return Image.open(io.BytesIO(z.read(path))).convert("RGBA")

        for metal in METALS:
            colors = PALETTE[metal]
            save(recolor(load("assets/tfc/textures/item/metal/ingot/copper.png"), colors),
                 f"item/{metal}_ingot")
            save(recolor(load("assets/tfc/textures/item/metal/double_ingot/copper.png"), colors),
                 f"item/{metal}_double_ingot")
            save(recolor(load("assets/tfc/textures/item/metal/sheet/copper.png"), colors),
                 f"item/{metal}_sheet")
            save(recolor(load("assets/tfc/textures/item/metal/double_sheet/copper.png"), colors),
                 f"item/{metal}_double_sheet")
            for tool in TOOLS:
                head_sprite = load(f"assets/tfc/textures/item/metal/{HEAD_NAME[tool]}/copper.png")
                save(recolor(head_sprite, colors), f"item/{metal}_{tool}_head")
                sprite = load(f"assets/tfc/textures/item/metal/{tool}/copper.png")
                mask = handle_mask(sprite, load(f"assets/tfc/textures/item/metal/{tool}/wrought_iron.png"))
                save(recolor_masked(sprite, colors, mask), f"item/{metal}_{tool}")
            with zipfile.ZipFile(VANILLA) as v:
                top = Image.open(io.BytesIO(v.read("assets/minecraft/textures/block/anvil_top.png"))).convert("RGBA")
                body = Image.open(io.BytesIO(v.read("assets/minecraft/textures/block/anvil.png"))).convert("RGBA")
            save(recolor(top, colors), f"block/{metal}_anvil_top")
            save(recolor(body, colors), f"block/{metal}_anvil")

        # kettle + cup: simple metal shapes from the sheet
        for name in ("steel_gold_kettle", "steel_gold_cup"):
            save(recolor(load("assets/tfc/textures/item/metal/sheet/copper.png"), PALETTE["steel_gold"]),
                 f"item/{name}")


def gen_models():
    for metal in METALS:
        for form in ("ingot", "double_ingot", "sheet", "double_sheet"):
            write(f"{RES}/assets/{MOD}/models/item/{metal}_{form}.json",
                  {"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"{MOD}:item/{metal}_{form}"}})
        for tool in TOOLS:
            write(f"{RES}/assets/{MOD}/models/item/{metal}_{tool}_head.json",
                  {"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"{MOD}:item/{metal}_{tool}_head"}})
            write(f"{RES}/assets/{MOD}/models/item/{metal}_{tool}.json",
                  {"parent": "minecraft:item/handheld",
                   "textures": {"layer0": f"{MOD}:item/{metal}_{tool}"}})
        # anvil blockstate/model (TFC anvil shape)
        write(f"{RES}/assets/{MOD}/blockstates/{metal}_anvil.json",
              {"variants": {"facing=north": {"model": f"{MOD}:block/{metal}_anvil"},
                            "facing=south": {"model": f"{MOD}:block/{metal}_anvil", "y": 180},
                            "facing=west": {"model": f"{MOD}:block/{metal}_anvil", "y": 270},
                            "facing=east": {"model": f"{MOD}:block/{metal}_anvil", "y": 90}}})
        write(f"{RES}/assets/{MOD}/models/block/{metal}_anvil.json",
              {"parent": "tfc:block/anvil",
               "textures": {"all": f"{MOD}:block/metal/smooth/{metal}",
                            "particle": f"{MOD}:block/metal/smooth/{metal}"}})
        write(f"{RES}/assets/{MOD}/models/item/{metal}_anvil.json",
              {"parent": f"{MOD}:block/{metal}_anvil"})
    for name in ("steel_gold_kettle", "steel_gold_cup"):
        write(f"{RES}/assets/{MOD}/models/item/{name}.json",
              {"parent": "minecraft:item/generated",
               "textures": {"layer0": f"{MOD}:item/{name}"}})
    write(f"{RES}/assets/{MOD}/models/item/iron_copper_alloy_bucket.json",
          {"parent": "minecraft:item/generated",
           "textures": {"layer0": f"{MOD}:item/iron_copper_alloy_bucket"}})
    write(f"{RES}/assets/{MOD}/models/item/iron_copper_alloy_milk_bucket.json",
          {"parent": "minecraft:item/generated",
           "textures": {"layer0": f"{MOD}:item/iron_copper_alloy_milk_bucket"}})


def gen_lang():
    zh, en = {}, {}
    for metal in METALS:
        base_zh, base_en = ZH[metal], "".join(w.capitalize() for w in metal.split("_"))
        for form, form_zh, form_en in (("ingot", "锭", "Ingot"), ("double_ingot", "双层锭", "Double Ingot"),
                                       ("sheet", "薄板", "Sheet"), ("double_sheet", "双层薄板", "Double Sheet")):
            zh[f"item.{MOD}.{metal}_{form}"] = base_zh + form_zh
            en[f"item.{MOD}.{metal}_{form}"] = f"{base_en} {form_en}"
        for tool in TOOLS:
            zh[f"item.{MOD}.{metal}_{tool}_head"] = base_zh + TOOL_ZH[tool] + "头"
            en[f"item.{MOD}.{metal}_{tool}_head"] = f"{base_en} {TOOL_EN[tool]} Head"
            zh[f"item.{MOD}.{metal}_{tool}"] = base_zh + TOOL_ZH[tool]
            en[f"item.{MOD}.{metal}_{tool}"] = f"{base_en} {TOOL_EN[tool]}"
        zh[f"block.{MOD}.{metal}_anvil"] = base_zh + "砧"
        en[f"block.{MOD}.{metal}_anvil"] = f"{base_en} Anvil"
    zh[f"item.{MOD}.iron_copper_alloy_bucket"] = "铁铜合金桶"
    en[f"item.{MOD}.iron_copper_alloy_bucket"] = "Iron-Copper Bucket"
    zh[f"item.{MOD}.iron_copper_alloy_milk_bucket"] = "铁铜合金奶桶"
    en[f"item.{MOD}.iron_copper_alloy_milk_bucket"] = "Iron-Copper Milk Bucket"
    zh[f"item.{MOD}.steel_gold_kettle"] = "钢金水壶"
    en[f"item.{MOD}.steel_gold_kettle"] = "Steel-Gold Kettle"
    zh[f"item.{MOD}.steel_gold_cup"] = "钢金小杯"
    en[f"item.{MOD}.steel_gold_cup"] = "Steel-Gold Cup"
    for lang, table in (("zh_cn", zh), ("en_us", en)):
        path = f"{RES}/assets/{MOD}/lang/{lang}.json"
        data = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
        data.update(table)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


def main():
    import runpy
    from pathlib import Path
    runpy.run_path(str(Path(__file__).resolve().parents[1] / "tools_gen_data.py"), run_name="__main__")
    gen_recipes()
    gen_textures()
    gen_models()
    gen_lang()
    print("tfc data/textures/models/lang generated")


if __name__ == "__main__":
    main()
