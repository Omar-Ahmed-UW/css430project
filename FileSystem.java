/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  Directory
 *  FileTable
 *  FileTableEntry
 *  SuperBlock
 *  SysLib
 */
public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem(int n) {
        this.superblock = new SuperBlock(n);
        this.directory = new Directory(this.superblock.inodeBlocks);
        this.filetable = new FileTable(this.directory);
        FileTableEntry fileTableEntry = this.open("/", "r");
        int n2 = this.fsize(fileTableEntry);
        if (n2 > 0) {
            byte[] arrby = new byte[n2];
            this.read(fileTableEntry, arrby);
            this.directory.bytes2directory(arrby);
        }
        this.close(fileTableEntry);
    }

    void sync() {
        FileTableEntry fileTableEntry = this.open("/", "w");
        byte[] arrby = this.directory.directory2bytes();
        this.write(fileTableEntry, arrby);
        this.close(fileTableEntry);
        this.superblock.sync();
    }

    boolean format(int n) {
        while (!this.filetable.fempty()) {
        }
        this.superblock.format(n);
        this.directory = new Directory(this.superblock.inodeBlocks);
        this.filetable = new FileTable(this.directory);
        return true;
    }

    FileTableEntry open(String string, String string2) {
        FileTableEntry fileTableEntry = this.filetable.falloc(string, string2);
        if (string2 == "w" && !this.deallocAllBlocks(fileTableEntry)) {
            return null;
        }
        return fileTableEntry;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean close(FileTableEntry fileTableEntry) {
        FileTableEntry fileTableEntry2 = fileTableEntry;
        synchronized (fileTableEntry2) {
            --fileTableEntry.count;
            if (fileTableEntry.count > 0) {
                return true;
            }
        }
        return this.filetable.ffree(fileTableEntry);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    int fsize(FileTableEntry fileTableEntry) {
        FileTableEntry fileTableEntry2 = fileTableEntry;
        synchronized (fileTableEntry2) {
            return fileTableEntry.inode.length;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    int read(FileTableEntry fileTableEntry, byte[] arrby) {
        if (fileTableEntry.mode == "w" || fileTableEntry.mode == "a") {
            return -1;
        }
        int n = 0;
        FileTableEntry fileTableEntry2 = fileTableEntry;
        synchronized (fileTableEntry2) {
            int n2;
            int n3;
            for (int i = arrby.length; i > 0 && fileTableEntry.seekPtr < this.fsize(fileTableEntry) && (n2 = fileTableEntry.inode.findTargetBlock(fileTableEntry.seekPtr)) != -1; i -= n3) {
                byte[] arrby2 = new byte[512];
                SysLib.rawread((int)n2, (byte[])arrby2);
                int n4 = fileTableEntry.seekPtr % 512;
                int n5 = 512 - n4;
                int n6 = this.fsize(fileTableEntry) - fileTableEntry.seekPtr;
                n3 = Math.min(Math.min(n5, i), n6);
                System.arraycopy(arrby2, n4, arrby, n, n3);
                fileTableEntry.seekPtr += n3;
                n += n3;
            }
            return n;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    int write(FileTableEntry fileTableEntry, byte[] arrby) {
        if (fileTableEntry.mode == "r") {
            return -1;
        }
        FileTableEntry fileTableEntry2 = fileTableEntry;
        synchronized (fileTableEntry2) {
            int n;
            int n2 = 0;
            for (int i = arrby.length; i > 0; i -= n) {
                byte[] arrby2;
                short s;
                int n3 = fileTableEntry.inode.findTargetBlock(fileTableEntry.seekPtr);
                if (n3 == -1) {
                    short s2 = (short)this.superblock.getFreeBlock();
                    switch (fileTableEntry.inode.registerTargetBlock(fileTableEntry.seekPtr, s2)) {
                        case 0: {
                            break;
                        }
                        case -2: 
                        case -1: {
                            SysLib.cerr((String)"ThreadOS: filesystem panic on write\n");
                            return -1;
                        }
                        case -3: {
                            s = (short)this.superblock.getFreeBlock();
                            if (!fileTableEntry.inode.registerIndexBlock(s)) {
                                SysLib.cerr((String)"ThreadOS: panic on write\n");
                                return -1;
                            }
                            if (fileTableEntry.inode.registerTargetBlock(fileTableEntry.seekPtr, s2) == 0) break;
                            SysLib.cerr((String)"ThreadOS: panic on write\n");
                            return -1;
                        }
                    }
                    n3 = s2;
                }
                if (SysLib.rawread((int)n3, (byte[])(arrby2 = new byte[512])) == -1) {
                    System.exit(2);
                }
                s = fileTableEntry.seekPtr % 512;
                int n4 = 512 - s;
                n = Math.min(n4, i);
                System.arraycopy(arrby, n2, arrby2, s, n);
                SysLib.rawwrite((int)n3, (byte[])arrby2);
                fileTableEntry.seekPtr += n;
                n2 += n;
                if (fileTableEntry.seekPtr <= fileTableEntry.inode.length) continue;
                fileTableEntry.inode.length = fileTableEntry.seekPtr;
            }
            fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
            return n2;
        }
    }

    private boolean deallocAllBlocks(FileTableEntry fileTableEntry) {
        int n;
        if (fileTableEntry.inode.count != 1) {
            return false;
        }
        byte[] arrby = fileTableEntry.inode.unregisterIndexBlock();
        if (arrby != null) {
            short s;
            n = 0;
            while ((s = SysLib.bytes2short((byte[])arrby, (int)n)) != -1) {
                this.superblock.returnBlock((int)s);
            }
        }
        n = 0;
        while (true) {
            if (n >= 11) break;
            if (fileTableEntry.inode.direct[n] != -1) {
                this.superblock.returnBlock((int)fileTableEntry.inode.direct[n]);
                fileTableEntry.inode.direct[n] = -1;
            }
            ++n;
        }
        fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
        return true;
    }

    boolean delete(String string) {
        FileTableEntry fileTableEntry = this.open(string, "w");
        short s = fileTableEntry.iNumber;
        return this.close(fileTableEntry) && this.directory.ifree(s);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    int seek(FileTableEntry fileTableEntry, int n, int n2) {
        FileTableEntry fileTableEntry2 = fileTableEntry;
        synchronized (fileTableEntry2) {
            switch (n2) {
                case 0: {
                    if (n >= 0 && n <= this.fsize(fileTableEntry)) {
                        fileTableEntry.seekPtr = n;
                        break;
                    }
                    return -1;
                }
                case 1: {
                    if (fileTableEntry.seekPtr + n >= 0 && fileTableEntry.seekPtr + n <= this.fsize(fileTableEntry)) {
                        fileTableEntry.seekPtr += n;
                        break;
                    }
                    return -1;
                }
                case 2: {
                    if (this.fsize(fileTableEntry) + n >= 0 && this.fsize(fileTableEntry) + n <= this.fsize(fileTableEntry)) {
                        fileTableEntry.seekPtr = this.fsize(fileTableEntry) + n;
                        break;
                    }
                    return -1;
                }
            }
            return fileTableEntry.seekPtr;
        }
    }
}