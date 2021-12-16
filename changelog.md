## 1.18.1-2.0.0.0
* Substantially refactored configuration
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
* Enabled village terraforming for all Workshops of Doom structures. This should make structures less strange when they generate on hilly or slopey terrain but may make them stranger in other ways.
