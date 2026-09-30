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
  |  glass pane                    <  stained glass pane (the "glass:" colour)             E  glass
  o  fence                         ]  fence gate    /  wooden pressure plate (on a fence: a table)
  Q  quartz pillar                 Y  oak log       *  oak leaves  Z  hay bale
  p  oak planks                    P  spruce planks
  1-4  spruce stairs rising to the north, east, south, west; 5-8 the same upside down
  _ -  spruce slab, bottom / top half                  D  wooden door (two layers)       e  chiseled quartz
Skystone:
  0  pillar, upright               ( )  pillar lying along x / along z
  9  chiseled                      j  polished                     k  tiles
  i l m n  stairs rising to the north, east, south, west;  J U V X  the same upside down (cornices, arches)
  y z  polished slab, bottom / top half                r  brick slab (fallen masonry)
Furnishings:
  b  a bed's head (against the wall)   u  its foot, beside it     q  carpet (the "carpet:" colour)
  d  torch (on a wall beside it, else standing)                   [  trapdoor shelf (hinged to a wall beside it)
  @  flower pot with a plant       &  brewing stand   $  enchanting table   +  jukebox
  }  an empty chest (furniture: a footlocker, a cupboard)

Doorways must stay open on layers 1-3: the middle three columns or rows next to each wall, two deep.
Rooms that link levels also keep each corner's 2 x 2 open on layers 1-3.
