/**
 * @Authors Phillip Burlachenko, Omar Ahmed
 * @File FileSystem.java
 * @Date 6/3/22
 */
public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    /**
     * The constructor for FileSystem class. 
     * @param diskBlock the disk blocks passed in to operate on the file system. 
     */
    public FileSystem(int diskBlock){
        //create a super block with disk blocks 
        superBlock = new SuperBlock(diskBlock);
        //create directory and register "/" inside directory entry @ 0
        directory = new Directory(superBlock.totalInodes);
        //create file table, then store directory inside file table. 
        fileTable = new FileTable(directory);

        FileTableEntry directoryEntry = open("/", "r");

        int directorySize = fsize(directoryEntry);

        if(0 < directorySize){
            byte[] directoryData = new byte[directorySize];
            read(directoryEntry, directoryData);
            directory.bytestodirectory(directoryData);
        }

        close(directoryEntry);

    }

    /**
     * Sync method is used to sync the data from directory to the disk 
     */
    public void sync(){
        //open the file table entry of the current directory we are working from
        FileTableEntry directoryEntry = open("/", "r");
        //convert the directory data into bytes
        byte[] directoryData = directory.directory2bytes();
        //write back all data from diretory to disk
        write(directoryEntry, directoryData);
        //close the file table entry 
        close(directoryEntry);
        //sync super block
        superBlock.sync();
    }

    /**
     * Format method is used to format the files 
     * @param fileToForm file numbers to format
     * @return true if succesful format, otherwise false if it was empty 
     */
    public boolean format(int fileToForm){
        if(fileTable.fempty() == false){
            return false; 
        }

        superBlock.format(fileToForm);
        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileTable(directory);
        return true;
    }

    /**
     * Open method allocates a new file table entry for the provided file name
     * and mode.
     * @param fileName the name of the file to be opened
     * @param mode the mode to open the file into
     * @return returns a file table entry resulting from operations on the provided file and mode
     */
    public FileTableEntry open(String fileName, String mode){
        //allocate new file table entry
        FileTableEntry fileTableEntry = fileTable.falloc(fileName, mode);
        //check if mode is "w" and dealloc call returns false, then return null
        if(fileTableEntry.mode.compareTo("w") == 0 && !deallocAllBlocks(fileTableEntry)){
            return null;
        }
        return fileTableEntry; //otherwise open operation worked on file table entry, so return
    }

    /**
     * close method closes the file table entry in the file table. 
     * Call ffree method implemeneted in filetable class
     * @param fileTableEntry fte to close
     * @return true if successful, false otherwise
     */
    public boolean close(FileTableEntry fileTableEntry){
        return fileTable.ffree(fileTableEntry);
    }

    /**
     * fsize returns the file size by calling file table entries inode length (should have access)
     * @param fileTableEntry fte
     * @return the integer size of the file size
     */
    public int fsize(FileTableEntry fileTableEntry){
        return fileTableEntry.inode.length;
    }

    /**
     * Read method reads from a file provided by the file descripter up to the buffer length. 
     * The operations starts at the position seekPtr pointer points @. If the bytes remaidning
     * between the seekPtr and the end of the file is less than the bytes size in buffer length,
     * syslib.read will read the amounted bytes and puts them into the beginning of the buffer. 
     * Incrementing teh seekPtr by the amount of bytes that have been read, and then returning the value
     * of the number of bytes, or negative number upon invalid operations. 
     * 
     * @param fileTableEntry file to read from provided by fd
     * @param buffer buffer to read into
     * @return correct ouput or error by invalid data 
     */
    public int read(FileTableEntry fileTableEntry, byte[] buffer){
        //edge cases
        //if statement to check if buffer or table entry is null or if either modes are invalid 
        if(fileTableEntry == null || buffer == null || 
                fileTableEntry.mode == "w" || fileTableEntry.mode == "a"){
                    return -1;
        }

        int sizeTotal = 0;
        //int totalSizeLeft = 0;
        int buffLength = buffer.length;

        synchronized(fileTableEntry){
            //iterate until length of file on entries matches the buffer size. make sure the buffer isnt maxed
            while(fileTableEntry.seekPtr < fsize(fileTableEntry) && 0 < sizeTotal){
                //capture the block number where the file entry seekPptr points to. (0)
                int blockNumber = fileTableEntry.inode.findTargetBlock(fileTableEntry.seekPtr);
                //check if we are at valid block
                if(blockNumber != -1){
                    //needed for rawread
                    byte[] tempData = new byte[Disk.blockSize];
                    //read up to size/length of buffer aka the location
                    SysLib.rawread(blockNumber, tempData);
                    int dataReadIn = fileTableEntry.seekPtr % Disk.blockSize;

                    //variables to hold information for calculations 
                    //need to keep track of the remainder of blocks and bytes 
                    //required for disk opeations and data in buffer
                    int remainderBlocks = Disk.blockSize - dataReadIn;
                    int remainderBytes = fsize(fileTableEntry) - fileTableEntry.seekPtr;

                    //get the remainder of data left to operate on. 
                    //Use min method from math library to calculate the min between the comparisons. 
                    //easier to do this instead of nested if/else statements 
                    int dataLeftRead = Math.min(Math.min(remainderBlocks,buffLength), remainderBytes);
                    //copyer data read stream
                    System.arraycopy(tempData, dataReadIn, buffer, sizeTotal, dataLeftRead);

                    //update variable we used to read into the byte buffer 
                    sizeTotal += dataLeftRead;
                    //update the location of the seekptr pointer 
                    fileTableEntry.seekPtr += dataLeftRead;
                    //update the total size 
                    sizeTotal -= dataLeftRead;
              
                }
                //if invalid data (location), break out of loop 
                else{break;}

            }
            return sizeTotal;
        }
    }

    /**
     * writes the contents of the buffer to the file
     * indicated by fd -> starting at the position indicated by the seek pointer.
     *
     * @param ftEnt the FileTableEntry that we want to write the data into
     * @param buf the buffer that has the data that needs to be rewritten
     * @return the location of where it was written on disk
     */
    public int write(FileTableEntry fileTableEntry, byte[] buffer) {
        // check if valid to write to file.
        if (buffer.length == 0 || fileTableEntry == null) {
            SysLib.cout("buffer length 0");
            return -1;
        }

        // cannot write if mode is read.
        if (fileTableEntry.mode.equals("r")) {
            return -1;
        }

        // synchornized for locking from different threads all at once.
        synchronized (fileTableEntry) {
            int offset = 0;
            int size = buffer.length;
            // iterate through entry and register index and target blocks.
            while (size > 0) {
                int blockNum = fileTableEntry.inode.findTargetBlock(fileTableEntry.seekPtr);
                if (blockNum == -1) {
                    short freeBlock = (short)this.superblock.getFreeBlock();
                    switch(fileTableEntry.inode.registerTargetBlock(fileTableEntry.seekPtr, freeBlock)) {
                        case -3:
                            short nextFreeBlock = (short)this.superblock.getFreeBlock();
                            if (!fileTableEntry.inode.registerIndexBlock(nextFreeBlock)) {
                                SysLib.cerr("ThreadOS: panic on write\n");
                                return -1;
                            }

                            if (fileTableEntry.inode.registerTargetBlock(fileTableEntry.seekPtr, freeBlock) != 0) {
                                SysLib.cerr("ThreadOS: panic on write\n");
                                return -1;
                            }
                        case 0:
                        default:
                            blockNum = freeBlock;
                            break;
                        case -1:
                        case -2:
                            SysLib.cerr("ThreadOS: filesystem panic on write\n");
                            return -1;
                    }
                }
                byte[] tempRead = new byte[Disk.blockSize];
                //this is the location to read from what it is pointing to
                if (SysLib.rawread(blockNum, tempRead) == -1){
                    System.exit(2);
                }
                int position = fileTableEntry.seekPtr % Disk.blockSize;
                int remaining = Disk.blockSize - position;
                int availablePlace = Math.min(remaining, size);
                System.arraycopy(buffer, offset, tempRead, position, availablePlace);
                SysLib.rawwrite(blockNum, tempRead);
                fileTableEntry.seekPtr += availablePlace;
                offset += availablePlace;
                size -= availablePlace;

                if (fileTableEntry.seekPtr > fileTableEntry.inode.length){
                    fileTableEntry.inode.length = fileTableEntry.seekPtr;
                }
                    

            }

            //update the inode
            fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
            return offset;
        }
    }

    /**
     * Deallocates all blocks associated with the fd file table entry.
     *
     * @param fileTableEntry fd to deallocate block from
     * @return true if success, false if not.
     */
    private boolean deallocAllBlocks(FileTableEntry fileTableEntry) {
        if (fileTableEntry.inode.count != 1) {
            return false;
        }
        byte[] temp = fileTableEntry.inode.unregisterIndexBlock();
        
        if (temp != null) {
            short index;
            // call returnBlock function of superblock.
            while ((index = SysLib.bytes2short(temp, 0)) != -1) {
                superblock.returnBlock(index);
            }
        }

        for (int i = 0; i < fileTableEntry.inode.directSize; i++) {
            if (fileTableEntry.inode.direct[i] != -1) {
                superblock.returnBlock(fileTableEntry.inode.direct[i]);
                fileTableEntry.inode.direct[i] = -1;
            }
        }

        // write it to disk.
        fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
        return true;
    }

     /**
     * Deletes the file and frees the block.
     *
     * @param filename file to delete
     * @return true if success, false if not.
     */
    public boolean delete(String filename) {
        FileTableEntry fileTableEntry = open(filename, "w");
        // delete file and return true.
        short FTINumber = fileTableEntry.FTINumber;
        if (close(fileTableEntry) && directory.ifree(FTINumber)){
            return true;
        }
        return false;
    }

    /**
     * Allows for seeking accross a files bytes.
     *
     * @param fileTableEntry  entry to change the seekPtr in
     * @param offset Amount of bytes specified to deiviate from original position
     * @param whence refrence of where to start the seeking.
     * @return -1 on fail or the new value of seekPtr
     */
    public int seek(FileTableEntry fileTableEntry, int offset, int whence) {
        synchronized (fileTableEntry) {
            if (fileTableEntry == null){
                return -1;
            } 

            // set seek ptr in file
            if (whence == SEEK_SET) {
                if (offset <= fsize(fileTableEntry) && offset >= 0){
                    fileTableEntry.seekPtr = offset;
                }
            } else if (whence == SEEK_CUR) {
                if (fileTableEntry.seekPtr + offset <= fsize(fileTableEntry) && ((fileTableEntry.seekPtr + offset) >= 0)){
                    fileTableEntry.seekPtr += offset;
                }
            } else if (whence == SEEK_END) {
                if (fsize(fileTableEntry) + offset >= 0 && fsize(fileTableEntry) + offset <= fsize(fileTableEntry)){
                    fileTableEntry.seekPtr = fileTableEntry.inode.length + offset;
                } else {
                    return -1;
                }
                    
            }
            return fileTableEntry.seekPtr;
        }
    }
}