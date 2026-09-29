Sky dungeon room templates. Each .room file is one room's inside; the header is described in RoomTemplate.java.
Only rooms listed in index.txt are used. The files are written (and checked) by tools/dungeon/write_rooms.py.

Legend (one character per block):
  .  open air                      #  Skystone Bricks (weathered at random)
  L  Lumenite Lamp                 B  bookshelf
  ~  water                         =  lava
  x  cobweb                        R  rubble (cobblestone, gravel or mossy cobble)
  I  iron bars                     G  grass block (floors)
  t  tall grass                    f  a flower                    w  Windbloom
  c  the room's chest: storeroom loot, the Sky Key in a key room, the treasure in the vault (else nothing)
  C  a second chest: half the time in storerooms, the riches in the vault (else nothing)
  s  a spawner if the room is a guard room (else nothing)       S  always a spawner
  ^  Trigger Stone (floors)        v  arrow dispenser, F  fire-charge dispenser (facing away from the nearest wall)
  %  Crumbling Skystone over a pit ,  a pit (open floor; the room's "pit:" fills its bottom)
  g  Gale Vent blowing up          h  Gale Vent blowing away from the nearest wall
  a  Sealed Alcove                 T  buried TNT (in the floor)
  W  Warden Seal                   ?  ruined: stone or air, half and half
  H  ladder (on the nearest wall)  K  cauldron      O  furnace     N  crafting table     A  anvil
  |  glass pane                    o  fence         M  mossy cobblestone
  Q  quartz pillar                 Y  oak log       *  oak leaves

Doorways must stay open on layers 1-3: the middle three columns or rows next to each wall, two deep.
Rooms that link levels also keep each corner's 2 x 2 open on layers 1-3.
