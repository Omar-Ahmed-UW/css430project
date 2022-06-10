/*
 * 
 * Author: Omar Ahmed
 * File: FileTable.java
 * Date: 6/2/22
 * 
 */
import java.util.Vector;

public class FileTable {
    private Vector table = new Vector();
    private Directory dir;

    public FileTable(Directory directory) {
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String filename, String mode) {
        // allocate a new file (structure) table entry for this file name 
        // allocate/retrieve and register the corresponding inode using dir 
        // increment this inode's count 
        // immediately write back this inode to the disk 
        // return a reference to this file (structure) table entry
        short s;
        Inode inode = null;
        while ((s = filename.equals("/") ? 0 : dir.namei(filename)) >= 0) {
            inode = new Inode(s);
            if (mode.compareTo("r") == 0) {
                if (inode.flag == 0 || inode.flag == 1) {
                    inode.flag = 1;
                    break;
                }
                try {
                    wait();
                }
                catch (InterruptedException interruptedException) {}
                continue;
            }
            if (inode.flag == 0 || inode.flag == 3) {
                inode.flag = 2;
                break;
            }
            if (inode.flag == 1 || inode.flag == 2) {
                inode.flag = (short)(inode.flag + 3);
                inode.toDisk(s);
            }
            try {
                wait();
            }
            catch (InterruptedException interruptedException) {}
        }
        if (mode.compareTo("r") != 0) {
            s = dir.ialloc(filename);
            inode = new Inode();
            inode.flag = 2;
        } else {
            return null;
        }
        inode.count = (short)(inode.count + 1);
        inode.toDisk(s);
        FileTableEntry fileTableEntry = new FileTableEntry(inode, s, mode);
        table.addElement(fileTableEntry);
        return fileTableEntry;
    }

    public synchronized boolean ffree(FileTableEntry e) {
        // receive a file table entry reference 
        // save the corresponding inode to the disk 
        // free this file table entry. 
        // return true if this file table entry found in my table
        if (table.removeElement(e)) {
            e.inode.count = (short)(e.inode.count - 1);
            // switch (e.inode.flag) {
            //     case 1: {
            //         e.inode.flag = 0;
            //         break;
            //     }
            //     case 2: {
            //         e.inode.flag = 0;
            //         break;
            //     }
            //     case 4: {
            //         e.inode.flag = 3;
            //         break;
            //     }
            //     case 5: {
            //         e.inode.flag = 3;
            //     }
            // }
            if(e.inode.count == 1 || e.inode.count == 2){
                e.inode.flag = 0;
            } else if(e.inode.count == 4 || e.inode.count == 5) {
                e.inode.flag = 3;
            }
            e.inode.toDisk(e.iNumber);
            e = null;
            notify();
            return true;
        }
        return false;
    }

    public synchronized boolean fempty() {
        return table.isEmpty();// return if table is empty  
    }                            // should be called before starting a format
}