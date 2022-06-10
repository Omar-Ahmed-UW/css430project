/**
 * @Authors Phillip Burlachenko
 * @File SuperBlock.java
 * @Date 6/8/22
 */

class SuperBlock
{
    public int totalBlocks;                     // the number of disk blocks
    public int totalInodes;                     // the number of inodes
    public int freeList;                        // the block number of the free list's head

    /**
     * SuperBlock(int blockAmount)
     * Overloaded SuperBlock() passes in the number of blocks to be instantiated and sets variables to their values.
     * @param blockAmount
     */
    public SuperBlock(int diskSize)
    {
        byte[] theSuperBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, theSuperBlock);
        totalBlocks = SysLib.bytes2int(theSuperBlock, 0);
        totalInodes = SysLib.bytes2int(theSuperBlock, 4);
        freeList = SysLib.bytes2int(theSuperBlock, 8);
        totalInodes = totalBlocks;
        totalBlocks = diskSize;
        format(64);
    }

    /**
     * sync()
     * Syncs the cache back to the physical disk.
     */
    void sync()
    {
        byte tempBlock[] = new byte[Disk.blockSize];
        SysLib.int2bytes(this.totalBlocks, tempBlock, 0);
        SysLib.int2bytes(this.totalInodes, tempBlock, 4);
        SysLib.int2bytes(this.freeList, tempBlock, 8);
        SysLib.rawwrite(0, tempBlock);
    }

    /**
     * format(int files)
     * This method takes in the number of nodes in the system and formats the blocks, cannot be undone.  Sets all
     * blocks to free.
     * @param files
     */
    void format(int files)
    {
        this.totalInodes = files;
        for(short i = 0; i < this.totalInodes; i++)
        {
            Inode newInode = new Inode();
            newInode.usedFlag = 0;
            newInode.toDisk(i);
        }
        this.freeList = 2 + this.totalInodes * 32 / 512;
        for(int i = this.freeList; i < this.totalBlocks; i++)
        {
            byte[] theBlock = new byte[512];
            for(int j = 0; j < 512; j++)
            {
                theBlock[j] = 0;
            }
            SysLib.int2bytes(i + 1, theBlock, 0);
            SysLib.rawwrite(i, theBlock);
        }
        this.sync();
    }

    
}