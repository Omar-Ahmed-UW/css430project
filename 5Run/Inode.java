/**
 * @Authors Omar Ahmed
 * @File Inode.java
 * @Date 6/8/22
 */

public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int maxBytes = 512;
    private final static int blockSize = 16;
    private final static int intBlock = 4;
    private final static int shortBlock = 2;

    public int fileSize;                           // file size in bytes
    public short count;                            // # file-table entries pointing to this, 0 for deletion
    public short usedFlag;                         // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    /**
     * Inode()
     * default constructor for inode class, sets class level variables to default values and sets all block pointers
     * to (-1) indicating that there are not pointing to data.
     */
    Inode( )
    {                                              // a default constructor
        fileSize = 0;
        count = 0;
        usedFlag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    /**
     * Inode(short iNumber)
     * Overloaded Inode constructor iNumber is the iNumber is the number associated with this inode and sets all
     * variables by reading from disk the values stored there.
     * @param iNumber this inode number
     */
    Inode( short iNumber )                          // retrieving iNode from disk
    {
        int nodeBlock = (iNumber / blockSize) + 1;  // Block number where inode is located on disk
        byte[] dataBuffer = new byte[maxBytes];     // buffer to hold data from nodeBlock
        SysLib.rawread(nodeBlock, dataBuffer);      // take block in from disk and put into dataBuffer

        int spaceTracker = (iNumber % blockSize) * iNodeSize;   // used to iterate through the block based on variable thickness
        fileSize = SysLib.bytes2int(dataBuffer, spaceTracker);  // read from dataBuffer into iNode, then iterate thickness variable forward
        spaceTracker += intBlock;
        count = SysLib.bytes2short(dataBuffer, spaceTracker);
        spaceTracker += shortBlock;
        usedFlag = SysLib.bytes2short(dataBuffer, spaceTracker);
        spaceTracker += shortBlock;
        for (int i = 0; i < directSize; i++)        // for loop to iterate through direct pointers
        {
            direct[i] = SysLib.bytes2short(dataBuffer, spaceTracker);
            spaceTracker += shortBlock;
        }
        indirect = SysLib.bytes2short(dataBuffer, spaceTracker);
    }

    /**
     * toDisk(short iNumber)
     * Method saves inode to disk. Takes in passed iNumber and reads from memory and stores to disk then returns the
     * block that inode is saved in.
     * @param iNumber
     * @return int block is the block number this inode is saved at
     */
    int toDisk( short iNumber )                     // save to disk as the i-th iNode
    {
        int nodeBlock = (iNumber / blockSize) + 1;  // skips SuperBlock to determine block to write to
        byte[] nodeBuffer = new byte[iNodeSize];
        int spaceTracker = 0;

        SysLib.int2bytes(fileSize, nodeBuffer, spaceTracker);  // writing to temp buffer
        spaceTracker += intBlock;
        SysLib.short2bytes(count, nodeBuffer, spaceTracker);
        spaceTracker += shortBlock;
        SysLib.short2bytes(usedFlag, nodeBuffer, spaceTracker);
        spaceTracker += shortBlock;

        for(int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(direct[i], nodeBuffer, spaceTracker);
            spaceTracker += shortBlock;
        }
        SysLib.short2bytes(indirect, nodeBuffer, spaceTracker);

        spaceTracker = (iNumber % blockSize) * iNodeSize;   // reset for inserting nodeBuffer into dataBuffer
        byte[] blockBuffer = new byte[maxBytes];            // holds the entire block from disk for iNode buffer insertion
        SysLib.rawread(nodeBlock, blockBuffer);
        // due to the inability for rawwrite to copy a subsection of a block, the iNode must be written to the buffer
        // System.lang arraycopy is used to copy the buffered iNode into the proper location in the buffered block
        System.arraycopy(nodeBuffer, 0, blockBuffer, spaceTracker, iNodeSize);
        SysLib.rawwrite(nodeBlock, blockBuffer);         // overwrites the location of original buffered block
        return nodeBlock;
    }
}