"""Syncs our data/ and assets/ into the newest TFC world as a datapack + resource pack,
so recipes/textures can be iterated with /reload and F3+T instead of restarting the game."""
import json, os, shutil, sys

SRC = "src/main/resources"
MC = r"D:/GAME/Minecraft TFC 1.20.1 UT/.minecraft"
DP_NAME = "copper_iron_alloy_live"
RP_NAME = "copper_iron_alloy_live"


def newest_world():
    saves = os.path.join(MC, "saves")
    worlds = [os.path.join(saves, d) for d in os.listdir(saves)
              if os.path.isdir(os.path.join(saves, d))]
    return max(worlds, key=os.path.getmtime) if worlds else None


def copy_tree(src, dst):
    if os.path.isdir(dst):
        shutil.rmtree(dst)
    shutil.copytree(src, dst)


def main():
    world = sys.argv[1] if len(sys.argv) > 1 else newest_world()
    if not world:
        print("no world found")
        return

    # datapack (recipes / tags / advancements / metal data)
    dp = os.path.join(world, "datapacks", DP_NAME)
    copy_tree(os.path.join(SRC, "data"), os.path.join(dp, "data"))
    with open(os.path.join(dp, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"pack": {"pack_format": 15, "description": "copper_iron_alloy live data"}}, f, indent=2)

    # resource pack (textures / models / lang)
    rp = os.path.join(MC, "resourcepacks", RP_NAME)
    copy_tree(os.path.join(SRC, "assets"), os.path.join(rp, "assets"))
    with open(os.path.join(rp, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"pack": {"pack_format": 15, "description": "copper_iron_alloy live assets"}}, f, indent=2)

    print("datapack ->", dp)
    print("resourcepack ->", rp)
    print("in game: /reload   (data)    F3+T   (assets)")


if __name__ == "__main__":
    main()
