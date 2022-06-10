/*
 * 
 * Author: Omar Ahmed
 * File: FileTable.java
 * Date: 6/2/22
 * 
 */

import java.util.Vector;
public class FileTable
{
    private Vector table;                           // the actual entity of this file table
    private Directory directory;                    // the root directory

    /**
     * FileTable( Directory directory )
     * Overloaded constructor for FileTable() takes in and instantiates a vector for FileTableEntry's and a directory
     * @param directory
     */
    public FileTable( Directory directory )
    {                                               // constructor
        table = new Vector( );                      // instantiate a file (structure) table
        this.directory = directory;                 // receive a reference to the Director
    }                                               // from the file system

    /**
     * fAlloc( String filename, String mode )
     * This method allocates a new file (structure) table entry for this file name
     * Allocate/retrieve and register the corresponding inode using directory
     * Increment this inode's count
     * Immediately writes back this inode to disk
     * @param filename
     * @param mode
     * @return FileTableEntry if success returns a FileTableEntry object, else null if error
     */
    public synchronized FileTableEntry fAlloc( String filename, String mode )
    {
        short iNumber;
        Inode iNode;

        while (true)
        {
            iNumber = (filename.equals("/") ? (short) 0 : directory.namei(filename));

            if (iNumber >= 0)
            {
                iNode = new Inode(iNumber);
                if (mode.equals("r"))
                {
                    if (iNode.usedFlag == 0 || iNode.usedFlag == 1 || iNode.usedFlag == 2)
                    {
                        iNode.usedFlag = 2;
                        break;
                    }
                    else if (iNode.usedFlag == 3)
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e)
                        {
                            SysLib.cerr("Read Error");
                        }
                    }
                }
                else
                {
                    if (iNode.usedFlag == 1 || iNode.usedFlag == 0)
                    {
                        iNode.usedFlag = 3;
                        break;
                    }
                    else
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e)
                        {
                            SysLib.cerr("Write Error");
                        }
                    }
                }
            }
            else if (!mode.equals("r"))
            {
                iNumber = directory.iAlloc(filename);
                iNode = new Inode(iNumber);
                iNode.usedFlag = 3;
                break;
            }
            else
            {
                return null;
            }
        }
        iNode.count++;
        iNode.toDisk(iNumber);
        FileTableEntry entry = new FileTableEntry(iNode, iNumber, mode);
        table.addElement(entry);
        return entry;
    }

    /**
     * fFree( FileTableEntry entry )
     * This method receives a FileTableEntry object reference
     * Saves the corresponding inode to disk
     * Frees this file from table entry
     * @param entry
     * @return return true if FileTableEntry found on table, else return false error
     */
    public synchronized boolean fFree( FileTableEntry entry )
    {
        Inode inode = new Inode(entry.iNumber);
        if (table.remove(entry))
        {
            if (inode.usedFlag == 2)
            {
                if (inode.count == 1)
                {
                    notify();
                    inode.usedFlag = 1;
                }
            }
            else if (inode.usedFlag == 3)
            {
                inode.usedFlag = 1;
                notifyAll();
            }
            inode.count--;
            inode.toDisk(entry.iNumber);
            return true;
        }
        return false;
    }
}