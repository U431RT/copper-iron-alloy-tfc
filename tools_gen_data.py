"""Deprecated path. The unified generator is tools/gen_assets.py.

This file still generates the metal datapack (definitions, fluid tags, alloys,
casting and heating) so older commands keep working. Textures, models, crafting
and the fridges live in tools/gen_assets.py plus tools/AssetPainter.java.
"""
import json, os, sys

MOD = "copper_iron_alloy"
RES = "src/main/resources"

METALS = {
    # name: (tier, melt temperature, heat capacity)
    "iron_copper_alloy":   (3, 1100, 0.0086),
    "steel_gold":          (4, 1450, 0.0086),
    "brittle_white_steel": (6, 1500, 0.0086),
    "white_steel":         (7, 1600, 0.0086),
}

FORMS = {"ingot": 100, "double_ingot": 200, "sheet": 200, "double_sheet": 400}

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")

def main():
    for metal, (tier, melt, heat) in METALS.items():
        write(f"{RES}/data/{MOD}/tfc/metals/{metal}.json", {
            "tier": tier,
            "fluid": f"{MOD}:metal/{metal}",
            "melt_temperature": melt,
            "specific_heat_capacity": heat,
            "ingots": {"tag": f"forge:ingots/{metal}"},
            "double_ingots": {"tag": f"forge:double_ingots/{metal}"},
            "sheets": {"tag": f"forge:sheets/{metal}"},
        })
        # fluid tag: still + flowing
        write(f"{RES}/data/{MOD}/tags/fluids/{metal}.json", {
            "replace": False,
            "values": [f"{MOD}:metal/{metal}", f"{MOD}:metal/flowing_{metal}"],
        })
        for form, amount in FORMS.items():
            write(f"{RES}/data/forge/tags/items/{form}s/{metal}.json".replace("ingots_","ingots/").replace("double_ingots","double_ingots"), {
                "replace": False,
                "values": [f"{MOD}:{metal}_{form}"],
            })
            write(f"{RES}/data/{MOD}/recipes/heating/{metal}_{form}.json", {
                "type": "tfc:heating",
                "ingredient": {"item": f"{MOD}:{metal}_{form}"},
                "result_fluid": {"fluid": f"{MOD}:metal/{metal}", "amount": amount},
                "temperature": melt,
            })
        # cast an ingot from the fire ingot mold
        write(f"{RES}/data/{MOD}/recipes/casting/{metal}_ingot.json", {
            "type": "tfc:casting",
            "mold": {"item": "tfc:ceramic/fire_ingot_mold"},
            "fluid": {"ingredient": f"{MOD}:metal/{metal}", "amount": 100},
            "result": {"item": f"{MOD}:{metal}_ingot"},
            "break_chance": 0.01,
        })

    # alloys
    write(f"{RES}/data/{MOD}/recipes/alloy/iron_copper_alloy.json", {
        "type": "tfc:alloy", "result": f"{MOD}:iron_copper_alloy",
        "contents": [
            {"metal": "tfc:copper", "min": 0.75, "max": 0.85},
            {"metal": "tfc:cast_iron", "min": 0.15, "max": 0.25},
        ],
    })
    write(f"{RES}/data/{MOD}/recipes/alloy/steel_gold.json", {
        "type": "tfc:alloy", "result": f"{MOD}:steel_gold",
        "contents": [
            {"metal": "tfc:rose_gold", "min": 0.60, "max": 0.80},
            {"metal": "tfc:steel", "min": 0.20, "max": 0.40},
        ],
    })
    print("data generated")

if __name__ == "__main__":
    main()
