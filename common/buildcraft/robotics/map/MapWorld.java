package buildcraft.robotics.map;

import java.io.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.*;

import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.hash.TLongHashSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import buildcraft.core.lib.utils.NBTUtils;

import cern.colt.map.OpenLongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public class MapWorld {
	private final OpenLongObjectHashMap regionMap;
	private final ObjectIntHashMap timeToUpdate = new ObjectIntHashMap();//<Chunk, Integer>
	private final LongLongHashMap regionUpdateTime;
	private final LongHashSet updatedChunks;
	private final File location;

	public MapWorld(World world, File location) {
		regionMap = new OpenLongObjectHashMap();
		regionUpdateTime = new LongLongHashMap();
		updatedChunks = new LongHashSet();

		String saveFolder = world.provider.getSaveFolder();
		if (saveFolder == null) {
			saveFolder = "world";
		}
		this.location = new File(location, saveFolder);
		try {
			this.location.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MapRegion getRegion(int x, int z) {
		long id = MapUtils.getIDFromCoords(x, z);
		MapRegion region = (MapRegion) regionMap.get(id);
		if (region == null) {
			region = new MapRegion(x, z);

			// Check in the location first
			File target = new File(location, "r" + x + "," + z + ".nbt");
			if (target.exists()) {
				try {
					InputStream f = new BufferedInputStream(new FileInputStream(target));
					byte[] data = new byte[(int) target.length()];
					f.read(data);
					f.close();

					NBTTagCompound nbt = NBTUtils.load(data);
					if (nbt != null) {
						region.readFromNBT(nbt);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			regionMap.put(id, region);
		}
		return region;
	}

	private MapChunk getChunk(int x, int z) {
		MapRegion region = getRegion(x >> 4, z >> 4);
		return region.getChunk(x & 15, z & 15);
	}

	public boolean hasChunk(int x, int z) {
		MapRegion region = getRegion(x >> 4, z >> 4);
		return region.hasChunk(x & 15, z & 15);
	}

	public void save() {
		long[] chunkList;
		synchronized (updatedChunks) {
			chunkList = updatedChunks.toArray();
			updatedChunks.clear();
		}

		for (long id : chunkList) {
			MapRegion region = (MapRegion) regionMap.get(id);
			if (region == null) {
				continue;
			}

			NBTTagCompound output = new NBTTagCompound();
			region.writeToNBT(output);
			byte[] data = NBTUtils.save(output);
			File file = new File(location, "r" + MapUtils.getXFromID(id) + "," + MapUtils.getZFromID(id) + ".nbt");

			try {
				OutputStream f = new BufferedOutputStream(new FileOutputStream(file));
				f.write(data);
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getColor(int x, int z) {
		MapChunk chunk = getChunk(x >> 4, z >> 4);
		return chunk.getColor(x & 15, z & 15);
	}

	public void tick() {
		if (timeToUpdate.size() > 0) {
			synchronized (timeToUpdate) {
				Set<Chunk> chunks = new HashSet<Chunk>();
				chunks.addAll(timeToUpdate.keySet());
				for (Chunk c : chunks) {
					int v = timeToUpdate.get(c);
					if (v > 1) {
						timeToUpdate.put(c, v - 1);
					} else {
						try {
							updateChunk(c);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void updateChunk(Chunk rchunk) {
		long id = MapUtils.getIDFromCoords(rchunk.xPosition, rchunk.zPosition);
		MapChunk chunk = getChunk(rchunk.xPosition, rchunk.zPosition);
		chunk.update(rchunk);
		synchronized (updatedChunks) {
	        updatedChunks.add(id);
        }
		synchronized (timeToUpdate) {
			timeToUpdate.remove(rchunk);
		}
		regionUpdateTime.put(id, System.nanoTime()/1000000L);
	}

	public long getUpdateTime(int x, int z) {
		return regionUpdateTime.get(MapUtils.getIDFromCoords(x, z));
	}

	public void updateChunkDelayed(Chunk chunk, byte time) {
		synchronized (timeToUpdate) {
			timeToUpdate.put(chunk, (int) time);
		}
	}
}
