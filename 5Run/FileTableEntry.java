/**
 * @Authors Omar Ahmed
 * @File FileTableEntry.java
 * @Date 6/8/22
 */

public class FileTableEntry
{
    public int seekPtr;
    public final Inode iNode;
    public final short iNumber;
    public int count;
    public final String mode;

    /**
     * FileTableEntry ( Inode i, short iNumber, String m )
     * Overloaded FileTableEntry- sets class variables and objects to values passed in.
     * @param iNode
     * @param iNumber
     * @param mode
     */
    public FileTableEntry ( Inode iNode, short iNumber, String mode ) {
        this.seekPtr = 0;
        this.iNode = iNode;
        this.iNumber = iNumber;
        this.count = 1;
        this.mode = mode;
        if ( mode.compareTo( "a" ) == 0 )
            seekPtr = iNode.fileSize;
    }
}