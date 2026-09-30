Sky dungeon room templates. Each .room file is one room's inside; the header is described in RoomTemplate.java.
Only rooms listed in index.txt are used. The files are written (and checked) by tools/dungeon/write_rooms.py from
tools/dungeon/rooms.py; surface buildings by tools/dungeon/buildings.py.

The writer dresses every room: a floor of Skystone Tiles, a Polished Skystone ceiling, and brick walls on a polished
plinth. In a template '#' follows the same rule: a tile in the floor layer, polished in the ceiling, brick between.

Legend (one character per block):
  .  open air                      #  masonry (see above)          L  Lumenite Lamp        B  bookshelf
  ~  water                         =  lava                         x  cobweb               I  iron bars
  R  cracked Skystone Bricks       M  mossy Skystone Bricks        G  grass block (floors)
  t  tall grass                    f  a flower                     w  Windbloom
  c  the room's chest: storeroom loot, the Sky Key in a key room, the treasure in the vault (else nothing)
  C  a second chest: half the time in storerooms, the riches in the vault (else nothing)
  s  a spawner if the room is a guard room (else nothing)       S  always a spawner
  ^  Trigger Stone (floors)        v  arrow dispenser, F  fire-charge dispenser (facing away from the nearest wall)
  %  Crumbling Skystone over a pit ,  a pit (open floor; the room's "pit:" fills its bottom)
  g  Gale Vent blowing up          h  Gale Vent blowing away from the nearest wall
  a  Sealed Alcove                 T  buried TNT (in the floor)
  W  Warden Seal                   ?  ruined: masonry or air, half and half
  H  ladder (on the nearest wall)  K  cauldron      O  furnace     N  crafting table     A  anvil
  "  a hearth's fire: a furnace alight (it glows and never goes out)
  |  glass pane                    <  stained glass pane (the "glass:" colour)             E  glass
  o  fence                         ]  fence gate    /  Wooden Table (of the timber wood)
  Y  log (timber)                  *  oak leaves    Z  hay bale
  p  oak planks                    P  planks (timber)
  1-4  wooden stairs rising to the north, east, south, west; 5-8 the same upside down
  _ -  wooden slab, bottom / top half                  D  wooden door (two layers)
       (stairs and slabs from the "eaves:" layer up are of the "roof:" material: spruce, dark oak or Skyslate)
Skyslate:
  Q  Skyslate bricks               e  Skyslate shingles
Skystone:
  0  pillar, upright               ( )  pillar lying along x / along z
  9  chiseled                      j  polished                     k  tiles
  i l m n  stairs rising to the north, east, south, west;  J U V X  the same upside down (cornices, arches)
  y z  polished slab, bottom / top half                r  brick slab (fallen masonry)
Furnishings:
  b  a bed's head (against the wall)   u  its foot, beside it     q  carpet (the "carpet:" colour)
  d  torch (on a wall beside it, else standing)                   [  wall shelf (hung on a wall beside it)
  \  weapon rack (hung on a wall beside it)
  @  flower pot with a plant       &  brewing stand   $  enchanting table   +  jukebox
  }  an empty chest (furniture: a footlocker, a cupboard)
  !  barrel      {  crate      :  pantry shelf (faces away from the wall beside it)
  `  market counter (the "goods:" kind)        '  awning (the "awning:" colour), hung from the wall beside it
The sky's own (Greek letters, as the ASCII ones are all taken; ';' starts a comment):
  Ψ ψ  candelabra, its arm along x / along z          ω  brazier            φ  fountain       Θ  orrery
  κ λ μ ν  wooden chair (of the timber) with its back to the north, east, south, west
  ι ζ ε η θ  Mason's Bench, Sawmill, Loom, Potter's Wheel, Glassblower's Furnace      τ  tanning rack (faces out)
  α β γ δ  pew with its back to the north, east, south, west       π Π  Skystone bench along x / along z
  ξ  Stormglass hanging from the block above          >  Stormglass vein

Headers for buildings: carpet, glass (colours); roof, timber, eaves; goods; awning. See RoomTemplate.java.

Doorways must stay open on layers 1-3: the middle three columns or rows next to each wall, two deep.
Entrance halls also keep each corner's 2 x 2 open on layers 1-3 (the shaft from the surface comes down there).
Shaft rooms ("roles: shaft_top" / "shaft_foot") are one cell, stand unturned, and are built round a ladder at the
middle (4, 4) facing south: a top room leaves the three layers over the hole open, a foot room carries a mast ('0')
just north of it the full height; both keep a light within two blocks.
