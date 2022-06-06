//Phillip Burlachenko Spring 22 final project hw5 
//SuperBlock.java 
public class SuperBlock{
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; //the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList; // the block numebr of the free lists head 
/**
 * Constructor
 * Public constructor used for the SuperBlock initialization. Takes int in param thats equal to the total number of blocks
 * on the dicks. Constructors reads a superblock from the disk and assigns variables for the total number of blocks, inodes, and 
 * the block number of the free lists head. 
 * @param diskSize
 */
    public SuperBlock(int diskSize){
        //read the superblock from disk
        //initialize the new block to byte block array of disk block size
        byte[] superBlock = new byte[Disk.blockSize];
        //read back
        SysLib.rawread(0, superBlock);
        //get assignments from blocks at locations 0,4,8
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = Syslib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);
        //check if disk block size is valid
        if (totalBlocks == diskSize && totalInodes > 0 && freeList >= 2){
            //disk contents are valid
            return;
        }
        else{
            //format to disk & and call format 
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }
/**
 * Sync method updates the superblock at block zero in the disk with the actions performed on the super class instance (this). 
 * The method works by writing back the in memory superlock to the disk using syslib.rarwrite. This includes the total number of blocks, inodes, and the free list. 
 */
    void sync(){
        //initialize the new block to byte block array of disk block size
        byte[] tempBlock = new byte[Disk.blockSize];
        Syslib.int2bytes(totalBlocks, tempBlock, 0);
        Syslib.int2bytes(totalInodes, tempBlock, 4);
        Syslib.int2bytes(freeList, tempBlock, 8);
        //write back to disk zero using rawwrite 
        SysLib.rawwrite(0, tempBlock);
    }

    /**
     * Method should return the first free block from the freelist. Free block should be the first integer 
     * returned from the freelist queue. If freelist returns -1, we know there is no free blocks left. 
     * @return free block number or -1
     */
    public int getFreeBlock(){
        //get new free block from freelist
        int freeBlock = freeList;
        //if freeblock not valid, return back
        if(freeBlock < 0){
            return freeBlock; 
        }
        else{
            //free block exists
            //get block from queue and write back. 
            byte[] tempBlock = new byte[Disk.blockSize];
            Syslib.rawread(freeBlock, tempBlock);
            this.freeList = SysLib.bytes2int(tempBlock,0);
            Syslib.int2bytes(0, tempBlock, 0);
            Syslib.rawwrite(freeBlock, tempBlock);
            return freeBlock;
        }
        return -1;
    }

    /**
     * ReturnBlock method tries to add a enqueue a given block by the provided blockNumber to the end of the free list queue. The free list queue operates in FIFO fashion.
     * If the block fails to enqueue, the operation fails and returns false. 
     * @param blockNumber is the block to enqueue
     * @return true or false 
     */
    public boolean returnBlock(int blockNumber){
        //if blockNumber invalid, return false
        if(blockNumber < 0){
            return false;
        }
        //if blockNumber falls within range, enqueue the block
        else if(blockNumber >= 0 && blockNumber < totalBlocks){
            //create temp block of disk block size (should be 512bytes)
            byte[] tempBlock = new byte[Disk.blockSize];
            //overwrite data
            for(int i = 0; i < Disk.blockSize; i++){
                tempBlock[i] = 0;
            }
            //update temp block and freelist
            SysLib.int2bytes(freeList, tempBlock, 0);
            SysLib.rawwrite(blockNumber, tempBlock);
            freeList = blockNumber;
            return true;
        }
        return false; 
    }
/**
 * Format method reformats disk and initalizes the superblock, each inode, and freeblocks. Values are written back to disk. 
 * @param files
 */
    void format( int files){
        //for superblock
        totalInodes = files;
        //set default if invalid number passed in
        if(files < 0){
            totalInodes = defaultInodeBlocks;
        }
        //initialize and set the tempNodes (includes flag)
        for(int i = 0; i < totalInodes; i++){
            Inode tempNode = new Inode();
            tempNode.flag = 0;
            tempNode.toDisk((short)i);
        }
        //initialize and set freelist 
        freeList = 2 + totalInodes * 32 / Disk.blockSize;
        //iterate through freelist, initialize and set new blocks
        for(int i = freeList; i < totalBlocks; i++){
            //create temp block of disk block size (should be 512bytes)
            byte[] tempBlock = new byte[Disk.blockSize];
            //overwrite data to default
            for(int n = 0; n < Disk.blockSize; n++){
                tempBlock[n] = 0;
            }
            //write back
            SysLib.int2bytes(i+1,tempBlock,0);
            SysLib.rawwrite(i,tempBlock);
        }
        //after formating, make sure we sync with this instance 
        sync();
    }




}