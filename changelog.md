## 1.18.1-2.0.0.0
* Workshops of Doom now depends on Structure Spawn Loader https://www.curseforge.com/minecraft/mc-mods/structure-spawn-loader
* Substantially refactored configuration, everything that was previously in forge configs is now defined via jsons
* Mob spawn lists for structures are now defined via Structure Spawn Loader jsons
* Structure size and start pools are now defined in json in the data/workshopsofdoom/worldgen/configured_structure_feature datapack folder, ala vanilla datapacks
* -- These use the same data format as vanilla village structures, except that size is not capped at 7
* The manner in which structures are added is defined in jsons in the data/workshopsofdoom/worldgen/workshopsofdoom/configured_noise_settings_modifier datapack folder
* -- This feature is very experimental, documentation and format is still WIP. These features may be moved into a separate library in the future.
* -- Structures' spacing values are defined here
* -- Which biomes the structures spawn in are also defined here, there are currently three ways to define biomes
* -- -- `"type": "workshopsofdoom:biomes", "biomes": []` to specify a list of biome IDs the structure can spawn in
* -- -- `"type": "workshopsofdoom:category", "categories": ["some_biome_category"]` specifies zero or more vanilla biome categories, e.g. "icy"
* -- -- `"type": "workshopsofdoom:biome_type", "biome_type": "SOME_BIOME_TYPE"` specifies a forge biome dictionary type (in all caps), e.g. "MOUNTAIN"
* -- By default, which dimensions Workshops of Doom's structures are added to are specified in data/workshopsofdoom/tags/dimensions/spawn_workshopsofdoom_structures.json (the noise settings modifier jsons refer to this tag)

* Changed default quarry spacing from 32/12 to 24/10 (made more frequent)
* Mountain mines now spawn in biomes categorized as "mountain" (in addition to "extreme_hills"), including the new mountain biomes
* Enabled village-style terraforming for all Workshops of Doom structures. This should make structures less strange when they generate on hilly or slopey terrain but may make them stranger in other ways.
* Rebalanced and reworked mob spawning
* Pillagers and excavators can now spawn in the entire encapsulating bounding box of Workshops of Doom structures (previously they only spawned within individual pieces' bounding boxes, similar to nether fortresses)
* Vanilla mobs can now spawn within the area of quarries and mines, though at a lower rate than pillagers and excavators
* (The biome spawn lists are still overridden, so mods or datapacks must add Structure Spawn Loader entries for workshops of doom structures to allow other mods' mobs to spawn within Workshops of Doom structures)
* Reduced the amount of persistant mobs spawned when workshops are first generated -- half as many vindicators and pillagers are generated, excavators are spawned at about 1/4 the former rate
* Workshops now override the biome spawn lists, allowing pillagers and excavators to randomly spawn in addition to standard mobs (see two points above for ramifications re: other mods' mobs)
* Substantially optimized structure generation for Workshops of Doom structures
* Increased generation depth of Workshops of Doom structures
* Added copper ore and ingots to loot tables
* Loot tables that roll ingots and gems will now generate vanilla items by default instead of anything with the same forge tag (datapacks and mods can still add items to these loot tables by adding items to the loot tags)
* Reduced the spawn rate of minecarts, increased the chance of them spawning with a chest
